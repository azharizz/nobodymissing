package org.nobodymissing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.nobodymissing.domain.d
import org.nobodymissing.domain.distanceM
import org.nobodymissing.domain.nearestHigher
import org.nobodymissing.domain.s
import org.nobodymissing.features.AskAnswer

// Evacuee mode (elderly and deaf first): huge type, read aloud, dark text on soft colour, never white text. Same screens as web public.tsx.

@Composable
fun Evacuee(ui: Ui) {
    val r = ui.route.value.name
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (r) {
            "help" -> item { EvHelp(ui) }
            "emap" -> item { EvMap(ui) }
            "family" -> item { EvFamily(ui) }
            "ask" -> item { EvAsk(ui) }
            else -> item { EvHome(ui) }
        }
    }
}

@Composable
fun EvTop(ui: Ui, title: String, sub: String? = null) = Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundBtn(ui, "back", ui.f.t("back")) { ui.go("home") }; Spacer(Modifier.width(14.dp))
    Column { H(title, 30.sp, NM.Navy); sub?.let { P(it, 17.sp, NM.Ink2, bold = true) } }
}

@Composable
fun EvHome(ui: Ui) {
    val f = ui.f
    val news = f.announcements.filter { it.shown }
    val a = news.firstOrNull()
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.padding(top = 26.dp), verticalAlignment = Alignment.CenterVertically) {
            // Long-press Kit to hand the phone back to staff.
            Box(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { ui.evacueeMode.value = false; ui.go("now") }) }) { Kit(ui.kit, if (a != null) Mood.IDLE else Mood.SLEEP, 74.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { P(shelterName(ui), 17.sp, NM.Ink2, bold = true); H(greeting(ui), 30.sp, NM.Navy) }
            Row(Modifier.soft(RoundedCornerShape(50)).padding(4.dp)) { listOf("en" to "EN", "id" to "ID").forEach { (k, l) ->
                Text(l, Modifier.clip(RoundedCornerShape(50)).background(if (f.lang.value == k) NM.Navy else Color.Transparent).clickable { f.lang.value = k }.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = if (f.lang.value == k) Color.White else NM.Ink2, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) } }
        }
        Rise(0) { Column(Modifier.fillMaxWidth().soft(R28, NM.Honey).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CompositionLocalProvider(LocalContentColor provides NM.HoneyInk) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(NM.HoneyAcc)); Spacer(Modifier.width(8.dp)); Eyebrow(f.t("now")) }
                if (a == null) H(f.t("noNews"), 26.sp) else {
                    val (title, time, place) = headline(a)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(Color.White), contentAlignment = Alignment.Center) { Ic(ui.icons, cardIcon(a), 46.dp, NM.HoneyAcc) }
                        Spacer(Modifier.width(16.dp))
                        Column { H(title, 40.sp); time?.let { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "clock", 20.dp); P(" $it", 22.sp, bold = true) } }
                            place?.let { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "pin", 20.dp); P(" $it", 22.sp, bold = true) } } }
                    }
                    P("“${a.text}”", 16.sp)
                    SayButton(ui, a.id, listOfNotNull(title, time, place).joinToString(". "))
                }
            }
        } }
        BigBtn(ui, 1, NM.SKY, "map", f.t("map"), f.t("mapSub")) { ui.go("emap") }
        BigBtn(ui, 2, NM.ROSE, "help", f.t("needHelp"), f.t("needHelpSub")) { ui.go("help") }
        BigBtn(ui, 3, NM.SAGE, "family", f.t("myFamily"), f.t("myFamilySub")) { ui.go("family") }
        BigBtn(ui, 4, NM.LAV, "chat", f.t("askKit"), f.t("askKitSub")) { ui.go("ask") }
    }
}

@Composable
fun BigBtn(ui: Ui, i: Int, tone: NM.Tone, icon: String, title: String, sub: String, onClick: () -> Unit) = Rise(i) {
    Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).soft(R28, tone.bg).clickable(onClick = onClick).padding(start = 16.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 34.dp, tone.acc) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { H(title, 27.sp, tone.fg); P(sub, 16.sp, tone.fg, bold = true) }
        Ic(ui.icons, "chevron", 22.dp, tone.fg.copy(alpha = .6f))
    }
}

@Composable
fun EvHelp(ui: Ui) {
    val f = ui.f
    var spot by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf<String?>(null) }
    var writing by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val kinds = listOf(Triple("medicine", "medicine", NM.SAGE), Triple("sick", "sick", NM.ROSE), Triple("moving", "mobility", NM.SKY), Triple("write", "chat", NM.LAV))
    fun send(k: String, text: String? = null) { f.askHelp(k, spot.trim(), text); sent = k; writing = false; msg = "" }
    EvTop(ui, f.t("needHelp"), f.t("helpTap"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().soft(R22).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(NM.Sky), contentAlignment = Alignment.Center) { Ic(ui.icons, "bed", 22.dp, NM.SkyAcc) }; Spacer(Modifier.width(12.dp))
            Column { P(f.t("mySpot"), 15.sp, NM.Ink2); Box { if (spot.isEmpty()) H(f.t("spotHint"), 24.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(spot, { spot = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = NM.Navy), singleLine = true) } }
        }
        kinds.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { row.forEach { (k, icon, tone) ->
            Column(Modifier.weight(1f).height(156.dp).then(if (sent == k) Modifier.border(4.dp, NM.Navy, R28) else Modifier).soft(R28, tone.bg).clickable { if (k == "write") writing = true else send(k) }.padding(18.dp)) {
                Row { Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 30.dp, tone.acc) }
                    Spacer(Modifier.weight(1f)); if (sent == k) Pill(f.t("sent"), NM.Navy, Color.White, ui, "check") }
                Spacer(Modifier.weight(1f)); H(helpWord(ui, k), 23.sp, tone.fg)
            } } } }
        if (writing) CardBox { Field(msg, { msg = it }, f.t("writeHint"), Modifier.fillMaxWidth().heightIn(min = 90.dp)); Btn(ui, f.t("send"), "send", BtnStyle.PRIMARY, full = true, enabled = msg.isNotBlank()) { send("write", msg.trim()) } }
        sent?.let { Row(Modifier.fillMaxWidth().clip(R22).background(NM.Sage).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Kit(ui.kit, Mood.IDLE, 52.dp); Spacer(Modifier.width(14.dp)); P(f.t("volunteerComing", "spot" to spot.ifBlank { f.t("mySpot") }), 19.sp, NM.SageInk, bold = true) } }
    }
}

@Composable
fun EvMap(ui: Ui) {
    val f = ui.f
    val pack = f.shared.pack
    val sh = pack.shelters.firstOrNull { it.id == f.shelterId.value } ?: pack.shelters.first()
    val higher = nearestHigher(pack, sh.lat, sh.lng, f.rules.currentEvent, f.shared.defaults.d("higherMinGainM") ?: 2.0)
    val hs = higher?.let { h -> pack.shelters.first { it.id == h.id } }
    val places = f.shared.json("demo/region-aceh.json")["places"]!!.jsonArray.map { it.jsonObject }
    val say = higher?.let { "${f.t("higherPlace")}. ${f.t("higherAway", "name" to it.name, "m" to it.distanceM, "dir" to it.direction, "gain" to it.handGain)}" } ?: f.t("higherPlace")
    EvTop(ui, f.t("map"))
    ShelterMap(ui, listOfNotNull(hs?.let { Marker(it.id, it.lat, it.lng, "higher", NM.SageAcc, f.t("higherPlace")) }), you = sh.lat to sh.lng) {
        MapChip(f.t("floodRisk"), Color(0xFFB9D1E8)); MapChip(f.t("higherPlace"), NM.SageAcc)
    }
    Column(Modifier.offset(y = (-30).dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(NM.Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        higher?.let { h -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(NM.Sage), contentAlignment = Alignment.Center) { Ic(ui.icons, "higher", 28.dp, NM.SageAcc) }; Spacer(Modifier.width(14.dp))
            Column { H(f.t("higherPlace"), 24.sp, NM.Navy); P(f.t("higherAway", "name" to h.name, "m" to h.distanceM, "dir" to h.direction, "gain" to h.handGain), 17.sp, NM.Ink2) } } }
        SayButton(ui, "map-higher", say, NM.SkyInk)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { places.forEach { p ->
            val kind = p.s("kind")!!; val icon = mapOf("medical" to "doctor", "water" to "water", "toilet" to "toilet", "kitchen" to "food")[kind] ?: "pin"
            Column(Modifier.weight(1f).soft(R16).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(if (kind == "medical") NM.Rose else NM.Sky), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 20.dp, if (kind == "medical") NM.RoseAcc else NM.SkyAcc) }
                H(f.t(mapOf("medical" to "doctor").getOrDefault(kind, kind)), 13.sp); P("${distanceM(sh.lat, sh.lng, p.d("lat")!!, p.d("lng")!!).toInt()} m", 13.sp, NM.Ink2)
            } } }
        f.open.value?.takeIf { it.places.isNotEmpty() }?.let { o -> CardBox { H(f.t("openNearby"), 20.sp); o.places.forEach { pl -> Column { H(pl.name, 17.sp); P(listOfNotNull(if (pl.openNow == true) f.t("openNow") else null, pl.hours).joinToString(" · "), 14.sp, NM.Ink2) } }; Badge(ui, "maps") } }
        Note(ui, "info", f.t("hazardNote", "event" to f.rules.currentEvent))
    }
}

@Composable
fun EvFamily(ui: Ui) {
    val f = ui.f
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<org.nobodymissing.domain.PublicHit>?>(null) }
    EvTop(ui, f.t("myFamily"), f.t("everyShelter"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().soft(R22).padding(start = 18.dp, end = 8.dp).heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
            Ic(ui.icons, "search", 28.dp, NM.Ink2); Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) { if (q.isEmpty()) H(f.t("search"), 24.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = NM.Navy), singleLine = true) }
            Btn(ui, f.t("search"), style = BtnStyle.PRIMARY, small = true, enabled = q.isNotBlank()) { hits = f.familySearch(q) }
        }
        hits?.let { if (it.isEmpty()) H(f.t("notFound"), 22.sp) }
        hits?.forEachIndexed { i, h -> Rise(i) { Row(Modifier.fillMaxWidth().soft(R28).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(h.name, NM.SKY, 66.dp); Spacer(Modifier.width(16.dp))
            Column { H(h.name, 24.sp, NM.Navy); P("${h.ageBand} · ${h.shelterId}", 18.sp, NM.Ink2); Spacer(Modifier.height(8.dp)); Pill(if (h.status == "safe") f.t("safe") else h.status, NM.Sage, NM.SageInk, ui, "check") }
        } } }
        if (f.signal.value != "back") Note(ui, "device", f.t("offlineFamily"))
    }
}

@Composable
fun EvAsk(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var asked by remember { mutableStateOf(false) }
    var ans by remember { mutableStateOf<AskAnswer?>(null) }
    EvTop(ui, f.t("askKit"), f.t("askPrivate"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.heightIn(min = 72.dp)) { if (q.isEmpty()) H(f.t("askKitHint"), 24.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = NM.Navy)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { q = it }
                Btn(ui, f.t("ask"), "send", BtnStyle.PRIMARY, enabled = q.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { ans = f.askKit(q); asked = true; busy = false } }
            }
        }
        if (busy) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Kit(ui.kit, Mood.THINK, 110.dp) }
        if (asked && !busy) Rise(0) { Column(Modifier.fillMaxWidth().soft(R28, NM.Lav).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CompositionLocalProvider(LocalContentColor provides NM.LavInk) {
                val a = ans
                Row(verticalAlignment = Alignment.CenterVertically) { Kit(ui.kit, if (a?.source == "cloud") Mood.CLOUD else Mood.DEVICE, 56.dp); Spacer(Modifier.width(10.dp)); a?.let { Badge(ui, it.source) } }
                H(a?.answer ?: f.t("askVolunteer"), 26.sp)
                a?.let { Row(Modifier.fillMaxWidth().clip(R16).background(Color.White.copy(alpha = .65f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(ui.icons, cardIcon(it.card), 22.dp); Spacer(Modifier.width(10.dp)); P("${f.t("fromNotice")}: “${it.card.text}”", 16.sp, bold = true) } }
                SayButton(ui, "ask", a?.answer ?: f.t("askVolunteer"), NM.LavInk)
                if (a == null) Btn(ui, f.t("needHelp"), "help", BtnStyle.DANGER, full = true) { ui.go("help") }
            }
        } }
    }
}
