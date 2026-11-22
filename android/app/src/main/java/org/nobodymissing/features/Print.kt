package org.nobodymissing.features

import kotlinx.serialization.json.*
import org.nobodymissing.domain.s

private fun esc(s: String?) = (s ?: "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

/** Kit as a still SVG for paper (the same shared parts the app draws). */
fun Features.kitSvg(size: Int = 64): String {
    val vb = shared.mascot["viewBox"]!!.jsonArray.map { it.jsonPrimitive.content }
    val paths = shared.mascot["parts"]!!.jsonArray.map { it.jsonObject }.joinToString("") { p ->
        val fill = p.s("fill"); val stroke = p.s("stroke")
        """<path d="${esc(p.s("d"))}" fill="${esc(fill ?: "none")}"${stroke?.let { """ stroke="${esc(it)}" stroke-width="${p["sw"]}" stroke-linecap="round"""" } ?: ""}${p["opacity"]?.let { """ opacity="$it"""" } ?: ""}/>"""
    }
    return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${vb[0]} ${vb[1]}" width="$size" height="$size" aria-hidden="true">$paths</svg>"""
}

/** Paper fallback (§6): roster, medicine round and the tell-in-person list, readable in black and white. */
fun Features.dailyListsHtml(now: Long = System.currentTimeMillis()): String {
    val here = people.filter { it.confirmed }
    val name = { id: String -> people.firstOrNull { it.id == id }?.name ?: id }
    val nice = { s: String -> s.replace('_', ' ') }
    val sch = schedule()
    val reach = reach(announcements.lastOrNull { it.shown } ?: announcements.lastOrNull())
    val time = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(now)
    fun table(head: List<String>, rows: List<List<String>>) =
        "<table><tr>${head.joinToString("") { "<th>${esc(it)}</th>" }}</tr>${rows.joinToString("") { r -> "<tr>${r.joinToString("") { "<td>$it</td>" }}</tr>" }}</table>"
    val box = "<span class=\"box\"></span>"
    return """<!doctype html><html lang="${lang.value}"><head><meta charset="utf-8"><title>${esc(t("printLists"))}</title><style>
body{font-family:sans-serif;color:#000;margin:24px;font-size:15px}h1{font-size:24px;display:flex;align-items:center;gap:12px;margin:0 0 4px}
h2{font-size:19px;margin:22px 0 6px;break-after:avoid}table{width:100%;border-collapse:collapse}th,td{border:1.5px solid #000;padding:6px 8px;text-align:left;vertical-align:top}
th{background:#eee}tr{break-inside:avoid}.box{display:inline-block;width:18px;height:18px;border:2px solid #000}.note{font-size:13px}</style></head><body>
<h1>${kitSvg()}${esc(t("printLists"))}</h1><p class="note">${esc(t("printKeep", "time" to time))}</p>
<h2>${esc(t("rosterTitle", "n" to here.size))}</h2>
${table(listOf(t("colName"), t("colAge"), t("colNeeds")), here.map { listOf("<b>${esc(it.name)}</b>", it.age?.toString() ?: "", esc(it.flags.joinToString(", ") { f -> nice(f) })) })}
<h2>${esc(t("medicineRound"))}</h2>
${if (sch.alerts.isNotEmpty()) "<p><b>${esc(t("cantMiss"))}:</b> ${sch.alerts.joinToString(" · ") { esc("${name(it.personId)} · ${it.generic}") }}</p>" else ""}
${table(listOf("", t("colName"), t("colMedicine"), t("given")), sch.slots.flatMap { (slot, list) -> list.map { e -> listOf("<b>${esc(slot)}</b>", esc(name(e.personId)), esc("${e.generic} · ${e.doseAsWritten ?: ""}"), box) } })}
<h2>${esc(t("tellInPerson", "n" to reach.remaining))}</h2>
${table(listOf(t("colName"), t("colNeeds"), t("colTold")), reach.list.map { r -> listOf("<b>${esc(name(r.id))}</b>", esc(r.reasons.joinToString(", ") { nice(it) }), if (r.told) "✓" else box) })}
</body></html>"""
}
