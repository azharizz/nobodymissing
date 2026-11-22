package org.nobodymissing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.nobodymissing.features.RunLog

/** Hybrid made visible (same as web): green = this phone, blue = cloud, and what left the shelter. */
@Composable
fun KitCard(ui: Ui) {
    val f = ui.f; val k = f.kitToday()
    val left = k.sent.filterValues { it > 0 }; val personal = k.sent.getValue("photo") + k.sent.getValue("name")
    val sentText = left.entries.joinToString(" · ") { (kind, n) -> f.t("sent_$kind", "n" to n) }
    CardBox(Modifier.fillMaxWidth().clickable { ui.go("kit") }) {
        Row(verticalAlignment = Alignment.CenterVertically) { H(f.t("kitToday"), 18.sp, NM.Navy, Modifier.weight(1f)); P(f.t("nWaiting", "n" to f.jobs.count { it.status.state != "sent" }), 13.sp, NM.Ink2) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Triple(k.device, "onPhoneBig", true), Triple(k.cloud, "inCloudBig", false)).forEach { (n, key, dev) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (dev) NM.Sage else NM.Sky).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    H("$n", 34.sp, if (dev) NM.SageInk else NM.SkyInk)
                    Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, if (dev) "device" else "cloud", 15.dp, if (dev) NM.SageInk else NM.SkyInk); Spacer(Modifier.width(6.dp)); P(f.t(key), 14.sp, if (dev) NM.SageInk else NM.SkyInk, bold = true) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ic(ui.icons, "lock", 15.dp, if (personal == 0) NM.SageAcc else NM.SkyAcc); Spacer(Modifier.width(8.dp))
            P(when { left.isEmpty() -> f.t("leftNone"); personal == 0 -> f.t("leftNothing") + " · " + sentText; else -> sentText }, 14.sp, if (personal == 0) NM.SageAcc else NM.SkyAcc, bold = true)
        }
        P(f.t("kitTap"), 13.sp, NM.Ink2)
    }
}

@Composable
fun KitLog(ui: Ui) {
    val f = ui.f; val k = f.kitToday(); val sends = f.shared.defaults["cloudSends"]?.jsonObject.orEmpty()
    Screen({ Top(ui, f.t("whereKitRan")) }) {
        pad { KitCard(ui) }
        if (k.log.isEmpty()) pad { Empty(ui, f.t("noKitYet"), Mood.IDLE) } else pad { EvalCard(ui) }
        k.log.forEach { l -> pad { KitRow(ui, l, (sends[l.task] as? JsonPrimitive)?.content) } }
    }
}

@Composable
private fun KitRow(ui: Ui, l: RunLog, sends: String?) {
    val f = ui.f
    val (bg, ink, icon, label) = when {
        l.source == "device" -> listOf(NM.Sage, NM.SageInk, "device", f.t("ranOnPhone"))
        l.source == "cloud" -> listOf(NM.Sky, NM.SkyInk, "cloud", f.t("ranInCloud"))
        l.status == "queued" || l.status == "ask_staff" -> listOf(NM.AmberSoft, NM.AmberInk, "clock", f.t(if (l.status == "queued") "ranWaiting" else "ranAsked"))
        else -> listOf(NM.Line, NM.Ink, "lock", f.t("ranBlocked"))
    }
    bg as androidx.compose.ui.graphics.Color; ink as androidx.compose.ui.graphics.Color
    CardBox(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(ui.icons, icon as String, 14.dp, ink); Spacer(Modifier.width(5.dp)); P(label as String, 13.sp, ink, bold = true) }
            Spacer(Modifier.weight(1f))
            P(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(l.at), 13.sp, NM.Ink2)
        }
        P(f.t("task_${l.task}"), 16.sp, bold = true)
        P(f.t("why_${l.why}") + if (l.source != null) " · %.1f s".format(l.ms / 1000.0) else "", 14.sp, NM.Ink2)
        P(if (l.source == "cloud") f.t("sentWhat", "what" to f.t("what_${sends ?: "text"}")) else f.t("sentNothing"), 14.sp, bold = true)
    }
}

/** Measured, not claimed: shared/eval/results.json, same numbers as web. */
@Composable
fun EvalCard(ui: Ui) {
    val f = ui.f
    val sets = runCatching { f.shared.json("eval/results.json")["sets"]!!.jsonArray.map { it.jsonObject } }.getOrDefault(emptyList())
    sets.forEach { set ->
        CardBox(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "check", 20.dp); Spacer(Modifier.width(8.dp)); H(f.t("measuredTitle"), 18.sp) }
            P(set["title"]!!.jsonPrimitive.content, 15.sp, bold = true)
            P(f.t("measuredNote", "n" to set["n"]!!.jsonPrimitive.content, "date" to set["date"]!!.jsonPrimitive.content) + " · " + set["where"]!!.jsonPrimitive.content, 13.sp, NM.Ink2)
            val cols = set["columns"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("en", "id")
            val colKey = mapOf("en" to "colEn", "id" to "colId", "names" to "colNames", "ages" to "colAges", "needs" to "colNeeds")
            Row { P(f.t("colWhere"), 12.sp, NM.Ink2, modifier = Modifier.weight(2f)); cols.forEach { P(f.t(colKey[it] ?: it), 12.sp, NM.Ink2, modifier = Modifier.weight(1f)) }; P(f.t("colTime"), 12.sp, NM.Ink2, modifier = Modifier.weight(1f)) }
            set["rows"]!!.jsonArray.map { it.jsonObject }.forEach { r ->
                val path = r["path"]!!.jsonPrimitive.content; val ms = r["medianMs"]!!.jsonPrimitive.content.toDouble()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    P(r["label"]!!.jsonPrimitive.content, 14.sp, when (path) { "device" -> NM.SageAcc; "cloud" -> NM.SkyAcc; else -> NM.Ink }, bold = path != "keywords", modifier = Modifier.weight(2f))
                    cols.forEach { c -> P("${r[c]?.jsonPrimitive?.content ?: "—"}%", 14.sp, bold = true, modifier = Modifier.weight(1f)) }
                    P(if (ms > 0) "%.1f s".format(ms / 1000) else "—", 14.sp, modifier = Modifier.weight(1f))
                }
            }
            P(set["decision"]!!.jsonPrimitive.content, 14.sp, NM.Ink2)
        }
    }
}
