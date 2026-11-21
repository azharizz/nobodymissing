package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Medicine Round (6.9). Dose is only ever "as written".
data class CardMed(val generic: String?, val strength: String?, val doseAsWritten: String?, val timesPerDay: Int?, val qtyLeft: Int?)
data class Card(val personId: String, val meds: List<CardMed>) {
    companion object {
        fun from(o: JsonObject) = Card(o.s("personId")!!, o["meds"]!!.jsonArray.map { it.jsonObject.let { m -> CardMed(m.s("generic"), m.s("strength"), m.s("doseAsWritten"), m.i("timesPerDay"), m.i("qtyLeft")) } })
    }
}
data class SlotEntry(val personId: String, val generic: String, val doseAsWritten: String?)
data class Alert(val personId: String, val generic: String, val type: String, val supplyDays: Int?)
data class Shortage(val generic: String, val strength: String?, val people: Int)
data class Schedule(val slots: Map<String, List<SlotEntry>>, val counts: Map<String, Int>, val alerts: List<Alert>, val shortages: List<Shortage>, val unscheduled: Int)

fun buildSchedule(cards: List<Card>, r: Rules): Schedule {
    val slots = sortedMapOf<String, MutableList<SlotEntry>>()
    val alerts = mutableListOf<Alert>()
    val short = linkedMapOf<String, Shortage>()
    var unscheduled = 0
    for (c in cards) for (m in c.meds) {
        val times = m.timesPerDay?.let { r.slotTimes[it] }
        if (m.generic == null || times == null) { unscheduled++; continue }
        for (t in times) slots.getOrPut(t) { mutableListOf() } += SlotEntry(c.personId, m.generic, m.doseAsWritten)
        val days = m.qtyLeft?.let { it / m.timesPerDay }
        r.cantMiss[m.generic]?.let { alerts += Alert(c.personId, m.generic, it, days) }
        if (days != null && days <= r.shortageDays) {
            val k = "${m.generic}|${m.strength ?: ""}"
            short[k] = short[k]?.let { it.copy(people = it.people + 1) } ?: Shortage(m.generic, m.strength, 1)
        }
    }
    return Schedule(
        slots, slots.mapValues { it.value.size },
        alerts.sortedWith(compareBy<Alert> { it.supplyDays ?: Int.MAX_VALUE }.thenBy { it.personId }),
        short.values.sortedWith(compareBy<Shortage> { it.generic }.thenBy { it.strength ?: "" }), unscheduled,
    )
}

data class RequestItem(val generic: String, val strength: String, val form: String, val qty: Int) {
    val key get() = "$generic|$strength|$form"
    companion object { fun from(o: JsonObject) = RequestItem(o.s("generic")!!, o.s("strength")!!, o.s("form")!!, o.i("qty")!!) }
}
data class CombinedRequest(val fromShelters: List<String>, val items: List<RequestItem>)

/** Cross-shelter supply request: by generic name, counts only, no evacuee names. */
fun combineRequests(shelters: List<Pair<String, List<RequestItem>>>): CombinedRequest {
    val m = linkedMapOf<String, RequestItem>()
    for ((_, items) in shelters) for (i in items) m[i.key] = m[i.key]?.let { it.copy(qty = it.qty + i.qty) } ?: i
    return CombinedRequest(shelters.map { it.first }.distinct().sorted(), m.values.sortedBy { it.key })
}
