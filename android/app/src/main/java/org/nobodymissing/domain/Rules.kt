package org.nobodymissing.domain

import kotlinx.serialization.json.*

/** Remote Config defaults (shared/rules/defaults.json), bundled for offline use. */
data class Rules(
    val elderlyAge: Int, val reachAge: Int, val reachFlags: List<String>, val shortageDays: Int, val reviewConfidence: Double,
    val accuracyBarPoints: Double, val cantMiss: Map<String, String>, val slotTimes: Map<Int, List<String>>,
    val queuePriority: Map<String, Int>, val routerPolicy: Map<String, TaskPolicy>, val fieldPriority: Map<String, Double>,
    val scoutSuccessesNeeded: Int, val currentEvent: String,
) {
    companion object {
        fun from(o: JsonObject) = Rules(
            elderlyAge = o.i("elderlyAge")!!, reachAge = o.i("reachAge")!!, reachFlags = o.strings("reachFlags"),
            shortageDays = o.i("shortageDays")!!, reviewConfidence = o.d("reviewConfidence")!!, accuracyBarPoints = o.d("accuracyBarPoints")!!,
            cantMiss = o["cantMiss"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content },
            slotTimes = o["slotTimes"]!!.jsonObject.entries.associate { it.key.toInt() to it.value.jsonArray.map { t -> t.jsonPrimitive.content } },
            queuePriority = o["queuePriority"]!!.jsonObject.mapValues { it.value.jsonPrimitive.int },
            routerPolicy = o["routerPolicy"]!!.jsonObject.mapValues { (_, v) -> v.jsonObject.let { p -> TaskPolicy(p.b("sensitive")!!, p.b("cloudOnly")!!, p.s("schema")!!, p.s("model")!!, p.d("deviceAccuracy"), p.d("cloudAccuracy")) } },
            fieldPriority = o["fieldPriority"]!!.jsonObject.mapValues { it.value.jsonPrimitive.double },
            scoutSuccessesNeeded = o["scout"]!!.jsonObject.i("successesNeeded")!!, currentEvent = o.s("currentEvent")!!,
        )
    }
}
