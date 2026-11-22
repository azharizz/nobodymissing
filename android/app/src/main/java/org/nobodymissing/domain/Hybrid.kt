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
fun askKitAnswer(cardId: String?, answer: String, cardIds: List<String>): Pair<String, String>? =
    if (cardId != null && cardId in cardIds && answer.isNotBlank()) answer.trim() to cardId else null
