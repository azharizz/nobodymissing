package org.nobodymissing.domain

import kotlinx.serialization.json.*

// Tiny helpers for reading shared/ JSON without a serialization plugin.
fun JsonObject.s(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
fun JsonObject.i(k: String): Int? = (this[k] as? JsonPrimitive)?.intOrNull
fun JsonObject.d(k: String): Double? = (this[k] as? JsonPrimitive)?.doubleOrNull
fun JsonObject.b(k: String): Boolean? = (this[k] as? JsonPrimitive)?.booleanOrNull
fun JsonObject.strings(k: String): List<String> = (this[k] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
