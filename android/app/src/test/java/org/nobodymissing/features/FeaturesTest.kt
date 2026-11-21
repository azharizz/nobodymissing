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

    @Test fun familySearchLocalMinimalFields() {
        val f = features(FakeInference("unavailable"))
        assertEquals("80-89", f.familySearch("Hasan").single().ageBand)
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
