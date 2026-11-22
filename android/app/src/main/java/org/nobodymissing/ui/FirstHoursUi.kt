package org.nobodymissing.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.nobodymissing.domain.distanceM
import org.nobodymissing.domain.i
import org.nobodymissing.domain.encodeCheckinQr
import org.nobodymissing.domain.encodePickupQr
import org.nobodymissing.domain.Checkin
import org.nobodymissing.domain.Pickup

// Evacuee screens for the first hours (same as web ui/firstHours.tsx): Go now -> Show the way -> I need pickup -> check-in card.

@Composable
fun BigInput(value: String, hint: String, size: Int = 22, onChange: (String) -> Unit) = Box(Modifier.heightIn(min = 60.dp)) {
    if (value.isEmpty()) H(hint, size.sp, NM.Ink2.copy(alpha = .5f))
    BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, color = NM.Navy))
}

/** Ask for location once; returns a function that requests one fix. */
@SuppressLint("MissingPermission")
@Composable
fun rememberLocate(onPos: (Double, Double) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    fun fix() { val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))?.let { onPos(it.latitude, it.longitude) }
        runCatching { lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, { onPos(it.latitude, it.longitude) }, null) } }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) fix() }
    return { perm.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
}

@Composable
fun EvGo(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf("") }
    val w = f.warning.value; val dest = f.destination()
    val locate = rememberLocate { la, ln -> f.myPos.value = la to ln }
    if (w == null) EvTop(ui, f.t("goSafety"), f.t("goSafetySub")) else {
        val tone = if (w.w.level == "go_now") NM.ROSE else NM.HONEY
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)).background(tone.bg).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CompositionLocalProvider(LocalContentColor provides tone.fg) {
                Row(verticalAlignment = Alignment.CenterVertically) { RoundBtn(ui, "back", f.t("back")) { ui.go("home") }; Spacer(Modifier.width(12.dp)); Kit(ui.kit, if (w.w.level == "go_now") Mood.ALERT else Mood.IDLE, 58.dp); Spacer(Modifier.width(10.dp)); Eyebrow(f.t("hazard_${w.w.hazard}")) }
                H(w.w.plain, 30.sp)
                P("${w.w.action ?: f.t("level_${w.w.level}")} ${f.t("takeMeds")}", 20.sp, bold = true)
                Badge(ui, if (w.w.by == "keywords") "keywords" else w.w.by, text = f.t(when (w.w.by) { "device" -> "plainOnPhone"; "cloud" -> if (w.sources.isNotEmpty()) "officialCloud" else "plainInCloud"; else -> "plainByRules" }))
                if (w.w.by != "cloud" && w.text.isNotBlank()) P("${f.t("original")}: “${w.text}”", 15.sp)
                w.sources.forEach { s -> P(s.title ?: s.uri ?: "", 14.sp) }
            }
        }
    }
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (w != null) {
            if (dest != null) Column(Modifier.fillMaxWidth().soft(R28).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(f.t("goHere"), NM.SageAcc); H(dest.h.name, 26.sp, NM.Navy)
                P("${dest.h.distanceM} m ${dest.h.direction}   ${f.t("higherBy", "m" to dest.h.handGain)}", 18.sp, NM.Ink2, bold = true)
                P(f.t("fromHere", "where" to f.t(if (dest.gps) "myLocation" else "theShelter")), 15.sp, NM.Ink2)
            } else Note(ui, "info", f.t("noHigher"))
            BigBtn(ui, 1, NM.SKY, "nav", f.t("showWay"), f.t("goHere")) { if (dest != null) ui.go("way") }
            BigBtn(ui, 2, NM.ROSE, "mobility", f.t("needPickup"), f.t("needPickupSub")) { ui.go("pickup") }
            SayButton(ui, "warn", listOfNotNull(f.t("hazard_${w.w.hazard}"), f.t("level_${w.w.level}"), w.w.plain, dest?.let { "${f.t("goHere")}: ${it.h.name}, ${it.h.distanceM} m ${it.h.direction}" }).joinToString(". "), if (w.w.level == "go_now") NM.RoseInk else NM.HoneyInk)
        }
        Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            H(f.t("gotWarning"), 20.sp, NM.Navy)
            BigInput(text, f.t("pasteWarning"), 18) { text = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MicButton(ui, Modifier.weight(1f)) { text = it }
                Btn(ui, f.t("makePlain"), "sparkle", BtnStyle.PRIMARY, enabled = text.isNotBlank() && busy.isEmpty(), modifier = Modifier.weight(1f)) { busy = "plain"; scope.launch { f.warn(text); busy = "" } }
            }
            if (f.adapters.env == "test") Btn(ui, f.t("exampleWarning"), style = BtnStyle.LINK) { text = f.t("exampleText") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(ui, f.t("useMyLocation"), "pin", small = true, modifier = Modifier.weight(1f)) { locate() }
            Btn(ui, f.t("checkOfficial"), "cloud", small = true, enabled = f.signal.value == "back" && busy.isEmpty(), modifier = Modifier.weight(1f)) { busy = "latest"; scope.launch { f.latestWarning(); busy = "" } }
        }
    }
}

/** Live GPS + compass while this screen is open; the arrow points to the higher place, the phone buzzes when off course. */
@SuppressLint("MissingPermission")
@Composable
fun EvWay(ui: Ui) {
    val f = ui.f
    val ctx = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var granted by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { perm.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    DisposableEffect(granted) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val l = LocationListener { f.myPos.value = it.latitude to it.longitude }
        if (granted) runCatching { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 3f, l) }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rot = FloatArray(9); val ori = FloatArray(3)
        val sl = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { SensorManager.getRotationMatrixFromVector(rot, e.values); SensorManager.getOrientation(rot, ori); heading = ((Math.toDegrees(ori[0].toDouble()) + 360) % 360).toFloat() }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sm.registerListener(sl, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { lm.removeUpdates(l); sm.unregisterListener(sl) }
    }
    val dest = f.destination() ?: return Column { EvTop(ui, f.t("showWay")); Note(ui, "info", f.t("noHigher")) }
    val from = dest.from
    val m = distanceM(from.first, from.second, dest.lat, dest.lng).toInt()
    val b = run { val r = Math.PI / 180; val dl = (dest.lng - from.second) * r
        val y = Math.sin(dl) * Math.cos(dest.lat * r); val x = Math.cos(from.first * r) * Math.sin(dest.lat * r) - Math.sin(from.first * r) * Math.cos(dest.lat * r) * Math.cos(dl)
        ((Math.atan2(y, x) / r + 360) % 360).toFloat() }
    val rot = heading?.let { b - it } ?: b
    val angle by animateFloatAsState(rot, label = "arrow")
    val off = heading != null && Math.abs(((rot + 540) % 360) - 180) > 60
    var lastBuzz by remember { mutableStateOf(0L) }
    if (off && System.currentTimeMillis() - lastBuzz > 8000) { lastBuzz = System.currentTimeMillis()
        @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)) }
    val arrived = m <= (f.shared.defaults.i("arrivedM") ?: 40)
    EvTop(ui, dest.h.name)
    Box {
        ShelterMap(ui, listOf(Marker("dest", dest.lat, dest.lng, "higher", NM.SageAcc, f.t("higherBy", "m" to dest.h.handGain))), you = from, aspect = .85f)
        Row(Modifier.padding(14.dp).fillMaxWidth().clip(R22).background(NM.Navy).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(66.dp).clip(R20).background(NM.Amber), contentAlignment = Alignment.Center) { Box(Modifier.rotate(angle)) { Ic(ui.icons, "nav", 38.dp, NM.Ink) } }
            Spacer(Modifier.width(16.dp))
            Column { H(if (arrived) f.t("imHere") else f.t("walkM", "m" to m), 28.sp, Color.White)
                if (!arrived) P("${f.t("headDir", "dir" to dest.h.direction)} · ${f.t("minutes", "n" to maxOf(1, m / 70))}", 16.sp, NM.OnNavy2) }
        }
    }
    Column(Modifier.offset(y = (-24).dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(NM.Card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (f.myPos.value == null) Note(ui, "pin", f.t("noGps"))
        if (arrived) Row(Modifier.fillMaxWidth().clip(R22).background(NM.Sage).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Kit(ui.kit, Mood.IDLE, 48.dp); Spacer(Modifier.width(12.dp)); P(f.t("arrived"), 18.sp, NM.SageInk, bold = true) }
        else Note(ui, "vibrate", f.t("vibrateNote"))
        Note(ui, "info", f.t("directionNote"))
        Btn(ui, f.t("imHere"), "check", BtnStyle.PRIMARY, full = true) { ui.go("checkin") }
    }
}

@Composable
fun EvPickup(ui: Ui) {
    val f = ui.f
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(takeHandOff()) }
    var busy by remember { mutableStateOf(false) }
    var p by remember { mutableStateOf<Pickup?>(null) }
    val locate = rememberLocate { la, ln -> f.myPos.value = la to ln }
    val mine = f.myPickup.value
    EvTop(ui, f.t("needPickup"), f.t("sayWho"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (mine == null) {
            Column(Modifier.fillMaxWidth().soft(R28).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BigInput(text, f.t("pickupHint")) { text = it; p = null }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MicButton(ui, Modifier.weight(1f)) { text = it; p = null }
                    Btn(ui, f.t("ask"), "sparkle", BtnStyle.PRIMARY, enabled = text.isNotBlank() && !busy, modifier = Modifier.weight(1f)) { busy = true; scope.launch { p = f.understandPickup(text); busy = false } }
                }
            }
            p?.let { x -> Column(Modifier.fillMaxWidth().soft(R28, NM.Lav).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompositionLocalProvider(LocalContentColor provides NM.LavInk) {
                    Eyebrow(f.t("kitUnderstood"))
                    P("${f.t("who")}: ${x.who} · ${f.t("people_n", "n" to x.people)}", 17.sp, bold = true)
                    P("${f.t("needs")}: ${(listOf(f.t("mob_${x.mobility}")) + x.needs.map { f.t("need_$it") }).joinToString(" · ")}", 17.sp, bold = true)
                    P("${f.t("where")}: ${f.t(if (f.myPos.value != null) "hereGps" else "theShelter")}", 17.sp, bold = true)
                    if (x.by == "device") Badge(ui, "device", text = f.t("understoodOnPhone")) else Badge(ui, "keywords", text = f.t("keywordsOnly"))
                    if (f.myPos.value == null) Btn(ui, f.t("useMyLocation"), "pin", small = true) { locate() }
                    Btn(ui, f.t("sendPickup"), "send", BtnStyle.PRIMARY, full = true) { f.sendPickup(x) }
                }
            } }
        } else {
            val sent = f.pickupSent()
            Row(Modifier.fillMaxWidth().clip(R16).background(if (sent) NM.Sage else NM.Honey).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(ui.icons, if (sent) "check" else "clock", 22.dp, if (sent) NM.SageInk else NM.HoneyInk); Spacer(Modifier.width(10.dp)); P(f.t(if (sent) "pickupSent" else if (f.signal.value == "back") "pickupSending" else "pickupWaiting"), 17.sp, if (sent) NM.SageInk else NM.HoneyInk, bold = true) }
            Column(Modifier.fillMaxWidth().soft(R22).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(qr(encodePickupQr(mine.first), 560).asImageBitmap(), f.t("showVolunteer"), Modifier.size(240.dp)); H(mine.first.who, 20.sp); P(f.t("showVolunteer"), 15.sp, NM.Ink2)
            }
        }
    }
}

private val SELF_FLAGS = listOf("deaf", "hard_of_hearing", "cant_walk", "wheelchair", "needs_meds", "infant", "pregnant")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvCheckinCard(ui: Ui) {
    val f = ui.f
    var name by remember { mutableStateOf(f.familyGroup.value?.me?.name ?: "") }
    var age by remember { mutableStateOf("") }
    var flags by remember { mutableStateOf(listOf<String>()) }
    EvTop(ui, f.t("atShelter"), f.t("showAtDoor"))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Field(name, { name = it }, f.t("yourName"), Modifier.weight(1f)); Field(age, { age = it.filter(Char::isDigit).take(3) }, "Age", Modifier.width(90.dp)) }
        if (name.isNotBlank()) Column(Modifier.fillMaxWidth().clip(R28).background(NM.Navy).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.clip(R20).background(Color.White).padding(12.dp)) { Image(qr(encodeCheckinQr(Checkin(name, age.toIntOrNull(), flags)), 480).asImageBitmap(), f.t("checkinCard"), Modifier.size(200.dp)) }
            H(name, 24.sp, Color.White)
        }
        Eyebrow(f.t("tellStaffOptional"), NM.Ink2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SELF_FLAGS.forEach { fl -> val on = fl in flags
                Text(f.t("flag_$fl"), Modifier.clip(RoundedCornerShape(50)).background(if (on) NM.Navy else NM.Card).border(1.5.dp, if (on) NM.Navy else NM.Border, RoundedCornerShape(50))
                    .clickable { flags = if (on) flags - fl else flags + fl }.padding(horizontal = 16.dp, vertical = 12.dp), color = if (on) Color.White else NM.Ink, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        }
        if ("deaf" in flags || "hard_of_hearing" in flags) Row(Modifier.fillMaxWidth().clip(R22).background(NM.Sage).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Kit(ui.kit, Mood.IDLE, 48.dp); Spacer(Modifier.width(12.dp)); P(f.t("writeNotCall"), 18.sp, NM.SageInk, bold = true) }
    }
}
