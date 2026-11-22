package org.nobodymissing.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*
import org.nobodymissing.domain.d
import org.nobodymissing.domain.s
import org.nobodymissing.features.Shared

// Design tokens: the same values as web/src/ui/styles.css :root ("calm authority": navy anchor, Kit's amber lantern, warm ivory, no gradients).
object NM {
    val Navy = Color(0xFF1C2F4A); val Ink = Color(0xFF14161A); val Ink2 = Color(0xFF5A6170); val Line = Color(0xFFE7E2D9); val Ivory = Color(0xFFFAF7F2)
    val Card = Color.White; val Quiet = Color(0xFFF1EEE8); val Border = Color(0xFFCFC8BC); val OnNavy2 = Color(0xFFC9D3E0)
    val Amber = Color(0xFFF2A93B); val AmberInk = Color(0xFF8A5A00); val AmberSoft = Color(0xFFFFF7E3); val AmberLine = Color(0xFFE8C77A)
    val Honey = Color(0xFFFCE7B2); val HoneyInk = Color(0xFF4A3300); val HoneyAcc = Color(0xFFC98A14)
    val Sky = Color(0xFFDCE8F5); val SkyInk = Color(0xFF17324F); val SkyAcc = Color(0xFF2C5A8C)
    val Rose = Color(0xFFF8DCD5); val RoseInk = Color(0xFF7E2214); val RoseAcc = Color(0xFFC4402B)
    val Sage = Color(0xFFD8ECDF); val SageInk = Color(0xFF1E5236); val SageAcc = Color(0xFF2F7A52)
    val Lav = Color(0xFFE9E1F4); val LavInk = Color(0xFF4B2A6B)
    val Local = Color(0xFFEEF2E8); val LocalInk = Color(0xFF3B5A26); val LocalLine = Color(0xFFA9C08F)
    val Urgent = Color(0xFFC4402B); val Done = Color(0xFF2F7A52)
    /** Soft fill + dark text of the same family (never white text on colour for evacuees). */
    data class Tone(val bg: Color, val fg: Color, val acc: Color)
    val SKY = Tone(Sky, SkyInk, SkyAcc); val ROSE = Tone(Rose, RoseInk, RoseAcc); val SAGE = Tone(Sage, SageInk, SageAcc)
    val LAV = Tone(Lav, LavInk, LavInk); val HONEY = Tone(Honey, HoneyInk, HoneyAcc)
}

/** Headings: Manrope ExtraBold. Body: Atkinson Hyperlegible (made for low vision). Bundled (offline). */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Manrope = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(org.nobodymissing.R.font.manrope, androidx.compose.ui.text.font.FontWeight.ExtraBold,
        variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(800))),
    androidx.compose.ui.text.font.Font(org.nobodymissing.R.font.manrope, androidx.compose.ui.text.font.FontWeight.Bold,
        variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(700))),
)
val Atkinson = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(org.nobodymissing.R.font.atkinson_regular, androidx.compose.ui.text.font.FontWeight.Normal),
    androidx.compose.ui.text.font.Font(org.nobodymissing.R.font.atkinson_bold, androidx.compose.ui.text.font.FontWeight.Bold),
)

private fun hex(s: String) = Color(android.graphics.Color.parseColor(s))

/** Line icon from shared/icons (same SVG paths as web). */
class Icons(shared: Shared) {
    private val cache = shared.icons.mapValues { (name, d) ->
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = PathParser().parsePathString(d).toNodes(), fill = null, stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        ).build()
    }
    operator fun get(name: String): ImageVector = cache[name] ?: cache.getValue("info")
}

@Composable
fun Ic(icons: Icons, name: String, size: Dp = 22.dp, tint: Color = androidx.compose.ui.graphics.Color.Unspecified, label: String? = null) =
    M3Icon(icons[name], contentDescription = label, modifier = Modifier.size(size), tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint)

enum class Mood { IDLE, DEVICE, CLOUD, ALERT, SLEEP, THINK }

internal class Part(val g: String, val path: Path, val fill: Color?, val stroke: Color?, val sw: Float, val alpha: Float)

/** The mascot artwork, parsed once from shared/mascot/kit.json. */
class Mascot(shared: Shared) {
    private val m = shared.mascot
    val w = m["viewBox"]!!.jsonArray[0].jsonPrimitive.float
    val h = m["viewBox"]!!.jsonArray[1].jsonPrimitive.float
    val centers = m["centers"]!!.jsonObject.mapValues { (_, v) -> v.jsonArray.let { Offset(it[0].jsonPrimitive.float, it[1].jsonPrimitive.float) } }
    val beacon = m["beaconColors"]!!.jsonObject.mapValues { hex(it.value.jsonPrimitive.content) }
    val order = m["order"]!!.jsonArray.map { it.jsonPrimitive.content }
    internal val parts = m["parts"]!!.jsonArray.map { it.jsonObject }.map { p ->
        Part(p.s("g")!!, PathParser().parsePathString(p.s("d")!!).toPath(), p.s("fill")?.let(::hex), p.s("stroke")?.let(::hex), (p.d("sw") ?: 0.0).toFloat(), (p.d("opacity") ?: 1.0).toFloat())
    }
}

/** Kit, the calm firefighter cat, drawn from shared/mascot/kit.json; same moods and motion as web:
 *  idle = breathing + blink + tail sway · device = amber lantern glowing · cloud = blue lantern, walking · alert = lantern flashing red/blue, hop · sleep = eyes closed, lantern off. */
@Composable
fun Kit(mascot: Mascot, mood: Mood = Mood.IDLE, size: Dp = 56.dp) {
    val t = rememberInfiniteTransition(label = "kit")
    val bob by t.animateFloat(0f, 1f, infiniteRepeatable(tween(when (mood) { Mood.ALERT -> 700; Mood.CLOUD -> 600; else -> 3600 }, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val blink by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "blink")
    val sway by t.animateFloat(0f, -9f, infiniteRepeatable(tween(when (mood) { Mood.ALERT -> 700; Mood.CLOUD -> 1200; Mood.SLEEP -> 6000; else -> 2800 }, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    val glow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(when (mood) { Mood.ALERT -> 500; Mood.CLOUD -> 1200; Mood.THINK -> 900; else -> 2600 }), RepeatMode.Reverse), label = "glow")
    val siren by t.animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "siren")
    val lanternColor = when (mood) {
        Mood.CLOUD -> mascot.beacon.getValue("cloud")
        Mood.ALERT -> if (siren < .5f) mascot.beacon.getValue("alert") else mascot.beacon.getValue("alert2")
        Mood.SLEEP -> NM.Line
        else -> mascot.beacon.getValue("device")
    }
    val eyesY = when { mood == Mood.SLEEP -> .12f; blink in .46f..0.5f -> .1f; else -> 1f }
    val lift = when (mood) { Mood.ALERT -> -6f * bob; Mood.CLOUD -> -1.5f * bob; else -> 0f }
    val breathe = if (mood == Mood.IDLE || mood == Mood.DEVICE || mood == Mood.SLEEP) 1f + .015f * bob else 1f
    Canvas(Modifier.size(size, size * (mascot.h / mascot.w))) {
        val k = this.size.width / mascot.w
        withTransform({ scale(k, k, Offset.Zero); translate(0f, lift); scale(breathe, 2f - breathe, mascot.centers.getValue("base")) }) {
            for (g in mascot.order) for (p in mascot.parts.filter { it.g == g }) {
                val c = mascot.centers[p.g]
                withTransform({
                    when (p.g) {
                        "tail" -> rotate(sway, c!!)
                        "eyes" -> scale(1f, eyesY, c!!)
                        "glow" -> scale(1f + .3f * glow, 1f + .3f * glow, c!!)
                    }
                }) {
                    val alpha = when (p.g) { "glow" -> if (mood == Mood.SLEEP) 0f else .3f + .55f * glow; else -> p.alpha }
                    val fill = if (p.g == "beacon" || p.g == "glow") lanternColor else p.fill
                    fill?.let { drawPath(p.path, it, alpha = alpha) }
                    p.stroke?.let { drawPath(p.path, it, alpha = alpha, style = Stroke(p.sw, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                }
            }
        }
    }
}
