package org.nobodymissing.features

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.*
import org.nobodymissing.adapters.Adapters
import org.nobodymissing.domain.*

/** Shared JSON (rules, schemas, prompts, drugs, region pack, synthetic demo shelter) bundled from shared/ as assets. */
class Shared(private val read: (String) -> String) {
    fun json(p: String) = Json.parseToJsonElement(read(p)).jsonObject
    val defaults = json("rules/defaults.json")
    val rules = Rules.from(defaults)
    val schemas = listOf("roster", "careCard", "announcementCard", "fieldReport", "request", "translation").associateWith { json("schemas/$it.json") }
    val prompts = json("prompts/pack.json")["tasks"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    val drugs = Drug.listFrom(json("drugs/demo-drugs.json"))
    val keywords = Keywords.from(json("rules/keywords.json"))
    val pack = Pack.from(json("regions/aceh-demo.json"))
    val demo = json("fixtures/demo-shelter.json")
    companion object { fun from(c: Context) = Shared { c.assets.open(it).bufferedReader().readText() } }
}

data class UiPerson(val id: String, val name: String, val age: Int?, val flags: List<String>, val confirmed: Boolean, val needsReview: Boolean, val source: String)
data class UiMed(val personId: String, val nameAsWritten: String, val match: DrugMatch, val strength: String?, val dose: String?, val timesPerDay: Int?, val qtyLeft: Int?, val generic: String?, val confirmedBy: String?)
data class UiJob(val id: String, val type: String, val path: String, val payload: Map<String, Any?>, val createdAt: Long, var status: JobStatus = JobStatus("waiting", 0))

/** FeatureFactory: wires every feature to its ports. Features never construct adapters. */
class Features(val adapters: Adapters, val shared: Shared) {
    val rules = shared.rules
    val privateMode = mutableStateOf(false)
    val signal = mutableStateOf("no_signal")
    val scoutAlert = mutableStateOf(false)
    private var scoutRun = 0
    val router = InferenceRouterFactory.create(adapters.inference, rules, shared.schemas, shared.prompts,
        deviceTimeoutMs = shared.defaults["timeouts"]!!.jsonObject.i("deviceMs")!!.toLong(), cloudTimeoutMs = shared.defaults["timeouts"]!!.jsonObject.i("cloudMs")!!.toLong()) {
        Ctx(signal.value == "back", privateMode.value)
    }
    val people = mutableStateListOf<UiPerson>().apply {
        shared.demo["people"]!!.jsonArray.map { it.jsonObject }.forEach { add(UiPerson(it.s("id")!!, it.s("nameAsWritten")!!, it.i("age"), it.strings("flags"), true, false, "staff")) }
    }
    val meds = mutableStateListOf<UiMed>().apply {
        shared.demo["cards"]!!.jsonArray.map { it.jsonObject }.forEach { c -> c["meds"]!!.jsonArray.map { it.jsonObject }.forEach { m ->
            add(UiMed(c.s("personId")!!, m.s("nameAsWritten")!!, matchDrug(m.s("nameAsWritten")!!, m.s("strength"), shared.drugs), m.s("strength"), m.s("doseAsWritten"), m.i("timesPerDay"), m.i("qtyLeft"), m.s("generic"), m.s("confirmedBy")))
        } }
    }
    val jobs = mutableStateListOf<UiJob>()

    // F1 roster
    suspend fun scanRoster(photo: Bitmap, cloudOk: Boolean = false): RouteResult {
        val r = router.run("roster_read", image = photo, staffApprovedCloud = cloudOk)
        if (r.status == "ok" || r.status == "draft") mapRosterRows(r.data!!.jsonObject["rows"]!!.jsonArray.map { RosterRow.from(it.jsonObject) }, r.source!!, rules)
            .forEach { people += UiPerson(java.util.UUID.randomUUID().toString(), it.nameAsWritten, it.age, it.suggestedFlags, false, it.needsReview || r.status == "draft", it.inferredBy) }
        return r
    }
    fun confirmPerson(id: String) { val i = people.indexOfFirst { it.id == id }; if (i >= 0) people[i] = people[i].copy(confirmed = true, needsReview = false) }

    // F4 care card + F9 Medicine Round
    suspend fun readMeds(personId: String, photo: Bitmap, cloudOk: Boolean = false): RouteResult {
        val r = router.run("care_card_read", image = photo, staffApprovedCloud = cloudOk)
        if (r.status == "ok" || r.status == "draft") r.data!!.jsonObject["meds"]!!.jsonArray.map { it.jsonObject }.forEach { m ->
            meds += UiMed(personId, m.s("nameAsWritten")!!, matchDrug(m.s("nameAsWritten")!!, m.s("strength"), shared.drugs), m.s("strength"), m.s("doseAsWritten"), m.i("timesPerDay"), m.i("qtyLeft"), null, null)
        }
        return r
    }
    fun confirmMed(index: Int, generic: String, by: String) { meds[index] = meds[index].copy(generic = generic, confirmedBy = by) }
    fun schedule() = buildSchedule(meds.filter { it.confirmedBy != null && it.generic != null }.groupBy { it.personId }
        .map { (pid, ms) -> Card(pid, ms.map { CardMed(it.generic, it.strength, it.dose, it.timesPerDay, it.qtyLeft) }) }, rules)
    fun requestSupply() {
        val s = schedule()
        if (s.alerts.isNotEmpty()) enqueue("cant_miss_alert", "alerts", mapOf("counts" to s.alerts.groupingBy { it.type }.eachCount(), "minSupplyDays" to s.alerts.minOf { it.supplyDays ?: 99 }))
        if (s.shortages.isNotEmpty()) enqueue("med_shortage", "requests", mapOf("items" to s.shortages.map { mapOf("generic" to it.generic, "strength" to it.strength, "qty" to it.people) }))
    }

    // F12-lite field check (demo entries from the synthetic shelter, hazard from the region pack)
    fun fieldList(): List<Pair<String, RankedRow>> {
        val entries = shared.demo["field"]!!.jsonArray.map { it.jsonObject }
        val names = entries.associate { it.s("id")!! to it.s("name")!! }
        val rows = entries.map { e ->
            val loc = e["location"] as? JsonObject
            val p = PriorityInput.from(e["person"]!!.jsonObject.let { JsonObject(it + ("hazard" to JsonObject(emptyMap()))) })
            val hazard = if (loc == null) Hazard(unknown = true) else Hazard(hand = handAt(shared.pack, loc.d("lat")!!, loc.d("lng")!!))
            val hrs = e.s("lastContactAt")?.removeSuffix("h")?.toDoubleOrNull()
            FieldRow(e.s("id")!!, null, p.copy(hazard = hazard, hoursSinceContact = hrs))
        }
        return sortFieldList(rows, rules.currentEvent, rules.fieldPriority).map { names.getValue(it.id) to it }
    }

    // F2 offline keyword cards
    fun keywordCard(text: String) = keywordIcons(text, shared.keywords)

    // F11 outbox + scout
    fun enqueue(type: String, collection: String, payload: Map<String, Any?>) { val id = java.util.UUID.randomUUID().toString(); jobs += UiJob(id, type, "$collection/$id", payload, System.currentTimeMillis()) }
    suspend fun scoutCheck() {
        val s = scoutStep(scoutRun, adapters.connectivity.check(), rules.scoutSuccessesNeeded)
        scoutRun = s.run; signal.value = s.state
        if (s.alert) scoutAlert.value = true
        if (s.state == "back") flush()
    }
    suspend fun flush() {
        for (j in orderJobs(jobs.filter { it.status.state != "sent" }, rules.queuePriority, { it.type }, { it.createdAt })) {
            if (j.status.state == "failed") j.status = transition(j.status, "retry")
            j.status = transition(transition(j.status, "start"), "send")
            j.status = runCatching { adapters.sync.put(j.path, j.payload); transition(j.status, "ack") }.getOrElse { transition(j.status, "fail") }
        }
        val copy = jobs.toList(); jobs.clear(); jobs.addAll(copy) // refresh Compose
    }
}

object FeatureFactory { fun create(adapters: Adapters, shared: Shared) = Features(adapters, shared) }
