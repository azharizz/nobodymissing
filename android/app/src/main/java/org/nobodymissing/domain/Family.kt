package org.nobodymissing.domain

import java.security.SecureRandom

// Family group by QR (no accounts). Same format and rules as web domain/family.ts (shared/fixtures/hybrid.json).
data class Member(val id: String, val name: String, val shelter: String, val status: String, val at: Long)

private val FAM = Regex("^f-[0-9a-f]{20}$")
private val QR = Regex("^NMF1:([^:]+):([^:]+):([^:]+):(safe|need_help):(\\d+):(.{1,60})$")
fun newFamilyId() = "f-" + ByteArray(10).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
fun encodeFamilyQr(fam: String, me: Member) = "NMF1:$fam:${me.id}:${me.shelter}:${me.status}:${me.at}:${me.name}"

/** Trust boundary: anything scanned is checked field by field. */
fun decodeFamilyQr(text: String): Pair<String, Member>? {
    val m = QR.matchEntire(text.trim()) ?: return null
    val (fam, id, shelter, status, at, name) = m.destructured
    return if (FAM.matches(fam)) fam to Member(id, name, shelter, status, at.toLong()) else null
}

/** Newer update wins per member; nobody is dropped. */
fun mergeMembers(mine: List<Member>, theirs: List<Member>): List<Member> {
    val out = mine.toMutableList()
    for (t in theirs) { val i = out.indexOfFirst { it.id == t.id }; if (i < 0) out += t else if (t.at > out[i].at) out[i] = t }
    return out
}
