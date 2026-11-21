package org.nobodymissing.ports

import android.graphics.Bitmap
import kotlinx.serialization.json.JsonObject

// Ports: features depend only on these. Adapters come from AdapterFactory.
data class InferenceRequest(val task: String, val prompt: String, val text: String?, val image: Bitmap?, val schema: JsonObject, val mode: String, val model: String)
data class InferenceResult(val raw: String, val source: String)

/** Firebase AI Logic only (D11). onDeviceStatus: available | downloadable | downloading | unavailable. */
interface InferencePort {
    suspend fun onDeviceStatus(): String
    suspend fun generate(r: InferenceRequest): InferenceResult
}
interface SyncPort { suspend fun put(path: String, data: Map<String, Any?>) }
interface ConnectivityPort { suspend fun check(): Boolean }
