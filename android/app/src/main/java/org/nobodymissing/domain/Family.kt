package org.nobodymissing.domain

import java.security.SecureRandom

// Family group by QR (no accounts) + spelling-aware family search. Same rules as web domain/family.ts (shared/fixtures/hybrid.json).
data class Member(val id: String, val name: String, val shelter: String, val status: String, val at: Long)

private val FAM = Regex("^f-[0-9a-f]{20}$")
private val ROW = Regex("^([^,]{1,40}),([^,]{1,40}),(safe|need_help),(\\d{1,15}),(.{1,60})$")
fun newFamilyId() = "f-" + ByteArray(10).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
private fun clean(s: String) = s.replace(Regex("[|,:]"), "").trim().take(60)

/** First member = the person showing the QR; the QR carries the whole small group (max 8). */
fun encodeFamilyQr(fam: String, members: List<Member>) =
    "NMF2:$fam|" + members.take(8).joinToString("|") { listOf(clean(it.id), clean(it.shelter), it.status, it.at, clean(it.name)).joinToString(",") }

/** Trust boundary: anything scanned is checked field by field; one bad entry rejects the code. */
fun decodeFamilyQr(text: String): Pair<String, List<Member>>? {
    val parts = text.trim().split("|")
    val fam = Regex("^NMF2:(.+)$").matchEntire(parts[0])?.groupValues?.get(1) ?: return null
    val rows = parts.drop(1)
    if (!FAM.matches(fam) || rows.isEmpty() || rows.size > 12) return null
    return fam to rows.map { r -> val (id, sh, st, at, name) = (ROW.matchEntire(r) ?: return null).destructured; Member(id, name, sh, st, at.toLong()) }
}

/** Newer update wins per member; nobody is dropped. */
fun mergeMembers(mine: List<Member>, theirs: List<Member>): List<Member> {
    val out = mine.toMutableList()
    for (t in theirs) { val i = out.indexOfFirst { it.id == t.id }; if (i < 0) out += t else if (t.at > out[i].at) out[i] = t }
    return out
}

/** Offline spelling rules (abbreviations both ways + joined first two words). The cloud adds more when online. */
fun offlineVariants(name: String, rules: Map<String, List<String>>): List<String> {
    val toks = name.trim().split(Regex("\\s+"))
    val out = mutableListOf<String>()
    toks.forEachIndexed { i, t ->
        val key = rules.keys.firstOrNull { it.equals(t, true) }
        val back = rules.entries.firstOrNull { (_, alts) -> alts.any { it.equals(t, true) } }?.key
        for (alt in (key?.let { rules.getValue(it) } ?: emptyList()) + listOfNotNull(back)) out += (toks.take(i) + alt + toks.drop(i + 1)).joinToString(" ")
    }
    if (toks.size >= 2) out += toks[0] + toks[1] + if (toks.size > 2) " " + toks.drop(2).joinToString(" ") else ""
    return out.distinct().filter { !it.equals(name.trim(), true) }
}

data class FamilyHit(val name: String, val ageBand: String, val shelterId: String, val status: String, val match: String)
/** Exact name first, then other spellings. An age (if known) drops people more than 10 years off. A person always confirms. */
fun searchVariants(name: String, variants: List<String>, age: Int?, records: List<PublicRec>): List<FamilyHit> {
    val pool = records.filter { age == null || it.age == null || kotlin.math.abs(it.age - age) <= 10 }
    val seen = mutableSetOf<String>()
    fun hit(r: PublicRec, m: String) = if (seen.add(r.id)) listOf(FamilyHit(r.name, ageBand(r.age), r.shelterId, r.status, m)) else emptyList()
    val exact = pool.filter { familySearch(name, listOf(it)).isNotEmpty() }.flatMap { hit(it, "exact") }
    // Offline safety net: every word of the name matches a word in the record with at most one letter different (words of 4+ letters).
    val q = normalizeName(name).split(" ").filter { it.isNotEmpty() }
    fun close(r: PublicRec): Boolean { val w = normalizeName(r.name).split(" "); return q.isNotEmpty() && q.all { t -> w.any { it == t || (t.length >= 4 && levenshtein(t, it) <= 1) } } }
    val spelling = pool.filter { r -> !r.hidden && (variants.any { familySearch(it, listOf(r)).isNotEmpty() } || close(r)) }.flatMap { hit(it, "spelling") }
    return exact + spelling
}

/** Check-in: people already here who might be this person (same spelling-aware match as family search). A person decides. */
fun maybeAlreadyHere(name: String, age: Int?, records: List<PublicRec>, variants: List<String> = emptyList()) =
    records.filter { searchVariants(name, variants, age, listOf(it)).isNotEmpty() }.map { it.id }

data class LookFor(val name: String, val age: Int?, val place: String?)
private val RELATION = Regex("\\b(my|saya|aku|nenek|kakek|ibu|bapak|pak|bu|ayah|mama|papa|anak|adik|kakak|suami|istri|om|tante|paman|bibi|grandmother|grandfather|grandma|grandpa|mother|father|son|daughter|brother|sister|husband|wife|uncle|aunt|named|bernama|namanya)\\b", RegexOption.IGNORE_CASE)
/** No device model: pull name, age and place out of "my grandmother Nurhayati, about 80, from Sukajadi" with plain rules. */
fun parseLookFor(text: String): LookFor {
    val age = Regex("(\\d{1,3})\\s*(an|s)?\\b").find(text)?.groupValues?.get(1)?.toIntOrNull()
    val place = Regex("\\b(?:from|dari)\\s+([\\p{L} ]+?)(?:[,.]|$)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
    val head = text.split(Regex("[,.]|\\b(?:about|around|sekitar|kira-kira|umur|usia|aged?|from|dari)\\b|\\d", RegexOption.IGNORE_CASE))[0]
    return LookFor(head.replace(RELATION, " ").replace(Regex("\\s+"), " ").trim().ifEmpty { text.trim() }, age, place)
}
