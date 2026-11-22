package org.nobodymissing.adapters

import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.ondevice.FirebaseAIOnDevice
import com.google.firebase.ai.ondevice.OnDeviceModelStatus
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.*
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
        val maps = r.grounding == "maps"; val search = r.grounding == "search"
        val model = ai.generativeModel(
            modelName = r.model,
            generationConfig = if (maps || search) null else generationConfig { responseMimeType = "application/json"; if (r.mode.endsWith("cloud")) responseJsonSchema = typed(r.schema) },
            tools = if (maps) listOf(Tool.googleMaps()) else if (search) listOf(Tool.googleSearch()) else null,
            toolConfig = if (maps && r.lat != null && r.lng != null) ToolConfig(retrievalConfig = RetrievalConfig.builder().setLatLng(LatLng(r.lat, r.lng)).build()) else null,
            onDeviceConfig = OnDeviceConfig(mode = mode),
        )
        // Typed schema constrains cloud calls like web (not on-device: unverified on AICore); the schema also travels in the prompt; domain/Validate.kt always checks the output.
        val prompt = "${r.prompt}\nReturn only JSON matching this JSON Schema: ${r.schema}" + (r.text?.let { "\n\n$it" } ?: "")
        val res = model.generateContent(content { r.image?.let { image(it) }; text(prompt) })
        val places = res.candidates.firstOrNull()?.groundingMetadata?.groundingChunks.orEmpty().mapNotNull { c -> c.maps?.let { GroundingPlace(it.title, it.uri, it.placeId) } ?: c.web?.let { GroundingPlace(it.title, it.uri, null) } }
        return InferenceResult(res.text ?: "", if (res.inferenceSource == InferenceSource.ON_DEVICE) "device" else "cloud", places)
    }
}

/** Shared JSON Schema -> SDK JsonSchema, shape only: the cloud refuses length/count limits ("too many states"); Validate.kt checks them. */
internal fun typed(s: JsonObject): JsonSchema<*> {
    val types = (s["type"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: listOf(s["type"]!!.jsonPrimitive.content)
    val nullable = "null" in types; val t = types.first { it != "null" }
    val title = s["title"]?.jsonPrimitive?.content
    val enum = (s["enum"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
    return when {
        enum != null -> JsonSchema.enumeration(enum, null, nullable, title)
        t == "object" -> { val props = s["properties"]!!.jsonObject; val req = (s["required"] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
            JsonSchema.obj(props.mapValues { typed(it.value.jsonObject) }, props.keys.filter { it !in req }, null, nullable, title) }
        t == "array" -> JsonSchema.array(typed(s["items"]!!.jsonObject), null, nullable, title)
        t == "integer" -> JsonSchema.integer(null, nullable, title)
        t == "number" -> JsonSchema.double(null, nullable, title)
        t == "boolean" -> JsonSchema.boolean(null, nullable, title)
        else -> JsonSchema.string(null, nullable, null, title)
    }
}
