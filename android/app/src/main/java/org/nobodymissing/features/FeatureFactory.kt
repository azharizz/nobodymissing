package org.nobodymissing.features

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.*
import org.nobodymissing.adapters.Adapters
import org.nobodymissing.domain.*

/** Shared JSON (rules, schemas, prompts, drugs, region pack, synthetic demo shelter) bundled from shared/ as assets. */
class Shared(private val read: (String) -> String, remoteRules: String? = null) {
    fun json(p: String) = Json.parseToJsonElement(read(p)).jsonObject
    /** Remote Config "rules" (last activated, available offline) wins over the bundled defaults, key by key. */
    val defaults = json("rules/defaults.json").let { d -> remoteRules?.let { runCatching { JsonObject(d + Json.parseToJsonElement(it).jsonObject) }.getOrNull() } ?: d }
    val rules = Rules.from(defaults)
    val schemas = listOf("roster", "careCard", "announcementCard", "fieldReport", "request", "translation").associateWith { json("schemas/$it.json") }
    val prompts = json("prompts/pack.json")["tasks"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    val drugs = Drug.listFrom(json("drugs/demo-drugs.json"))
    val keywords = Keywords.from(json("rules/keywords.json"))
    val pack = Pack.from(json("regions/aceh-demo.json"))
    val demo = json("fixtures/demo-shelter.json")
    companion object { fun from(c: Context, remoteRules: String? = null) = Shared({ c.assets.open(it).bufferedReader().readText() }, remoteRules) }
}

data class UiPerson(val id: String, val name: String, val age: Int?, val flags: List<String>, val confirmed: Boolean, val needsReview: Boolean, val source: String)
data class UiMed(val personId: String, val nameAsWritten: String, val match: DrugMatch, val strength: String?, val dose: String?, val timesPerDay: Int?, val qtyLeft: Int?, val generic: String?, val confirmedBy: String?)
data class UiCard(val icon: String, val title: String, val time: String?, val place: String?)
data class UiAnnouncement(val id: String, val text: String, val icons: List<String>, val cards: List<UiCard>, val source: String, val draft: Boolean, val shown: Boolean = false)
data class UiField(val id: String, val name: String, val lat: Double?, val lng: Double?, val hoursSinceContact: Double?, val person: PriorityInput, val needsReview: Boolean = false)
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

    // F12-lite field check: entries from the synthetic demo + volunteer reports; hazard from the region pack
    val field = mutableStateListOf<UiField>().apply {
        shared.demo["field"]!!.jsonArray.map { it.jsonObject }.forEach { e ->
            val loc = e["location"] as? JsonObject
            add(UiField(e.s("id")!!, e.s("name")!!, loc?.d("lat"), loc?.d("lng"), e.s("lastContactAt")?.removeSuffix("h")?.toDoubleOrNull(),
                PriorityInput.from(JsonObject(e["person"]!!.jsonObject + ("hazard" to JsonObject(emptyMap()))))))
        }
    }
    fun fieldList(): List<Pair<String, RankedRow>> {
        val rows = field.map { e ->
            val hazard = if (e.lat == null || e.lng == null) Hazard(unknown = true) else Hazard(hand = handAt(shared.pack, e.lat, e.lng))
            FieldRow(e.id, null, e.person.copy(hazard = hazard, hoursSinceContact = e.hoursSinceContact))
        }
        val names = field.associate { it.id to it.name }
        return sortFieldList(rows, rules.currentEvent, rules.fieldPriority).map { names.getValue(it.id) to it }
    }
    suspend fun fieldReport(note: String, lat: Double?, lng: Double?, cloudOk: Boolean = false): RouteResult {
        val r = router.run("field_report", text = note, staffApprovedCloud = cloudOk)
        if (r.status == "ok" || r.status == "draft") {
            val people = r.data!!.jsonObject["people"]!!.jsonArray.map { it.jsonObject }
            people.forEach { p ->
                val needs = p.strings("medicalNeeds").map { it.lowercase() }
                val cantMiss = needs.any { n -> rules.cantMiss.keys.any { n.contains(it) } }
                field += UiField(java.util.UUID.randomUUID().toString(), p.s("name")!!, lat, lng, 0.0,
                    PriorityInput(age = p.i("age"), mobility = p.i("mobility") ?: 0, cantMiss = cantMiss, supplyDays = if (cantMiss) 0 else null, dailyMeds = !cantMiss && needs.isNotEmpty()), needsReview = true)
            }
            enqueue("field_report", "fieldReports", mapOf("note" to note, "lat" to lat, "lng" to lng, "people" to people.map { it.s("name") }))
        }
        return r
    }

    // F2 announcements: keyword icons at once (offline, exact), AI cards via the router; staff approve before showing.
    val announcements = mutableStateListOf<UiAnnouncement>()
    suspend fun announce(text: String, lang: String): UiAnnouncement {
        val kw = keywordIcons(text, shared.keywords)
        val r = router.run(if (lang == "id") "announcement_id" else "announcement", text = text)
        val ai = if (r.status == "ok" || r.status == "draft") r.data!!.jsonObject["cards"]!!.jsonArray.map { it.jsonObject }
            .map { UiCard(it.s("icon")!!, it.s("title")!!, it.s("time"), it.s("place")) } else null
        val a = UiAnnouncement(java.util.UUID.randomUUID().toString(), text, kw.icons, ai ?: listOf(UiCard(kw.icons.firstOrNull() ?: "info", text.take(40), kw.times.firstOrNull(), kw.place)),
            if (ai != null) r.source!! else "keywords", r.status == "draft")
        announcements.add(0, a)
        return a
    }
    fun approve(id: String) {
        val i = announcements.indexOfFirst { it.id == id }; if (i < 0) return
        announcements[i] = announcements[i].copy(shown = true)
        val a = announcements[i]
        enqueue("other", "shelters/SD3/announcements", mapOf("text" to a.text, "icons" to a.icons, "cards" to a.cards.map { mapOf("icon" to it.icon, "title" to it.title, "time" to it.time, "place" to it.place) }))
    }

    // F3 family search: this shelter only (minimal fields, D6)
    fun familySearch(q: String) = familySearch(q, people.filter { it.confirmed }.map { PublicRec(it.id, it.name, it.age, "SD3", "safe") })

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
