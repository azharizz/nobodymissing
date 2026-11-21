package org.nobodymissing.adapters

import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.ondevice.FirebaseAIOnDevice
import com.google.firebase.ai.ondevice.OnDeviceModelStatus
import com.google.firebase.ai.type.GenerativeBackend
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
        val model = ai.generativeModel(
            modelName = r.model,
            generationConfig = generationConfig { responseMimeType = "application/json" },
            onDeviceConfig = OnDeviceConfig(mode = mode),
        )
        // The schema travels in the prompt too; the output is always validated by domain/Validate.kt.
        val prompt = "${r.prompt}\nReturn only JSON matching this JSON Schema: ${r.schema}" + (r.text?.let { "\n\n$it" } ?: "")
        val res = model.generateContent(content { r.image?.let { image(it) }; text(prompt) })
        return InferenceResult(res.text ?: "", if (res.inferenceSource == InferenceSource.ON_DEVICE) "device" else "cloud")
    }
}
