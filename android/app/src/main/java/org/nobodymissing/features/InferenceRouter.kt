package org.nobodymissing.features

import android.graphics.Bitmap
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.nobodymissing.domain.*
import org.nobodymissing.ports.*

data class Ctx(val online: Boolean, val privateMode: Boolean)
data class RouteResult(val status: String, val data: JsonElement? = null, val source: String? = null, val errors: List<String> = emptyList(), val places: List<GroundingPlace> = emptyList())
/** Extra prompt values ({name} in the prompt pack) and a rounded location for Maps grounding. */
data class Extra(val vars: Map<String, String> = emptyMap(), val lat: Double? = null, val lng: Double? = null)

/** The ONLY way features call AI (D11). Same order as web: decide -> privacy guard -> AI Logic -> timeout -> validate. */
class InferenceRouter(
    private val inference: InferencePort, private val rules: Rules, private val schemas: Map<String, JsonObject>,
    private val prompts: Map<String, String>, private val models: Map<String, String>,
    private val deviceTimeoutMs: Long, private val cloudTimeoutMs: Long, private val ctx: () -> Ctx,
) {
    var deviceCount = 0; private set
    var cloudCount = 0; private set

    private suspend fun call(task: String, p: TaskPolicy, mode: String, text: String?, image: Bitmap?, x: Extra): InferenceResult {
        val primary = models[p.model] ?: p.model
        val backup = models.getValue("cloudLite")
        return try { callModel(task, p, mode, text, image, primary, x) } catch (e: Exception) {
            // Overloaded/failed primary cloud model -> backup model (6.14).
            if (mode.endsWith("cloud") && primary != backup && e !is TimeoutCancellationException) callModel(task, p, mode, text, image, backup, x) else throw e
        }
    }

    private suspend fun callModel(task: String, p: TaskPolicy, mode: String, text: String?, image: Bitmap?, model: String, x: Extra): InferenceResult {
        val prompt = x.vars.entries.fold(prompts[task] ?: "") { s, (k, v) -> s.replace("{$k}", v) }
        val req = InferenceRequest(task, prompt, text, image, schemas.getValue(p.schema), mode, model, p.grounding, x.lat, x.lng)
        return withTimeout(if (mode.endsWith("device")) deviceTimeoutMs else cloudTimeoutMs) { inference.generate(req) }
    }

    /** Load Gemini Nano into memory at app start so the first real question isn't slow.
     *  Only when the model is already on the phone; on-device only, no user data, result ignored. */
    suspend fun warm(): Boolean {
        if (inference.onDeviceStatus() != "available") return false
        return runCatching { withTimeout(deviceTimeoutMs * 2) { inference.generate(InferenceRequest("warm", "Reply with {}", null, null, kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("object")) }, "only_on_device", models.getValue("cloudLite"))) } }.isSuccess
    }

    suspend fun run(task: String, text: String? = null, image: Bitmap? = null, staffApprovedCloud: Boolean = false, extra: Extra = Extra()): RouteResult {
        val p = rules.routerPolicy[task] ?: return RouteResult("blocked")
        val c = ctx()
        val status = inference.onDeviceStatus()
        val d = decide(task, RouterCtx(isOnDeviceReady(true, status), c.online, c.privateMode, staffApprovedCloud), rules.routerPolicy, rules.accuracyBarPoints)
        when (d.action) { "ask_staff" -> return RouteResult("ask_staff"); "queue" -> return RouteResult("queued"); "block" -> return RouteResult("blocked") }
        val mode = d.mode ?: return RouteResult("blocked")
        val res = try { call(task, p, mode, text, image, extra) } catch (e: Exception) {
            val deviceTried = mode.endsWith("device")
            if (deviceTried && p.sensitive) return RouteResult("ask_staff", errors = listOf(e.toString()))
            if (deviceTried && c.online && !c.privateMode) runCatching { call(task, p, "only_in_cloud", text, image, extra) }.getOrElse { return RouteResult("error", errors = listOf(it.toString())) }
            else return RouteResult(if (e is TimeoutCancellationException) "queued" else "error", errors = listOf(e.toString()))
        }
        // Privacy guard post-check: an on-device-only request must never be answered by the cloud.
        if (mode == "only_on_device" && res.source != "device") return RouteResult("blocked", errors = listOf("cloud answered an on-device-only request"))
        if (res.source == "device") deviceCount++ else cloudCount++
        val v = parseAndValidate(res.raw, schemas.getValue(p.schema))
        if (!v.valid) return RouteResult("invalid", source = res.source, errors = v.errors)
        return RouteResult(if (d.action == "device_draft") "draft" else "ok", v.value, res.source, places = res.places)
    }
}

/** InferenceRouterFactory: builds the router from the Remote Config policy table. */
object InferenceRouterFactory {
    fun create(inference: InferencePort, rules: Rules, schemas: Map<String, JsonObject>, prompts: Map<String, String>,
               models: Map<String, String> = mapOf("cloudAccurate" to "gemini-3.8-flash", "cloudLite" to "gemini-3.5-flash-lite"),
               deviceTimeoutMs: Long = 8000, cloudTimeoutMs: Long = 15000, ctx: () -> Ctx) =
        InferenceRouter(inference, rules, schemas, prompts, models, deviceTimeoutMs, cloudTimeoutMs, ctx)
}
