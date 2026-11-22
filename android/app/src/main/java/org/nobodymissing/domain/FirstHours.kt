package org.nobodymissing.domain

// The first hours: warning -> the way -> pickup -> check-in. Same rules as web domain/firstHours.ts (shared/fixtures/hybrid.json).
private fun has(text: String, words: List<String>): Boolean { val t = " ${text.lowercase()} "; return words.any { t.contains(it.lowercase()) } }

data class WarningPlain(val hazard: String, val level: String, val plain: String, val action: String?, val by: String) : java.io.Serializable
data class WarnRules(val goNowWords: List<String>, val hazardWords: Map<String, List<String>>)
/** Official warning -> plain words. "evakuasi segera" in the original always means go now, whatever the model says. */
fun plainWarning(text: String, ai: WarningPlain?, by: String, r: WarnRules): WarningPlain {
    val goNow = has(text, r.goNowWords)
    val hazard = r.hazardWords.entries.firstOrNull { has(text, it.value) }?.key ?: "other"
    if (ai != null) return WarningPlain(if (ai.hazard == "other") hazard else ai.hazard, if (goNow) "go_now" else ai.level, ai.plain, ai.action, by)
    val first = Regex("^.*?[.!?](\\s|$)").find(text.trim())?.value?.trim() ?: text.trim()
    return WarningPlain(hazard, if (goNow) "go_now" else "prepare", first.take(160), null, "keywords")
}

/** "Where should I go?" is answered from the offline flood map, never by a model guess. */
fun isWhereToGo(text: String, whereWords: List<String>) = has(text, whereWords)

data class Checkin(val name: String, val age: Int?, val flags: List<String>, val by: String = "keywords")
/** Volunteer's sentence at the door -> name, age, flags (keywords). */
fun parseCheckin(text: String, flagWords: Map<String, List<String>>): Checkin {
    val age = Regex("\\b(\\d{1,3})\\b").find(text)?.groupValues?.get(1)?.toInt()
    return Checkin(text.split(Regex("[,;]|\\d"))[0].trim().take(60), age?.takeIf { it in 1..120 }, flagWords.keys.filter { has(text, flagWords.getValue(it)) })
}
/** Flags the words clearly say are never dropped, even if the model forgot them. */
fun mergeCheckin(ai: Checkin?, text: String, flagWords: Map<String, List<String>>): Checkin {
    val r = parseCheckin(text, flagWords)
    return if (ai == null) r else Checkin(ai.name.ifBlank { r.name }, ai.age ?: r.age, (ai.flags + r.flags).distinct(), "device")
}

data class Pickup(val who: String, val mobility: Int, val needs: List<String>, val people: Int, val by: String = "keywords")
/** "My mother can't walk, needs insulin" -> pickup request (keywords). */
fun parsePickup(text: String, needWords: Map<String, List<String>>, mobilityWords: Map<String, List<String>>, peopleWords: Map<String, List<String>> = emptyMap()) = Pickup(
    text.split(Regex("[,;.]"))[0].trim().take(40), (mobilityWords.filter { has(text, it.value) }.keys.map { it.toInt() } + 0).max(), needWords.keys.filter { has(text, needWords.getValue(it)) },
    (peopleWords.filter { has(text, it.value) }.keys.map { it.toInt() } + 1).max())
/** Mobility, needs and headcount can only go up. */
fun mergePickup(ai: Pickup?, text: String, needWords: Map<String, List<String>>, mobilityWords: Map<String, List<String>>, peopleWords: Map<String, List<String>> = emptyMap()): Pickup {
    val r = parsePickup(text, needWords, mobilityWords, peopleWords)
    return if (ai == null) r else Pickup(ai.who.ifBlank { r.who }, maxOf(ai.mobility, r.mobility), (ai.needs + r.needs).distinct(), maxOf(1, ai.people, r.people), "device")
}
/** A pickup becomes a Field Check entry ranked by the same priority rules. */
fun pickupPriority(p: Pickup) = PriorityInput(age = null, mobility = minOf(3, p.mobility), cantMiss = p.needs.any { it in listOf("insulin", "dialysis", "oxygen") },
    dailyMeds = p.needs.contains("daily_meds"), hearing = p.needs.contains("deaf"), dementia = p.needs.contains("dementia"))

private val FLAGS = listOf("hard_of_hearing", "deaf", "needs_meds", "wheelchair", "cant_walk", "lives_alone", "foreign_language", "infant", "pregnant", "elderly", "dementia")
private val NEEDS = listOf("insulin", "dialysis", "oxygen", "daily_meds", "baby", "pregnant", "wheelchair", "bedridden", "deaf", "blind", "dementia", "other")
private fun clean(s: String) = s.replace("|", "").trim().take(60)

/** Check-in card QR: name, age, needs the person chose themselves. */
fun encodeCheckinQr(c: Checkin) = "NMC1|${clean(c.name)}|${c.age ?: ""}|${c.flags.filter { it in FLAGS }.joinToString(",")}"
fun decodeCheckinQr(text: String): Checkin? {
    val m = Regex("^NMC1\\|([^|]{1,60})\\|(\\d{0,3})\\|([a-z_,]*)$").matchEntire(text.trim()) ?: return null
    val age = m.groupValues[2].toIntOrNull(); val flags = m.groupValues[3].split(",").filter { it.isNotEmpty() }
    if ((age != null && age > 120) || flags.any { it !in FLAGS }) return null
    return Checkin(m.groupValues[1].trim(), age, flags)
}

data class PickupRecord(val id: String, val lat: Double, val lng: Double, val mobility: Int, val needs: List<String>, val people: Int, val at: Long, val who: String) : java.io.Serializable
/** Pickup QR: carried to any volunteer by hand when there is no signal at all. */
fun encodePickupQr(p: PickupRecord): String {
    fun n(d: Double) = if (d == Math.floor(d)) d.toLong().toString() else d.toString()
    return "NMP1|${clean(p.id)}|${n(p.lat)}|${n(p.lng)}|${p.mobility}|${p.needs.joinToString(",")}|${p.people}|${p.at}|${clean(p.who)}"
}
fun decodePickupQr(text: String): PickupRecord? {
    val m = Regex("^NMP1\\|([^|]{1,40})\\|(-?\\d+(?:\\.\\d+)?)\\|(-?\\d+(?:\\.\\d+)?)\\|([0-3])\\|([a-z_,]*)\\|(\\d{1,2})\\|(\\d{1,15})\\|([^|]{1,40})$").matchEntire(text.trim()) ?: return null
    val g = m.groupValues; val lat = g[2].toDouble(); val lng = g[3].toDouble(); val needs = g[5].split(",").filter { it.isNotEmpty() }
    if (kotlin.math.abs(lat) > 90 || kotlin.math.abs(lng) > 180 || needs.any { it !in NEEDS }) return null
    return PickupRecord(g[1], lat, lng, g[4].toInt(), needs, g[6].toInt(), g[7].toLong(), g[8])
}
