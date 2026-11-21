package org.nobodymissing.domain

import kotlinx.serialization.json.*
import java.text.Normalizer

// Deterministic drug-name matcher (6.9). Not AI. Never auto-accepts.
data class Drug(val generic: String, val brands: List<String>, val strengths: List<String>, val form: String) {
    companion object {
        fun listFrom(o: JsonObject) = o["drugs"]!!.jsonArray.map { it.jsonObject.let { d -> Drug(d.s("generic")!!, d.strings("brands"), d.strings("strengths"), d.s("form")!!) } }
    }
}
data class DrugMatch(val status: String, val generic: String?, val candidates: List<String>, val lookAlikes: List<String>, val strengthKnown: Boolean?, val needsConfirm: Boolean)

private val STRENGTH_RE = Regex("""(\d+(?:\.\d+)?)\s*(mg|mcg|g|ml|units/ml|iu)\b""", RegexOption.IGNORE_CASE)
fun normStrength(s: String) = s.lowercase().replace(Regex("\\s+"), "").replace(Regex("[^a-z0-9./]"), "")
fun stripAccents(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
private fun norm(s: String) = stripAccents(s.lowercase()).replace(Regex("[^a-z ]"), " ").replace(Regex("\\s+"), " ").trim()

fun levenshtein(a: String, b: String): Int {
    val row = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = row[0]; row[0] = i
        for (j in 1..b.length) {
            val tmp = row[j]
            row[j] = minOf(row[j] + 1, row[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = tmp
        }
    }
    return row[b.length]
}

fun matchDrug(text: String, strength: String?, list: List<Drug>): DrugMatch {
    val str = strength ?: STRENGTH_RE.find(text)?.value
    val name = norm(STRENGTH_RE.replaceFirst(text, ""))
    val exact = list.firstOrNull { it.generic == name || name in it.brands }
    if (exact != null) {
        val look = list.filter { it !== exact && levenshtein(it.generic, exact.generic) <= 3 }.map { it.generic }.sorted()
        val known = str?.let { s -> exact.strengths.any { normStrength(it) == normStrength(s) } }
        return DrugMatch("matched", exact.generic, emptyList(), look, known, true)
    }
    val maxDist = maxOf(1, name.length / 4)
    val cands = list.flatMap { d -> (listOf(d.generic) + d.brands).map { d.generic to levenshtein(it, name) } }
        .filter { it.second <= maxDist }
        .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first })
        .map { it.first }.distinct().take(3)
    return DrugMatch(if (cands.isEmpty()) "unknown" else "candidates", null, cands, emptyList(), null, true)
}

data class Med(val generic: String, val strength: String, val form: String) {
    companion object { fun from(o: JsonObject) = Med(o.s("generic")!!, o.s("strength")!!, o.s("form")!!) }
}

fun substitutionLevel(prescribed: Med, available: Med): String = when {
    prescribed.generic != available.generic -> "ask_doctor"
    normStrength(prescribed.strength) == normStrength(available.strength) && prescribed.form == available.form -> "same"
    else -> "different_strength_or_form"
}
