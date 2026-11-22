package org.nobodymissing.adapters

import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.ondevice.FirebaseAIOnDevice
import com.google.firebase.ai.ondevice.OnDeviceModelStatus
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import org.nobodymissing.ports.*

@OptIn(com.google.firebase.ai.type.PublicPreviewAPI::class)
/** The only AI adapter on Android: Firebase AI Logic hybrid (Gemini Nano via AICore + Gemini cloud). */
class FirebaseAiLogicInference : InferencePort {
    private val ai = Firebase.ai(backend = GenerativeBackend.googleAI())

    override suspend fun onDeviceStatus(): String = when (runCatching { FirebaseAIOnDevice.checkStatus() }.getOrNull()) {
        OnDeviceModelStatus.AVAILABLE -> "available"
        OnDeviceModelStatus.DOWNLOADING -> "downloading"
        OnDeviceModelStatus.DOWNLOADABLE -> "downloadable"
        else -> "unavailable"
    }

    /** Background download (never blocks the UI). */
    fun download() = FirebaseAIOnDevice.download()

    override suspend fun generate(r: InferenceRequest): InferenceResult {
        val mode = when (r.mode) {
            "only_on_device" -> InferenceMode.ONLY_ON_DEVICE
            "prefer_on_device" -> InferenceMode.PREFER_ON_DEVICE
            "prefer_in_cloud" -> InferenceMode.PREFER_IN_CLOUD
            else -> InferenceMode.ONLY_IN_CLOUD
        }
        // Grounding with Google Maps (cloud only): no JSON mode with tools; the router parses + validates the text.
        val maps = r.grounding == "maps"
        val model = ai.generativeModel(
            modelName = r.model,
            generationConfig = if (maps) null else generationConfig { responseMimeType = "application/json" },
            tools = if (maps) listOf(Tool.googleMaps()) else null,
            toolConfig = if (maps && r.lat != null && r.lng != null) ToolConfig(retrievalConfig = RetrievalConfig.builder().setLatLng(LatLng(r.lat, r.lng)).build()) else null,
            onDeviceConfig = OnDeviceConfig(mode = mode),
        )
        // The schema travels in the prompt too; the output is always validated by domain/Validate.kt.
        val prompt = "${r.prompt}\nReturn only JSON matching this JSON Schema: ${r.schema}" + (r.text?.let { "\n\n$it" } ?: "")
        val res = model.generateContent(content { r.image?.let { image(it) }; text(prompt) })
        val places = res.candidates.firstOrNull()?.groundingMetadata?.groundingChunks.orEmpty().mapNotNull { it.maps }.map { GroundingPlace(it.title, it.uri, it.placeId) }
        return InferenceResult(res.text ?: "", if (res.inferenceSource == InferenceSource.ON_DEVICE) "device" else "cloud", places)
    }
}
