package org.nobodymissing.domain

import java.text.Normalizer
import kotlin.math.pow

// Safety contracts for the three hybrid features. Same as web domain/hybrid.ts, same fixtures (shared/fixtures/hybrid.json).

data class MedLine(val name: String, val strength: String?)
/** Privacy split: the cloud request is built from a whitelist, so names, beds and photos cannot get in. */
fun medCheckInput(meds: List<Triple<String, String?, String?>>) = meds.map { (asWritten, strength, generic) -> MedLine(generic?.takeIf { it.isNotBlank() } ?: asWritten, strength) }

/** Coarse location: rounded before it leaves the device (2 decimals is about 1 km). */
fun coarseLocation(lat: Double, lng: Double, decimals: Int): Pair<Double, Double> {
    val f = 10.0.pow(decimals)
    fun r(x: Double) = Math.round(x * f) / f
    return r(lat) to r(lng)
}

data class Cross(val name: String, val offline: String?, val status: String)
/** The cloud's generic name is checked against the offline drug list. A disagreement is shown, never hidden. */
fun crossCheck(items: List<Pair<String, String?>>, drugs: List<Drug>) = items.map { (name, generic) ->
    val m = matchDrug(name, null, drugs)
    val offline = m.generic ?: m.candidates.singleOrNull()
    val cloud = generic?.lowercase()?.trim()?.ifEmpty { null }
    Cross(name, offline, when { offline != null && cloud != null -> if (offline == cloud) "agree" else "mismatch"; cloud != null -> "cloud_only"; offline != null -> "offline_only"; else -> "unknown" })
}

data class GroundedChunk(val title: String?, val uri: String?, val placeId: String?)
private fun key(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("[^a-z0-9]"), "")
/** Only places Google Maps itself returned (grounding chunks) survive; returns (index of place, chunk). */
fun groundedPlaces(names: List<String>, chunks: List<GroundedChunk>): List<Pair<Int, GroundedChunk>> = names.mapIndexedNotNull { i, n ->
    val k = key(n)
    chunks.filter { it.title != null }.firstOrNull { val t = key(it.title!!); t == k || t.contains(k) || k.contains(t) }?.let { i to it }
}

/** Ask Kit may only answer from today's notices: an answer must cite a real card, otherwise it is discarded. */
fun urgentWordsIn(text: String, words: List<String>): Boolean { val t = " ${text.lowercase()} "; return words.any { t.contains(it.lowercase()) } }
val KIT_INTENTS = listOf("greeting", "where_to_go", "help", "pickup", "family", "notice", "unknown")
data class KitReply(val intent: String, val answer: String?, val cardId: String?)
/** Ask Kit: the model only picks what the person wants; the app answers with its own words and real screens.
 *  Only a notice answer shows model text, and only if it cites a real notice. Urgent words always mean help. */
fun askKitAnswer(intent: String?, cardId: String?, answer: String, cardIds: List<String>, text: String = "", urgentWords: List<String> = emptyList()): KitReply {
    if (urgentWordsIn(text, urgentWords)) return KitReply("help", null, null)
    val real = cardId != null && cardId in cardIds && answer.isNotBlank()
    val i = if (intent in KIT_INTENTS) intent!! else if (real) "notice" else "unknown"
    if (i == "notice") return if (real) KitReply(i, answer.trim(), cardId) else KitReply("unknown", null, null)
    return KitReply(i, null, null)
}

data class Triage(val kind: String, val urgent: Boolean, val summary: String, val by: String)
private val ICON_KIND = mapOf("medicine" to "medicine", "doctor" to "sick", "food" to "food", "water" to "water", "toilet" to "toilet", "baby" to "baby", "move" to "moving")
/** "I need help" in the evacuee's own words. Urgency can only go up; no device model -> keyword rules and the original words. */
fun triageHelp(text: String, ai: Triple<String, Boolean, String>?, urgentWords: List<String>, icons: List<String>): Triage {
    val t = " ${text.lowercase()} "
    val urgentWord = urgentWords.any { t.contains(it.lowercase()) }
    if (ai != null) return Triage(ai.first, ai.second || urgentWord, ai.third, "device")
    return Triage(icons.firstNotNullOfOrNull { ICON_KIND[it] } ?: if (urgentWord) "sick" else "other", urgentWord, text.trim(), "keywords")
}
