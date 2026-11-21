package org.nobodymissing.domain

// QR courier (6.10 level 2): numbered QR-sized chunks, reassembled in any order.
fun chunk(payload: String, size: Int): List<String> {
    val n = maxOf(1, (payload.length + size - 1) / size)
    return (0 until n).map { i -> "NM1|${i + 1}/$n|${payload.substring(minOf(i * size, payload.length), minOf((i + 1) * size, payload.length))}" }
}

private val CHUNK_RE = Regex("""^NM1\|(\d+)/(\d+)\|([\s\S]*)$""")

fun join(chunks: List<String>): String? {
    val parts = mutableMapOf<Int, String>()
    var total = -1
    for (c in chunks) {
        val m = CHUNK_RE.find(c) ?: return null
        val t = m.groupValues[2].toInt()
        if (total >= 0 && t != total) return null
        total = t; parts[m.groupValues[1].toInt()] = m.groupValues[3]
    }
    if (total < 1) return null
    val sb = StringBuilder()
    for (i in 1..total) sb.append(parts[i] ?: return null)
    return sb.toString()
}
