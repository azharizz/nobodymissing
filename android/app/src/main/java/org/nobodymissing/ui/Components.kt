package org.nobodymissing.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nobodymissing.features.Features
import org.nobodymissing.features.RouteResult
import org.nobodymissing.features.UiAnnouncement
import java.util.Locale

/** Where we are in the app. Evacuee and staff modes share one activity (Settings switches a phone to evacuee mode). */
data class Route(val name: String, val arg: String? = null)

/** Everything a screen needs: features + shared icons + the mascot + navigation + speech. */
class Ui(val f: Features, val icons: Icons, val kit: Mascot, val speaker: Speaker) {
    val route = mutableStateOf(Route("now"))
    val evacueeMode = mutableStateOf(false)
    fun go(name: String, arg: String? = null) { route.value = Route(name, arg) }
    // The last photo and AI result, handed from the camera to the screen that checks it (memory only).
    var lastPhoto: Bitmap? = null; var lastResult: RouteResult? = null; var lastKind = ""; var lastPerson: String? = null
}

/** OS text-to-speech: an output device, not an AI call. One utterance at a time. */
class Speaker(ctx: Context) {
    var speaking by mutableStateOf<String?>(null); private set
    private val tts = TextToSpeech(ctx) {}.apply {
        setSpeechRate(.9f)
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { speaking = null }
            @Deprecated("Deprecated in Java") override fun onError(id: String?) { speaking = null }
        })
    }
    fun toggle(id: String, text: String, lang: String) {
        tts.stop()
        if (speaking == id) { speaking = null; return }
        tts.language = if (lang == "id") Locale.forLanguageTag("id-ID") else Locale.UK
        speaking = id
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }
}

// ---------- Type ----------
@Composable
fun H(text: String, size: TextUnit = 22.sp, color: Color = Color.Unspecified, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier, color = if (color == Color.Unspecified) LocalContentColor.current else color, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold,
        fontSize = size, lineHeight = size * 1.1f, letterSpacing = (-0.02).sp, maxLines = maxLines)
@Composable
fun P(text: String, size: TextUnit = 16.sp, color: Color = Color.Unspecified, bold: Boolean = false, modifier: Modifier = Modifier) =
    Text(text, modifier, color = if (color == Color.Unspecified) LocalContentColor.current else color, fontFamily = Atkinson, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = size, lineHeight = size * 1.35f)
@Composable
fun Eyebrow(text: String, color: Color = Color.Unspecified) = Text(text.uppercase(), color = if (color == Color.Unspecified) LocalContentColor.current else color, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp)

// ---------- Surfaces ----------
val R28 = RoundedCornerShape(28.dp); val R22 = RoundedCornerShape(22.dp); val R16 = RoundedCornerShape(16.dp); val R20 = RoundedCornerShape(20.dp)
fun Modifier.soft(shape: androidx.compose.ui.graphics.Shape = R22, bg: Color = NM.Card) = this.shadow(5.dp, shape, ambientColor = NM.Navy.copy(alpha = .10f), spotColor = NM.Navy.copy(alpha = .10f)).clip(shape).background(bg)

@Composable
fun CardBox(modifier: Modifier = Modifier, bg: Color = NM.Card, amber: Boolean = false, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val m = if (amber) modifier.fillMaxWidth().clip(R22).background(NM.AmberSoft).border(2.dp, NM.AmberLine, R22) else modifier.fillMaxWidth().soft(R22, bg)
    CompositionLocalProvider(LocalContentColor provides if (amber) NM.AmberInk else NM.Ink) {
        Column(m.padding(pad), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

enum class BtnStyle { PRIMARY, QUIET, AMBER, DANGER, LINK }
@Composable
fun Btn(ui: Ui, label: String, icon: String? = null, style: BtnStyle = BtnStyle.QUIET, full: Boolean = false, enabled: Boolean = true, small: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .97f else 1f, spring(dampingRatio = .5f), label = "press")
    val (bg, fg) = when (style) { BtnStyle.PRIMARY -> NM.Navy to Color.White; BtnStyle.AMBER -> NM.Amber to NM.Ink; BtnStyle.DANGER -> NM.Rose to NM.RoseInk; BtnStyle.LINK -> Color.Transparent to NM.Navy; else -> NM.Card to NM.Ink }
    val shape = if (small) R16 else R20
    Row((if (full) modifier.fillMaxWidth() else modifier).scale(s).heightIn(min = if (small) 48.dp else 60.dp)
        .then(if (style == BtnStyle.PRIMARY) Modifier.shadow(8.dp, shape, spotColor = NM.Navy.copy(alpha = .3f)) else Modifier).clip(shape).background(bg)
        .then(if (style == BtnStyle.QUIET) Modifier.border(1.5.dp, NM.Border, shape) else Modifier)
        .clickable(src, null, enabled = enabled, onClick = onClick).padding(horizontal = if (small) 14.dp else 20.dp).then(if (enabled) Modifier else Modifier.scale(1f)),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        val c = if (enabled) fg else fg.copy(alpha = .45f)
        icon?.let { Ic(ui.icons, it, 20.dp, c); Spacer(Modifier.width(10.dp)) }
        Text(label, color = c, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 16.sp else 18.sp, maxLines = 2)
    }
}

@Composable
fun RoundBtn(ui: Ui, icon: String, label: String, onNavy: Boolean = false, onClick: () -> Unit) =
    Box(Modifier.size(if (onNavy) 48.dp else 56.dp).then(if (onNavy) Modifier.clip(CircleShape).background(Color.White.copy(alpha = .12f)) else Modifier.soft(CircleShape))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) { Ic(ui.icons, icon, 22.dp, if (onNavy) Color.White else NM.Navy, label) }

@Composable
fun Pill(text: String, bg: Color = NM.Quiet, fg: Color = NM.Ink, ui: Ui? = null, icon: String? = null) =
    Row(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ui != null && icon != null) { Ic(ui.icons, icon, 14.dp, fg); Spacer(Modifier.width(6.dp)) }
        Text(text, color = fg, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }

/** Where the AI ran, always visible (icon + words, never colour alone). */
@Composable
fun Badge(ui: Ui, source: String?, draft: Boolean = false, text: String? = null) {
    val f = ui.f
    val (bg, fg, ic, word) = when {
        draft -> listOf(NM.AmberSoft, NM.AmberInk, "alert", text ?: f.t("review"))
        source == "device" -> listOf(NM.Local, NM.LocalInk, "device", text ?: f.t("madeOnPhone"))
        source == "cloud" -> listOf(NM.Sky, NM.SkyInk, "cloud", text ?: f.t("madeInCloud"))
        source == "maps" -> listOf(NM.Card, NM.Ink, "map", text ?: f.t("fromMaps"))
        source == "keywords" -> listOf(NM.Quiet, NM.Ink, "keywords", text ?: f.t("keywords"))
        else -> return
    }
    Row(Modifier.clip(RoundedCornerShape(50)).background(bg as Color).then(if (source == "device" && !draft) Modifier.border(1.5.dp, NM.LocalLine, RoundedCornerShape(50)) else if (source == "maps") Modifier.border(1.5.dp, NM.Border, RoundedCornerShape(50)) else Modifier)
        .padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Ic(ui.icons, ic as String, 15.dp, fg as Color); Spacer(Modifier.width(8.dp))
        Text(word as String, color = fg, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
fun Note(ui: Ui, icon: String, text: String, color: Color = NM.Ink2) = Row(verticalAlignment = Alignment.Top) {
    Box(Modifier.padding(top = 2.dp)) { Ic(ui.icons, icon, 16.dp, color) }; Spacer(Modifier.width(8.dp)); P(text, 15.sp, color)
}

/** Never send private data to the cloud silently: the staff member decides here. */
@Composable
fun RouteNotice(ui: Ui, r: RouteResult?, onCloud: () -> Unit) {
    val f = ui.f
    when (r?.status) {
        "ask_staff" -> CardBox(amber = true) { Note(ui, "cloud", f.t("askCloud"), NM.AmberInk); Btn(ui, f.t("sendCloud"), "send", BtnStyle.PRIMARY, small = true, onClick = onCloud) }
        "draft" -> CardBox(amber = true) { Note(ui, "alert", f.t("review"), NM.AmberInk) }
        "queued" -> CardBox { Note(ui, "clock", f.t("queued")) }
        "blocked" -> CardBox { Note(ui, "lock", f.t("blocked")) }
        "invalid", "error" -> CardBox(amber = true) { Note(ui, "alert", f.t("invalid"), NM.AmberInk) }
        else -> {}
    }
}

/** Every list item rises into place, staggered. */
@Composable
fun Rise(i: Int, content: @Composable () -> Unit) {
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(shown, enter = fadeIn(tween(400, delayMillis = 55 * minOf(i, 8))) + slideInVertically(tween(500, delayMillis = 55 * minOf(i, 8), easing = EaseOutCubic)) { it / 6 }) { content() }
}

@Composable
fun Empty(ui: Ui, text: String, mood: Mood = Mood.SLEEP) = Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Kit(ui.kit, mood, 110.dp); Spacer(Modifier.height(8.dp)); P(text, 18.sp, NM.Ink2)
}

@Composable
fun Avatar(name: String, tone: NM.Tone = NM.SKY, size: Dp = 56.dp) = Box(Modifier.size(size).clip(CircleShape).background(tone.bg), contentAlignment = Alignment.Center) {
    H(initial(name), (size.value * .4f).sp, tone.fg)
}
fun initial(n: String) = n.replace(Regex("^(Pak|Ibu|Bu|Bapak|Nenek|Kakek|M\\.)\\s+", RegexOption.IGNORE_CASE), "").take(1).uppercase()
fun nice(s: String) = s.replace('_', ' ')

/** Read aloud: big outlined button in the evacuee colours. */
@Composable
fun SayButton(ui: Ui, id: String, text: String, color: Color = NM.HoneyInk) {
    val on = ui.speaker.speaking == id
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(R20).background(Color.White.copy(alpha = if (on) 1f else .7f)).border(2.dp, color, R20)
        .clickable { ui.speaker.toggle(id, text, ui.f.lang.value) }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Ic(ui.icons, if (on) "stop" else "speaker", 22.dp, color); Spacer(Modifier.width(10.dp)); H(if (on) ui.f.t("stop") else ui.f.t("readAloud"), 20.sp, color)
    }
}

/** OS speech-to-text (offline preferred). The text then goes to Nano like typed text. */
@Composable
fun MicButton(ui: Ui, modifier: Modifier = Modifier, onText: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText)
    }
    Row(modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).background(NM.Rose).clickable {
        runCatching { launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (ui.f.lang.value == "id") "id-ID" else "en-US").putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)) }
    }.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Ic(ui.icons, "mic", 20.dp, NM.RoseInk); Spacer(Modifier.width(8.dp)); H(ui.f.t("speak"), 16.sp, NM.RoseInk)
    }
}

/** Read a picked image, downscaled to 1280 px (small prompt, fast on-device). */
fun loadBitmap(ctx: Context, uri: Uri): Bitmap? = runCatching {
    val b = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
    val k = minOf(1f, 1280f / maxOf(b.width, b.height))
    if (k < 1f) Bitmap.createScaledBitmap(b, (b.width * k).toInt(), (b.height * k).toInt(), true) else b
}.getOrNull()

/** First card of an announcement as words. */
fun headline(a: UiAnnouncement): Triple<String, String?, String?> {
    val c = a.cards.firstOrNull()
    val title = if (a.source == "keywords" || c == null) a.text.split(Regex("\\s+")).take(4).joinToString(" ") else c.title
    return Triple(title, c?.time, c?.place)
}
fun cardIcon(a: UiAnnouncement) = a.cards.firstOrNull()?.icon ?: a.icons.firstOrNull() ?: "info"

// ---------- Map: OpenStreetMap tiles (cached on disk for offline) + our hazard overlay + markers. Same projection as web. ----------
data class Marker(val id: String, val lat: Double, val lng: Double, val icon: String, val color: Color, val label: String, val halo: Boolean = false, val sel: Boolean = false, val onClick: (() -> Unit)? = null)

private const val TILE = 256.0
fun worldPx(lat: Double, lng: Double, z: Int): Pair<Double, Double> {
    val n = TILE * (1 shl z); val r = Math.toRadians(lat)
    return (lng + 180) / 360 * n to (1 - kotlin.math.ln(kotlin.math.tan(r) + 1 / kotlin.math.cos(r)) / Math.PI) / 2 * n
}
/** Highest zoom where the points use at most 60% of the box (same rule as web fitView). */
fun fitView(pts: List<Pair<Double, Double>>, w: Double, h: Double, maxZoom: Int): Triple<Int, Double, Double> {
    var z = maxZoom
    while (z > 3) {
        val px = pts.map { worldPx(it.first, it.second, z) }
        if (px.maxOf { it.first } - px.minOf { it.first } <= w * .6 && px.maxOf { it.second } - px.minOf { it.second } <= h * .6) break
        z--
    }
    val px = pts.map { worldPx(it.first, it.second, z) }
    return Triple(z, (px.maxOf { it.first } + px.minOf { it.first }) / 2 - w / 2, (px.maxOf { it.second } + px.minOf { it.second }) / 2 + h * .04 - h / 2)
}

/** Tiles this phone has seen stay on disk, so the map still shows with no signal (no bulk download). */
object Tiles {
    suspend fun get(ctx: Context, url: String): androidx.compose.ui.graphics.ImageBitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching<androidx.compose.ui.graphics.ImageBitmap?> {
            val f = java.io.File(ctx.cacheDir, "tiles/" + url.substringAfter("://").replace('/', '_'))
            if (!f.exists()) {
                f.parentFile!!.mkdirs()
                val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.setRequestProperty("User-Agent", "NobodyMissing-App/1.0"); c.connectTimeout = 8000; c.readTimeout = 8000
                c.inputStream.use { i -> f.outputStream().use { i.copyTo(it) } }
            }
            BitmapFactory.decodeFile(f.path)?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun ShelterMap(ui: Ui, markers: List<Marker>, you: Pair<Double, Double>? = null, aspect: Float = .8f, modifier: Modifier = Modifier, chips: @Composable RowScope.() -> Unit = {}) {
    val p = ui.f.shared.pack
    val cfg = ui.f.shared.defaults["map"]!! as kotlinx.serialization.json.JsonObject
    val tileUrl = (cfg["tiles"] as kotlinx.serialization.json.JsonPrimitive).content
    val maxZoom = (cfg["maxZoom"] as kotlinx.serialization.json.JsonPrimitive).content.toInt()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(0.dp)).background(Color(0xFFEEEDE6))) {
        val w = maxWidth.value.toDouble(); val h = maxHeight.value.toDouble()
        val pts = (markers.map { it.lat to it.lng } + listOfNotNull(you)).ifEmpty { listOf(p.originLat to p.originLng, p.originLat + p.rows * p.step to p.originLng + p.cols * p.step) }
        val (z, left, top) = fitView(pts, w, h, maxZoom)
        fun xy(lat: Double, lng: Double) = worldPx(lat, lng, z).let { (x, y) -> (x - left) to (y - top) }
        var tx = kotlin.math.floor(left / TILE).toInt()
        while (tx * TILE < left + w) {
            var ty = kotlin.math.floor(top / TILE).toInt()
            while (ty * TILE < top + h) {
                val url = tileUrl.replace("{z}", "$z").replace("{x}", "$tx").replace("{y}", "$ty")
                val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, url) { value = Tiles.get(ctx, url) }
                bmp?.let { androidx.compose.foundation.Image(it, null, Modifier.offset((tx * TILE - left).dp, (ty * TILE - top).dp).size(TILE.dp), alpha = .9f) }
                ty++
            }
            tx++
        }
        Canvas(Modifier.fillMaxSize()) {
            val k = density
            for (r in 0 until p.rows) for (c in 0 until p.cols) {
                val hand = p.hand[r][c]; if (hand > 5) continue
                val (x0, y1) = xy(p.originLat + r * p.step, p.originLng + c * p.step); val (x1, y0) = xy(p.originLat + (r + 1) * p.step, p.originLng + (c + 1) * p.step)
                val o = Offset((x0 * k).toFloat(), (y0 * k).toFloat()); val sz = Size(((x1 - x0) * k).toFloat(), ((y1 - y0) * k).toFloat())
                drawRect(if (hand < 2) Color(0x472C5A8C) else Color(0x2E5E95CF), o, sz)
                if (hand >= 2) { var d = -sz.height; while (d < sz.width) { drawLine(Color(0x592C5A8C), Offset(o.x + maxOf(d, 0f), o.y + maxOf(-d, 0f)), Offset(o.x + minOf(d + sz.height, sz.width), o.y + minOf(sz.width - d, sz.height)), 3f * k); d += 11f * k } }
            }
        }
        you?.let { (la, ln) -> val (x, y) = xy(la, ln); Box(Modifier.offset((x - 13).dp, (y - 13).dp).size(26.dp).clip(CircleShape).background(NM.Navy).border(5.dp, Color.White, CircleShape)) }
        markers.forEach { m -> val (x, y) = xy(m.lat, m.lng)
            Column(Modifier.offset((x - 50).dp, (y - 72).dp).width(100.dp).then(if (m.onClick != null) Modifier.clickable(onClick = m.onClick) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(46.dp).then(if (m.halo) Modifier.border(8.dp, NM.Urgent.copy(alpha = .2f), RoundedCornerShape(50)) else Modifier).rotate(-45f)
                    .shadow(6.dp, RoundedCornerShape(23.dp, 23.dp, 23.dp, 6.dp)).clip(RoundedCornerShape(23.dp, 23.dp, 23.dp, 6.dp)).background(m.color).border(3.dp, Color.White, RoundedCornerShape(23.dp, 23.dp, 23.dp, 6.dp)), contentAlignment = Alignment.Center) {
                    Box(Modifier.rotate(45f)) { Ic(ui.icons, m.icon, 20.dp, Color.White) }
                }
                Spacer(Modifier.height(4.dp))
                Text(m.label, Modifier.soft(RoundedCornerShape(10.dp), if (m.sel) NM.Navy else NM.Card).padding(horizontal = 10.dp, vertical = 3.dp), color = if (m.sel) Color.White else NM.Ink,
                    fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
            }
        }
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = chips)
        Text(ui.f.t("mapCredit"), Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .85f)).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = NM.Ink2, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun MapChip(text: String, dot: Color) = Row(Modifier.soft(RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(8.dp)); Text(text, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = NM.Ink)
}
