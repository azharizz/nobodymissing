package org.nobodymissing.domain

import kotlinx.serialization.json.*
import kotlin.math.*

// Region pack lookups (6.11b). Same logic as web regionPack.ts. Never says "safe".
data class PackShelter(val id: String, val name: String, val lat: Double, val lng: Double, val hazards: List<String>)
data class Pack(val originLng: Double, val originLat: Double, val step: Double, val cols: Int, val rows: Int, val hand: List<List<Double>>, val shelters: List<PackShelter>) {
    companion object {
        fun from(o: JsonObject): Pack {
            val g = o["grid"]!!.jsonObject; val origin = g["origin"]!!.jsonArray.map { it.jsonPrimitive.double }
            return Pack(origin[0], origin[1], g.d("step")!!, g.i("cols")!!, g.i("rows")!!, g["hand"]!!.jsonArray.map { r -> r.jsonArray.map { it.jsonPrimitive.double } },
                o["shelters"]!!.jsonArray.map { it.jsonObject.let { s -> PackShelter(s.s("id")!!, s.s("name")!!, s.d("lat")!!, s.d("lng")!!, s.strings("hazards")) } })
        }
    }
}
data class HigherPlace(val id: String, val name: String, val distanceM: Long, val direction: String, val handGain: Double)

fun handAt(p: Pack, lat: Double, lng: Double): Double? {
    val r = floor((lat - p.originLat) / p.step).toInt(); val c = floor((lng - p.originLng) / p.step).toInt()
    return if (r < 0 || c < 0 || r >= p.rows || c >= p.cols) null else p.hand[r][c]
}

fun distanceM(a: Double, b: Double, c: Double, d: Double): Double {
    val rad = PI / 180; val dp = (c - a) * rad; val dl = (d - b) * rad
    val h = sin(dp / 2).pow(2) + cos(a * rad) * cos(c * rad) * sin(dl / 2).pow(2)
    return 2 * 6371000 * asin(sqrt(h))
}

fun direction(a: Double, b: Double, c: Double, d: Double): String {
    val rad = PI / 180; val dl = (d - b) * rad
    val y = sin(dl) * cos(c * rad); val x = cos(a * rad) * sin(c * rad) - sin(a * rad) * cos(c * rad) * cos(dl)
    val deg = (atan2(y, x) / rad + 360) % 360
    return listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[Math.round(deg / 45).toInt() % 8]
}

fun nearestHigher(p: Pack, lat: Double, lng: Double, event: String, minGain: Double): HigherPlace? {
    val here = handAt(p, lat, lng) ?: return null
    val best = p.shelters.filter { event in it.hazards }
        .map { s -> Triple(s, (handAt(p, s.lat, s.lng) ?: Double.NEGATIVE_INFINITY) - here, distanceM(lat, lng, s.lat, s.lng)) }
        .filter { it.second >= minGain }.minByOrNull { it.third } ?: return null
    return HigherPlace(best.first.id, best.first.name, Math.round(best.third), direction(lat, lng, best.first.lat, best.first.lng), Math.round(best.second * 10) / 10.0)
}
