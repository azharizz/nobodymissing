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

// ---------- Map: the offline region pack drawn as a calm map (no tiles, no network) ----------
data class Marker(val id: String, val lat: Double, val lng: Double, val icon: String, val color: Color, val label: String, val halo: Boolean = false, val sel: Boolean = false, val onClick: (() -> Unit)? = null)

@Composable
fun ShelterMap(ui: Ui, markers: List<Marker>, you: Pair<Double, Double>? = null, aspect: Float = .8f, modifier: Modifier = Modifier, chips: @Composable RowScope.() -> Unit = {}) {
    val p = ui.f.shared.pack
    val pts = markers.map { it.lat to it.lng } + listOfNotNull(you)
    var minLat = pts.minOfOrNull { it.first } ?: p.originLat; var maxLat = pts.maxOfOrNull { it.first } ?: (p.originLat + p.rows * p.step)
    var minLng = pts.minOfOrNull { it.second } ?: p.originLng; var maxLng = pts.maxOfOrNull { it.second } ?: (p.originLng + p.cols * p.step)
    var spanLat = maxOf(maxLat - minLat, .006) * 1.6; var spanLng = maxOf(maxLng - minLng, .006) * 1.5
    if (spanLng / spanLat > aspect) spanLat = spanLng / aspect else spanLng = spanLat * aspect
    val cLat = (minLat + maxLat) / 2 + spanLat * .04; val cLng = (minLng + maxLng) / 2
    minLat = cLat - spanLat / 2; maxLat = cLat + spanLat / 2; minLng = cLng - spanLng / 2; maxLng = cLng + spanLng / 2
    val mLng = minLng; val mLat = maxLat; val sLng = spanLng; val sLat = spanLat
    val fx = { lng: Double -> ((lng - mLng) / sLng).toFloat() }
    val fy = { lat: Double -> ((mLat - lat) / sLat).toFloat() }
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(aspect).background(Color(0xFFEEEDE6))) {
        val w = maxWidth; val h = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            for (r in 0 until p.rows) for (c in 0 until p.cols) {
                val hand = p.hand[r][c]; if (hand > 5) continue
                val lat0 = p.originLat + r * p.step; val lng0 = p.originLng + c * p.step
                val x0 = fx(lng0) * size.width; val y0 = fy(lat0 + p.step) * size.height
                val sz = Size(fx(lng0 + p.step) * size.width - x0, fy(lat0) * size.height - y0)
                drawRoundRect(if (hand < 2) Color(0xFFA9C7E3) else Color(0xFFCFE0EF), Offset(x0, y0), sz, CornerRadius(40f))
                if (hand >= 2) { var k = -sz.height; while (k < sz.width) { drawLine(Color(0xFFB9D1E8), Offset(x0 + maxOf(k, 0f), y0 + maxOf(-k, 0f)), Offset(x0 + minOf(k + sz.height, sz.width), y0 + minOf(sz.width - k, sz.height)), 7f); k += 22f } }
            }
        }
        you?.let { (la, ln) -> Box(Modifier.offset(w * fx(ln) - 13.dp, h * fy(la) - 13.dp).size(26.dp).shadow(0.dp, CircleShape).clip(CircleShape).background(NM.Navy).border(5.dp, Color.White, CircleShape)) }
        markers.forEach { m ->
            Column(Modifier.offset(w * fx(m.lng) - 50.dp, h * fy(m.lat) - 72.dp).width(100.dp).then(if (m.onClick != null) Modifier.clickable(onClick = m.onClick) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}
@Composable
fun MapChip(text: String, dot: Color) = Row(Modifier.soft(RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(8.dp)); Text(text, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = NM.Ink)
}
