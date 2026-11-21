package org.nobodymissing.domain

import kotlin.math.abs

// Cross-shelter roster merge + public family search (F3, D6). Same logic as web dedupe.ts.
private val HONORIFICS = setOf("pak", "bapak", "bpk", "bu", "ibu", "mr", "mrs", "ms", "san", "sdr", "sdri")
private val OLD_SPELLING = listOf("oe" to "u", "dj" to "j", "tj" to "c", "sj" to "sy", "nj" to "ny")

fun normalizeName(name: String): String {
    var s = stripAccents(name.lowercase()).replace(Regex("[^a-z]+"), " ")
    for ((a, b) in OLD_SPELLING) s = s.replace(a, b)
    return s.split(" ").filter { it.isNotEmpty() && it !in HONORIFICS }.joinToString(" ")
}

data class Rec(val id: String, val name: String, val age: Int?)
private fun sortedKey(n: String) = normalizeName(n).split(" ").sorted().joinToString(" ")

fun samePerson(a: Rec, b: Rec): Boolean {
    if (a.age != null && b.age != null && abs(a.age - b.age) > 2) return false
    val ka = sortedKey(a.name); val kb = sortedKey(b.name)
    if (ka == kb) return true
    return ka.replace(" ", "").length >= 5 && kb.replace(" ", "").length >= 5 && levenshtein(ka, kb) <= 1
}

fun groupDuplicates(records: List<Rec>): List<List<String>> {
    val parent = IntArray(records.size) { it }
    fun find(i: Int): Int = if (parent[i] == i) i else find(parent[i]).also { parent[i] = it }
    for (i in records.indices) for (j in i + 1 until records.size) if (samePerson(records[i], records[j])) parent[find(j)] = find(i)
    val groups = linkedMapOf<Int, MutableList<String>>()
    records.forEachIndexed { i, r -> groups.getOrPut(find(i)) { mutableListOf() } += r.id }
    return groups.values.map { it.sorted() }.sortedBy { it[0] }
}

data class PublicRec(val id: String, val name: String, val age: Int?, val shelterId: String, val status: String, val hidden: Boolean = false)
data class PublicHit(val name: String, val ageBand: String, val shelterId: String, val status: String)
fun ageBand(age: Int?) = if (age == null) "unknown" else "${age / 10 * 10}-${age / 10 * 10 + 9}"

fun familySearch(query: String, records: List<PublicRec>): List<PublicHit> {
    val q = normalizeName(query).split(" ").filter { it.isNotEmpty() }
    if (q.isEmpty()) return emptyList()
    return records.filter { r -> !r.hidden && normalizeName(r.name).split(" ").let { t -> q.all { it in t } } }
        .map { PublicHit(it.name, ageBand(it.age), it.shelterId, it.status) }
}
