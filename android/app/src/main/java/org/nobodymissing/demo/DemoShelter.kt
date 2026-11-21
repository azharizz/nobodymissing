package org.nobodymissing.demo

import kotlinx.serialization.json.*
import org.nobodymissing.domain.*
import org.nobodymissing.features.*

/** DEMO ONLY: loads the synthetic Aceh shelter (shared/demo/shelter.json). Real shelters start empty. */
object DemoShelter {
    fun load(f: Features) {
        val demo = f.shared.json("demo/shelter.json")
        f.shelterId.value = demo["shelter"]!!.jsonObject.s("id")!!
        f.people.clear(); f.meds.clear(); f.field.clear(); f.jobs.clear(); f.announcements.clear()
        demo["people"]!!.jsonArray.map { it.jsonObject }.forEach { f.people += UiPerson(it.s("id")!!, it.s("nameAsWritten")!!, it.i("age"), it.strings("flags"), true, false, "staff") }
        demo["cards"]!!.jsonArray.map { it.jsonObject }.forEach { c -> c["meds"]!!.jsonArray.map { it.jsonObject }.forEach { m ->
            f.meds += UiMed(c.s("personId")!!, m.s("nameAsWritten")!!, matchDrug(m.s("nameAsWritten")!!, m.s("strength"), f.shared.drugs), m.s("strength"), m.s("doseAsWritten"), m.i("timesPerDay"), m.i("qtyLeft"), m.s("generic"), m.s("confirmedBy"))
        } }
        demo["field"]!!.jsonArray.map { it.jsonObject }.forEach { e ->
            val loc = e["location"] as? JsonObject
            f.field += UiField(e.s("id")!!, e.s("name")!!, loc?.d("lat"), loc?.d("lng"), e.s("lastContactAt")?.removeSuffix("h")?.toDoubleOrNull(),
                PriorityInput.from(JsonObject(e["person"]!!.jsonObject + ("hazard" to JsonObject(emptyMap())))))
        }
    }
}
