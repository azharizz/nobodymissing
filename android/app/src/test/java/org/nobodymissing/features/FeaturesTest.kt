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
        var intent = "notice"
        val inf = FakeInference("available") { InferenceResult("""{"intent":"$intent","cardId":"$id","answer":"Lunch is at 12:00."}""", "device") }
        val f = features(inf)
        val a = f.announce("Lunch 12.00 in the hall", "en"); f.approve(a.id); id = a.id
        assertEquals("Lunch is at 12:00.", f.askKit("When is lunch?").answer)
        id = "nope"
        assertEquals("unknown", f.askKit("When is lunch?").intent)
        intent = "greeting"; assertEquals(AskAnswer("greeting", null, null, "device"), f.askKit("hi bro"))
        val n = inf.calls.size
        assertEquals("where_to_go", f.askKit("where should I go?").intent); assertEquals(n, inf.calls.size)
    }

    @Test fun helpTriageStaysOnDevice() = runBlocking {
        val inf = FakeInference("available") { InferenceResult("""{"kind":"sick","urgent":false,"summary":"Father has chest pain"}""", "device") }
        val f = features(inf).also { it.signal.value = "back" }
        val t = f.triage("bapak saya nyeri dada")
        assertEquals("device", t.by); assertTrue(t.urgent)
        assertEquals("only_on_device", inf.calls.single().mode)
        val off = FakeInference("unavailable")
        val g = features(off).also { it.signal.value = "back" }
        assertEquals("keywords", g.triage("need insulin").by)
        assertTrue(off.calls.isEmpty())
    }

    @Test fun familyGroupQrCarriesGroupAndGossipJoinsGroups() = runBlocking {
        val cloud = MemorySync()
        fun mk() = FeatureFactory.create(Adapters("test", FakeInference("unavailable"), cloud, FakeConnectivity(true)), shared)
        val a = mk(); val b = mk(); val c = mk()
        a.setMe("Cut Nurlaila", "safe"); b.setMe("Teuku Hasballah", "need_help"); c.setMe("M. Rizki", "safe")
        assertEquals(listOf("M. Rizki"), b.scanFamily(c.myQr()!!)!!.map { it.name })
        assertEquals(listOf("Teuku Hasballah", "M. Rizki"), a.scanFamily(b.myQr()!!)!!.map { it.name })
        assertEquals(null, a.scanFamily("hello"))
        listOf(a, b, c).forEach { it.signal.value = "back" }
        b.syncFamily(); a.syncFamily(); b.syncFamily(); c.syncFamily()
        assertEquals(listOf("Cut Nurlaila", "Teuku Hasballah"), c.familyGroup.value!!.members.map { it.name }.sorted())
    }

    @Test fun familySearchDeviceParsesCloudSpells() = runBlocking {
        val inf = FakeInference("available") { if (it.task == "family_query") InferenceResult("""{"name":"Hasan Basry","age":80,"place":null}""", "device") else InferenceResult("""{"variants":["Hasan Basri"]}""", "cloud") }
        val f = features(inf).also { it.loadDemo(); it.signal.value = "back" }
        val r = f.familySmart("kakek saya Hasan Basry, sekitar 80")
        assertEquals("device", r.parsedBy); assertEquals("cloud", r.variantsBy)
        assertEquals(listOf("Pak Hasan Basri" to "spelling"), r.hits.map { it.name to it.match })
        assertEquals("only_on_device", inf.calls.first { it.task == "family_query" }.mode)
        assertEquals("Hasan Basry", inf.calls.first { it.task == "name_variants" }.text)
    }

    @Test fun firstHoursWarningWayPickupCheckin() = runBlocking {
        val inf = FakeInference("available") { when (it.task) {
            "warning_plain" -> InferenceResult("""{"hazard":"flood","level":"prepare","plain":"River rising."}""", "device")
            "pickup_request" -> InferenceResult("""{"who":"Ibu saya","mobility":1,"needs":[],"people":1}""", "device")
            "checkin_parse" -> InferenceResult("""{"name":"Hasan Basri","age":82,"flags":["hard_of_hearing"]}""", "device")
            else -> InferenceResult("{}", "device") } }
        val sync = MemorySync()
        val f = FeatureFactory.create(Adapters("test", inf, sync, FakeConnectivity(true)), shared).also { it.loadDemo() }
        assertEquals("go_now", f.warn("Warga bantaran diminta evakuasi segera, banjir.").w.level)
        f.myPos.value = 4.2815 to 98.0565
        assertEquals("Masjid Jami' al Chairitah", f.destination()!!.h.name)
        val p = f.understandPickup("Ibu saya tidak bisa jalan, perlu insulin")
        assertEquals(3, p.mobility); assertEquals(listOf("insulin"), p.needs)
        assertEquals("only_on_device", inf.calls.first { it.task == "pickup_request" }.mode)
        val rec = f.sendPickup(p)
        assertTrue(sync.docs.isEmpty())
        f.signal.value = "back"; f.flush()
        assertTrue(f.pickupSent())
        val staff = FeatureFactory.create(Adapters("test", FakeInference("unavailable"), sync, FakeConnectivity(true)), shared).also { it.loadDemo(); it.signal.value = "back" }
        staff.shelterId.value = f.myPickup.value!!.second.split("/")[1]
        staff.syncPickups()
        assertEquals("P1", staff.fieldList().first { it.second.id == "pk-${rec.id}" }.second.result.tier)
        val c = f.understandCheckin("Hasan Basri, 82, tidak bisa dengar, pakai insulin")
        assertEquals(listOf("hard_of_hearing", "needs_meds"), c.flags)
        val id = f.checkin(c)
        assertTrue(f.reach(null).list.any { it.id == id })
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
    @Test fun stateSurvivesAppKill() = runBlocking {
        val f = features()
        f.loadDemo(); f.lang.value = "id"
        f.checkin(org.nobodymissing.domain.Checkin("Siti Aminah", 77, listOf("needs_meds")))
        f.jobs[0].status = org.nobodymissing.domain.JobStatus("sending", 1)   // killed mid-send
        f.router.deviceCount = 3
        val file = File.createTempFile("state", ".bin").apply { deleteOnExit() }
        assertTrue(f.saveTo(file).isSuccess)
        val g = features()
        assertTrue(g.loadFrom(file))
        assertEquals(f.people.toList(), g.people.toList())
        assertEquals(f.meds.toList(), g.meds.toList())
        assertEquals(f.field.toList(), g.field.toList())
        assertEquals("id", g.lang.value)
        assertEquals("waiting", g.jobs[0].status.state)                       // sent again, not lost
        assertEquals(3, g.router.deviceCount)                                 // live counter: same day, kept
        assertFalse(features().loadFrom(File(file.path + ".missing")))
    }

    @Test fun dailyListsPrintRosterMedicineAndReach() {
        val f = features(); f.loadDemo()
        val html = f.dailyListsHtml(0)
        assertTrue(html.contains("<svg"))                                      // Kit on paper
        f.people.filter { it.confirmed }.forEach { assertTrue(it.name, html.contains(it.name.replace("&", "&amp;"))) }
        f.schedule().slots.keys.forEach { assertTrue(html.contains(it)) }
        assertFalse(html.contains("<script"))
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
