package org.nobodymissing.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import org.nobodymissing.features.RouteResult
import org.nobodymissing.domain.strings

// Same screens, words, icons, colours and mascot as the web app (web/src/ui/staff.tsx + public.tsx). Design: docs/design/index.html.
private const val HOSTED = "https://nobodymissing.web.app/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = (application as NobodyMissingApp).features
        val ui = Ui(f, Icons(f.shared), Mascot(f.shared), Speaker(this))
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = NM.Navy, background = NM.Ivory, surface = NM.Card, onSurface = NM.Ink),
                typography = Typography(bodyLarge = TextStyle(fontFamily = Atkinson, fontSize = 17.sp, color = NM.Ink))) {
                CompositionLocalProvider(LocalContentColor provides NM.Ink) { App(ui) }
            }
        }
    }
}

@Composable
fun App(ui: Ui) {
    val f = ui.f
    LaunchedEffect(Unit) { while (true) { f.scoutCheck(); delay(1000L * f.rules.scoutIntervalSec) } }
    val r = ui.route.value
    BackHandler(r.name != "now" && r.name != "home") { ui.go(if (ui.evacueeMode.value) "home" else "now") }
    Box(Modifier.fillMaxSize().background(NM.Ivory)) {
        if (ui.evacueeMode.value) Evacuee(ui) else {
            AnimatedContent(r, Modifier.fillMaxSize(), transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(450, easing = EaseOutCubic)) { it / 18 }) togetherWith fadeOut(tween(120))
            }, label = "screen") { s ->
                when (s.name) {
                    "camera" -> Camera(ui, s.arg)
                    "map" -> FieldMap(ui, s.arg)
                    "announce" -> Announce(ui)
                    "checkin" -> StaffCheckIn(ui)
                    "people" -> People(ui)
                    "person" -> CareCard(ui, s.arg!!)
                    "review" -> Review(ui)
                    "round" -> Round(ui)
                    "settings" -> Settings(ui)
                    else -> Now(ui)
                }
            }
            if (r.name != "camera") TabBar(ui, r.name, Modifier.align(Alignment.BottomCenter))
            AnimatedVisibility(f.scoutAlert.value, enter = fadeIn(), exit = fadeOut()) { ScoutAlert(ui) }
        }
    }
}

/** Floating white bar; the camera is the raised centre button. */
@Composable
fun TabBar(ui: Ui, on: String, modifier: Modifier = Modifier) {
    val f = ui.f
    Box(modifier.padding(12.dp).fillMaxWidth().height(96.dp), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.fillMaxWidth().height(76.dp).shadow(14.dp, RoundedCornerShape(28.dp), spotColor = NM.Navy.copy(alpha = .2f)).clip(RoundedCornerShape(28.dp)).background(NM.Card), verticalAlignment = Alignment.CenterVertically) {
            @Composable fun tab(k: String, icon: String, label: String) {
                val sel = on == k || (k == "people" && on == "person") || (k == "now" && on == "round")
                Column(Modifier.weight(1f).fillMaxHeight().clickable { ui.go(k) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Ic(ui.icons, icon, 22.dp, if (sel) NM.Navy else NM.Ink2); Text(label, color = if (sel) NM.Navy else NM.Ink2, fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            tab("now", "home", f.t("tabNow")); tab("map", "map", f.t("map")); Spacer(Modifier.weight(1.25f)); tab("checkin", "door", f.t("checkIn")); tab("people", "people", f.t("people"))
        }
        Box(Modifier.size(74.dp).shadow(12.dp, CircleShape, spotColor = NM.Navy.copy(alpha = .4f)).clip(CircleShape).background(NM.Navy).border(6.dp, NM.Ivory, CircleShape)
            .clickable { ui.go("camera") }.align(Alignment.TopCenter), contentAlignment = Alignment.Center) { Ic(ui.icons, "camera", 30.dp, Color.White, f.t("tabCamera")) }
    }
}

/** A staff screen: header + scrollable content with room for the tab bar. */
@Composable
fun Screen(header: @Composable () -> Unit, content: LazyListScope.() -> Unit) =
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { header() }
        content()
    }
fun LazyListScope.pad(key: Any? = null, content: @Composable () -> Unit) = item(key) { Box(Modifier.padding(horizontal = 20.dp)) { content() } }

@Composable
fun Top(ui: Ui, title: String, back: String? = "now", trailing: @Composable () -> Unit = {}) =
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        back?.let { RoundBtn(ui, "back", ui.f.t("back")) { ui.go(it) }; Spacer(Modifier.width(12.dp)) }
        H(title, 26.sp, NM.Navy, Modifier.weight(1f)); trailing()
    }

@Composable
fun Hybrid(ui: Ui) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    Ic(ui.icons, "device", 15.dp, NM.Ink2); P(" " + ui.f.t("onThisPhone", "n" to ui.f.router.deviceCount) + "    ", 13.sp, NM.Ink2)
    Ic(ui.icons, "cloud", 15.dp, NM.Ink2); P(" " + ui.f.t("cloudN", "n" to ui.f.router.cloudCount), 13.sp, NM.Ink2)
}

/** One task row on the Now list. */
@Composable
fun Task(ui: Ui, icon: String, tone: NM.Tone?, title: String, sub: String?, tag: String? = null, act: String = "chevron", onAct: (() -> Unit)? = null, onClick: (() -> Unit)? = null) =
    Row(Modifier.fillMaxWidth().soft(R22).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(tone?.bg ?: NM.Quiet), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 22.dp, tone?.acc ?: NM.Ink) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            tag?.let { Eyebrow(it, NM.Urgent) }
            H(title, 17.sp); sub?.let { P(it, 15.sp, NM.Ink2) }
        }
        if (onAct != null) Box(Modifier.size(52.dp).clip(R16).border(1.5.dp, NM.Border, R16).clickable(onClick = onAct), contentAlignment = Alignment.Center) { Ic(ui.icons, act, 22.dp) }
        else Ic(ui.icons, act, 22.dp, NM.Ink2)
    }

// ---------- Now ----------
@Composable
fun Now(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var more by remember { mutableStateOf(false) }
    val sch = f.schedule()
    val back = f.signal.value == "back"
    val waiting = f.jobs.count { it.status.state != "sent" }
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    val p1 = f.fieldList().filter { it.second.result.tier == "P1" && f.fieldEntry(it.second.id).status == "unchecked" }
    val slot = sch.slots.keys.firstOrNull()
    val given = slot?.let { s -> sch.slots[s]!!.count { f.round["$s|${it.personId}|${it.generic}"] == "taken" } } ?: 0
    val toCheck = f.people.count { !it.confirmed }
    val tasks = buildList<@Composable () -> Unit> {
        // Urgent evacuee requests first, then can't-miss medicine.
        f.help.filter { !it.done }.sortedByDescending { it.urgent }.forEach { h -> add { Task(ui, if (h.urgent) "alert" else if (h.kind == "write") "chat" else "help", if (h.kind == "write" && !h.urgent) NM.LAV else NM.ROSE,
            f.t("asksFor", "spot" to (if (h.spot.isBlank() || h.spot == "?") f.t("someone") else h.spot), "kind" to f.t("kind_${h.kind}")), listOfNotNull(h.summary?.let { s -> s + if (h.by == "device") " · " + f.t("readsOnPhone") else "" }, h.text?.takeIf { it != h.summary }?.let { "“$it”" }).joinToString("\n").ifEmpty { null },
            tag = if (h.urgent) f.t("urgent") else null, act = "check", onAct = { f.helpDone(h.id) }) } }
        sch.alerts.forEach { a -> add { Task(ui, "medicine", NM.ROSE, "${name(a.personId)} · ${a.generic}", f.t("onlyDaysLeft", "n" to (a.supplyDays ?: "?")), f.t("cantMiss")) { ui.go("person", a.personId) } } }
        sch.alerts.forEach { a -> add { Task(ui, "medicine", NM.ROSE, "${name(a.personId)} · ${a.generic}", f.t("onlyDaysLeft", "n" to (a.supplyDays ?: "?")), f.t("cantMiss")) { ui.go("person", a.personId) } } }
        p1.forEach { (n, r) -> add { Task(ui, "field", NM.ROSE, n, r.result.why.joinToString(" · "), f.t("visitFirst")) { ui.go("map", r.id) } } }
        slot?.let { s -> add { Task(ui, "clock", NM.HONEY, f.t("roundAt", "slot" to s), f.t("roundSub", "n" to sch.slots[s]!!.size, "given" to given)) { ui.go("round") } } }
        if (toCheck > 0) add { Task(ui, "people", NM.SKY, f.t("checkList"), f.t("toCheck", "n" to toCheck)) { ui.go("review") } }
        if (!back && f.lastSignalAt.value == null) add { Task(ui, "power", NM.SAGE, f.t("noSignalShort"), f.t("airplaneTip"), act = "info") }
    }
    Screen({
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)).background(NM.Navy).padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp, 60.dp).clip(RoundedCornerShape(18.dp)).background(Color.White), contentAlignment = Alignment.Center) { Kit(ui.kit, if (back) (if (waiting > 0) Mood.CLOUD else Mood.IDLE) else Mood.DEVICE, 46.dp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { P(greeting(ui), 15.sp, NM.OnNavy2); H(shelterName(ui), 22.sp, Color.White) }
                if (f.adapters.env == "test") { Pill("DEMO", NM.AmberSoft, NM.AmberInk); Spacer(Modifier.width(8.dp)) }
                RoundBtn(ui, "settings", f.t("settings"), onNavy = true) { ui.go("settings") }
            }
            Row(Modifier.fillMaxWidth().clip(R16).background(Color.White.copy(alpha = .08f)).clickable { scope.launch { f.scoutCheck() } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                val t = rememberInfiniteTransition(label = "lamp"); val g by t.animateFloat(.2f, .06f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "g")
                Box(Modifier.size(22.dp).clip(CircleShape).background((if (back) Color(0xFF7FB3E8) else NM.Amber).copy(alpha = g)), contentAlignment = Alignment.Center) { Box(Modifier.size(12.dp).clip(CircleShape).background(if (back) Color(0xFF7FB3E8) else NM.Amber)) }
                Spacer(Modifier.width(10.dp))
                Text(buildAnnotated(if (back) f.t("signalOn") else f.t("noSignalShort"), " " + if (back) (if (waiting > 0) f.t("nWaiting", "n" to waiting) else f.t("allSent")) else "${f.t("workingHere")} · ${f.t("nWaiting", "n" to waiting)}"),
                    Modifier.weight(1f), color = Color(0xFFE4EAF2), fontFamily = Atkinson, fontSize = 15.sp)
                Ic(ui.icons, "sync", 18.dp, Color.White.copy(alpha = .6f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                @Composable fun stat(n: Int, label: String, alarm: Boolean, go: String) = Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .08f)).clickable { ui.go(go) }.padding(12.dp)) {
                    H("$n", 34.sp, if (alarm && n > 0) Color(0xFFFFB4A6) else Color.White); P(label, 13.sp, NM.OnNavy2)
                }
                stat(sch.alerts.size, f.t("cantMiss"), true, "round"); stat(p1.size, f.t("visitFirst"), true, "map"); stat(f.reach(f.announcements.firstOrNull()).remaining, f.t("toTell"), false, "announce")
            }
        }
    }) {
        pad { Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) { H(f.t("needsYouNow"), 22.sp, modifier = Modifier.weight(1f)); Btn(ui, f.t("announce"), "announce", small = true) { ui.go("announce") } } }
        (if (more) tasks else tasks.take(4)).forEachIndexed { i, t -> pad { Rise(i) { t() } } }
        if (tasks.size > 4) pad { Btn(ui, if (more) f.t("seeLess") else f.t("seeMore", "n" to tasks.size - 4), style = BtnStyle.LINK) { more = !more } }
        if (tasks.isEmpty()) pad { Empty(ui, if (f.people.isEmpty()) f.t("emptyShelter") else f.t("allClear"), if (f.people.isEmpty()) Mood.SLEEP else Mood.IDLE) }
        pad { Hybrid(ui) }
    }
}
fun buildAnnotated(bold: String, rest: String) = androidx.compose.ui.text.buildAnnotatedString {
    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)); append(bold); pop(); append(rest)
}
fun greeting(ui: Ui) = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).let { ui.f.t(if (it < 11) "goodMorning" else if (it < 17) "goodAfternoon" else "goodEvening") }
fun shelterName(ui: Ui) = ui.f.shelterId.value.let { if (it == "shelter-1") "Shelter" else "Shelter ${it.replace("SD", "SD ")}" }
fun helpWord(ui: Ui, k: String) = when (k) { "medicine" -> ui.f.t("helpMedicine"); "sick" -> ui.f.t("helpSick"); "moving" -> ui.f.t("helpMoving"); "write" -> ui.f.t("helpWrite"); else -> nice(k) }

// ---------- Camera: point at paper, press once. Modes like a phone camera. ----------
@Composable
fun Camera(ui: Ui, arg: String?) {
    val f = ui.f
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(arg?.substringBefore(':') ?: "list") }
    var pid by remember { mutableStateOf(arg?.substringAfter(':', "")?.ifEmpty { null } ?: f.people.firstOrNull { it.confirmed }?.id) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    fun run(b: Bitmap) {
        busy = true
        scope.launch {
            if (mode == "list") { ui.lastResult = f.scanRoster(b); ui.lastKind = "list"; ui.lastPhoto = b; busy = false; ui.go("review") }
            else { val p = pid ?: return@launch; ui.lastResult = f.readMeds(p, b); ui.lastKind = "medicine"; ui.lastPerson = p; ui.lastPhoto = b; busy = false; ui.go("person", p) }
        }
    }
    val shoot = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(::run) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u: Uri? -> u?.let { loadBitmap(ctx, it) }?.let(::run) }
    val canShoot = mode != "medicine" || pid != null
    Column(Modifier.fillMaxSize().background(Color(0xFF0B0D11))) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(30.dp)).background(Color(0xFF1C2230))) {
            if (mode == "report") Column(Modifier.fillMaxSize().padding(16.dp).padding(top = 64.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(note, { note = it }, f.t("fieldHint"), Modifier.fillMaxWidth().heightIn(min = 160.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MicButton(ui, Modifier.weight(1f)) { note = it } }
                RouteNotice(ui, r) { scope.launch { r = f.fieldReport(note, null, null, cloudOk = true) } }
                Btn(ui, if (busy) f.t("reading") else f.t("report"), "send", BtnStyle.AMBER, full = true, enabled = note.isNotBlank() && !busy) {
                    busy = true; scope.launch { r = f.fieldReport(note, null, null); busy = false; if (r?.status == "ok" || r?.status == "draft") { note = ""; ui.go("map") } }
                }
            } else {
                val t = rememberInfiniteTransition(label = "frame"); val k by t.animateFloat(1f, 1.03f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "k")
                Box(Modifier.fillMaxSize().padding(horizontal = 46.dp, vertical = 90.dp).scale(k).border(3.dp, NM.Amber, RoundedCornerShape(14.dp)))
                Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Ic(ui.icons, "camera", 40.dp, NM.OnNavy2); Spacer(Modifier.height(8.dp)); P(f.t("pointAtPage"), 16.sp, NM.OnNavy2) }
                Row(Modifier.align(Alignment.BottomCenter).padding(20.dp).clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (busy) { Kit(ui.kit, Mood.THINK, 26.dp); Spacer(Modifier.width(8.dp)); H(f.t("reading"), 15.sp, NM.Ink) }
                    else { Box(Modifier.size(10.dp).clip(CircleShape).background(NM.Done)); Spacer(Modifier.width(8.dp)); H(if (canShoot) f.t("readsOnPhone") else f.t("whoIsThisFor"), 15.sp, NM.Ink) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Glass(ui, "x") { ui.go("now") }; Spacer(Modifier.weight(1f))
                if (mode == "medicine") {
                    var open by remember { mutableStateOf(false) }
                    Box { Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .16f)).clickable { open = true }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        H("${f.t("forPerson")}: ${f.people.firstOrNull { it.id == pid }?.name ?: "—"}", 14.sp, Color.White) }
                        DropdownMenu(open, { open = false }) { f.people.forEach { p -> DropdownMenuItem({ Text(p.name) }, { pid = p.id; open = false }) } }
                    }
                } else Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .16f)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(ui.icons, "device", 16.dp, Color.White); Spacer(Modifier.width(8.dp)); H(f.t("readsOnPhone"), 14.sp, Color.White) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("list" to "modeList", "medicine" to "modeMedicine", "report" to "modeReport").forEach { (k, l) ->
                Text(f.t(l), Modifier.clip(RoundedCornerShape(50)).background(if (mode == k) NM.Amber.copy(alpha = .18f) else Color.Transparent).clickable { mode = k }.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (mode == k) NM.Amber else Color.White.copy(alpha = .65f), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 30.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A3242)).border(2.dp, Color.White.copy(alpha = .4f), RoundedCornerShape(14.dp))
                .clickable(enabled = canShoot && mode != "report") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { Ic(ui.icons, "download", 20.dp, Color.White, f.t("choosePhoto")) }
            Box(Modifier.size(86.dp).border(5.dp, Color.White, CircleShape).padding(10.dp).clip(CircleShape).background(if (busy) NM.Amber else Color.White)
                .clickable(enabled = canShoot && !busy && mode != "report") { shoot.launch(null) })
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A3242)).clickable { mode = "report" }, contentAlignment = Alignment.Center) { Ic(ui.icons, "mic", 20.dp, Color.White, f.t("modeReport")) }
        }
    }
}
@Composable
fun Glass(ui: Ui, icon: String, onClick: () -> Unit) = Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 20.dp, Color.White) }

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, big: Boolean = false) =
    Box(modifier.clip(R16).background(NM.Card).border(1.5.dp, NM.Border, R16).padding(14.dp)) {
        val style = TextStyle(fontFamily = if (big) Manrope else Atkinson, fontWeight = if (big) FontWeight.ExtraBold else FontWeight.Normal, fontSize = if (big) 22.sp else 18.sp, color = NM.Navy)
        if (value.isEmpty()) Text(hint, style = style.copy(color = NM.Ink2.copy(alpha = .7f)))
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = style, cursorBrush = SolidColor(NM.Navy))
    }

/** The original photo is always shown beside the AI result. */
@Composable
fun Photo(ui: Ui, b: Bitmap) = Box(Modifier.fillMaxWidth().height(220.dp).clip(R22).background(Color(0xFF2A3242))) {
    Image(b.asImageBitmap(), ui.f.t("originalPhoto"), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    Text(ui.f.t("originalPhoto"), Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .7f)).padding(horizontal = 12.dp, vertical = 5.dp),
        color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
}

// ---------- Check the list ----------
private val FLAGS = listOf("hard_of_hearing", "deaf", "needs_meds", "wheelchair", "cant_walk", "lives_alone", "foreign_language", "infant", "pregnant", "elderly", "dementia")

@Composable
fun Review(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf<String?>(null) }
    var r by remember { mutableStateOf(if (ui.lastKind == "list") ui.lastResult else null) }
    val rows = f.people.filter { !it.confirmed }
    val clear = rows.filter { !it.needsReview }
    Screen({ Top(ui, f.t("checkList")) }) {
        if (ui.lastKind == "list") ui.lastPhoto?.let { b -> pad { Photo(ui, b) } }
        rows.firstOrNull()?.source?.let { s -> pad { Badge(ui, s, text = if (s == "device") f.t("readOnPhone") else f.t("readByCloud")) } }
        pad { RouteNotice(ui, r) { ui.lastPhoto?.let { b -> scope.launch { r = f.scanRoster(b, cloudOk = true) } } } }
        rows.forEachIndexed { i, p -> pad(p.id) { Rise(i) {
            if (open == p.id) CardBox { PersonForm(ui, p) { open = null } }
            else if (p.needsReview) CardBox(Modifier.clickable { open = p.id }, amber = true) { H("${p.name}${p.age?.let { ", $it" } ?: ""}?", 18.sp); P(f.t("hardToRead"), 15.sp, bold = true) }
            else CardBox(Modifier.clickable { open = p.id }) { H("${p.name}${p.age?.let { ", $it" } ?: ""}", 18.sp); P(p.flags.joinToString(" · ") { nice(it) }.ifEmpty { "—" }, 15.sp, NM.Ink2) }
        } } }
        if (rows.isEmpty()) pad { Empty(ui, f.t("allChecked"), Mood.IDLE) }
        else pad { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(ui, f.t("fix"), "edit", modifier = Modifier.weight(1f)) { open = (rows.firstOrNull { it.needsReview } ?: rows.first()).id }
            Btn(ui, f.t("confirmN", "n" to clear.size), "check", BtnStyle.PRIMARY, enabled = clear.isNotEmpty(), modifier = Modifier.weight(1.6f)) { clear.forEach { f.confirmPerson(it.id) } }
        } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonForm(ui: Ui, p: org.nobodymissing.features.UiPerson, onDone: () -> Unit) {
    val f = ui.f
    var name by remember { mutableStateOf(p.name) }; var age by remember { mutableStateOf(p.age?.toString() ?: "") }; var flags by remember { mutableStateOf(p.flags) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Field(name, { name = it }, "Name", Modifier.weight(1f)); Field(age, { age = it.filter(Char::isDigit) }, "Age", Modifier.width(90.dp)) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FLAGS.forEach { fl -> val on = fl in flags
            Text(nice(fl), Modifier.clip(RoundedCornerShape(50)).background(if (on) NM.Navy else NM.Card).border(1.5.dp, if (on) NM.Navy else NM.Border, RoundedCornerShape(50))
                .clickable { flags = if (on) flags - fl else flags + fl }.padding(horizontal = 14.dp, vertical = 8.dp), color = if (on) Color.White else NM.Ink, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
    }
    Btn(ui, f.t("confirm"), "check", BtnStyle.PRIMARY, full = true) { f.editPerson(p.id, name.trim(), age.toIntOrNull()); f.confirmPerson(p.id, flags); onDone() }
}

// ---------- People ----------
@Composable
fun People(ui: Ui) {
    val f = ui.f
    var q by remember { mutableStateOf("") }
    val list = f.people.filter { it.confirmed && it.name.contains(q, true) }
    val toCheck = f.people.count { !it.confirmed }
    val hasMeds = f.meds.map { it.personId }.toSet()
    val tones = listOf(NM.SKY, NM.ROSE, NM.SAGE, NM.LAV)
    Screen({ Top(ui, f.t("people"), back = null) { Pill(f.t("nPeople", "n" to list.size)) } }) {
        pad { Row(Modifier.fillMaxWidth().soft(R22).padding(start = 18.dp, end = 8.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Ic(ui.icons, "search", 22.dp, NM.Ink2); Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) { if (q.isEmpty()) H(f.t("search"), 20.sp, NM.Ink2.copy(alpha = .6f)); BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NM.Navy), singleLine = true) }
        } }
        if (toCheck > 0) pad { Task(ui, "people", NM.SKY, f.t("checkList"), f.t("toCheck", "n" to toCheck)) { ui.go("review") } }
        list.forEachIndexed { i, p -> pad(p.id) { Rise(i) { Row(Modifier.fillMaxWidth().soft(R22).clickable { ui.go("person", p.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.name, tones[i % 4], 44.dp); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { H(p.name, 18.sp); P("${p.age ?: "?"} · ${p.flags.joinToString(", ") { nice(it) }.ifEmpty { "—" }}", 15.sp, NM.Ink2) }
            if (p.id in hasMeds) Ic(ui.icons, "medicine", 20.dp, NM.Ink2); Ic(ui.icons, "chevron", 20.dp, NM.Ink2)
        } } } }
        if (f.people.isEmpty()) pad { Empty(ui, f.t("emptyShelter")) }
    }
}

// ---------- Care card: everything a medic needs at a glance + the two cloud helpers ----------
@Composable
fun CareCard(ui: Ui, id: String) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    val p = f.people.firstOrNull { it.id == id } ?: return Screen({ Top(ui, f.t("people"), "people") }) { pad { Empty(ui, f.t("notFound")) } }
    var r by remember { mutableStateOf(if (ui.lastKind == "medicine" && ui.lastPerson == id) ui.lastResult else null) }
    val meds = f.meds.withIndex().filter { it.value.personId == id }
    val pending = meds.filter { it.value.confirmedBy == null }
    val confirmed = meds.filter { it.value.confirmedBy != null }.map { it.value }
    val slots = f.shared.defaults["slotTimes"]!!.let { it as kotlinx.serialization.json.JsonObject }
    val read = (listOf(p.name) + confirmed.map { "${it.generic} ${it.strength ?: ""}, ${it.dose ?: ""}" }).joinToString(". ")
    Screen({
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)).background(NM.Navy).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row { RoundBtn(ui, "back", f.t("back"), onNavy = true) { ui.go("people") }; Spacer(Modifier.weight(1f))
                RoundBtn(ui, if (ui.speaker.speaking == "card-$id") "stop" else "speaker", f.t("readAloud"), onNavy = true) { ui.speaker.toggle("card-$id", read, f.lang.value) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.border(4.dp, Color.White.copy(alpha = .2f), CircleShape)) { Avatar(p.name, NM.SKY, 72.dp) }; Spacer(Modifier.width(14.dp))
                Column { H(p.name, 26.sp, Color.White); P("${p.age ?: "?"} · ${shelterName(ui)}", 16.sp, NM.OnNavy2) }
            }
            if (p.flags.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(p.flags) { Pill(nice(it), Color.White.copy(alpha = .12f), Color.White) } }
        }
    }) {
        if (ui.lastKind == "medicine" && ui.lastPerson == id) ui.lastPhoto?.let { b -> pad { Photo(ui, b) } }
        pad { RouteNotice(ui, r) { ui.lastPhoto?.let { b -> scope.launch { r = f.readMeds(id, b, cloudOk = true) } } } }
        if (pending.isNotEmpty()) pad { H(f.t("toConfirm"), 22.sp) }
        pending.forEach { (i, m) -> pad("p$i") { CardBox(amber = true) {
            H("“${m.nameAsWritten}” ${m.strength ?: ""}", 18.sp); P("${m.dose ?: ""} · ${m.timesPerDay ?: "?"}×/day", 15.sp)
            if (m.match.lookAlikes.isNotEmpty()) Note(ui, "alert", f.t("lookAlike", "names" to m.match.lookAlikes.joinToString()), NM.AmberInk)
            if (m.match.strengthKnown == false) Note(ui, "alert", f.t("strengthUnknown"), NM.AmberInk)
            if (m.match.status == "unknown") Note(ui, "info", f.t("unknownName"), NM.AmberInk)
            (listOfNotNull(m.match.generic) + m.match.candidates).distinct().forEach { g -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(ui, "$g · ${f.t("evacuees")}", "check", small = true, modifier = Modifier.weight(1f)) { f.confirmMed(i, g, "family") }
                Btn(ui, f.t("medic"), "doctor", small = true) { f.confirmMed(i, g, "medic") }
            } }
        } } }
        pad { Row(verticalAlignment = Alignment.Bottom) { H(f.t("medicines"), 22.sp, modifier = Modifier.weight(1f)); P(f.t("asWritten"), 15.sp, NM.Ink2) } }
        if (confirmed.isEmpty()) pad { P(f.t("noMeds"), 16.sp, NM.Ink2) }
        confirmed.forEachIndexed { i, m -> pad("m$i") { Rise(i) { Row(Modifier.fillMaxWidth().soft(R16).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val times = (slots[(m.timesPerDay ?: 1).toString()] as? kotlinx.serialization.json.JsonArray)?.joinToString(" ") { (it as kotlinx.serialization.json.JsonPrimitive).content } ?: "—"
            H(times, 15.sp, NM.Ink2, Modifier.width(56.dp)); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) { H("${m.generic} ${m.strength ?: ""}", 18.sp); P("${m.dose ?: ""} · ${f.t("confirmedBy", "who" to m.confirmedBy)}", 15.sp, NM.Ink2) }
            val d = if (m.qtyLeft != null && (m.timesPerDay ?: 0) > 0) m.qtyLeft / m.timesPerDay!! else null
            d?.let { Pill(f.t("nDays", "n" to it), if (it <= f.rules.shortageDays) NM.Rose else NM.Quiet, if (it <= f.rules.shortageDays) NM.RoseInk else NM.Ink) }
        } } } }
        if (meds.isNotEmpty()) { pad { MedCheckCard(ui, id) }; pad { FindOpenCard(ui) } }
        pad { Btn(ui, f.t("addMedPhoto"), "camera", BtnStyle.PRIMARY, full = true) { ui.go("camera", "medicine:$id") } }
    }
}

/** 1 · Medicine check: drug names + strengths only go to the cloud; the answer is for a pharmacist and is cross-checked offline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedCheckCard(ui: Ui, personId: String) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val c = f.medChecks[personId]
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "sparkle", 22.dp); Spacer(Modifier.width(8.dp)); H(f.t("checkTitle"), 20.sp, modifier = Modifier.weight(1f)); if (c != null && !c.pending) Badge(ui, "cloud") }
        if (c == null) Note(ui, "lock", f.t("checkWhy"))
        if (c?.pending == true) Note(ui, "clock", f.t("checkWaiting"))
        if (c != null && !c.pending) {
            c.items.forEach { it ->
                HorizontalDivider(color = NM.Line)
                H(it.name + (it.generic?.takeIf { g -> !g.equals(it.name, true) }?.let { g -> " → $g" } ?: ""), 18.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (c.cross.firstOrNull { x -> x.name == it.name }?.status) {
                        "agree" -> Pill(f.t("agree"), NM.Sage, NM.SageInk, ui, "check")
                        "mismatch" -> Pill(f.t("mismatch", "offline" to (c.cross.first { x -> x.name == it.name }.offline ?: "")), NM.AmberSoft, NM.AmberInk, ui, "alert")
                        "cloud_only" -> Pill(f.t("cloudOnly"), NM.AmberSoft, NM.AmberInk, ui, "info")
                    }
                    if (it.cantMiss) Pill(f.t("cantMiss"), NM.Rose, NM.RoseInk)
                }
                if (it.alsoKnownAs.isNotEmpty()) P("${f.t("alsoKnown")}: ${it.alsoKnownAs.joinToString()}", 15.sp, NM.Ink2)
                if (it.swaps.isNotEmpty()) P("${f.t("swaps")}: ${it.swaps.joinToString()}", 15.sp, NM.Ink2)
                it.note?.let { n -> P(n, 15.sp, NM.Ink2) }
            }
            HorizontalDivider(color = NM.Line)
            H(f.t("interactions"), 18.sp)
            if (c.interactions.isEmpty()) P(f.t("noInteractions"), 15.sp, NM.Ink2)
            c.interactions.forEach { x -> Row(verticalAlignment = Alignment.CenterVertically) { Pill(x.severity, if (x.severity == "high") NM.Rose else NM.AmberSoft, if (x.severity == "high") NM.RoseInk else NM.AmberInk); Spacer(Modifier.width(8.dp)); P("${x.a} + ${x.b}${x.note?.let { ": $it" } ?: ""}", 15.sp) } }
            Note(ui, "doctor", f.t("forPharmacist"))
        }
        Btn(ui, f.t("checkCloud"), "cloud", enabled = !busy, full = true) { busy = true; scope.launch { f.checkMeds(personId); busy = false } }
    }
}

/** 2 · Find it open: Grounding with Google Maps near a rounded location. Only places Google Maps returned are shown. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FindOpenCard(ui: Ui) {
    val f = ui.f
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<String?>(null) }
    var noLoc by remember { mutableStateOf(false) }
    val o = f.open.value
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "clock", 22.dp); Spacer(Modifier.width(8.dp)); H(f.t("findOpen"), 20.sp, modifier = Modifier.weight(1f)); if (o != null && !o.pending) Badge(ui, "maps") }
        Note(ui, "lock", f.t("findOpenWhy"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            f.needs().forEach { n -> val on = o?.need == n
                Text((if (busy == n) "… " else "") + f.t("need_$n"), Modifier.clip(RoundedCornerShape(50)).background(if (on) NM.Navy else NM.Card).border(1.5.dp, if (on) NM.Navy else NM.Border, RoundedCornerShape(50))
                    .clickable(enabled = busy == null) { busy = n; scope.launch { noLoc = f.findOpen(n) == "no_location"; busy = null } }.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = if (on) Color.White else NM.Ink, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
        }
        if (noLoc) Note(ui, "info", f.t("noLocation"))
        if (o?.pending == true) Note(ui, "clock", f.t("openWaiting"))
        if (o != null && !o.pending && o.places.isEmpty()) Note(ui, "info", f.t("noOpenFound"))
        if (o != null && !o.pending) o.places.forEach { pl -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (pl.openNow == true) NM.Sage else NM.Quiet), contentAlignment = Alignment.Center) { Ic(ui.icons, if (o.need == "pharmacy") "medicine" else "doctor", 20.dp, NM.SageInk) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { H(pl.name, 17.sp); P(listOfNotNull(pl.openNow?.let { if (it) f.t("openNow") else f.t("closedNow") }, pl.hours, pl.distanceKm?.let { f.t("kmAway", "km" to it) }).joinToString(" · "), 14.sp, NM.Ink2) }
            pl.uri?.let { u -> Box(Modifier.size(48.dp).clip(CircleShape).border(1.5.dp, NM.Border, CircleShape).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } }, contentAlignment = Alignment.Center) { Ic(ui.icons, "external", 20.dp, NM.Navy, f.t("openInMaps")) } }
        } }
    }
}

// ---------- Medicine round ----------
@Composable
fun Round(ui: Ui) {
    val f = ui.f
    val sch = f.schedule()
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    Screen({ Top(ui, f.t("medicineRound")) }) {
        sch.alerts.forEach { a -> pad { Task(ui, "medicine", NM.ROSE, "${name(a.personId)} · ${a.generic}", f.t("onlyDaysLeft", "n" to (a.supplyDays ?: "?")), f.t("cantMiss")) { ui.go("person", a.personId) } } }
        if (sch.slots.isEmpty()) pad { Empty(ui, f.t("noMeds")) }
        sch.slots.forEach { (slot, list) -> pad(slot) { CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) { H(slot, 26.sp, NM.Navy, Modifier.weight(1f)); Pill("${list.size}") }
            list.forEach { e -> val key = "$slot|${e.personId}|${e.generic}"; val st = f.round[key]
                HorizontalDivider(color = NM.Line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { H(name(e.personId), 16.sp, if (st == "taken") NM.Done else NM.Ink); P("${e.generic} · ${e.doseAsWritten ?: ""}", 14.sp, NM.Ink2) }
                    listOf("taken" to "check", "refused" to "x", "missing" to "missing").forEach { (v, ic) -> val on = st == v
                        Box(Modifier.padding(start = 6.dp).size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (on) NM.Navy else NM.Quiet).clickable { f.round[key] = v }, contentAlignment = Alignment.Center) {
                            Ic(ui.icons, ic, 20.dp, if (on) Color.White else NM.Ink, f.t(if (v == "taken") "given" else v)) } }
                }
            }
        } } }
        if (sch.shortages.isNotEmpty()) pad { CardBox {
            Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "package", 22.dp); Spacer(Modifier.width(8.dp)); H(f.t("stock"), 20.sp) }
            sch.shortages.forEach { P(f.t("outOf", "n" to it.people, "drug" to "${it.generic} ${it.strength ?: ""}", "days" to f.rules.shortageDays)) }
            Btn(ui, f.t("requestSupplies"), "send", BtnStyle.PRIMARY, full = true) { f.requestSupply() }
            Note(ui, "lock", f.t("countsOnly"))
        } }
        pad { FindOpenCard(ui) }
    }
}

// ---------- Map · Field check ----------
@Composable
fun FieldMap(ui: Ui, arg: String?) {
    val f = ui.f
    val rows = f.fieldList()
    var sel by remember { mutableStateOf(arg ?: (rows.firstOrNull { f.fieldEntry(it.second.id).let { e -> e.status == "unchecked" && e.pickup != null } } ?: rows.firstOrNull { f.fieldEntry(it.second.id).status == "unchecked" })?.second?.id) }
    val ctx = LocalContext.current
    fun scanPickup() = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(ctx, com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()).startScan()
        .addOnSuccessListener { b -> b.rawValue?.let { org.nobodymissing.domain.decodePickupQr(it) }?.let { p -> f.importPickup(p); sel = "pk-${p.id}" } }
    val sh = f.shared.pack.shelters.firstOrNull { it.id == f.shelterId.value } ?: f.shared.pack.shelters.first()
    val color = mapOf("P1" to NM.Urgent, "P2" to NM.AmberInk, "P3" to NM.Ink2)
    val hp = rows.firstOrNull { it.second.id == sel }?.let { f.higherPlace(f.fieldEntry(it.second.id)) }?.let { h -> f.shared.pack.shelters.firstOrNull { it.id == h.id } }
    val markers = listOf(Marker("shelter", sh.lat, sh.lng, "home", NM.Navy, shelterName(ui))) + listOfNotNull(hp?.let { Marker("higher", it.lat, it.lng, "higher", NM.SageAcc, f.t("higherPlace")) }) + rows.mapNotNull { (n, r) ->
        val e = f.fieldEntry(r.id); if (e.lat == null || e.lng == null) null else {
            val open = e.status == "unchecked"
            Marker(r.id, e.lat, e.lng, if (!open) "check" else if (r.result.tier == "P1") "mobility" else "pin", if (open) color[r.result.tier]!! else NM.Done, if (n.length > 16) n.take(15) + "…" else n, open && r.result.tier == "P1", sel == r.id) { sel = r.id }
        } }
    val row = rows.firstOrNull { it.second.id == sel }
    val p1 = rows.count { it.second.result.tier == "P1" && f.fieldEntry(it.second.id).status == "unchecked" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 130.dp)) {
        item { ShelterMap(ui, markers, aspect = 1f) { MapChip("${f.t("visitFirst")} · $p1", NM.Urgent)
            Row(Modifier.soft(RoundedCornerShape(50)).clickable { scanPickup() }.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "qr", 16.dp, NM.Ink); Spacer(Modifier.width(6.dp)); Text(f.t("scanPickup"), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = NM.Ink) } } }
        item { Column(Modifier.offset(y = (-30).dp).fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(NM.Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFD9D4CB)))
            if (row != null) {
                val (n, r) = row; val e = f.fieldEntry(r.id)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(n, NM.ROSE); Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (e.pickup != null) f.t("pickupFirst") else if (r.result.tier == "P1") f.t("visitFirst").uppercase() else r.result.tier, Modifier.clip(RoundedCornerShape(8.dp)).background(color[r.result.tier]!!).padding(horizontal = 8.dp, vertical = 3.dp), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        H("$n${e.person.age?.let { ", $it" } ?: ""}", 22.sp)
                        P(e.pickup?.let { pk -> f.t("askedWhen", "when" to f.t("minAgo", "n" to maxOf(0L, (System.currentTimeMillis() - pk.at) / 60_000))) } ?: f.t("lastChecked", "when" to (e.hoursSinceContact?.let { f.t("hAgo", "n" to it.toInt()) } ?: f.t("never"))), 15.sp, NM.Ink2)
                    }
                    if (e.needsReview) Badge(ui, null, true)
                }
                @OptIn(ExperimentalLayoutApi::class) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { e.pickup?.needs?.forEach { Pill(f.t("need_$it"), NM.Rose, NM.RoseInk) }; r.result.why.forEach { Pill(it, NM.Rose, NM.RoseInk) } }
                f.higherPlace(e)?.let { h -> Row(Modifier.fillMaxWidth().clip(R16).background(NM.Sage).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ic(ui.icons, "higher", 20.dp, NM.SageInk); Spacer(Modifier.width(12.dp)); P(f.t("higherFor", "name" to h.name, "m" to h.distanceM, "dir" to h.direction), 16.sp, NM.SageInk, bold = true) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (e.pickup != null) Btn(ui, f.t("pickedUp"), "check", BtnStyle.PRIMARY, modifier = Modifier.weight(1f)) { f.visited(r.id, "moved_to_shelter") }
                    else Btn(ui, f.t("safeAtHome"), "check", modifier = Modifier.weight(1f)) { f.visited(r.id, "safe_home"); sel = rows.firstOrNull { it.second.id != r.id && f.fieldEntry(it.second.id).status == "unchecked" }?.second?.id }
                    Btn(ui, f.t("needsEvacuation"), "evacuate", BtnStyle.DANGER, modifier = Modifier.weight(1f)) { f.visited(r.id, "needs_evacuation") }
                }
                Btn(ui, f.t("report"), "mic", BtnStyle.PRIMARY, full = true) { ui.go("camera", "report") }
                Note(ui, "info", f.t("hazardNote", "event" to f.rules.currentEvent))
            } else Empty(ui, f.t("allChecked"), Mood.IDLE)
            FindOpenCard(ui)
        } }
    }
}

// ---------- Check-in at the door: one spoken sentence (or their card) per person, understood on this phone ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffCheckIn(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var c by remember { mutableStateOf<org.nobodymissing.domain.Checkin?>(null) }
    var card by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<Pair<String, org.nobodymissing.domain.Checkin>?>(null) }
    fun scanCard() = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(ctx, com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()).startScan()
        .addOnSuccessListener { b -> b.rawValue?.let { org.nobodymissing.domain.decodeCheckinQr(it) }?.let { c = it; card = true } }
    val reachFlags = f.shared.defaults.strings("reachFlags")
    Screen({ Top(ui, f.t("checkIn"), back = null) { Badge(ui, "device", text = f.t("readsOnPhone")) } }) {
        pad { Row(Modifier.fillMaxWidth().clip(R22).background(NM.Navy).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            H("${f.people.count { it.confirmed }}", 28.sp, Color.White); Spacer(Modifier.width(8.dp)); P(f.t("checkedIn"), 15.sp, NM.OnNavy2, modifier = Modifier.weight(1f)) } }
        pad { Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigInput(text, f.t("checkinHint"), 20) { text = it; c = null }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                MicButton(ui, Modifier.weight(1f)) { text = it; c = null }
                RoundBtn(ui, "qr", f.t("scanCard")) { scanCard() }
                Btn(ui, f.t("add"), "sparkle", BtnStyle.PRIMARY, enabled = text.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { c = f.understandCheckin(text); card = false; busy = false } }
            }
        } }
        c?.let { x -> pad { CardBox {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Field(x.name, { c = x.copy(name = it) }, "Name", Modifier.weight(1f)); Field(x.age?.toString() ?: "", { v -> c = x.copy(age = v.filter(Char::isDigit).toIntOrNull()) }, "Age", Modifier.width(90.dp)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FLAGS.forEach { fl -> val on = fl in x.flags
                    Text(nice(fl), Modifier.clip(RoundedCornerShape(50)).background(if (on) NM.Navy else NM.Card).border(1.5.dp, if (on) NM.Navy else NM.Border, RoundedCornerShape(50))
                        .clickable { c = x.copy(flags = if (on) x.flags - fl else x.flags + fl) }.padding(horizontal = 14.dp, vertical = 8.dp), color = if (on) Color.White else NM.Ink, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
            }
            val dup = f.alreadyHere(x.name, x.age)
            if (dup.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NM.HONEY.bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "warning", 18.dp, NM.HoneyInk); Spacer(Modifier.width(8.dp)); P(f.t("maybeHere"), 16.sp, NM.HoneyInk, bold = true) }
                dup.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) {
                    P("${p.name}${p.age?.let { ", $it" } ?: ""}", 16.sp, NM.HoneyInk, bold = true, modifier = Modifier.weight(1f))
                    Btn(ui, f.t("samePerson"), small = true) { c = null; text = ""; ui.go("person", p.id) } } }
            }
            if (card) Badge(ui, "keywords", text = f.t("fromCard")) else if (x.by == "device") Badge(ui, "device", text = f.t("readOnPhone")) else Badge(ui, "keywords", text = f.t("keywordsOnly"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(ui, f.t("close"), "x", modifier = Modifier.weight(1f)) { c = null }
                Btn(ui, f.t(if (dup.isNotEmpty()) "notSame" else "confirmNext"), "check", BtnStyle.PRIMARY, enabled = x.name.isNotBlank(), modifier = Modifier.weight(1.6f)) { done = f.checkin(x) to x; c = null; text = "" }
            }
        } } }
        done?.let { (id, x) -> pad { CardBox {
            H(x.name, 18.sp)
            if (x.flags.any { it in reachFlags }) Note(ui, "check", f.t("addedTell"), NM.Done)
            if ("needs_meds" in x.flags) Btn(ui, f.t("addedMeds"), "medicine", BtnStyle.LINK) { ui.go("camera", "medicine:$id") }
        } } }
        pad { Btn(ui, f.t("paperList"), "camera", full = true) { ui.go("camera", "list") } }
    }
}

// ---------- Announce ----------
@Composable
fun Announce(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf(false) }
    val a = f.announcements.firstOrNull()
    val reach = f.reach(a)
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    Screen({ Top(ui, f.t("announce")) }) {
        pad { Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.heightIn(min = 70.dp)) { if (text.isEmpty()) P(f.t("typeOrSpeak"), 22.sp, NM.Ink2.copy(alpha = .6f)); BasicTextField(text, { text = it }, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Atkinson, fontSize = 22.sp, color = NM.Ink)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { text = it }
                Btn(ui, f.t("makeCards"), "sparkle", BtnStyle.PRIMARY, enabled = text.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { f.announce(text.trim(), f.lang.value); text = ""; busy = false } }
            }
        } }
        if (a == null) pad { Empty(ui, f.t("noNews"), Mood.IDLE) } else {
            val (title, time, place) = headline(a)
            pad { Eyebrow(f.t("whatEveryoneSees"), NM.Ink2) }
            pad(a.id) { Rise(0) { Row(Modifier.fillMaxWidth().clip(R22).background(NM.Honey).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).clip(R20).background(Color.White), contentAlignment = Alignment.Center) { Ic(ui.icons, cardIcon(a), 32.dp, NM.HoneyAcc) }
                Spacer(Modifier.width(14.dp)); Column { H(title, 28.sp, NM.HoneyInk); P(listOfNotNull(time, place).joinToString(" · "), 18.sp, NM.HoneyInk, bold = true) }
            } } }
            pad { Row(verticalAlignment = Alignment.CenterVertically) { Badge(ui, a.source, a.draft); Spacer(Modifier.width(10.dp)); P("“${a.text}”", 15.sp, NM.Ink2) } }
            if (reach.list.isNotEmpty()) pad { Row(Modifier.fillMaxWidth().soft(R16).clickable { list = !list }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val tones = listOf(NM.SKY, NM.ROSE, NM.SAGE)
                Row { reach.list.filter { !it.told }.take(3).forEachIndexed { i, e -> Box(Modifier.offset(x = (-10 * i).dp).size(36.dp).clip(CircleShape).background(Color.White).padding(3.dp).clip(CircleShape).background(tones[i].bg), contentAlignment = Alignment.Center) { H(initial(name(e.id)), 13.sp, tones[i].fg) } } }
                Column(Modifier.weight(1f).padding(start = 4.dp)) { H(f.t("nToTell", "n" to reach.remaining), 16.sp); P(f.t("tellWhy"), 14.sp, NM.Ink2) }
                Ic(ui.icons, "chevron", 20.dp, NM.Ink2)
            } }
            if (list) pad { CardBox {
                val done = if (reach.list.isEmpty()) 0f else (reach.list.size - reach.remaining).toFloat() / reach.list.size
                LinearProgressIndicator({ done }, Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)), color = NM.Done, trackColor = NM.Quiet)
                reach.list.forEach { e -> Row(Modifier.fillMaxWidth().clickable { f.toggleTold(a.id, e.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (e.told) NM.Done else Color.Transparent).border(2.dp, if (e.told) NM.Done else NM.Border, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { if (e.told) Ic(ui.icons, "check", 18.dp, Color.White) }
                    Spacer(Modifier.width(12.dp)); Column { H(name(e.id), 17.sp); P(e.reasons.joinToString { nice(it) }, 14.sp, NM.Ink2) }
                } }
            } }
            pad { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(ui, f.t("print"), "print", modifier = Modifier.weight(1f)) { }
                if (a.shown) Row(Modifier.weight(1.6f).heightIn(min = 60.dp).clip(R20).background(NM.Sage), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "check", 20.dp, NM.SageInk); Spacer(Modifier.width(8.dp)); H(f.t("shown"), 17.sp, NM.SageInk) }
                else Btn(ui, f.t("showEveryone"), "send", BtnStyle.PRIMARY, modifier = Modifier.weight(1.6f)) { f.approve(a.id) }
            } }
        }
    }
}

fun qr(text: String, size: Int = 400): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) for (y in 0 until size) setPixel(x, y, if (m[x, y]) android.graphics.Color.parseColor("#14161A") else android.graphics.Color.WHITE)
    }
}

// ---------- Settings ----------
@Composable
fun Settings(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var chunks by remember { mutableStateOf<List<String>?>(null) }
    var paste by remember { mutableStateOf("") }
    @Composable fun title(icon: String, text: String) = Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, icon, 22.dp); Spacer(Modifier.width(8.dp)); H(text, 20.sp) }
    Screen({ Top(ui, f.t("settings")) }) {
        pad { CardBox { title("download", f.t("shelterReady"))
            Btn(ui, f.t("prepareDevice"), "device", BtnStyle.PRIMARY, full = true) { scope.launch {
                val inf = f.adapters.inference
                if (inf is FirebaseAiLogicInference) runCatching { inf.download().collect { status = f.t("onDeviceModel", "status" to it.toString()) } }
                status = f.t("onDeviceModel", "status" to inf.onDeviceStatus())
            } }
            if (status.isNotEmpty()) P(status, 15.sp, NM.Ink2) } }
        pad { CardBox { title("globe", f.t("language"))
            Row(Modifier.clip(RoundedCornerShape(50)).background(NM.Quiet).padding(4.dp)) { listOf("en" to "English", "id" to "Bahasa Indonesia").forEach { (k, l) ->
                Text(l, Modifier.clip(RoundedCornerShape(50)).background(if (f.lang.value == k) NM.Navy else Color.Transparent).clickable { f.lang.value = k }.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (f.lang.value == k) Color.White else NM.Ink2, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) } } } }
        pad { CardBox { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "lock", 22.dp); Spacer(Modifier.width(10.dp)); P(f.t("privateMode"), 16.sp, bold = true, modifier = Modifier.weight(1f))
            Switch(f.privateMode.value, { f.privateMode.value = it }, colors = SwitchDefaults.colors(checkedTrackColor = NM.Navy)) } } }
        pad { CardBox { Row(verticalAlignment = Alignment.CenterVertically) { Ic(ui.icons, "people", 22.dp); Spacer(Modifier.width(10.dp)); P(f.t("evacuees") + ": " + f.t("askKitSub"), 16.sp, bold = true, modifier = Modifier.weight(1f))
            Btn(ui, f.t("open"), "chevron", small = true) { ui.evacueeMode.value = true; ui.go("home") } } } }
        pad { CardBox { title("signal", f.t("signalScout"))
            P("${f.t("signal")}: ${nice(f.signal.value)} · ${f.t("lastSeen")}: ${f.lastSignalAt.value?.let { java.text.DateFormat.getTimeInstance().format(it) } ?: f.t("never")}", 15.sp, NM.Ink2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Btn(ui, f.t("checkNow"), "signal", small = true, modifier = Modifier.weight(1f)) { scope.launch { f.scoutCheck() } }; Btn(ui, f.t("syncNow"), "sync", small = true, modifier = Modifier.weight(1f)) { scope.launch { f.flush() } } }
            f.jobs.takeLast(8).forEach { j -> Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(when (j.status.state) { "sent" -> NM.Done; "failed" -> NM.Urgent; else -> NM.Amber })); Spacer(Modifier.width(10.dp))
                P(nice(j.type), 15.sp, modifier = Modifier.weight(1f)); P(j.status.state, 13.sp, NM.Ink2) } }
        } }
        pad { CardBox { title("qr", f.t("courier"))
            Btn(ui, f.t("exportQueue"), "qr", small = true) { chunks = f.exportChunks() }
            chunks?.let { cs -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(cs.size) { i -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Image(qr(cs[i]).asImageBitmap(), "${f.t("courier")} ${i + 1}/${cs.size}", Modifier.size(200.dp).clip(R16)); H("${i + 1}/${cs.size}", 14.sp) } } } }
            Field(paste, { paste = it }, f.t("importCodes"), Modifier.fillMaxWidth().heightIn(min = 70.dp))
            Btn(ui, f.t("import"), "download", small = true) { status = f.t("imported", "n" to f.importChunks(paste.lines().filter { it.isNotBlank() })); paste = "" }
        } }
        pad { CardBox { title("people", f.t("joinQrs"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("volunteer" to "#/staff/now?join=${f.shelterId.value}&role=volunteer", "medic" to "#/staff/now?join=${f.shelterId.value}&role=medic", "evacuees" to "#/public").forEach { (k, path) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Image(qr(HOSTED + path, 240).asImageBitmap(), f.t(k), Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))); P(f.t(k), 13.sp, bold = true) }
                }
            } } }
        pad { Btn(ui, f.t("loadDemo"), "demo", full = true) { f.loadDemo() } }
        if (f.adapters.env == "test") pad { Note(ui, "demo", f.t("demoBadge")) }
    }
}

// ---------- Signal Scout alert: the one loud moment ----------
@Composable
fun ScoutAlert(ui: Ui) {
    val f = ui.f
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        @Suppress("DEPRECATION")
        (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }
    val t = rememberInfiniteTransition(label = "halo")
    val h by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "h")
    val sent = f.jobs.count { it.status.state == "sent" }
    Column(Modifier.fillMaxSize().background(NM.Navy).clickable(enabled = false) {}.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(250.dp + (30 * h).dp).clip(CircleShape).background(NM.Amber.copy(alpha = .05f)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(250.dp).clip(CircleShape).background(NM.Amber.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Kit(ui.kit, Mood.ALERT, 170.dp) } }
        Spacer(Modifier.height(22.dp))
        H(f.t("signalBackTitle"), 40.sp, Color.White)
        Spacer(Modifier.height(10.dp))
        Text("${f.t("bringPhones")} ${f.t("sentOf", "sent" to sent, "total" to f.jobs.size, "waiting" to f.jobs.size - sent)}", color = NM.OnNavy2, fontFamily = Atkinson, fontSize = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator({ if (f.jobs.isEmpty()) 1f else sent.toFloat() / f.jobs.size }, Modifier.width(300.dp).height(10.dp).clip(RoundedCornerShape(50)), color = NM.Amber, trackColor = Color.White.copy(alpha = .12f))
        Spacer(Modifier.height(22.dp))
        Btn(ui, f.t("gotIt"), style = BtnStyle.AMBER, modifier = Modifier.widthIn(min = 220.dp)) { f.scoutAlert.value = false }
    }
}
