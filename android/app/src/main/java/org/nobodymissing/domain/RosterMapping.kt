package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Map validated AI roster rows into person drafts. AI never confirms; staff do (6.8b).
data class RosterRow(val name: String, val age: Int?, val sex: String?, val flags: List<String>, val notes: String?, val confidence: Double) {
    companion object { fun from(o: JsonObject) = RosterRow(o.s("name")!!, o.i("age"), o.s("sex"), o.strings("flags"), o.s("notes"), o.d("confidence")!!) }
}
data class PersonDraft(val nameAsWritten: String, val age: Int?, val sex: String?, val suggestedFlags: List<String>, val notes: String?, val needsReview: Boolean, val inferredBy: String, val confirmedBy: String?)

fun mapRosterRows(rows: List<RosterRow>, source: String, r: Rules) = rows.map { row ->
    val flags = row.flags.toMutableList()
    if (row.age != null && row.age >= r.elderlyAge && "elderly" !in flags) flags += "elderly"
    PersonDraft(row.name, row.age, row.sex, flags, row.notes, row.confidence < r.reviewConfidence, source, null)
}
