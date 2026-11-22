package org.nobodymissing.domain

// InferenceRouter policy (6.3). Same table + logic as web router.ts.
data class TaskPolicy(val sensitive: Boolean, val cloudOnly: Boolean, val schema: String, val model: String, val deviceAccuracy: Double? = null, val cloudAccuracy: Double? = null, val grounding: String? = null)
data class RouterCtx(val onDeviceAvailable: Boolean, val online: Boolean, val privateMode: Boolean, val staffApprovedCloud: Boolean)
data class Decision(val action: String, val mode: String?)

private fun d(action: String, mode: String? = null) = Decision(action, mode)

fun decide(task: String, ctx: RouterCtx, policy: Map<String, TaskPolicy>, barPoints: Double): Decision {
    val p = policy[task] ?: return d("block")
    val dev = ctx.onDeviceAvailable; val online = ctx.online; val priv = ctx.privateMode
    if (p.cloudOnly) return if (priv) d("block") else if (online) d("cloud", "only_in_cloud") else d("queue")
    if (p.sensitive) {
        if (priv) return if (dev) d("device", "only_on_device") else d("block")
        // Staff approval wins over "device available": it comes after the device failed or was too slow.
        if (ctx.staffApprovedCloud) return if (online) d("cloud", "only_in_cloud") else d("queue")
        return if (dev) d("device", "only_on_device") else d("ask_staff")
    }
    val deviceWeak = p.deviceAccuracy != null && p.cloudAccuracy != null && p.cloudAccuracy - p.deviceAccuracy > barPoints
    if (deviceWeak) {
        if (online && !priv) return d("cloud", "prefer_in_cloud")
        if (dev) return d("device_draft", "only_on_device")
        return if (priv) d("block") else d("queue")
    }
    if (dev) return d("device", if (priv) "only_on_device" else "prefer_on_device")
    if (priv) return d("block")
    return if (online) d("cloud", "only_in_cloud") else d("queue")
}

/** Privacy guard: on-device only when the on-device API exists AND the model is ready. */
fun isOnDeviceReady(apiPresent: Boolean, availability: String) = apiPresent && availability == "available"
