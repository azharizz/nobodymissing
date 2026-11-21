package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Offline announcement icons + times + place from a deterministic keyword list (D11).
data class Keywords(val icons: Map<String, List<String>>, val placeWords: List<String>) {
    companion object {
        fun from(o: JsonObject) = Keywords(o["icons"]!!.jsonObject.mapValues { it.value.jsonArray.map { w -> w.jsonPrimitive.content } }, o.strings("placeWords"))
    }
}
data class IconResult(val icons: List<String>, val times: List<String>, val place: String?)

private fun pad(n: Int) = n.toString().padStart(2, '0')
private val TRAIL = Regex("[,.!?;:]+$")

fun keywordIcons(text: String, kw: Keywords): IconResult {
    val lower = text.lowercase()
    val found = mutableListOf<Pair<Int, String>>()
    for ((icon, words) in kw.icons) {
        var best = Int.MAX_VALUE
        for (w in words) Regex("(^|[^\\p{L}])${Regex.escape(w)}(?=[^\\p{L}]|$)").find(lower)?.let { best = minOf(best, it.range.first + it.groupValues[1].length) }
        if (best < Int.MAX_VALUE) found += best to icon
    }
    val times = mutableListOf<Pair<Int, String>>()
    Regex("""\b(\d{1,2})[.:](\d{2})\b""").findAll(lower).forEach { m ->
        val h = m.groupValues[1].toInt(); val mi = m.groupValues[2].toInt()
        if (h <= 23 && mi <= 59) times += m.range.first to "${pad(h)}:${pad(mi)}"
    }
    Regex("""\b(?:jam|pukul)\s+(\d{1,2})(?![.:]\d|\d)""").findAll(lower).forEach { m ->
        val h = m.groupValues[1].toInt(); if (h <= 23) times += m.range.first to "${pad(h)}:00"
    }
    Regex("""\b(\d{1,2})\s*(am|pm)\b""").findAll(lower).forEach { m ->
        val n = m.groupValues[1].toInt()
        if (n <= 12) times += m.range.first to "${pad(n % 12 + if (m.groupValues[2] == "pm") 12 else 0)}:00"
    }
    return IconResult(found.sortedBy { it.first }.map { it.second }, times.sortedBy { it.first }.map { it.second }.distinct(), findPlace(text, kw.placeWords))
}

private fun findPlace(text: String, placeWords: List<String>): String? {
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    fun bare(w: String) = w.lowercase().replace(TRAIL, "")
    for (i in words.indices) {
        if (bare(words[i]) !in placeWords) continue
        val out = mutableListOf<String>()
        for (j in i + 1 until words.size) {
            val w = words[j]; val b = bare(w)
            if (b in placeWords || b == "jam" || b == "pukul" || b.firstOrNull()?.isDigit() == true) break
            out += w.replace(TRAIL, "")
            if (Regex("[,.!?;:]$").containsMatchIn(w)) break
        }
        if (out.isNotEmpty()) return out.joinToString(" ")
    }
    return null
}
