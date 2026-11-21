package org.nobodymissing.features

import android.graphics.Bitmap
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.nobodymissing.domain.*
import org.nobodymissing.ports.*

data class Ctx(val online: Boolean, val privateMode: Boolean)
data class RouteResult(val status: String, val data: JsonElement? = null, val source: String? = null, val errors: List<String> = emptyList())

/** The ONLY way features call AI (D11). Same order as web: decide -> privacy guard -> AI Logic -> timeout -> validate. */
class InferenceRouter(
    private val inference: InferencePort, private val rules: Rules, private val schemas: Map<String, JsonObject>,
    private val prompts: Map<String, String>, private val models: Map<String, String>,
    private val deviceTimeoutMs: Long, private val cloudTimeoutMs: Long, private val ctx: () -> Ctx,
) {
    var deviceCount = 0; private set
    var cloudCount = 0; private set

    private suspend fun call(task: String, p: TaskPolicy, mode: String, text: String?, image: Bitmap?): InferenceResult {
        val req = InferenceRequest(task, prompts[task] ?: "", text, image, schemas.getValue(p.schema), mode, models[p.model] ?: p.model)
        return withTimeout(if (mode.endsWith("device")) deviceTimeoutMs else cloudTimeoutMs) { inference.generate(req) }
    }

    suspend fun run(task: String, text: String? = null, image: Bitmap? = null, staffApprovedCloud: Boolean = false): RouteResult {
        val p = rules.routerPolicy[task] ?: return RouteResult("blocked")
        val c = ctx()
        val status = inference.onDeviceStatus()
        val d = decide(task, RouterCtx(isOnDeviceReady(true, status), c.online, c.privateMode, staffApprovedCloud), rules.routerPolicy, rules.accuracyBarPoints)
        when (d.action) { "ask_staff" -> return RouteResult("ask_staff"); "queue" -> return RouteResult("queued"); "block" -> return RouteResult("blocked") }
        val mode = d.mode ?: return RouteResult("blocked")
        val res = try { call(task, p, mode, text, image) } catch (e: Exception) {
            val deviceTried = mode.endsWith("device")
            if (deviceTried && p.sensitive) return RouteResult("ask_staff", errors = listOf(e.toString()))
            if (deviceTried && c.online && !c.privateMode) runCatching { call(task, p, "only_in_cloud", text, image) }.getOrElse { return RouteResult("error", errors = listOf(it.toString())) }
            else return RouteResult(if (e is TimeoutCancellationException) "queued" else "error", errors = listOf(e.toString()))
        }
        // Privacy guard post-check: an on-device-only request must never be answered by the cloud.
        if (mode == "only_on_device" && res.source != "device") return RouteResult("blocked", errors = listOf("cloud answered an on-device-only request"))
        if (res.source == "device") deviceCount++ else cloudCount++
        val v = parseAndValidate(res.raw, schemas.getValue(p.schema))
        if (!v.valid) return RouteResult("invalid", source = res.source, errors = v.errors)
        return RouteResult(if (d.action == "device_draft") "draft" else "ok", v.value, res.source)
    }
}

/** InferenceRouterFactory: builds the router from the Remote Config policy table. */
object InferenceRouterFactory {
    fun create(inference: InferencePort, rules: Rules, schemas: Map<String, JsonObject>, prompts: Map<String, String>,
               models: Map<String, String> = mapOf("cloudAccurate" to "gemini-3.8-flash", "cloudLite" to "gemini-3.5-flash-lite"),
               deviceTimeoutMs: Long = 8000, cloudTimeoutMs: Long = 15000, ctx: () -> Ctx) =
        InferenceRouter(inference, rules, schemas, prompts, models, deviceTimeoutMs, cloudTimeoutMs, ctx)
}
