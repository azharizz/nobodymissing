package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Anti-corruption layer: same JSON Schema subset as web validate.ts.
data class Validation(val valid: Boolean, val value: JsonElement?, val errors: List<String>)

private fun typeOf(v: JsonElement): String = when (v) {
    is JsonNull -> "null"
    is JsonArray -> "array"
    is JsonObject -> "object"
    is JsonPrimitive -> when {
        v.isString -> "string"
        v.booleanOrNull != null -> "boolean"
        v.doubleOrNull?.let { it % 1.0 == 0.0 } == true -> "integer"
        else -> "number"
    }
}

fun validate(v: JsonElement, s: JsonObject, path: String = "$", errors: MutableList<String> = mutableListOf()): List<String> {
    s["type"]?.let { t ->
        val types = if (t is JsonArray) t.map { it.jsonPrimitive.content } else listOf(t.jsonPrimitive.content)
        val vt = typeOf(v)
        if (vt !in types && !(vt == "integer" && "number" in types)) { errors += "$path: expected ${types.joinToString("|")}"; return errors }
    }
    (s["enum"] as? JsonArray)?.let { if (v !in it) errors += "$path: not allowed" }
    if (v is JsonPrimitive && !v.isString) v.doubleOrNull?.let { n ->
        s.d("minimum")?.let { if (n < it) errors += "$path: < $it" }
        s.d("maximum")?.let { if (n > it) errors += "$path: > $it" }
    }
    if (v is JsonPrimitive && v.isString) s.i("maxLength")?.let { if (v.content.length > it) errors += "$path: too long" }
    if (v is JsonArray) {
        s.i("maxItems")?.let { if (v.size > it) errors += "$path: too many items" }
        (s["items"] as? JsonObject)?.let { item -> v.forEachIndexed { i, x -> validate(x, item, "$path[$i]", errors) } }
    }
    if (v is JsonObject) {
        s.strings("required").forEach { if (it !in v) errors += "$path.$it: required" }
        (s["properties"] as? JsonObject)?.forEach { (k, sub) -> v[k]?.let { validate(it, sub.jsonObject, "$path.$k", errors) } }
    }
    return errors
}

/** Small models wrap JSON in prose or code fences: take the outermost {...}. */
fun parseJson(raw: String): JsonElement? {
    runCatching { return Json.parseToJsonElement(raw) }
    val a = raw.indexOf('{'); val b = raw.lastIndexOf('}')
    if (a < 0 || b <= a) return null
    return runCatching { Json.parseToJsonElement(raw.substring(a, b + 1)) }.getOrNull()
}

fun parseAndValidate(raw: String, schema: JsonObject): Validation {
    val v = parseJson(raw) ?: return Validation(false, null, listOf("not JSON"))
    val errors = validate(v, schema)
    return Validation(errors.isEmpty(), if (errors.isEmpty()) v else null, errors)
}
