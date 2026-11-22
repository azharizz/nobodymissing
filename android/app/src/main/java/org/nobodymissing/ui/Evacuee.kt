package org.nobodymissing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.asImageBitmap
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
            "go" -> item { EvGo(ui) }
            "way" -> item { EvWay(ui) }
            "pickup" -> item { EvPickup(ui) }
            "checkin" -> item { EvCheckinCard(ui) }
            "help" -> item { EvHelp(ui) }
            "emap" -> item { EvMap(ui) }
            "family" -> item { EvFamily(ui) }
            "ask" -> item { EvAsk(ui) }
            else -> item { EvHome(ui) }
        }
    }
}

@Composable
fun EvTop(ui: Ui, title: String, sub: String? = null, back: String = if (ui.route.value.name in listOf("way", "pickup")) "go" else "home") = Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundBtn(ui, "back", ui.f.t("back")) { ui.go(back) }; Spacer(Modifier.width(14.dp))
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
        f.warning.value?.takeIf { it.w.level != "advice" }?.let { w -> Row(Modifier.fillMaxWidth().clip(R28).background(NM.Rose).border(3.dp, NM.RoseAcc, R28).clickable { ui.go("go") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Kit(ui.kit, Mood.ALERT, 56.dp); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { H("${f.t("hazard_${w.w.hazard}")} · ${f.t("level_${w.w.level}")}", 22.sp, NM.RoseInk); P(w.w.plain, 16.sp, NM.RoseInk) }
            Ic(ui.icons, "chevron", 22.dp, NM.RoseInk) } }
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
        BigBtn(ui, 1, NM.ROSE, "warn", f.t("goSafety"), f.t("goSafetySub")) { ui.go("go") }
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
    val scope = rememberCoroutineScope()
    var spot by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var words by remember { mutableStateOf(takeHandOff()) }
    var busy by remember { mutableStateOf(false) }
    var tri by remember { mutableStateOf<org.nobodymissing.domain.Triage?>(null) }
    val kinds = listOf(Triple("medicine", "medicine", NM.SAGE), Triple("sick", "sick", NM.ROSE), Triple("moving", "mobility", NM.SKY), Triple("baby", "baby", NM.LAV))
    fun send(k: String, text: String? = null, t: org.nobodymissing.domain.Triage? = null) { f.askHelp(k, spot.trim(), text, t); sent = k to (t?.urgent ?: false); tri = null; words = "" }
    EvTop(ui, f.t("needHelp"), f.t("helpTap"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth().soft(R22).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(NM.Sky), contentAlignment = Alignment.Center) { Ic(ui.icons, "bed", 22.dp, NM.SkyAcc) }; Spacer(Modifier.width(12.dp))
            Column { P(f.t("mySpot"), 15.sp, NM.Ink2); Box { if (spot.isEmpty()) H(f.t("spotHint"), 24.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(spot, { spot = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = NM.Navy), singleLine = true) } }
        }
        Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Kit(ui.kit, if (busy) Mood.THINK else Mood.IDLE, 52.dp); Spacer(Modifier.width(10.dp)); H(f.t("tellKit"), 22.sp, NM.Navy) }
            Box(Modifier.heightIn(min = 60.dp)) { if (words.isEmpty()) H(f.t("tellKitHint"), 22.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(words, { words = it; tri = null }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = NM.Navy)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { words = it; tri = null }
                Btn(ui, f.t("ask"), "sparkle", BtnStyle.PRIMARY, enabled = words.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { tri = f.triage(words); busy = false } }
            }
        }
        tri?.let { t -> val tone = if (t.urgent) NM.ROSE else NM.LAV
            Column(Modifier.fillMaxWidth().clip(R28).background(tone.bg).then(if (t.urgent) Modifier.border(3.dp, NM.RoseAcc, R28) else Modifier).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompositionLocalProvider(LocalContentColor provides tone.fg) {
                    Eyebrow(f.t("kitUnderstood"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill(f.t("kind_${t.kind}"), Color.White, tone.fg); if (t.urgent) Pill(f.t("urgent"), NM.Rose, NM.RoseInk, ui, "alert") }
                    H(t.summary, 24.sp)
                    if (t.by == "device") Badge(ui, "device", text = f.t("understoodOnPhone")) else Badge(ui, "keywords", text = f.t("keywordsOnly"))
                    Btn(ui, f.t("sendToStaff"), "send", BtnStyle.PRIMARY, full = true) { send(t.kind, words.trim(), t) }
                }
            } }
        Eyebrow(f.t("orTapOne"), NM.Ink2)
        kinds.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { row.forEach { (k, icon, tone) ->
            Column(Modifier.weight(1f).height(156.dp).then(if (sent?.first == k) Modifier.border(4.dp, NM.Navy, R28) else Modifier).soft(R28, tone.bg).clickable { send(k) }.padding(18.dp)) {
                Row { Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 30.dp, tone.acc) }
                    Spacer(Modifier.weight(1f)); if (sent?.first == k) Pill(f.t("sent"), NM.Navy, Color.White, ui, "check") }
                Spacer(Modifier.weight(1f)); H(f.t("kind_$k"), 23.sp, tone.fg)
            } } } }
        sent?.let { (_, urgent) -> Row(Modifier.fillMaxWidth().clip(R22).background(if (urgent) NM.Rose else NM.Sage).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Kit(ui.kit, if (urgent) Mood.ALERT else Mood.IDLE, 52.dp); Spacer(Modifier.width(14.dp))
            P(if (urgent) f.t("urgentNow") else f.t("volunteerComing", "spot" to spot.ifBlank { f.t("mySpot") }), 19.sp, if (urgent) NM.RoseInk else NM.SageInk, bold = true) } }
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
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf(takeHandOff()) }
    var busy by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf<org.nobodymissing.features.SmartFamily?>(null) }
    EvTop(ui, f.t("myFamily"), f.t("everyShelter"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.heightIn(min = 60.dp)) { if (q.isEmpty()) H(f.t("lookForHint"), 20.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = NM.Navy)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { q = it }
                Btn(ui, f.t("search"), "search", BtnStyle.PRIMARY, enabled = q.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { res = f.familySmart(q); busy = false } }
            }
        }
        res?.let { r -> CardBox {
            H("${f.t("lookingFor")}: ${r.name}" + (r.age?.let { " · " + f.t("aged", "n" to it) } ?: "") + (r.place?.let { " · " + f.t("fromPlace", "p" to it) } ?: ""), 18.sp)
            if (r.parsedBy == "device") Badge(ui, "device", text = f.t("understoodOnPhone")) else Badge(ui, "keywords", text = f.t("keywordsOnly"))
            if (r.variants.isNotEmpty()) { P("${f.t("spellings")}: ${r.variants.joinToString()}", 15.sp, NM.Ink2); Badge(ui, if (r.variantsBy == "cloud") "cloud" else "keywords", text = f.t(if (r.variantsBy == "cloud") "spellingsCloud" else "spellingsOffline")) }
        } }
        res?.let { if (it.hits.isEmpty()) H(f.t("notFound"), 22.sp) }
        res?.hits?.forEachIndexed { i, h -> Rise(i) { Row(Modifier.fillMaxWidth().soft(R28).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(h.name, NM.SKY, 66.dp); Spacer(Modifier.width(16.dp))
            Column { H(h.name, 24.sp, NM.Navy); P("${h.ageBand} · ${h.shelterId}", 18.sp, NM.Ink2); Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Pill(if (h.status == "safe") f.t("safe") else h.status, NM.Sage, NM.SageInk, ui, "check")
                    Pill(f.t(if (h.match == "exact") "matchExact" else "matchSpelling"), if (h.match == "exact") NM.Quiet else NM.AmberSoft, if (h.match == "exact") NM.Ink else NM.AmberInk) } }
        } } }
        if (res?.hits?.isNotEmpty() == true) Note(ui, "info", f.t("confirmWithStaff"))
        if (f.signal.value != "back") Note(ui, "device", f.t("offlineFamily"))
        FamilyGroupCard(ui)
    }
}

/** Family group by QR: show mine, scan theirs. Works offline face to face; syncs online when apart. */
@Composable
fun FamilyGroupCard(ui: Ui) {
    val f = ui.f
    val g = f.familyGroup.value
    var name by remember { mutableStateOf(g?.me?.name ?: "") }
    var show by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun got(text: String) { val m = f.scanFamily(text); msg = m?.let { f.t("linked", "name" to it.joinToString { x -> x.name }) + " " + f.t("scanBack") } ?: f.t("badQr"); if (m != null) { paste = ""; show = true; scope.launch { f.syncFamily() } } }
    // Live scanner from Google Play services (QR only); falls back to the paste box if it is not available.
    fun scan() = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(ctx, com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()).startScan()
        .addOnSuccessListener { b -> b.rawValue?.let(::got) }.addOnFailureListener { msg = f.t("noCamera") }
    fun ago(at: Long) = ((System.currentTimeMillis() - at) / 60000).let { m -> if (m < 1) f.t("justNow") else if (m < 60) f.t("minAgo", "n" to m) else f.t("hAgo", "n" to m / 60) }
    @Composable fun row(m: org.nobodymissing.domain.Member, me: Boolean) = Row(Modifier.fillMaxWidth().soft(R22).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(m.name, if (me) NM.HONEY else NM.SKY); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { H(if (me) "${m.name} · ${f.t("me")}" else m.name, 20.sp, NM.Navy)
            P("${m.shelter} · ${f.t("updatedAgo", "when" to ago(m.at))}${if (!me) g?.via?.get(m.id)?.let { " · " + f.t(if (it == "qr") "viaQr" else "viaOnline") } ?: "" else ""}", 15.sp, NM.Ink2) }
        Pill(f.t("status_${m.status}"), if (m.status == "safe") NM.Sage else NM.Rose, if (m.status == "safe") NM.SageInk else NM.RoseInk)
    }
    Column(Modifier.fillMaxWidth().clip(R22).background(NM.Sage).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompositionLocalProvider(LocalContentColor provides NM.SageInk) {
            Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "family", 22.dp); Spacer(Modifier.width(8.dp)); H(f.t("familyGroup"), 20.sp) }
            P(f.t("familyGroupWhy"), 16.sp)
            P(f.t("yourName"), 14.sp, bold = true); Field(name, { name = it }, f.t("yourName"), Modifier.fillMaxWidth())
            Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(4.dp)) { listOf("safe", "need_help").forEach { st -> val on = (g?.me?.status ?: "safe") == st
                Text(f.t("status_$st"), Modifier.clip(RoundedCornerShape(50)).background(if (on) NM.Navy else Color.Transparent).clickable(enabled = name.isNotBlank()) { f.setMe(name, st); scope.launch { f.syncFamily() } }.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (on) Color.White else NM.Ink2, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(ui, f.t("showMyQr"), "qr", small = true, enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { if (g == null || g.me.name != name.trim()) f.setMe(name, g?.me?.status ?: "safe"); show = !show }
                Btn(ui, f.t("scanFamilyQr"), "camera", BtnStyle.PRIMARY, small = true, enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { if (g == null) f.setMe(name, "safe"); scan() }
            }
            if (show) f.myQr()?.let { q -> Column(Modifier.fillMaxWidth().clip(R22).background(Color.White).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(qr(q, 600).asImageBitmap(), f.t("showMyQr"), Modifier.size(260.dp)); P("${f.familyGroup.value!!.me.name} · ${f.familyGroup.value!!.me.shelter}", 15.sp, NM.Ink2); P(f.t("groupInQr"), 14.sp, NM.Ink2) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Field(paste, { paste = it }, f.t("pasteCode"), Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Btn(ui, f.t("import"), small = true, enabled = paste.isNotBlank()) { got(paste) } }
            if (msg.isNotEmpty()) Note(ui, "info", msg, NM.SageInk)
            g?.let { row(it.me, true); it.members.forEach { m -> row(m, false) }; if (it.members.isEmpty()) P(f.t("noFamilyYet"), 16.sp) }
            Note(ui, "device", f.t("familyOfflineNote"), NM.SageInk)
        }
    }
}

/** Kit -> Help / Pickup / Family: carry the person's own words so nobody types twice. In memory only. */
private var handOff = ""
fun takeHandOff(): String { val t = handOff; handOff = ""; return t }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvAsk(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var ans by remember { mutableStateOf<AskAnswer?>(null) }
    fun ask(text: String) { if (text.isBlank()) return; q = text; busy = true; ans = null; scope.launch { ans = f.askKit(text); busy = false } }
    // The model only picks the intent; these words and buttons are the app's own (rules in code).
    val a = ans
    val dest = if (a?.intent == "where_to_go") f.destination() else null
    val reply = when (a?.intent) {
        null -> ""
        "notice" -> a.answer!!
        "where_to_go" -> dest?.let { f.t("goAnswer", "name" to it.h.name, "m" to it.h.distanceM, "dir" to it.h.direction, "gain" to it.h.handGain, "where" to f.t(if (it.gps) "myLocation" else "theShelter")) } ?: f.t("noHigher")
        "greeting" -> f.t("kitHi"); "help" -> f.t(if (a.urgent) "kitUrgent" else "kitToHelp"); "pickup" -> f.t("kitToPickup"); "family" -> f.t("kitToFamily")
        else -> f.t("kitUnknown")
    }
    data class Go(val to: String, val icon: String, val label: String, val style: BtnStyle)
    val main = when {
        a?.intent == "where_to_go" && dest != null -> Go("way", "nav", f.t("showWay"), BtnStyle.PRIMARY)
        a?.intent == "help" -> Go("help", "help", f.t("needHelp"), BtnStyle.DANGER)
        a?.intent == "pickup" -> Go("pickup", "mobility", f.t("needPickup"), BtnStyle.DANGER)
        a?.intent == "family" -> Go("family", "family", f.t("findFamily"), BtnStyle.PRIMARY)
        else -> null
    }
    EvTop(ui, f.t("askKit"), f.t("askPrivate"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (a == null) Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Kit(ui.kit, if (busy) Mood.THINK else Mood.IDLE, 120.dp)
            H(f.t(if (busy) "kitThinking" else "askKitHello"), 20.sp, NM.Navy, Modifier.soft(R22).padding(horizontal = 20.dp, vertical = 16.dp))
        }
        Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.heightIn(min = 72.dp)) { if (q.isEmpty()) H(f.t("askKitHint"), 24.sp, NM.Ink2.copy(alpha = .5f)); BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = NM.Navy)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { ask(it) }
                Btn(ui, f.t("ask"), "send", BtnStyle.PRIMARY, enabled = q.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { ask(q) }
            }
        }
        if (a != null) Rise(0) { Column(Modifier.fillMaxWidth().soft(R28, NM.Lav).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CompositionLocalProvider(LocalContentColor provides NM.LavInk) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Kit(ui.kit, if (a.source == "cloud") Mood.CLOUD else Mood.DEVICE, 56.dp); Spacer(Modifier.width(10.dp))
                    when {
                        a.source == "rules" && a.intent == "where_to_go" -> Badge(ui, "device", text = f.t("fromFloodMap"))
                        a.source == "rules" -> Badge(ui, "keywords", text = f.t("plainByRules"))
                        a.intent == "notice" -> Badge(ui, a.source)
                        else -> Badge(ui, a.source, text = f.t("kitUnderstood"))
                    }
                }
                H(reply, 24.sp)
                a.card?.let { Row(Modifier.fillMaxWidth().clip(R16).background(Color.White.copy(alpha = .65f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(ui.icons, cardIcon(it), 22.dp); Spacer(Modifier.width(10.dp)); P("${f.t("fromNotice")}: “${it.text}”", 16.sp, bold = true) } }
                SayButton(ui, "ask", reply, NM.LavInk)
                main?.let { m -> Btn(ui, m.label, m.icon, m.style, full = true) { handOff = q.trim(); ui.go(m.to) } }
            }
        } }
        if (!busy) FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(ui, f.t("whereGo"), "higher", small = true) { ask(f.t("whereGo")) }
            listOf(Triple("pickup", "mobility", "needPickup"), Triple("help", "help", "needHelp"), Triple("family", "family", "findFamily"), Triple("home", "announce", "todayNotices"))
                .filter { it.first != main?.to }.forEach { (to, icon, key) -> Btn(ui, f.t(key), icon, small = true) { ui.go(to) } }
        }
    }
}
