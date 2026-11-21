package org.nobodymissing.ui

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nobodymissing.NobodyMissingApp
import org.nobodymissing.features.Features
import org.nobodymissing.features.RouteResult

private val Orange = Color(0xFFF28C00)
private val Danger = Color(0xFFC40000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val f = (application as NobodyMissingApp).features
        setContent { MaterialTheme(typography = Typography(bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp))) { App(f) } }
    }
}

@Composable
fun App(f: Features) {
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { f.scoutCheck(); delay(1000L * f.shared.defaults["scout"]!!.let { (it as kotlinx.serialization.json.JsonObject)["intervalSec"].toString().toLong() }) } }
    val tabs = listOf("🏠 Today", "👥 People", "💊 Medicine", "📢 Announce", "🏚 Field")
    Scaffold(
        topBar = { StatusBar(f) },
        bottomBar = { NavigationBar { tabs.forEachIndexed { i, t -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(t.take(2)) }, label = { Text(t.drop(2)) }) } } },
    ) { pad ->
        Box(Modifier.padding(pad).padding(12.dp)) {
            when (tab) { 0 -> Today(f); 1 -> People(f); 2 -> Medicine(f); 3 -> Announce(f); else -> Field(f) }
        }
        if (f.scoutAlert.value) AlertDialog(onDismissRequest = { f.scoutAlert.value = false }, confirmButton = { Button({ f.scoutAlert.value = false }) { Text("OK") } },
            title = { Text("🐱 Signal is back here!", fontSize = 28.sp) }, text = { Text("Bring devices to sync. Sent ${f.jobs.count { it.status.state == "sent" }} of ${f.jobs.size} items.") })
    }
}

@Composable
fun StatusBar(f: Features) {
    val waiting = f.jobs.count { it.status.state != "sent" }
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🐱 Shelter SD 3", fontWeight = FontWeight.Bold)
        Text(if (f.signal.value == "back") "☁️ Cloud" else "🏮 Device")
        if (waiting > 0) Text("⏳ Waiting ($waiting)")
        if (f.adapters.env == "test") Text("DEMO (no Firebase)")
    }
}

@Composable
fun Today(f: Features) {
    val s = f.schedule()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("💊 ${s.counts.values.firstOrNull() ?: 0}", "next round ${s.counts.keys.firstOrNull() ?: ""}")
        Tile("⚠️ ${s.alerts.size}", "can't-miss", danger = s.alerts.isNotEmpty())
        Tile("🏚 ${f.fieldList().count { it.second.result.tier == "P1" }}", "P1 visits", danger = true)
        Tile("👥 ${f.people.size}", "people")
        Text("Today: ${f.router.deviceCount} on device · ${f.router.cloudCount} cloud", color = Color.Gray)
    }
}

@Composable
fun Tile(big: String, label: String, danger: Boolean = false) = OutlinedCard(Modifier.fillMaxWidth(), border = CardDefaults.outlinedCardBorder().copy(width = if (danger) 3.dp else 1.dp)) {
    Column(Modifier.padding(16.dp)) { Text(big, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = if (danger) Danger else Color.Unspecified); Text(label) }
}

@Composable
fun RouteNotice(r: RouteResult?, onCloud: () -> Unit) {
    when (r?.status) {
        "ask_staff" -> Column { Text("On-device AI isn't ready. Send this photo to the cloud? Private data will leave the device."); Button(onCloud) { Text("☁️ Send to cloud") } }
        "draft" -> Text("⚠️ Needs review")
        "queued" -> Text("⏳ Kit will send this when the signal comes back.")
        "blocked" -> Text("🔒 Private mode: nothing leaves this device.")
        "invalid", "error" -> Text("The AI answer did not pass the check. Please type it in or try again.", color = Danger)
        else -> {}
    }
}

@Composable
fun CameraButton(label: String, onPhoto: (Bitmap) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(onPhoto) }
    Button({ launcher.launch(null) }, Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.Black)) { Text("📷 $label", fontWeight = FontWeight.Bold) }
}

@Composable
fun People(f: Features) {
    val scope = rememberCoroutineScope()
    var last by remember { mutableStateOf<Pair<Bitmap, RouteResult>?>(null) }
    var q by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("🔍 Family search (this shelter)") })
        if (q.isNotBlank()) f.familySearch(q).forEach { Text("${it.name} · ${it.ageBand} · ${it.shelterId} · ${it.status}", fontWeight = FontWeight.Bold) }
        last?.let { (bmp, r) -> RouteNotice(r) { scope.launch { last = bmp to f.scanRoster(bmp, cloudOk = true) } } }
        LazyColumn(Modifier.weight(1f)) {
            items(f.people) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${if (p.needsReview) "⚠️ " else ""}${p.name}, ${p.age ?: "?"} · ${p.flags.joinToString()} ${if (!p.confirmed) "(${p.source})" else ""}")
                    if (!p.confirmed) Button({ f.confirmPerson(p.id) }) { Text("✓ Confirm") }
                }
            }
        }
        CameraButton("Scan list") { bmp -> scope.launch { last = bmp to f.scanRoster(bmp) } }
    }
}

@Composable
fun Medicine(f: Features) {
    val s = f.schedule()
    val name = { id: String -> f.people.firstOrNull { it.id == id }?.name ?: id }
    LazyColumn {
        items(s.alerts) { a -> Text("⚠️ Can't-miss: ${name(a.personId)} · ${a.generic} (${a.type}) · ${a.supplyDays ?: "?"} day(s) left", color = Danger, fontWeight = FontWeight.Bold) }
        s.slots.forEach { (slot, list) ->
            item { Text("$slot · ${list.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            items(list) { e -> Text("${name(e.personId)} · ${e.generic} · ${e.doseAsWritten}") }
        }
        items(f.meds.withIndex().filter { it.value.confirmedBy == null }.toList()) { (i, m) ->
            Column { Text("⚠️ “${m.nameAsWritten}” ${m.strength ?: ""} → ${m.match.status}")
                if (m.match.lookAlikes.isNotEmpty()) Text("Look-alike: ${m.match.lookAlikes.joinToString()}", color = Danger)
                (listOfNotNull(m.match.generic) + m.match.candidates).forEach { g -> Button({ f.confirmMed(i, g, "staff") }) { Text("✓ $g") } } }
        }
        item { Button({ f.requestSupply() }) { Text("📦 Request supplies (counts only)") } }
    }
}

private val ICON = mapOf("food" to "🍙", "water" to "💧", "medicine" to "💊", "doctor" to "🩺", "move" to "🚌", "warning" to "⚠️", "toilet" to "🚻", "info" to "ℹ️", "baby" to "🍼", "power" to "🔌")

@Composable
fun Badge(source: String, draft: Boolean) = Text(when { draft -> "⚠️ Needs review"; source == "device" -> "🏮 Device"; source == "cloud" -> "☁️ Cloud"; else -> "🔤 Keywords (no AI)" }, fontWeight = FontWeight.Bold)

@Composable
fun Announce(f: Features) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf("id") }
    var busy by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Announcement (type or dictate)") })
            Row { listOf("id" to "Bahasa", "en" to "English").forEach { (k, l) -> FilterChip(lang == k, { lang = k }, { Text(l) }) ; Spacer(Modifier.width(8.dp)) } }
            Button({ busy = true; scope.launch { f.announce(text.trim(), lang); text = ""; busy = false } }, Modifier.fillMaxWidth().height(56.dp), enabled = text.isNotBlank() && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.Black)) { Text(if (busy) "…" else "✍️ Make cards", fontWeight = FontWeight.Bold) }
        }
        items(f.announcements) { a ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("Original: “${a.text}”", color = Color.Gray); Badge(a.source, a.draft)
                a.cards.forEach { c -> Text("${ICON[c.icon] ?: "ℹ️"} ${c.title}${c.time?.let { " · $it" } ?: ""}", fontSize = 26.sp, fontWeight = FontWeight.Bold); c.place?.let { Text("📍 $it", fontSize = 20.sp) } }
                if (a.shown) Text("✅ Shown") else Button({ f.approve(a.id) }) { Text("➤ Show") }
            } }
        }
        item { Text("Tell in person:", fontWeight = FontWeight.Bold) }
        items(f.people.filter { p -> p.confirmed && (p.flags.any { it in f.rules.reachFlags } || (p.age ?: 0) >= f.rules.reachAge) }) { Text("• ${it.name} · ${it.flags.filter { fl -> fl in f.rules.reachFlags }.joinToString()}") }
    }
}

@Composable
fun Field(f: Features) {
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var r by remember { mutableStateOf<RouteResult?>(null) }
    LazyColumn {
        items(f.fieldList()) { (name, row) ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("${row.result.tier} · $name", fontWeight = FontWeight.Bold, color = if (row.result.tier == "P1") Danger else Color.Unspecified)
                Text("Why: ${row.result.why.joinToString(" · ").ifEmpty { "—" }}")
            }
        }
        item {
            RouteNotice(r) { scope.launch { r = f.fieldReport(note, null, null, cloudOk = true) } }
            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Pak Hasan, 78, can't walk, needs insulin") })
            Button({ scope.launch { r = f.fieldReport(note, null, null); if (r?.status == "ok") note = "" } }, Modifier.fillMaxWidth(), enabled = note.isNotBlank()) { Text("📝 Report") }
        }
    }
}
