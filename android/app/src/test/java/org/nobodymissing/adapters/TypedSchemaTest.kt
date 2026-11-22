package org.nobodymissing.adapters

import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TypedSchemaTest {
    @Test fun everySharedSchemaConvertsForTheCloud() {
        val files = File("../../shared/schemas").listFiles { f -> f.extension == "json" }!!
        files.forEach { typed(Json.parseToJsonElement(it.readText()).jsonObject) }   // throws on anything unsupported
        assertEquals(true, files.size >= 15)
    }
}
