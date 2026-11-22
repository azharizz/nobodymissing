package org.nobodymissing.features

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.nobodymissing.adapters.Adapters
import org.nobodymissing.adapters.FakeConnectivity
import org.nobodymissing.adapters.FakeInference
import org.nobodymissing.adapters.MemorySync
import org.nobodymissing.ports.InferenceResult
import java.io.File

class FeaturesTest {
    private val shared = Shared({ File("../../shared/$it").readText() })
    private fun features(inf: FakeInference, online: Boolean = true) =
        FeatureFactory.create(Adapters("test", inf, MemorySync(), FakeConnectivity(online)), shared)

    @Test fun announcementGetsAiCardsAndKeepsOriginal() = runBlocking {
        val f = features(FakeInference("available") { InferenceResult("""{"cards":[{"icon":"food","title":"Lunch","time":"12:00","place":"Hall"}]}""", "device") })
        val a = f.announce("Makan siang jam 12.00 di aula", "en")
        assertEquals("device", a.source)
        assertEquals("Lunch", a.cards.first().title)
        assertEquals(listOf("food"), a.icons)
        assertEquals("Makan siang jam 12.00 di aula", a.text)
        assertFalse(a.shown)
    }

    @Test fun announcementOfflineFallsBackToKeywords() = runBlocking {
        val f = features(FakeInference("unavailable"), online = false)
        val a = f.announce("Makan siang jam 12.00 di aula", "id")
        assertEquals("keywords", a.source)
        assertEquals("12:00", a.cards.first().time)
    }

    @Test fun approveQueuesForSync() = runBlocking {
        val f = features(FakeInference("unavailable"), online = false)
        val a = f.announce("Air bersih jam 15.00", "id")
        f.approve(a.id)
        assertTrue(f.announcements.first().shown)
        assertEquals("other", f.jobs.single().type)
    }

    @Test fun fieldReportAddsReviewEntryRankedByRules() = runBlocking {
        val f = features(FakeInference("available") { InferenceResult("""{"people":[{"name":"Pak Hasan","age":78,"mobility":3,"medicalNeeds":["insulin"]}]}""", "device") })
        f.fieldReport("Pak Hasan, 78, can't walk, needs insulin", null, null)
        val row = f.fieldList().first { it.first == "Pak Hasan" }
        assertEquals("P1", row.second.result.tier)
        assertEquals("field_report", f.jobs.single().type)
    }

    @Test fun medicineCheckSendsDrugNamesOnlyAndWaitsOffline() = runBlocking {
        val inf = FakeInference("unavailable") { InferenceResult("""{"items":[{"name":"insulin glargine","generic":"insulin glargine","cantMiss":true}],"interactions":[]}""", "cloud") }
        val f = features(inf).also { it.loadDemo() }
        val pid = f.meds.first().personId
        assertEquals("queued", f.checkMeds(pid))
        assertTrue(inf.calls.isEmpty())
        f.signal.value = "back"
        assertEquals("done", f.checkMeds(pid))
        val req = inf.calls.single()
        assertEquals("only_in_cloud", req.mode)
        assertEquals(null, req.image)
        assertFalse(req.text!!.contains(f.people.first { it.id == pid }.name))
        assertEquals("agree", f.medChecks[pid]!!.cross.first().status)
    }

    @Test fun findOpenUsesMapsGroundingAndDropsInventedPlaces() = runBlocking {
        val inf = FakeInference("unavailable") { InferenceResult("""{"places":[{"name":"Apotek Real","openNow":true},{"name":"Made Up Clinic"}]}""", "cloud", listOf(org.nobodymissing.ports.GroundingPlace("Apotek Real", "https://maps.google.com/?cid=9", null))) }
        val f = features(inf).also { it.loadDemo(); it.signal.value = "back" }
        assertEquals("done", f.findOpen("pharmacy"))
        assertEquals("maps", inf.calls.single().grounding)
        inf.calls.single().lat!!.let { assertEquals(Math.round(it * 100) / 100.0, it, 1e-12) } // rounded, about 1 km
        assertEquals(listOf("Apotek Real"), f.open.value!!.places.map { it.name })
    }

    @Test fun askKitNeedsARealCard() = runBlocking {
        var id = ""
        val f = features(FakeInference("available") { InferenceResult("""{"cardId":"$id","answer":"Lunch is at 12:00."}""", "device") })
        val a = f.announce("Lunch 12.00 in the hall", "en"); f.approve(a.id); id = a.id
        assertEquals("Lunch is at 12:00.", f.askKit("When is lunch?")!!.answer)
        id = "nope"
        assertEquals(null, f.askKit("When is lunch?"))
    }

    @Test fun familySearchLocalMinimalFields() {
        val f = features(FakeInference("unavailable")).also { it.loadDemo() }
        assertEquals("80-89", f.familySearch("Hasan").single().ageBand)
    }
}

class ParityTest {
    private val shared = Shared({ File("../../shared/$it").readText() })
    private fun features() = FeatureFactory.create(Adapters("test", FakeInference("unavailable"), MemorySync(), FakeConnectivity(false)), shared)

    @Test fun realShelterStartsEmptyDemoLoadsOnRequest() {
        val f = features()
        assertTrue(f.people.isEmpty()); assertTrue(f.field.isEmpty()); assertTrue(f.meds.isEmpty())
        f.loadDemo()
        assertTrue(f.people.isNotEmpty()); assertEquals("SD3", f.shelterId.value)
    }

    @Test fun sharedStringsInBothLanguages() {
        val f = features()
        assertEquals("Today", f.t("today"))
        f.lang.value = "id"
        assertEquals("Hari ini", f.t("today"))
        assertEquals("Hari ini: 2 di perangkat · 1 cloud · 0 menunggu", f.t("counter", "device" to 2, "cloud" to 1, "waiting" to 0))
    }

    @Test fun qrCourierHandsTheQueueToTheScout() {
        val worker = features(); val scout = features()
        worker.enqueue("family_update", "public", mapOf("name" to "Hasan", "age" to 80))
        assertEquals(1, scout.importChunks(worker.exportChunks(40)))
        assertEquals("family_update", scout.jobs.single().type)
        assertEquals("Hasan", scout.jobs.single().payload["name"])
        assertEquals(0, scout.importChunks(worker.exportChunks(40)))  // idempotent: same job id is skipped
    }
}

class RemoteRulesTest {
    @Test fun remoteConfigRulesOverrideBundledDefaultsKeyByKey() {
        val s = Shared({ File("../../shared/$it").readText() }, """{"elderlyAge": 60}""")
        assertEquals(60, s.rules.elderlyAge)
        assertEquals(75, s.rules.reachAge)
    }
    @Test fun brokenRemoteConfigFallsBackToDefaults() {
        assertEquals(65, Shared({ File("../../shared/$it").readText() }, "not json").rules.elderlyAge)
    }
}
