package org.nobodymissing.ports

import android.graphics.Bitmap
import kotlinx.serialization.json.JsonObject

// Ports: features depend only on these. Adapters come from AdapterFactory.
/** grounding "maps" = Grounding with Google Maps (cloud only) near lat/lng, already rounded by the caller. */
data class InferenceRequest(val task: String, val prompt: String, val text: String?, val image: Bitmap?, val schema: JsonObject, val mode: String, val model: String,
                            val grounding: String? = null, val lat: Double? = null, val lng: Double? = null)
data class GroundingPlace(val title: String?, val uri: String?, val placeId: String?)
data class InferenceResult(val raw: String, val source: String, val places: List<GroundingPlace> = emptyList())

/** Firebase AI Logic only (D11). onDeviceStatus: available | downloadable | downloading | unavailable. */
interface InferencePort {
    suspend fun onDeviceStatus(): String
    suspend fun generate(r: InferenceRequest): InferenceResult
}
interface SyncPort { suspend fun put(path: String, data: Map<String, Any?>) }
interface ConnectivityPort { suspend fun check(): Boolean }
