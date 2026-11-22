package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Field Check priority (PRODUCT.md 6.11c). Same logic as web/src/domain/priority.ts.
data class Hazard(val floodClass: Int? = null, val hand: Double? = null, val landslideHigh: Boolean = false, val tsunamiZone: Boolean = false, val unknown: Boolean = false, val pinNearby: Boolean = false) : java.io.Serializable
data class TierOverride(val tier: String, val reason: String) : java.io.Serializable
data class PriorityInput(
    val age: Int? = null, val mobility: Int = 0, val cantMiss: Boolean = false, val supplyDays: Int? = null, val dailyMeds: Boolean = false,
    val hearing: Boolean = false, val dementia: Boolean = false, val alone: Boolean = false, val hoursSinceContact: Double? = null,
    val hazard: Hazard = Hazard(), val override: TierOverride? = null,
) : java.io.Serializable {
    companion object {
        fun from(o: JsonObject): PriorityInput {
            val h = o["hazard"]!!.jsonObject
            val ov = (o["override"] as? JsonObject)?.let { TierOverride(it.s("tier")!!, it.s("reason")!!) }
            return PriorityInput(o.i("age"), o.i("mobility") ?: 0, o.b("cantMiss") ?: false, o.i("supplyDays"), o.b("dailyMeds") ?: false,
                o.b("hearing") ?: false, o.b("dementia") ?: false, o.b("alone") ?: false, o.d("hoursSinceContact"),
                Hazard(h.i("floodClass"), h.d("hand"), h.b("landslideHigh") ?: false, h.b("tsunamiZone") ?: false, h.b("unknown") ?: false, h.b("pinNearby") ?: false), ov)
        }
    }
}
data class PriorityResult(val score: Int, val tier: String, val why: List<String>)

fun hazardPoints(h: Hazard, ev: String, w: Map<String, Double>): Int {
    fun p(k: String) = w.getValue(k).toInt()
    if (h.unknown) return p("unknownLocation")
    var pts = 0
    if (ev == "flood") {
        if (h.floodClass != null) pts = h.floodClass
        else if (h.hand != null) pts = when {
            h.hand < w.getValue("handDeep") -> 3
            h.hand < w.getValue("handMedium") -> 2
            h.hand <= w.getValue("handShallow") -> 1
            else -> 0
        }
    }
    if (ev == "tsunami" && h.tsunamiZone) pts = p("tsunamiZone")
    if ((ev == "landslide" || ev == "earthquake" || ev == "flood") && h.landslideHigh) pts += p("landslideHigh")
    return pts + if (h.pinNearby) p("pinNearby") else 0
}

fun priority(p: PriorityInput, ev: String, w: Map<String, Double>): PriorityResult {
    fun pt(k: String) = w.getValue(k).toInt()
    val hz = hazardPoints(p.hazard, ev, w)
    val why = mutableListOf<String>()
    var s = hz
    if (hz >= 2) why += "hazard at location"
    if (p.hazard.unknown) why += "location unknown"
    s += p.mobility; if (p.mobility >= 2) why += "limited mobility"
    val lowSupply = p.cantMiss && (p.supplyDays ?: 0) <= w.getValue("lowSupplyDays")
    if (p.cantMiss) { s += pt("cantMiss") + if (lowSupply) pt("cantMissLowSupply") else 0; why += if (p.supplyDays == null) "can't-miss treatment" else "can't-miss treatment, ${p.supplyDays} day(s) left" }
    else if (p.dailyMeds) s += pt("dailyMeds")
    if (p.hearing) { s += pt("hearing"); why += "hard of hearing" }
    if (p.dementia) { s += pt("dementia"); why += "dementia" }
    if (p.alone) { s += pt("alone"); why += "lives alone" }
    val age = p.age ?: 0
    if (age >= 85) s += pt("age85") else if (age >= 75) s += pt("age75")
    if (p.hoursSinceContact == null) { s += pt("neverChecked"); why += "never checked" }
    else if (p.hoursSinceContact > 24) s += pt("contactOver24h")
    val cantWalk = p.mobility == 3
    val override = (cantWalk && hz >= 2) || lowSupply || (p.hazard.unknown && (cantWalk || p.cantMiss))
    var tier = if (override || s >= pt("p1")) "P1" else if (s >= pt("p2")) "P2" else "P3"
    p.override?.let { tier = it.tier; why += "staff override: ${it.reason}" }
    return PriorityResult(s, tier, why)
}

data class FieldRow(val id: String, val distanceM: Double?, val person: PriorityInput)
data class RankedRow(val id: String, val distanceM: Double?, val result: PriorityResult)

fun sortFieldList(rows: List<FieldRow>, ev: String, w: Map<String, Double>): List<RankedRow> =
    rows.map { RankedRow(it.id, it.distanceM, priority(it.person, ev, w)) }
        .sortedWith(compareBy<RankedRow> { it.result.tier }.thenByDescending { it.result.score }.thenBy { it.distanceM ?: Double.MAX_VALUE })
