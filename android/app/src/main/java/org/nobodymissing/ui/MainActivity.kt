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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nobodymissing.NobodyMissingApp
import org.nobodymissing.R
import org.nobodymissing.adapters.FirebaseAiLogicInference
import org.nobodymissing.features.Features
import org.nobodymissing.features.RouteResult

// Same structure, building blocks and words as the web staff app (web/src/ui/staff.tsx), with a native feel.
private val Orange = Color(0xFFF28C00)
private val Danger = Color(0xFFC40000)
private val Review = Color(0xFFFFF8E1)
private val ICON = mapOf("food" to "🍙", "water" to "💧", "medicine" to "💊", "doctor" to "🩺", "move" to "🚌", "warning" to "⚠️", "toilet" to "🚻", "info" to "ℹ️", "baby" to "🍼", "power" to "🔌")
private const val HOSTED = "https://nobodymissing.web.app/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = (application as NobodyMissingApp).features
        setContent { MaterialTheme(typography = Typography(bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp))) { App(f) } }
    }
}

enum class Tab(val icon: String, val key: String) { TODAY("🏠", "today"), PEOPLE("👥", "people"), MEDICINE("💊", "medicine"), ANNOUNCE("📢", "announce"), FIELD("🏚", "field"), SETTINGS("⚙️", "settings") }

@Composable
fun App(f: Features) {
    var tab by remember { mutableStateOf(Tab.TODAY) }
    LaunchedEffect(Unit) { while (true) { f.scoutCheck(); delay(1000L * f.rules.scoutIntervalSec) } }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { StatusBar(f) },
            bottomBar = {
                Column {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(tab == t, { tab = t }, icon = { Text(t.icon, fontSize = 20.sp) },
                                label = { if (t != Tab.SETTINGS) Text(f.t(t.key), fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Orange))
                        }
                    }
                    val c = f.router
                    Text(f.t("counter", "device" to c.deviceCount, "cloud" to c.cloudCount, "waiting" to f.jobs.count { it.status.state != "sent" }),
                        Modifier.fillMaxWidth().background(Color.Black).padding(4.dp), color = Color.White, fontSize = 12.sp)
                }
            },
        ) { pad ->
            Box(Modifier.padding(pad).padding(12.dp)) {
                when (tab) {
                    Tab.TODAY -> Today(f) { tab = it }
                    Tab.PEOPLE -> People(f)
                    Tab.MEDICINE -> Medicine(f)
                    Tab.ANNOUNCE -> Announce(f)
                    Tab.FIELD -> Field(f)
                    Tab.SETTINGS -> Settings(f)
                }
            }
        }
        if (f.scoutAlert.value) ScoutAlert(f)
    }
}

@Composable
fun Kit(size: Dp = 40.dp) = Image(painterResource(R.drawable.kit), contentDescription = "Kit", modifier = Modifier.size(size))

@Composable
fun Pill(text: String) = Text(text, Modifier.border(2.dp, Color.Black, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 13.sp)

/** Status = icon + word, never colour alone. */
@Composable
fun StatusBar(f: Features) {
    val waiting = f.jobs.count { it.status.state != "sent" }
    val back = f.signal.value == "back"
    Row(Modifier.fillMaxWidth().background(Color.White).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Kit()
        Text(if (f.shelterId.value == "shelter-1") "Shelter" else "Shelter ${f.shelterId.value}", fontWeight = FontWeight.Bold)
        Pill(if (back) "📶 ${f.t("cloud")}" else "🏮 ${f.t("device")}")
        if (waiting > 0) Pill("⏳ ${f.t("waiting")} ($waiting)")
        if (f.privateMode.value) Pill("🔒")
        if (f.adapters.env == "test") Pill(f.t("demoBadge"))
    }
}

@Composable
fun Badge(f: Features, source: String?, draft: Boolean = false) {
    val (txt, bg) = when {
        draft -> "⚠️ ${f.t("review")}" to Color(0xFFFFE1A8)
        source == "device" -> "🏮 ${f.t("device")}" to Color(0xFFD8F5D0)
        source == "cloud" -> "☁️ ${f.t("cloud")}" to Color(0xFFD5E6FF)
        source == "keywords" -> "🔤 ${f.t("keywords")}" to Color(0xFFEEEEEE)
        else -> return
    }
    Text(txt, Modifier.background(bg, RoundedCornerShape(6.dp)).border(2.dp, Color.Black, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
fun MainButton(label: String, enabled: Boolean = true, onClick: () -> Unit) =
    Button(onClick, Modifier.fillMaxWidth().height(56.dp), enabled = enabled, shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, Color.Black),
        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.Black)) { Text(label, fontWeight = FontWeight.Bold) }

@Composable
fun Card(danger: Boolean = false, review: Boolean = false, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(if (review) Review else Color.White, RoundedCornerShape(12.dp))
        .border(if (danger) 3.dp else 2.dp, if (danger) Danger else Color(0xFFCCCCCC), RoundedCornerShape(12.dp)).padding(12.dp), content = content)

@Composable
fun Empty(text: String) = Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Kit(56.dp); Text(text) }

@Composable
fun Tile(icon: String, n: Any, label: String, danger: Boolean = false, modifier: Modifier, onClick: () -> Unit) =
    Column(modifier.height(104.dp).border(if (danger) 3.dp else 2.dp, if (danger) Danger else Color.Black, RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.Center) { Text("$icon $n", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text(label) }

@Composable
fun Today(f: Features, go: (Tab) -> Unit) {
    val s = f.schedule()
    val next = s.counts.entries.firstOrNull()
    val tiles = listOf(
        Triple("💊", next?.value ?: 0, "${f.t("nextRound")} ${next?.key ?: ""}") to (Tab.MEDICINE to false),
        Triple("⚠️", s.alerts.size, f.t("cantMiss")) to (Tab.MEDICINE to s.alerts.isNotEmpty()),
        Triple("📢", f.reach(f.announcements.firstOrNull()).remaining, f.t("toTell")) to (Tab.ANNOUNCE to false),
        Triple("🏚", f.fieldList().count { it.second.result.tier == "P1" }, f.t("p1Visits")) to (Tab.FIELD to true),
        Triple("🙋", 0, f.t("help")) to (Tab.PEOPLE to false),
        Triple("👥", f.people.size, f.t("people")) to (Tab.PEOPLE to false),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (f.signal.value == "no_signal" && f.lastSignalAt.value == null) Card { Text("🏮 ${f.t("noSignal")} ${f.t("airplaneTip")}") }
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (t, nav) -> Tile(t.first, t.second, t.third, danger = nav.second, modifier = Modifier.weight(1f)) { go(nav.first) } }
            }
        }
        if (f.people.isEmpty()) Empty(f.t("emptyShelter"))
    }
}

@Composable
fun RouteNotice(f: Features, r: RouteResult?, onCloud: () -> Unit) {
    when (r?.status) {
        "ask_staff" -> Card(review = true) { Text(f.t("askCloud")); MainButton("☁️ ${f.t("sendCloud")}", onClick = onCloud) }
        "draft" -> Card(review = true) { Text("⚠️ ${f.t("review")}") }
        "queued" -> Card { Text("⏳ ${f.t("queued")}") }
        "blocked" -> Card { Text("🔒 ${f.t("blocked")}") }
        "invalid", "error" -> Card(review = true) { Text(f.t("invalid"), color = Danger) }
        else -> {}
    }
}

@Composable
fun CameraButton(label: String, onPhoto: (Bitmap) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(onPhoto) }
    MainButton("📷 $label") { launcher.launch(null) }
}

private val FLAGS = listOf("hard_of_hearing", "deaf", "needs_meds", "wheelchair", "cant_walk", "lives_alone", "foreign_language", "infant", "pregnant", "elderly", "dementia")

@Composable
fun People(f: Features) {
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    var q by remember { mutableStateOf("") }
    Column {
        LazyColumn(Modifier.weight(1f)) {
            // Review screen: the original photo on top, fields below, uncertain rows highlighted, ✓ Confirm.
            photo?.let { item { Image(it.asImageBitmap(), "Original list photo", Modifier.fillMaxWidth().heightIn(max = 280.dp).border(2.dp, Color.Black)) } }
            item { RouteNotice(f, r) { scope.launch { r = f.scanRoster(photo!!, cloudOk = true) } } }
            items(f.people.filter { !it.confirmed }) { p ->
                Card(review = p.needsReview) {
                    Badge(f, p.source, p.needsReview)
                    var flags by remember(p.id) { mutableStateOf(p.flags) }
                    Text("${p.name}, ${p.age ?: "?"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(FLAGS) { fl -> FilterChip(fl in flags, { flags = if (fl in flags) flags - fl else flags + fl }, { Text(fl.replace('_', ' ')) }) } }
                    MainButton("✓ ${f.t("confirm")}") { f.confirmPerson(p.id, flags) }
                }
            }
            item { OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("🔍 ${f.t("familySearchLocal")}") }) }
            if (q.isNotBlank()) items(f.familySearch(q)) { Text("${it.name} · ${it.ageBand} · ${it.shelterId} · ${it.status}", fontWeight = FontWeight.Bold) }
            items(f.people.filter { it.confirmed }) { p -> Text("${p.name}, ${p.age ?: "?"} · ${p.flags.joinToString()}", Modifier.padding(vertical = 6.dp)) }
        }
        CameraButton(f.t("scanList")) { bmp -> photo = bmp; scope.launch { r = f.scanRoster(bmp) } }
    }
}

@Composable
fun Medicine(f: Features) {
    val scope = rememberCoroutineScope()
    val s = f.schedule()
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    var pid by remember { mutableStateOf(f.people.firstOrNull()?.id) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    Column {
        LazyColumn(Modifier.weight(1f)) {
            items(s.alerts) { a -> Card(danger = true) { Text("⚠️ ${f.t("cantMiss")}: ${name(a.personId)} · ${a.generic} (${a.type}) · ${a.supplyDays ?: "?"} d", fontWeight = FontWeight.Bold) } }
            if (s.slots.isEmpty()) item { Empty(f.t("noMeds")) }
            s.slots.forEach { (slot, list) ->
                item {
                    Card {
                        Text("$slot · ${list.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        list.forEach { e ->
                            val key = "$slot|${e.personId}|${e.generic}"
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${name(e.personId)} · ${e.generic} · ${e.doseAsWritten ?: ""} ${f.round[key]?.let { "($it)" } ?: ""}", Modifier.weight(1f))
                                TextButton({ f.round[key] = "taken" }) { Text("✓") }
                                TextButton({ f.round[key] = "refused" }) { Text("✗") }
                                TextButton({ f.round[key] = "missing" }) { Text("∅") }
                            }
                        }
                    }
                }
            }
            if (s.shortages.isNotEmpty()) item {
                Card {
                    Text(f.t("stock"), fontWeight = FontWeight.Bold)
                    s.shortages.forEach { Text(f.t("outOf", "n" to it.people, "drug" to "${it.generic} ${it.strength ?: ""}", "days" to f.rules.shortageDays)) }
                    MainButton("📦 ${f.t("requestSupplies")}") { f.requestSupply() }
                    Text(f.t("countsOnly"), color = Color.Gray, fontSize = 14.sp)
                }
            }
            photo?.let { item { Image(it.asImageBitmap(), "Original medicine photo", Modifier.fillMaxWidth().heightIn(max = 280.dp).border(2.dp, Color.Black)) } }
            item { RouteNotice(f, r) { scope.launch { r = f.readMeds(pid!!, photo!!, cloudOk = true) } } }
            items(f.meds.withIndex().filter { it.value.confirmedBy == null }.toList()) { (i, m) ->
                Card(review = true) {
                    Badge(f, null, draft = true)
                    Text("${name(m.personId)}: “${m.nameAsWritten}” ${m.strength ?: ""} · ${m.dose ?: ""} · ${m.timesPerDay ?: "?"}×", fontWeight = FontWeight.Bold)
                    if (m.match.lookAlikes.isNotEmpty()) Text("⚠️ ${f.t("lookAlike", "names" to m.match.lookAlikes.joinToString())}", color = Danger)
                    if (m.match.strengthKnown == false) Text("⚠️ ${f.t("strengthUnknown")}", color = Danger)
                    if (m.match.status == "unknown") Text("❓ ${f.t("unknownName")}", color = Danger)
                    (listOfNotNull(m.match.generic) + m.match.candidates).forEach { g ->
                        Row { OutlinedButton({ f.confirmMed(i, g, "self") }) { Text("✓ $g (${f.t("evacuees")})") }; Spacer(Modifier.width(6.dp)); OutlinedButton({ f.confirmMed(i, g, "medic") }) { Text("✓ ${f.t("medic")}") } }
                    }
                }
            }
            item { Text("${f.t("person")}:"); LazyRow { items(f.people) { p -> FilterChip(pid == p.id, { pid = p.id }, { Text(p.name) }) } } }
        }
        CameraButton(f.t("medicinePhoto")) { bmp -> photo = bmp; pid?.let { scope.launch { r = f.readMeds(it, bmp) } } }
    }
}

@Composable
fun Announce(f: Features) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val a = f.announcements.firstOrNull()
    val reach = f.reach(a)
    Column {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("announcementHint")) }, minLines = 3) }
            item { MainButton(if (busy) "…" else "✍️ ${f.t("makeCards")}", enabled = text.isNotBlank() && !busy) { busy = true; scope.launch { f.announce(text.trim(), f.lang.value); text = ""; busy = false } } }
            if (a != null) item {
                Card {
                    Text("${f.t("original")}: “${a.text}”", color = Color.Gray)
                    Badge(f, a.source, a.draft)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Kit(56.dp)
                        Column { a.cards.forEach { c -> Column(Modifier.padding(4.dp).border(3.dp, Color.Black, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text("${ICON[c.icon] ?: "ℹ️"} ${c.title}${c.time?.let { " · $it" } ?: ""}", fontSize = 24.sp, fontWeight = FontWeight.Bold); c.place?.let { Text("📍 $it") } } } }
                    }
                    if (a.shown) Text("✅ ${f.t("shown")}") else MainButton("➤ ${f.t("show")}") { f.approve(a.id) }
                    Text(f.t("tellInPerson", "n" to reach.remaining), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    reach.list.forEach { e -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(e.told, { f.toggleTold(a.id, e.id) }); Text("${f.people.firstOrNull { it.id == e.id }?.name ?: e.id} · ${e.reasons.joinToString()}") } }
                }
            }
        }
    }
}

@Composable
fun Field(f: Features) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    val list = f.fieldList()
    Column {
        LazyColumn(Modifier.weight(1f)) {
            item { Text(f.t("hazardNote", "event" to f.rules.currentEvent), color = Color.Gray, fontSize = 14.sp) }
            if (list.isEmpty()) item { Empty(f.t("allChecked")) }
            items(list) { (name, row) ->
                val e = f.fieldEntry(row.id)
                Card(danger = row.result.tier == "P1") {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("${row.result.tier} · $name ", fontWeight = FontWeight.Bold); if (e.needsReview) Badge(f, null, true) }
                    Text("${f.t("why")}: ${row.result.why.joinToString(" · ").ifEmpty { "—" }}")
                    f.higherPlace(e)?.let { Text("⬆️ ${f.t("higherPlace")}: ${it.name}, ${it.distanceM} m ${it.direction}, +${it.handGain} m") }
                    Text(e.status, color = Color.Gray)
                    Row {
                        OutlinedButton({ f.visited(row.id, "safe_home") }) { Text("✓ ${f.t("safeAtHome")}") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton({ f.visited(row.id, "needs_evacuation") }) { Text("🚨") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton({ f.override(row.id, if (row.result.tier == "P1") "P3" else "P1", "staff decision") }) { Text("↕ ${f.t("override")}") }
                    }
                }
            }
            item { RouteNotice(f, r) { scope.launch { r = f.fieldReport(note, null, null, cloudOk = true) } } }
            item { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("fieldHint")) }) }
        }
        MainButton("📝 ${f.t("report")}", enabled = note.isNotBlank()) { scope.launch { r = f.fieldReport(note, null, null); if (r?.status == "ok") note = "" } }
    }
}

fun qr(text: String, size: Int = 400): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) for (y in 0 until size) setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
}

@Composable
fun Settings(f: Features) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var chunks by remember { mutableStateOf<List<String>?>(null) }
    var paste by remember { mutableStateOf("") }
    LazyColumn {
        item {
            Card {
                Text(f.t("shelterReady"), fontWeight = FontWeight.Bold)
                MainButton("⬇ ${f.t("prepareDevice")}") {
                    scope.launch {
                        val inf = f.adapters.inference
                        if (inf is FirebaseAiLogicInference) runCatching { inf.download().collect { status = f.t("onDeviceModel", "status" to it.toString()) } }
                        status = f.t("onDeviceModel", "status" to inf.onDeviceStatus())
                    }
                }
                Text(status)
            }
        }
        item { Card { Text(f.t("language"), fontWeight = FontWeight.Bold); Row { listOf("en" to "EN", "id" to "ID").forEach { (k, l) -> FilterChip(f.lang.value == k, { f.lang.value = k }, { Text(l) }); Spacer(Modifier.width(8.dp)) } } } }
        item { Card { Row(verticalAlignment = Alignment.CenterVertically) { Switch(f.privateMode.value, { f.privateMode.value = it }); Spacer(Modifier.width(8.dp)); Text("🔒 ${f.t("privateMode")}") } } }
        item {
            Card {
                Text(f.t("signalScout"), fontWeight = FontWeight.Bold)
                Text("${f.t("signal")}: ${f.signal.value} · ${f.t("lastSeen")}: ${f.lastSignalAt.value?.let { java.text.DateFormat.getTimeInstance().format(it) } ?: f.t("never")}")
                Row { OutlinedButton({ scope.launch { f.scoutCheck() } }) { Text("📶 ${f.t("checkNow")}") }; Spacer(Modifier.width(6.dp)); OutlinedButton({ scope.launch { f.flush() } }) { Text("⇪ ${f.t("syncNow")}") } }
                f.jobs.takeLast(8).forEach { Text("${it.type} · ${it.status.state}${if (it.status.retries > 0) " (retry ${it.status.retries})" else ""}") }
            }
        }
        item {
            Card {
                Text(f.t("courier"), fontWeight = FontWeight.Bold)
                OutlinedButton({ chunks = f.exportChunks() }) { Text(f.t("exportQueue")) }
                chunks?.forEachIndexed { i, c -> Image(qr(c).asImageBitmap(), "${f.t("courier")} ${i + 1}/${chunks!!.size}", Modifier.size(220.dp)); Text("${i + 1}/${chunks!!.size}") }
                OutlinedTextField(paste, { paste = it }, Modifier.fillMaxWidth(), placeholder = { Text(f.t("importCodes")) }, minLines = 2)
                OutlinedButton({ status = f.t("imported", "n" to f.importChunks(paste.lines().filter { it.isNotBlank() })); paste = "" }) { Text(f.t("import")) }
            }
        }
        item {
            Card {
                Text(f.t("joinQrs"), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("volunteer" to "#/staff/today?join=${f.shelterId.value}&role=volunteer", "medic" to "#/staff/today?join=${f.shelterId.value}&role=medic", "evacuees" to "#/public/news").forEach { (k, path) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Image(qr(HOSTED + path, 240).asImageBitmap(), f.t(k), Modifier.size(100.dp)); Text(f.t(k), fontSize = 13.sp) }
                    }
                }
            }
        }
        item { Card { OutlinedButton({ f.loadDemo() }) { Text("🧪 ${f.t("loadDemo")}") } } }
    }
}

/** Full-screen Kit: "Signal is back here!" (6.10 level 1), with vibration. */
@Composable
fun ScoutAlert(f: Features) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        @Suppress("DEPRECATION")
        (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }
    val sent = f.jobs.count { it.status.state == "sent" }
    Column(Modifier.fillMaxSize().background(Orange).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Kit(160.dp)
        Text(f.t("signalBack"), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp)
        Text(f.t("sentOf", "sent" to sent, "total" to f.jobs.size, "waiting" to f.jobs.size - sent), fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        Button({ f.scoutAlert.value = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("OK", fontSize = 20.sp) }
    }
}
