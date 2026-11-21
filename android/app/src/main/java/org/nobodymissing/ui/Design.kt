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

// Design tokens: the same values as web/src/ui/styles.css :root.
object NM {
    val Ink = Color(0xFF14213D); val Ink2 = Color(0xFF3A4A66); val Muted = Color(0xFF5F6B80)
    val Bg = Color(0xFFF6F3EE); val Surface = Color.White; val Surface2 = Color(0xFFFBF9F6); val Line = Color(0xFFE4DED5)
    val Accent = Color(0xFFFF6B2C); val Accent2 = Color(0xFFF2542D); val AccentSoft = Color(0xFFFFE8DC)
    val Danger = Color(0xFFD7263D); val DangerSoft = Color(0xFFFCE4E7)
    val Device = Color(0xFF1F9D63); val DeviceSoft = Color(0xFFDDF5E9)
    val Cloud = Color(0xFF2D6CDF); val CloudSoft = Color(0xFFE1EBFC)
    val Review = Color(0xFFB7791F); val ReviewSoft = Color(0xFFFFF4DB)
}

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
fun Ic(icons: Icons, name: String, size: Dp = 22.dp, tint: Color = NM.Ink, label: String? = null) =
    M3Icon(icons[name], contentDescription = label, modifier = Modifier.size(size), tint = tint)

enum class Mood { IDLE, DEVICE, CLOUD, ALERT, SLEEP }

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
    val glow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(when (mood) { Mood.ALERT -> 500; Mood.CLOUD -> 1200; else -> 2600 }), RepeatMode.Reverse), label = "glow")
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
