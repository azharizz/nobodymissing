package org.nobodymissing.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nobodymissing.NobodyMissingApp
import org.nobodymissing.adapters.FirebaseAiLogicInference
import org.nobodymissing.features.Features
import org.nobodymissing.features.RouteResult

// Same structure, building blocks, words, icons and mascot as the web staff app (web/src/ui/staff.tsx), with a native feel.
// Flat design: no gradients, no coloured side bars.
private const val HOSTED = "https://nobodymissing.web.app/"
private val CARD_ICON = setOf("food", "water", "medicine", "doctor", "move", "warning", "toilet", "info", "baby", "power")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = (application as NobodyMissingApp).features
        val ui = Ui(f, Icons(f.shared), Mascot(f.shared))
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = NM.Accent, background = NM.Bg, surface = NM.Surface, onSurface = NM.Ink),
                typography = Typography(bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = NM.Ink))) { App(ui) }
        }
    }
}

/** Everything a screen needs: features + shared icons + the mascot. */
class Ui(val f: Features, val icons: Icons, val kit: Mascot)

enum class Tab(val icon: String, val key: String) { TODAY("home", "today"), PEOPLE("people", "people"), MEDICINE("medicine", "medicine"), ANNOUNCE("announce", "announce"), FIELD("field", "field"), SETTINGS("settings", "settings") }

@Composable
fun App(ui: Ui) {
    val f = ui.f
    var tab by remember { mutableStateOf(Tab.TODAY) }
    LaunchedEffect(Unit) { while (true) { f.scoutCheck(); delay(1000L * f.rules.scoutIntervalSec) } }
    Box(Modifier.fillMaxSize().background(NM.Bg)) {
        Column(Modifier.fillMaxSize()) {
            StatusBar(ui)
            AnimatedContent(tab, Modifier.weight(1f), transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(450, easing = EaseOutCubic)) { it / 14 }) togetherWith fadeOut(tween(150))
            }, label = "tab") { t ->
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Text(f.t(t.key), fontSize = 32.sp, fontWeight = FontWeight.Black, color = NM.Ink, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    Box(Modifier.weight(1f)) {
                        when (t) {
                            Tab.TODAY -> Today(ui) { tab = it }
                            Tab.PEOPLE -> People(ui)
                            Tab.MEDICINE -> Medicine(ui)
                            Tab.ANNOUNCE -> Announce(ui)
                            Tab.FIELD -> Field(ui)
                            Tab.SETTINGS -> Settings(ui)
                        }
                    }
                }
            }
            NavBar(ui, tab) { tab = it }
            Counter(ui)
        }
        AnimatedVisibility(f.scoutAlert.value, enter = fadeIn(), exit = fadeOut()) { ScoutAlert(ui) }
    }
}

/** Floating dark pill with an orange indicator that springs to the selected tab. */
@Composable
fun NavBar(ui: Ui, tab: Tab, go: (Tab) -> Unit) {
    BoxWithConstraints(Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(NM.Ink).padding(6.dp)) {
        val itemW = maxWidth / Tab.entries.size
        val x by animateDpAsState(itemW * tab.ordinal, spring(dampingRatio = .62f, stiffness = 380f), label = "indicator")
        Box(Modifier.offset(x = x).width(itemW).height(58.dp).clip(RoundedCornerShape(20.dp)).background(NM.Accent))
        Row {
            Tab.entries.forEach { t ->
                val on = t == tab
                val s by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = .4f), label = "pop")
                Column(Modifier.width(itemW).height(58.dp).clip(RoundedCornerShape(20.dp)).clickable { go(t) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.scale(s)) { Ic(ui.icons, t.icon, 22.dp, if (on) Color.White else Color.White.copy(alpha = .72f), ui.f.t(t.key)) }
                    if (t != Tab.SETTINGS) Text(ui.f.t(t.key), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else Color.White.copy(alpha = .72f), maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun Counter(ui: Ui) {
    val f = ui.f
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        listOf("device" to f.router.deviceCount, "cloud" to f.router.cloudCount, "clock" to f.jobs.count { it.status.state != "sent" }).forEach { (ic, n) ->
            Ic(ui.icons, ic, 13.dp, NM.Ink2); Text(" $n   ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NM.Ink2)
        }
    }
}

@Composable
fun Chip(ui: Ui, icon: String, text: String, bg: Color = NM.Surface, border: Color = NM.Line, fg: Color = NM.Ink) =
    Row(Modifier.clip(RoundedCornerShape(50)).background(bg).border(1.5.dp, border, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(ui.icons, icon, 14.dp, fg); Spacer(Modifier.width(5.dp)); Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg)
    }

/** Status = icon + word, never colour alone. Kit lives here. */
@Composable
fun StatusBar(ui: Ui) {
    val f = ui.f
    val waiting = f.jobs.count { it.status.state != "sent" }
    val back = f.signal.value == "back"
    Column(Modifier.fillMaxWidth().background(NM.Bg).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kit(ui.kit, if (back) (if (waiting > 0) Mood.CLOUD else Mood.IDLE) else Mood.DEVICE, 50.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (f.shelterId.value == "shelter-1") "Shelter" else "Shelter ${f.shelterId.value}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(if (back) f.t("syncing") else f.t("noSignal"), fontSize = 12.sp, color = NM.Muted, maxLines = 1)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (back) Chip(ui, "signal", f.t("cloud"), NM.CloudSoft, NM.Cloud, Color(0xFF1A4699)) else Chip(ui, "device", f.t("device"), NM.DeviceSoft, NM.Device, Color(0xFF11603D))
            if (waiting > 0) Chip(ui, "clock", "${f.t("waiting")} $waiting")
            if (f.privateMode.value) Chip(ui, "lock", "Private")
            if (f.adapters.env == "test") Chip(ui, "demo", f.t("demoBadge"), NM.ReviewSoft, NM.Review)
        }
    }
    HorizontalDivider(color = NM.Line)
}

@Composable
fun Badge(ui: Ui, source: String?, draft: Boolean = false) {
    val f = ui.f
    when {
        draft -> Chip(ui, "alert", f.t("review"), NM.ReviewSoft, NM.Review, Color(0xFF7A4F0E))
        source == "device" -> Chip(ui, "device", f.t("device"), NM.DeviceSoft, NM.Device, Color(0xFF11603D))
        source == "cloud" -> Chip(ui, "cloud", f.t("cloud"), NM.CloudSoft, NM.Cloud, Color(0xFF1A4699))
        source == "keywords" -> Chip(ui, "keywords", f.t("keywords"), NM.Surface2, NM.Line, NM.Ink2)
    }
}

@Composable
fun Btn(ui: Ui, label: String, icon: String? = null, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true, full: Boolean = false, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .96f else 1f, spring(dampingRatio = .5f), label = "press")
    val fg = when { primary -> Color.White; danger -> NM.Danger; else -> NM.Ink }
    Button(onClick, (if (full) Modifier.fillMaxWidth() else Modifier).heightIn(min = 52.dp).scale(s), enabled = enabled, interactionSource = src, shape = RoundedCornerShape(16.dp),
        border = if (primary) null else BorderStroke(1.5.dp, if (danger) NM.Danger.copy(alpha = .4f) else NM.Line),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) NM.Accent else NM.Surface, contentColor = fg, disabledContainerColor = NM.Line)) {
        icon?.let { Ic(ui.icons, it, 20.dp, fg); Spacer(Modifier.width(8.dp)) }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun Card(danger: Boolean = false, review: Boolean = false, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp).shadow(if (danger || review) 0.dp else 6.dp, shape, ambientColor = NM.Ink.copy(alpha = .08f), spotColor = NM.Ink.copy(alpha = .08f))
        .clip(shape).background(when { danger -> NM.DangerSoft; review -> NM.ReviewSoft; else -> NM.Surface })
        .then(if (danger) Modifier.border(1.5.dp, NM.Danger, shape) else if (review) Modifier.border(1.5.dp, NM.Review, shape) else Modifier)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
fun Title(ui: Ui, icon: String, text: String) = Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, icon, 20.dp); Spacer(Modifier.width(8.dp)); Text(text, fontWeight = FontWeight.Black, fontSize = 17.sp) }

@Composable
fun Empty(ui: Ui, text: String) = Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Kit(ui.kit, Mood.SLEEP, 110.dp); Spacer(Modifier.height(8.dp)); Text(text, color = NM.Ink2, textAlign = TextAlign.Center) }

/** Every list item rises into place, staggered. */
@Composable
fun Rise(i: Int, content: @Composable () -> Unit) {
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(shown, enter = fadeIn(tween(400, delayMillis = 55 * minOf(i, 8))) + slideInVertically(tween(500, delayMillis = 55 * minOf(i, 8), easing = EaseOutCubic)) { it / 5 }) { content() }
}

@Composable
fun Tile(ui: Ui, icon: String, n: Any, label: String, danger: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "beat")
    val beat by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "beat")
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.height(128.dp).shadow(if (danger) 0.dp else 6.dp, shape).clip(shape).background(if (danger) NM.DangerSoft else NM.Surface).clickable(onClick = onClick).padding(16.dp)) {
        Box(Modifier.size(44.dp).scale(if (danger) beat else 1f).clip(RoundedCornerShape(14.dp)).background(if (danger) NM.Danger else NM.Surface2), contentAlignment = Alignment.Center) {
            Ic(ui.icons, icon, 24.dp, if (danger) Color.White else NM.Ink)
        }
        Spacer(Modifier.weight(1f))
        Text("$n", fontSize = 32.sp, fontWeight = FontWeight.Black, color = if (danger) NM.Danger else NM.Ink, lineHeight = 32.sp)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NM.Ink2, maxLines = 1)
    }
}

@Composable
fun Today(ui: Ui, go: (Tab) -> Unit) {
    val f = ui.f
    val s = f.schedule()
    val next = s.counts.entries.firstOrNull()
    val p1 = f.fieldList().count { it.second.result.tier == "P1" }
    val tiles = listOf(
        Triple("medicine", next?.value ?: 0, "${f.t("nextRound")} ${next?.key ?: ""}") to (Tab.MEDICINE to false),
        Triple("alert", s.alerts.size, f.t("cantMiss")) to (Tab.MEDICINE to s.alerts.isNotEmpty()),
        Triple("announce", f.reach(f.announcements.firstOrNull()).remaining, f.t("toTell")) to (Tab.ANNOUNCE to false),
        Triple("field", p1, f.t("p1Visits")) to (Tab.FIELD to (p1 > 0)),
        Triple("help", 0, f.t("help")) to (Tab.PEOPLE to false),
        Triple("people", f.people.size, f.t("people")) to (Tab.PEOPLE to false),
    )
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (f.signal.value == "no_signal" && f.lastSignalAt.value == null) item {
            Rise(0) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NM.AccentSoft).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Kit(ui.kit, Mood.DEVICE, 80.dp); Spacer(Modifier.width(12.dp))
                Column { Text(f.t("noSignal"), fontWeight = FontWeight.Bold); Text(f.t("airplaneTip"), fontSize = 13.sp, color = NM.Ink2) } } }
        }
        tiles.chunked(2).forEachIndexed { r, row ->
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { c, (t, nav) -> Box(Modifier.weight(1f)) { Rise(r * 2 + c) { Tile(ui, t.first, t.second, t.third, nav.second, Modifier.fillMaxWidth()) { go(nav.first) } } } }
            } }
        }
        if (f.people.isEmpty()) item { Empty(ui, f.t("emptyShelter")) }
    }
}

@Composable
fun RouteNotice(ui: Ui, r: RouteResult?, onCloud: () -> Unit) {
    val f = ui.f
    @Composable fun line(icon: String, text: String, color: Color = NM.Ink) = Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, icon, 20.dp, color); Spacer(Modifier.width(10.dp)); Text(text, color = color) }
    when (r?.status) {
        "ask_staff" -> Card(review = true) { line("cloud", f.t("askCloud")); Btn(ui, f.t("sendCloud"), "send", primary = true, full = true, onClick = onCloud) }
        "draft" -> Card(review = true) { line("alert", f.t("review")) }
        "queued" -> Card { line("clock", f.t("queued")) }
        "blocked" -> Card { line("lock", f.t("blocked")) }
        "invalid", "error" -> Card(review = true) { line("alert", f.t("invalid"), NM.Danger) }
        else -> {}
    }
}

@Composable
fun CameraButton(ui: Ui, label: String, onPhoto: (Bitmap) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(onPhoto) }
    Box(Modifier.padding(vertical = 8.dp)) { Btn(ui, label, "camera", primary = true, full = true) { launcher.launch(null) } }
}

/** The original photo is always shown beside the AI result. */
@Composable
fun Source(ui: Ui, bmp: Bitmap, scanning: Boolean = false) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = EaseInOutCubic)), label = "y")
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
        Image(bmp.asImageBitmap(), ui.f.t("original"), Modifier.fillMaxWidth().heightIn(max = 300.dp))
        if (scanning) Box(Modifier.offset(y = maxHeight * y).fillMaxWidth().height(4.dp).background(NM.Accent))
        Text(ui.f.t("original"), Modifier.padding(12.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .6f)).padding(horizontal = 10.dp, vertical = 3.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

private val FLAGS = listOf("hard_of_hearing", "deaf", "needs_meds", "wheelchair", "cant_walk", "lives_alone", "foreign_language", "infant", "pregnant", "elderly", "dementia")

@Composable
fun Avatar(name: String) = Box(Modifier.size(40.dp).clip(CircleShape).background(NM.AccentSoft), contentAlignment = Alignment.Center) {
    Text(name.replace(Regex("^(Pak|Ibu|Bu|Bapak)\\s+", RegexOption.IGNORE_CASE), "").take(1), color = NM.Accent2, fontWeight = FontWeight.Black)
}

@Composable
fun People(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    var q by remember { mutableStateOf("") }
    Column {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            photo?.let { item { Source(ui, it, busy) } }
            item { RouteNotice(ui, r) { scope.launch { busy = true; r = f.scanRoster(photo!!, cloudOk = true); busy = false } } }
            itemsIndexed(f.people.filter { !it.confirmed }) { i, p ->
                Rise(i) {
                    Card(review = p.needsReview) {
                        Badge(ui, p.source, p.needsReview)
                        var flags by remember(p.id) { mutableStateOf(p.flags) }
                        Text("${p.name}, ${p.age ?: "?"}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(FLAGS) { fl -> FilterChip(fl in flags, { flags = if (fl in flags) flags - fl else flags + fl }, { Text(fl.replace('_', ' ')) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NM.Ink, selectedLabelColor = Color.White)) }
                        }
                        Btn(ui, f.t("confirm"), "check", primary = true, full = true) { f.confirmPerson(p.id, flags) }
                    }
                }
            }
            item { OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), leadingIcon = { Ic(ui.icons, "search") }, placeholder = { Text(f.t("familySearchLocal")) }, shape = RoundedCornerShape(16.dp)) }
            if (q.isNotBlank()) items(f.familySearch(q)) { h -> Card { Row(verticalAlignment = Alignment.CenterVertically) { Avatar(h.name); Spacer(Modifier.width(12.dp)); Column { Text(h.name, fontWeight = FontWeight.Bold); Text("${h.ageBand} · ${h.shelterId} · ${h.status}", fontSize = 13.sp, color = NM.Muted) } } } }
            itemsIndexed(f.people.filter { it.confirmed }) { i, p ->
                Rise(i) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NM.Surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p.name); Spacer(Modifier.width(12.dp))
                    Column { Text(p.name, fontWeight = FontWeight.Bold); Text("${p.age ?: "?"} · ${p.flags.joinToString { it.replace('_', ' ') }}", fontSize = 13.sp, color = NM.Muted) }
                } }
            }
        }
        CameraButton(ui, f.t("scanList")) { bmp -> photo = bmp; busy = true; scope.launch { r = f.scanRoster(bmp); busy = false } }
    }
}


@Composable
fun Medicine(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    val s = f.schedule()
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    var pid by remember { mutableStateOf(f.people.firstOrNull()?.id) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    Column {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(s.alerts) { i, a ->
                Rise(i) { Card(danger = true) { Row(verticalAlignment = Alignment.CenterVertically) {
                    val t = rememberInfiniteTransition(label = "dot"); val k by t.animateFloat(1f, 1.4f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "k")
                    Box(Modifier.size(14.dp).scale(k).clip(CircleShape).background(NM.Danger)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(f.t("cantMiss"), fontSize = 12.sp, color = NM.Muted); Text("${name(a.personId)} · ${a.generic}", fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${a.supplyDays ?: "?"}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = NM.Danger); Text(f.t("days"), fontSize = 12.sp, color = NM.Muted) }
                } } }
            }
            if (s.slots.isEmpty()) item { Empty(ui, f.t("noMeds")) }
            s.slots.entries.forEachIndexed { si, (slot, list) ->
                item {
                    Rise(si + s.alerts.size) { Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(slot, fontSize = 28.sp, fontWeight = FontWeight.Black); Spacer(Modifier.width(10.dp))
                            Text("${list.size}", Modifier.clip(RoundedCornerShape(50)).background(NM.Ink).padding(horizontal = 10.dp, vertical = 2.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        list.forEach { e ->
                            val key = "$slot|${e.personId}|${e.generic}"
                            val st = f.round[key]
                            HorizontalDivider(color = NM.Line)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(name(e.personId), fontWeight = FontWeight.Bold); Text("${e.generic} · ${e.doseAsWritten ?: ""}", fontSize = 13.sp, color = NM.Muted) }
                                Row(Modifier.clip(RoundedCornerShape(14.dp)).background(NM.Surface2).border(1.5.dp, NM.Line, RoundedCornerShape(14.dp)).padding(3.dp)) {
                                    listOf(Triple("taken", "check", NM.Device), Triple("refused", "x", NM.Danger), Triple("missing", "missing", NM.Danger)).forEach { (v, ic, col) ->
                                        val on = st == v
                                        val bg by animateColorAsState(if (on) col else Color.Transparent, label = "seg")
                                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(bg).clickable { f.round[key] = v }, contentAlignment = Alignment.Center) { Ic(ui.icons, ic, 18.dp, if (on) Color.White else NM.Ink, f.t(if (v == "taken") "given" else v)) }
                                    }
                                }
                            }
                        }
                    } }
                }
            }
            if (s.shortages.isNotEmpty()) item {
                Card {
                    Title(ui, "package", f.t("stock"))
                    s.shortages.forEach { Text(f.t("outOf", "n" to it.people, "drug" to "${it.generic} ${it.strength ?: ""}", "days" to f.rules.shortageDays)) }
                    Btn(ui, f.t("requestSupplies"), "send", primary = true, full = true) { f.requestSupply() }
                    Text(f.t("countsOnly"), color = NM.Muted, fontSize = 13.sp)
                }
            }
            photo?.let { item { Source(ui, it) } }
            item { RouteNotice(ui, r) { scope.launch { r = f.readMeds(pid!!, photo!!, cloudOk = true) } } }
            itemsIndexed(f.meds.withIndex().filter { it.value.confirmedBy == null }) { _, (i, m) ->
                Card(review = true) {
                    Badge(ui, null, draft = true)
                    Text("${name(m.personId)}: “${m.nameAsWritten}” ${m.strength ?: ""} · ${m.dose ?: ""} · ${m.timesPerDay ?: "?"}×", fontWeight = FontWeight.Bold)
                    if (m.match.lookAlikes.isNotEmpty()) Text(f.t("lookAlike", "names" to m.match.lookAlikes.joinToString()), color = Color(0xFF8A4B00), fontWeight = FontWeight.Bold)
                    if (m.match.strengthKnown == false) Text(f.t("strengthUnknown"), color = Color(0xFF8A4B00), fontWeight = FontWeight.Bold)
                    if (m.match.status == "unknown") Text(f.t("unknownName"), color = Color(0xFF8A4B00), fontWeight = FontWeight.Bold)
                    (listOfNotNull(m.match.generic) + m.match.candidates).forEach { g ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Btn(ui, "$g · ${f.t("evacuees")}", "check") { f.confirmMed(i, g, "self") }; Btn(ui, f.t("medic"), "doctor") { f.confirmMed(i, g, "medic") } }
                    }
                }
            }
            item { Text(f.t("person"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(f.people) { p -> FilterChip(pid == p.id, { pid = p.id }, { Text(p.name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NM.Ink, selectedLabelColor = Color.White)) } } }
        }
        CameraButton(ui, f.t("medicinePhoto")) { bmp -> photo = bmp; pid?.let { scope.launch { r = f.readMeds(it, bmp) } } }
    }
}

@Composable
fun KCards(ui: Ui, cards: List<org.nobodymissing.features.UiCard>) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    cards.forEachIndexed { i, c ->
        val ic = if (c.icon in CARD_ICON) c.icon else "info"
        val (bg, fg) = when (ic) { "water" -> NM.CloudSoft to NM.Cloud; "warning", "move" -> NM.DangerSoft to NM.Danger; "doctor", "medicine" -> NM.DeviceSoft to NM.Device; else -> NM.AccentSoft to NM.Accent2 }
        Rise(i) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(2.5.dp, NM.Ink, RoundedCornerShape(18.dp)).background(NM.Surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) { Ic(ui.icons, ic, 30.dp, fg) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(c.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
                c.time?.let { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "clock", 16.dp, NM.Ink2); Text(" $it", fontWeight = FontWeight.Bold, color = NM.Ink2) } }
                c.place?.let { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "pin", 16.dp, NM.Ink2); Text(" $it", fontWeight = FontWeight.Bold, color = NM.Ink2) } }
            }
        } }
    }
}

@Composable
fun Announce(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val a = f.announcements.firstOrNull()
    val reach = f.reach(a)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Card {
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("announcementHint")) }, minLines = 3, shape = RoundedCornerShape(14.dp))
                Btn(ui, if (busy) "…" else f.t("makeCards"), "sparkle", primary = true, full = true, enabled = text.isNotBlank() && !busy) { busy = true; scope.launch { f.announce(text.trim(), f.lang.value); text = ""; busy = false } }
            }
        }
        if (a != null) item(key = a.id) {
            Rise(0) { Card {
                Badge(ui, a.source, a.draft)
                Text("${f.t("original")}: “${a.text}”", color = NM.Muted, fontSize = 14.sp)
                Kit(ui.kit, if (a.source == "cloud") Mood.CLOUD else Mood.DEVICE, 90.dp)
                val cards = if (a.source == "keywords") a.cards.map { it.copy(title = a.text.split(Regex("\\s+")).take(4).joinToString(" ")) } else a.cards
                KCards(ui, cards)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (a.shown) Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "check", 20.dp, NM.Device); Text(" ${f.t("shown")}", color = NM.Device, fontWeight = FontWeight.Bold) }
                    else Btn(ui, f.t("show"), "send", primary = true) { f.approve(a.id) }
                }
                Text(f.t("tellInPerson", "n" to reach.remaining), fontWeight = FontWeight.Black, fontSize = 18.sp)
                val done = if (reach.list.isEmpty()) 0f else (reach.list.size - reach.remaining).toFloat() / reach.list.size
                val prog by animateFloatAsState(done, tween(600), label = "prog")
                LinearProgressIndicator({ prog }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)), color = NM.Device, trackColor = NM.Surface2)
                reach.list.forEach { e -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { f.toggleTold(a.id, e.id) }.padding(vertical = 4.dp)) {
                    Checkbox(e.told, { f.toggleTold(a.id, e.id) }, colors = CheckboxDefaults.colors(checkedColor = NM.Device))
                    Column { Text(f.people.firstOrNull { it.id == e.id }?.name ?: e.id, fontWeight = FontWeight.Bold); Text(e.reasons.joinToString { it.replace('_', ' ') }, fontSize = 13.sp, color = NM.Muted) }
                } }
            } }
        }
    }
}

@Composable
fun Field(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    val list = f.fieldList()
    Column {
        LazyColumn(Modifier.weight(1f)) {
            item { Text(f.t("hazardNote", "event" to f.rules.currentEvent), color = NM.Muted, fontSize = 13.sp) }
            if (list.isEmpty()) item { Empty(ui, f.t("allChecked")) }
            itemsIndexed(list) { i, (name, row) ->
                val e = f.fieldEntry(row.id)
                val tierColor = when (row.result.tier) { "P1" -> NM.Danger; "P2" -> NM.Accent; else -> NM.Surface2 }
                Rise(i) { Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.result.tier, Modifier.clip(RoundedCornerShape(10.dp)).background(tierColor).padding(horizontal = 10.dp, vertical = 4.dp), color = if (row.result.tier == "P3") NM.Ink else Color.White, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(10.dp)); Text(name, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        if (e.needsReview) Badge(ui, null, true)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(row.result.why) { w -> Text(w, Modifier.clip(RoundedCornerShape(50)).background(NM.Surface2).border(1.dp, NM.Line, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                    f.higherPlace(e)?.let { h -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NM.DeviceSoft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Ic(ui.icons, "higher", 18.dp, NM.Device); Spacer(Modifier.width(8.dp)); Text("${f.t("higherPlace")}: ${h.name} · ${h.distanceM} m ${h.direction} · +${h.handGain} m", color = Color(0xFF11603D), fontSize = 14.sp) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Btn(ui, f.t("safeAtHome"), "check") { f.visited(row.id, "safe_home") }
                        Btn(ui, "", "evacuate", danger = true) { f.visited(row.id, "needs_evacuation") }
                        Btn(ui, f.t("override"), "override") { f.override(row.id, if (row.result.tier == "P1") "P3" else "P1", "staff decision") }
                    }
                    Text(e.status.replace('_', ' '), color = NM.Muted, fontSize = 12.sp)
                } }
            }
            item { RouteNotice(ui, r) { scope.launch { r = f.fieldReport(note, null, null, cloudOk = true) } } }
            item { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("fieldHint")) }, shape = RoundedCornerShape(16.dp)) }
        }
        Box(Modifier.padding(vertical = 8.dp)) { Btn(ui, f.t("report"), "send", primary = true, full = true, enabled = note.isNotBlank()) { scope.launch { r = f.fieldReport(note, null, null); if (r?.status == "ok") note = "" } } }
    }
}

fun qr(text: String, size: Int = 400): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) for (y in 0 until size) setPixel(x, y, if (m[x, y]) android.graphics.Color.parseColor("#14213D") else android.graphics.Color.WHITE)
    }
}

@Composable
fun Settings(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var chunks by remember { mutableStateOf<List<String>?>(null) }
    var paste by remember { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Rise(0) { Card {
            Title(ui, "download", f.t("shelterReady"))
            Btn(ui, f.t("prepareDevice"), "device", primary = true, full = true) {
                scope.launch {
                    val inf = f.adapters.inference
                    if (inf is FirebaseAiLogicInference) runCatching { inf.download().collect { status = f.t("onDeviceModel", "status" to it.toString()) } }
                    status = f.t("onDeviceModel", "status" to inf.onDeviceStatus())
                }
            }
            if (status.isNotEmpty()) Text(status, color = NM.Muted)
        } } }
        item { Rise(1) { Card {
            Title(ui, "globe", f.t("language"))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NM.Surface2).border(1.5.dp, NM.Line, RoundedCornerShape(14.dp)).padding(4.dp)) {
                listOf("en" to "English", "id" to "Bahasa Indonesia").forEach { (k, l) ->
                    val on = f.lang.value == k
                    val bg by animateColorAsState(if (on) NM.Ink else Color.Transparent, label = "lang")
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { f.lang.value = k }, contentAlignment = Alignment.Center) { Text(l, color = if (on) Color.White else NM.Ink, fontWeight = FontWeight.Bold) }
                }
            }
        } } }
        item { Rise(2) { Card { Row(verticalAlignment = Alignment.CenterVertically) {
            Ic(ui.icons, "lock"); Spacer(Modifier.width(10.dp)); Text(f.t("privateMode"), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Switch(f.privateMode.value, { f.privateMode.value = it }, colors = SwitchDefaults.colors(checkedTrackColor = NM.Ink))
        } } } }
        item { Rise(3) { Card {
            Title(ui, "signal", f.t("signalScout"))
            Text("${f.t("signal")}: ${f.signal.value.replace('_', ' ')} · ${f.t("lastSeen")}: ${f.lastSignalAt.value?.let { java.text.DateFormat.getTimeInstance().format(it) } ?: f.t("never")}", color = NM.Muted, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Btn(ui, f.t("checkNow"), "signal") { scope.launch { f.scoutCheck() } }; Btn(ui, f.t("syncNow"), "sync") { scope.launch { f.flush() } } }
            f.jobs.takeLast(8).forEach { j -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(when (j.status.state) { "sent" -> NM.Device; "failed" -> NM.Danger; "sending", "processing" -> NM.Cloud; else -> NM.Line }))
                Spacer(Modifier.width(10.dp)); Text(j.type.replace('_', ' '), Modifier.weight(1f), fontSize = 14.sp); Text(j.status.state, color = NM.Muted, fontSize = 12.sp)
            } }
        } } }
        item { Rise(4) { Card {
            Title(ui, "qr", f.t("courier"))
            Btn(ui, f.t("exportQueue"), "qr") { chunks = f.exportChunks() }
            chunks?.let { cs -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(cs.size) { i -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Image(qr(cs[i]).asImageBitmap(), "${f.t("courier")} ${i + 1}/${cs.size}", Modifier.size(200.dp).clip(RoundedCornerShape(14.dp))); Text("${i + 1}/${cs.size}", fontWeight = FontWeight.Bold) } } } }
            OutlinedTextField(paste, { paste = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("importCodes")) }, minLines = 2, shape = RoundedCornerShape(14.dp))
            Btn(ui, f.t("import"), "download") { status = f.t("imported", "n" to f.importChunks(paste.lines().filter { it.isNotBlank() })); paste = "" }
        } } }
        item { Rise(5) { Card {
            Title(ui, "people", f.t("joinQrs"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("volunteer" to "#/staff/today?join=${f.shelterId.value}&role=volunteer", "medic" to "#/staff/today?join=${f.shelterId.value}&role=medic", "evacuees" to "#/public/news").forEach { (k, path) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Image(qr(HOSTED + path, 240).asImageBitmap(), f.t(k), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))); Text(f.t(k), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        } } }
        item { Rise(6) { Card { Btn(ui, f.t("loadDemo"), "demo", full = true) { f.loadDemo() } } } }
    }
}

/** Full-screen Kit: "Signal is back here!" (6.10 level 1). Kit pops up, lantern flashing, ripples, vibration. */
@Composable
fun ScoutAlert(ui: Ui) {
    val f = ui.f
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        @Suppress("DEPRECATION")
        (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = 220f)) }
    val t = rememberInfiniteTransition(label = "ripple")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "r")
    val sent = f.jobs.count { it.status.state == "sent" }
    Box(Modifier.fillMaxSize().background(NM.Accent).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        listOf(0f, 1f / 3, 2f / 3).forEach { o ->
            val p = (ripple + o) % 1f
            Box(Modifier.size(200.dp).scale(.4f + 3.6f * p).border(3.dp, Color.White.copy(alpha = (1f - p) * .5f), CircleShape))
        }
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.scale(pop.value).offset(y = (60 * (1 - pop.value)).dp)) { Kit(ui.kit, Mood.ALERT, 200.dp) }
            Spacer(Modifier.height(16.dp))
            Text(f.t("signalBack"), fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp, color = Color.White, textAlign = TextAlign.Center)
            Text(f.t("sentOf", "sent" to sent, "total" to f.jobs.size, "waiting" to f.jobs.size - sent), fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button({ f.scoutAlert.value = false }, colors = ButtonDefaults.buttonColors(containerColor = NM.Ink), shape = RoundedCornerShape(16.dp), modifier = Modifier.heightIn(min = 52.dp).widthIn(min = 160.dp)) {
                Ic(ui.icons, "check", 20.dp, Color.White); Spacer(Modifier.width(8.dp)); Text("OK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
