package org.nobodymissing.features

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.nobodymissing.adapters.FakeInference
import org.nobodymissing.domain.Rules
import org.nobodymissing.ports.InferenceResult
import java.io.File

class InferenceRouterTest {
    private fun load(p: String) = Json.parseToJsonElement(File("../../shared/$p").readText()).jsonObject
    private val rules = Rules.from(load("rules/defaults.json"))
    private val schemas = listOf("roster", "careCard", "announcementCard", "fieldReport", "request", "translation").associateWith { load("schemas/$it.json") }
    private val prompts = load("prompts/pack.json")["tasks"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    private val ok = """{"rows":[{"name":"Hasan","age":82,"confidence":0.9}]}"""
    private fun router(f: FakeInference, online: Boolean = true, deviceMs: Long = 8000) =
        InferenceRouterFactory.create(f, rules, schemas, prompts, deviceTimeoutMs = deviceMs, cloudTimeoutMs = 15000) { Ctx(online, false) }

    @Test fun privacyGuardAsksStaffAndSendsNothing() = runBlocking {
        val f = FakeInference("unavailable")
        assertEquals("ask_staff", router(f).run("roster_read", "x").status)
        assertEquals(0, f.calls.size)
    }

    @Test fun sensitiveRunsOnlyOnDevice() = runBlocking {
        val f = FakeInference("available") { InferenceResult(ok, "device") }
        val r = router(f).run("roster_read", "x")
        assertEquals("ok", r.status); assertEquals("only_on_device", f.calls[0].mode)
    }

    @Test fun cloudAnswerToDeviceOnlyIsDiscarded() = runBlocking {
        val f = FakeInference("available") { InferenceResult(ok, "cloud") }
        assertEquals("blocked", router(f).run("roster_read", "x").status)
    }

    @Test fun invalidOutputNeverEntersDomain() = runBlocking {
        val f = FakeInference("available") { InferenceResult("""{"rows":[{"name":"x"}]}""", "device") }
        val r = router(f).run("roster_read", "x")
        assertEquals("invalid", r.status); assertEquals(null, r.data)
    }

    @Test fun slowDeviceOnSensitiveTaskAsksStaff() = runBlocking {
        val f = FakeInference("available") { delay(200); InferenceResult(ok, "device") }
        assertEquals("ask_staff", router(f, deviceMs = 20).run("roster_read", "x").status)
    }

    @Test fun offlineCloudTaskIsQueued() = runBlocking {
        val f = FakeInference("available")
        assertEquals("queued", router(f, online = false).run("supply_request", "x").status)
        assertEquals(0, f.calls.size)
    }

    @Test fun overloadedPrimaryModelFallsBackToBackup() = runBlocking {
        val f = FakeInference("unavailable") { if (it.model == "gemini-3.8-flash") error("500 high demand") else InferenceResult(ok, "cloud") }
        val r = router(f).run("roster_read", "x", staffApprovedCloud = true)
        assertEquals("ok", r.status)
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"), f.calls.map { it.model })
    }
}
