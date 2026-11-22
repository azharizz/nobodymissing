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
    val defaults = json("rules/defaults.json").let { d -> remoteRules?.let { runCatching {
        // Key-by-key merge: an older published template must not drop tasks added since.
        val r = Json.parseToJsonElement(it).jsonObject
        JsonObject(d + r + ("routerPolicy" to JsonObject(d["routerPolicy"]!!.jsonObject + (r["routerPolicy"] as? JsonObject ?: JsonObject(emptyMap())))))
    }.getOrNull() } ?: d }
    val rules = Rules.from(defaults)
    val schemas = listOf("roster", "careCard", "announcementCard", "fieldReport", "request", "translation", "medCheck", "findOpen", "askKit", "helpTriage", "familyQuery", "nameVariants", "warningPlain", "pickup", "checkin").associateWith { json("schemas/$it.json") }
    val prompts = json("prompts/pack.json")["tasks"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    // ponytail: only the synthetic demo drug list and region pack exist today; swap in real region packs when tools/region-pack ships them.
    val drugs = Drug.listFrom(json("demo/drugs.json"))
    val keywords = Keywords.from(json("rules/keywords.json"))
    val pack = Pack.from(json("demo/region-aceh.json"))
    /** Icons and the Kit mascot, shared with web (shared/icons, shared/mascot). */
    val icons = json("icons/icons.json")["icons"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    val mascot = json("mascot/kit.json")
    /** UI strings, shared with web (shared/i18n). */
    val strings = listOf("en", "id").associateWith { l -> json("i18n/$l.json").mapValues { it.value.jsonPrimitive.content } }
    companion object { fun from(c: Context, remoteRules: String? = null) = Shared({ c.assets.open(it).bufferedReader().readText() }, remoteRules) }
}

data class UiPerson(val id: String, val name: String, val age: Int?, val flags: List<String>, val confirmed: Boolean, val needsReview: Boolean, val source: String)
data class UiMed(val personId: String, val nameAsWritten: String, val match: DrugMatch, val strength: String?, val dose: String?, val timesPerDay: Int?, val qtyLeft: Int?, val generic: String?, val confirmedBy: String?)
data class UiCard(val icon: String, val title: String, val time: String?, val place: String?)
data class UiAnnouncement(val id: String, val text: String, val icons: List<String>, val cards: List<UiCard>, val source: String, val draft: Boolean, val shown: Boolean = false, val told: Set<String> = emptySet())
data class UiField(val id: String, val name: String, val lat: Double?, val lng: Double?, val hoursSinceContact: Double?, val person: PriorityInput, val needsReview: Boolean = false, val status: String = "unchecked", val pickup: PickupRecord? = null)
data class ActiveWarning(val w: WarningPlain, val text: String, val at: Long, val sources: List<org.nobodymissing.ports.GroundingPlace>)
data class Destination(val h: HigherPlace, val lat: Double, val lng: Double, val from: Pair<Double, Double>, val gps: Boolean)
data class UiHelp(val id: String, val kind: String, val spot: String, val text: String?, val done: Boolean = false, val summary: String? = null, val urgent: Boolean = false, val by: String? = null)
data class FamilyGroup(val fam: String, val fams: List<String>, val me: Member, val members: List<Member>, val via: Map<String, String>)
data class SmartFamily(val name: String, val age: Int?, val place: String?, val parsedBy: String, val variants: List<String>, val variantsBy: String, val hits: List<FamilyHit>)
data class MedCheckItem(val name: String, val generic: String?, val alsoKnownAs: List<String>, val swaps: List<String>, val cantMiss: Boolean, val note: String?)
data class Interaction(val a: String, val b: String, val severity: String, val note: String?)
data class MedCheckUi(val items: List<MedCheckItem>, val interactions: List<Interaction>, val cross: List<Cross>, val pending: Boolean)
data class OpenPlace(val name: String, val kind: String?, val distanceKm: Double?, val openNow: Boolean?, val hours: String?, val uri: String?)
data class OpenUi(val need: String, val places: List<OpenPlace>, val pending: Boolean)
const val JOB_TIMEOUT_MS = 30_000L
data class AskAnswer(val intent: String, val answer: String?, val card: UiAnnouncement?, val source: String, val urgent: Boolean = false)
data class UiJob(val id: String, val type: String, val path: String, val payload: Map<String, Any?>, val createdAt: Long, var status: JobStatus = JobStatus("waiting", 0))

/** FeatureFactory: wires every feature to its ports. Features never construct adapters. */
class Features(val adapters: Adapters, val shared: Shared) {
    val rules = shared.rules
    val privateMode = mutableStateOf(false)
    val signal = mutableStateOf("no_signal")
    val scoutAlert = mutableStateOf(false)
    val lang = mutableStateOf("en")
    val shelterId = mutableStateOf("shelter-1")
    val lastSignalAt = mutableStateOf<Long?>(null)

    /** Shared UI string with {placeholders} filled in. */
    fun t(key: String, vararg vars: Pair<String, Any?>): String =
        vars.fold(shared.strings[lang.value]?.get(key) ?: shared.strings.getValue("en")[key] ?: key) { s, (k, v) -> s.replace("{$k}", v.toString()) }

    /** DEMO: replace this device's shelter with the synthetic Aceh shelter. */
    fun loadDemo() = org.nobodymissing.demo.DemoShelter.load(this)
    private var scoutRun = 0
    val router = InferenceRouterFactory.create(adapters.inference, rules, shared.schemas, shared.prompts,
        deviceTimeoutMs = shared.defaults["timeouts"]!!.jsonObject.i("deviceMs")!!.toLong(), cloudTimeoutMs = shared.defaults["timeouts"]!!.jsonObject.i("cloudMs")!!.toLong()) {
        Ctx(signal.value == "back", privateMode.value)
    }
    val people = mutableStateListOf<UiPerson>()
    val meds = mutableStateListOf<UiMed>()
    val jobs = mutableStateListOf<UiJob>()
    /** Medicine Round marks: "slot|personId|generic" -> taken | refused | missing. */
    val round = androidx.compose.runtime.mutableStateMapOf<String, String>()

    // F1 roster
    suspend fun scanRoster(photo: Bitmap, cloudOk: Boolean = false): RouteResult {
        val r = router.run("roster_read", image = photo, staffApprovedCloud = cloudOk)
        if (r.status == "ok" || r.status == "draft") mapRosterRows(r.data!!.jsonObject["rows"]!!.jsonArray.map { RosterRow.from(it.jsonObject) }, r.source!!, rules)
            .forEach { people += UiPerson(java.util.UUID.randomUUID().toString(), it.nameAsWritten, it.age, it.suggestedFlags, false, it.needsReview || r.status == "draft", it.inferredBy) }
        return r
    }
    /** Staff fix a misread name or age before confirming. */
    fun editPerson(id: String, name: String, age: Int?) { val i = people.indexOfFirst { it.id == id }; if (i >= 0) people[i] = people[i].copy(name = name.ifBlank { people[i].name }, age = age) }
    /** Staff confirm name + flags; then the minimal public record and the roster go to the outbox. */
    fun confirmPerson(id: String, flags: List<String>? = null) {
        val i = people.indexOfFirst { it.id == id }; if (i < 0) return
        val p = people[i].copy(confirmed = true, needsReview = false, flags = flags ?: people[i].flags).also { people[i] = it }
        enqueue("family_update", "public", mapOf("name" to p.name, "age" to p.age?.let { it / 10 * 10 }, "shelterId" to shelterId.value, "status" to "safe", "hidden" to false))
    }

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
    val field = mutableStateListOf<UiField>()
    fun override(id: String, tier: String, reason: String) = field.replaceAll { if (it.id == id) it.copy(person = it.person.copy(override = TierOverride(tier, reason))) else it }
    fun visited(id: String, status: String) = field.replaceAll { if (it.id == id) it.copy(status = status, hoursSinceContact = 0.0) else it }
    fun fieldEntry(id: String) = field.first { it.id == id }
    fun higherPlace(e: UiField) = if (e.lat == null || e.lng == null) null else nearestHigher(shared.pack, e.lat, e.lng, rules.currentEvent, shared.defaults.d("higherMinGainM") ?: 2.0)
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
        // Times come from the deterministic parser, never from the model's formatting ("8 am" -> 08:00).
        fun time(t: String?) = t?.let { keywordIcons(it, shared.keywords).times.firstOrNull() }
        val ai = if (r.status == "ok" || r.status == "draft") r.data!!.jsonObject["cards"]!!.jsonArray.map { it.jsonObject }
            .map { UiCard(it.s("icon")!!, it.s("title")!!, time(it.s("time")), it.s("place")) } else null
        val a = UiAnnouncement(java.util.UUID.randomUUID().toString(), text, kw.icons, ai ?: listOf(UiCard(kw.icons.firstOrNull() ?: "info", text.take(40), kw.times.firstOrNull(), kw.place)),
            if (ai != null) r.source!! else "keywords", r.status == "draft")
        announcements.add(0, a)
        return a
    }
    fun toggleTold(id: String, personId: String) = announcements.replaceAll { if (it.id == id) it.copy(told = if (personId in it.told) it.told - personId else it.told + personId) else it }
    fun reach(a: UiAnnouncement?) = reachList(people.filter { it.confirmed }.map { ReachPerson(it.id, it.age, it.flags) }, a?.told?.toList() ?: emptyList(), rules)
    fun approve(id: String) {
        val i = announcements.indexOfFirst { it.id == id }; if (i < 0) return
        announcements[i] = announcements[i].copy(shown = true)
        val a = announcements[i]
        enqueue("other", "shelters/${shelterId.value}/announcements", mapOf("text" to a.text, "icons" to a.icons, "cards" to a.cards.map { mapOf("icon" to it.icon, "title" to it.title, "time" to it.time, "place" to it.place) }))
    }

    // ---- Hybrid: the cloud makes it better, the person stays on the phone (same as web features/hybrid.ts) ----
    val medChecks = androidx.compose.runtime.mutableStateMapOf<String, MedCheckUi>()
    val open = mutableStateOf<OpenUi?>(null)
    private val pendingChecks = mutableSetOf<String>()
    private var pendingOpen: String? = null

    /** 1 · Medicine check: drug names + strengths only (built by code, never names or photos); for a pharmacist, cross-checked offline. */
    suspend fun checkMeds(personId: String): String {
        val ms = meds.filter { it.personId == personId }.ifEmpty { return "failed" }
        val input = medCheckInput(ms.map { Triple(it.nameAsWritten, it.strength, it.generic) })
        val text = buildJsonObject { put("meds", JsonArray(input.map { m -> buildJsonObject { put("name", m.name); put("strength", m.strength) } })) }.toString()
        val r = router.run("med_check", text = text)
        if (r.status == "ok") {
            val o = r.data!!.jsonObject
            val items = o["items"]!!.jsonArray.map { it.jsonObject }.map { MedCheckItem(it.s("name")!!, it.s("generic"), it.strings("alsoKnownAs"), it.strings("swaps"), it.b("cantMiss") ?: false, it.s("note")) }
            val inter = o["interactions"]!!.jsonArray.map { it.jsonObject }.map { Interaction(it.s("a")!!, it.s("b")!!, it.s("severity")!!, it.s("note")) }
            medChecks[personId] = MedCheckUi(items, inter, crossCheck(items.map { it.name to it.generic }, shared.drugs), false)
            pendingChecks -= personId
            return "done"
        }
        if (r.status == "queued") { medChecks[personId] = MedCheckUi(emptyList(), emptyList(), emptyList(), true); pendingChecks += personId; return "queued" }
        return "failed"
    }

    /** 2 · Find it open: Grounding with Google Maps near a rounded shelter location; only places Google Maps returned. */
    suspend fun findOpen(need: String): String {
        val sh = shared.pack.shelters.firstOrNull { it.id == shelterId.value } ?: return "no_location"
        val (lat, lng) = coarseLocation(sh.lat, sh.lng, shared.defaults["findOpen"]!!.jsonObject.i("precisionDecimals")!!)
        val r = router.run("find_open", extra = Extra(mapOf("need" to need.replace('_', ' ')), lat, lng))
        if (r.status == "ok") {
            val ps = r.data!!.jsonObject["places"]!!.jsonArray.map { it.jsonObject }
            val kept = groundedPlaces(ps.map { it.s("name")!! }, r.places.map { GroundedChunk(it.title, it.uri, it.placeId) })
            open.value = OpenUi(need, kept.map { (i, c) -> ps[i].let { OpenPlace(it.s("name")!!, it.s("kind"), it.d("distanceKm"), it.b("openNow"), it.s("hours"), c.uri) } }, false)
            pendingOpen = null
            return "done"
        }
        if (r.status == "queued") { open.value = OpenUi(need, emptyList(), true); pendingOpen = need; return "queued" }
        return "failed"
    }
    fun needs() = shared.defaults["findOpen"]!!.jsonObject.strings("needs")

    /** 5 · Ask Kit: answers only from today's shown notices (device first). The question is never stored or synced. */
    /** Ask Kit. "Where do I go?" -> offline flood map (no model). Otherwise the phone's model picks the intent; the app answers. */
    suspend fun askKit(question: String): AskAnswer {
        val q = question.trim()
        if (isWhere(q)) return AskAnswer("where_to_go", null, null, "rules")
        val cards = announcements.filter { it.shown }.take(8)
        val list = cards.joinToString("\n") { a -> val c = a.cards.firstOrNull(); "id=${a.id} text=${listOfNotNull(c?.title, c?.time, c?.place).joinToString(" · ")} (said: ${a.text})" }.ifEmpty { "(none today)" }
        val r = router.run("ask_kit", text = q, extra = Extra(mapOf("cards" to list, "lang" to if (lang.value == "id") "Indonesian" else "English")))
        val o = if (r.status == "ok" || r.status == "draft") r.data?.jsonObject else null
        val k = askKitAnswer(o?.s("intent"), o?.s("cardId"), o?.s("answer") ?: "", cards.map { it.id }, q, words("urgentWords"))
        return AskAnswer(k.intent, k.answer, k.cardId?.let { id -> cards.first { it.id == id } }, if (o != null) r.source!! else "rules", urgentWordsIn(q, words("urgentWords")))
    }

    /** Queued cloud questions run again when the signal returns. */
    private suspend fun rerunPending() { pendingChecks.toList().forEach { checkMeds(it) }; pendingOpen?.let { findOpen(it) } }

    // Evacuee requests ("I need help"), shown on the staff Now list.
    val help = mutableStateListOf<UiHelp>()
    fun askHelp(kind: String, spot: String, text: String? = null, t: Triage? = null) { help += UiHelp(java.util.UUID.randomUUID().toString(), kind, spot.ifBlank { "?" }, text, summary = t?.summary, urgent = t?.urgent ?: false, by = t?.by) }

    /** "I need help" in the evacuee's own words: understood on this phone only (health words are private).
     *  No device model -> keyword rules. Urgency can only go up. Nothing is sent until the evacuee taps Send. */
    suspend fun triage(text: String): Triage {
        val r = router.run("help_triage", text = text.trim())
        val ai = if ((r.status == "ok" || r.status == "draft") && r.source == "device") r.data!!.jsonObject.let { Triple(it.s("kind")!!, it.b("urgent")!!, it.s("summary")!!) } else null
        return triageHelp(text, ai, shared.defaults.strings("urgentWords"), keywordIcons(text, shared.keywords).icons)
    }

    fun helpDone(id: String) = help.replaceAll { if (it.id == id) it.copy(done = true) else it }

    // ---- The first hours: warning -> the way -> pickup -> check-in (same as web features/firstHours.ts) ----
    private fun words(k: String) = shared.defaults.strings(k)
    private fun wordMap(k: String) = shared.defaults[k]!!.jsonObject.mapValues { it.value.jsonArray.map { x -> x.jsonPrimitive.content } }
    private val warnRules by lazy { WarnRules(words("goNowWords"), wordMap("hazardWords")) }
    val warning = mutableStateOf<ActiveWarning?>(null)
    val myPos = mutableStateOf<Pair<Double, Double>?>(null)
    val myPickup = mutableStateOf<Pair<PickupRecord, String>?>(null) // record + outbox path
    private fun langName() = if (lang.value == "id") "Indonesian" else "English"

    /** Any warning text -> plain words, on the phone first. Go-now words can only raise the level. */
    suspend fun warn(text: String): ActiveWarning {
        val r = router.run("warning_plain", text = text.trim(), extra = Extra(mapOf("lang" to langName())))
        val ai = if ((r.status == "ok" || r.status == "draft") && r.data != null) r.data.jsonObject.let { WarningPlain(it.s("hazard")!!, it.s("level")!!, it.s("plain")!!, it.s("action"), r.source!!) } else null
        return ActiveWarning(plainWarning(text, ai, r.source ?: "device", warnRules), text.trim(), System.currentTimeMillis(), emptyList()).also { warning.value = it }
    }
    /** Online only: latest official BMKG/BPBD warning (Grounding with Google Search, rounded location). */
    suspend fun latestWarning(): String {
        val p = here() ?: return "failed"
        if (signal.value != "back") return "offline"
        val (lat, lng) = coarseLocation(p.first, p.second, shared.defaults["findOpen"]!!.jsonObject.i("precisionDecimals")!!)
        val r = router.run("warning_latest", extra = Extra(mapOf("place" to "Kuala Simpang, Aceh Tamiang", "lang" to langName()), lat, lng))
        if (r.status != "ok") return "failed"
        val o = r.data!!.jsonObject
        val prev = warning.value
        val w = plainWarning(prev?.text ?: "", WarningPlain(o.s("hazard")!!, o.s("level")!!, o.s("plain")!!, o.s("action"), "cloud"), "cloud", warnRules)
        val level = if (prev?.w?.level == "go_now" && w.level != "go_now") "go_now" else w.level // a newer answer never lowers go-now
        warning.value = ActiveWarning(w.copy(level = level), o.s("source") ?: "", System.currentTimeMillis(), r.places.filter { it.uri != null }.take(3))
        return "done"
    }
    fun here(): Pair<Double, Double>? = myPos.value ?: (shared.pack.shelters.firstOrNull { it.id == shelterId.value } ?: shared.pack.shelters.firstOrNull())?.let { it.lat to it.lng }
    /** The higher place for this hazard from the offline flood data. Never "safe", only "higher". */
    /** "Where should I go?" -> answered from the offline flood map (rules in code), not by the model. */
    fun isWhere(q: String) = org.nobodymissing.domain.isWhereToGo(q, words("whereWords"))
    fun destination(): Destination? {
        val p = here() ?: return null
        val ev = warning.value?.w?.hazard?.takeIf { it in listOf("flood", "tsunami", "landslide", "earthquake") } ?: rules.currentEvent
        val h = nearestHigher(shared.pack, p.first, p.second, ev, shared.defaults.d("higherMinGainM") ?: 2.0) ?: return null
        val sh = shared.pack.shelters.first { it.id == h.id }
        return Destination(h, sh.lat, sh.lng, p, myPos.value != null)
    }
    /** "My mother can't walk, needs insulin": understood on this phone only; keywords if no model. Not sent yet. */
    suspend fun understandPickup(text: String): Pickup {
        val r = router.run("pickup_request", text = text.trim())
        val ai = if ((r.status == "ok" || r.status == "draft") && r.source == "device") r.data!!.jsonObject.let { Pickup(it.s("who") ?: "", it.i("mobility") ?: 0, it.strings("needs"), it.i("people") ?: 1) } else null
        return mergePickup(ai, text, wordMap("needWords"), wordMap("mobilityWords"), wordMap("peopleWords"))
    }
    /** Sent only on the person's tap, to the nearest shelter's pickup list through the outbox (waits for signal). */
    fun sendPickup(p: Pickup): PickupRecord {
        val pos = here()!!
        val near = shared.pack.shelters.minBy { distanceM(pos.first, pos.second, it.lat, it.lng) }
        val rec = PickupRecord(java.util.UUID.randomUUID().toString().take(12), Math.round(pos.first * 1e5) / 1e5, Math.round(pos.second * 1e5) / 1e5, p.mobility, p.needs, p.people, System.currentTimeMillis(), p.who)
        enqueue("pickup", "shelters/${near.id}/pickups", mapOf("id" to rec.id, "who" to rec.who, "lat" to rec.lat, "lng" to rec.lng, "mobility" to rec.mobility, "needs" to rec.needs, "people" to rec.people, "at" to rec.at), rec.id)
        myPickup.value = rec to "shelters/${near.id}/pickups/${rec.id}"
        return rec
    }
    fun pickupSent() = myPickup.value?.let { (_, path) -> jobs.any { it.path == path && it.status.state == "sent" } } ?: false
    /** Staff: a pickup (synced or scanned) becomes a Field Check entry ranked by the same rules. */
    fun importPickup(p: PickupRecord): Boolean {
        val id = "pk-${p.id}"
        if (field.any { it.id == id }) return false
        field.add(0, UiField(id, p.who, p.lat, p.lng, 0.0, pickupPriority(Pickup(p.who, p.mobility, p.needs, p.people)), pickup = p))
        return true
    }
    suspend fun syncPickups() {
        if (signal.value != "back") return
        runCatching { adapters.sync.list("shelters/${shelterId.value}/pickups") }.getOrNull()?.forEach { r ->
            val who = r["who"] as? String ?: return@forEach
            importPickup(PickupRecord(r["id"].toString(), (r["lat"] as Number).toDouble(), (r["lng"] as Number).toDouble(), (r["mobility"] as? Number)?.toInt() ?: 0,
                (r["needs"] as? List<*>)?.map { it.toString() } ?: emptyList(), (r["people"] as? Number)?.toInt() ?: 1, (r["at"] as? Number)?.toLong() ?: 0L, who))
        }
    }
    /** Staff at the door: one spoken sentence -> name, age, flags (on this phone only). */
    suspend fun understandCheckin(text: String): Checkin {
        val r = router.run("checkin_parse", text = text.trim())
        val ai = if ((r.status == "ok" || r.status == "draft") && r.source == "device") r.data!!.jsonObject.let { Checkin(it.s("name") ?: "", it.i("age"), it.strings("flags")) } else null
        return mergeCheckin(ai, text, wordMap("flagWords"))
    }
    /** Confirmed by a person: joins the roster with its flags (tell-in-person and medicine follow from the flags). */
    fun checkin(c: Checkin): String {
        val id = java.util.UUID.randomUUID().toString()
        people += UiPerson(id, c.name.trim(), c.age, c.flags, false, false, "staff")
        confirmPerson(id, c.flags)
        return id
    }

    // ---- Family group by QR: the QR carries the whole group (news hops phone to phone offline);
    // online, each phone shares everyone it knows into every family id it has seen (gossip), so groups join up.
    val familyGroup = mutableStateOf<FamilyGroup?>(null)
    fun setMe(name: String, status: String) {
        val g = familyGroup.value; val fam = g?.fam ?: newFamilyId()
        val me = Member(g?.me?.id ?: java.util.UUID.randomUUID().toString().take(12), name.trim().take(60), shelterId.value, status, System.currentTimeMillis())
        familyGroup.value = FamilyGroup(fam, g?.fams ?: listOf(fam), me, g?.members ?: emptyList(), g?.via ?: emptyMap())
    }
    fun myQr() = familyGroup.value?.let { encodeFamilyQr(it.fam, listOf(it.me) + it.members) }
    /** Learn everyone on a relative's QR. Two groups that meet converge on the smaller id; both ids stay synced. */
    fun scanFamily(text: String): List<Member>? {
        val (fam, ms) = decodeFamilyQr(text) ?: return null
        val g = familyGroup.value ?: return null
        val theirs = ms.filter { it.id != g.me.id }.ifEmpty { return null }
        val via = g.via + theirs.filter { t -> g.members.none { it.id == t.id && it.at >= t.at } }.associate { it.id to "qr" }
        familyGroup.value = g.copy(fam = minOf(g.fam, fam), fams = (g.fams + fam).distinct(), members = mergeMembers(g.members, theirs), via = via)
        return theirs
    }
    suspend fun syncFamily() {
        val g = familyGroup.value ?: return
        if (signal.value != "back") return
        runCatching {
            var members = g.members; val via = g.via.toMutableMap(); val remoteBy = mutableMapOf<String, List<Member>>()
            for (fam in g.fams) {
                val remote = adapters.sync.list("families/$fam/members").filter { it["name"] is String }
                    .map { Member(it["id"].toString(), it["name"].toString(), it["shelter"].toString(), if (it["status"] == "need_help") "need_help" else "safe", (it["at"] as? Number)?.toLong() ?: 0L) }
                remoteBy[fam] = remote
                val others = remote.filter { it.id != g.me.id }
                others.filter { r -> members.none { it.id == r.id && it.at >= r.at } }.forEach { via[it.id] = "online" }
                members = mergeMembers(members, others)
            }
            // Gossip: write anyone a family id is missing or has an older copy of.
            for (fam in g.fams) for (m in listOf(g.me) + members) {
                val have = remoteBy.getValue(fam).firstOrNull { it.id == m.id }
                if (have == null || have.at < m.at) adapters.sync.put("families/$fam/members/${m.id}", mapOf("name" to m.name, "shelter" to m.shelter, "status" to m.status, "at" to m.at))
            }
            familyGroup.value = familyGroup.value!!.copy(members = members, via = via)
        }
    }

    /** Hybrid family search. Device: understand "my grandmother Nurhayati, about 80" privately (name, age, place); no model -> rules.
     *  Cloud (online, only the name): Indonesian/Acehnese spellings; offline -> spelling rules. Matching is deterministic; a person confirms. */
    suspend fun familySmart(text: String): SmartFamily {
        val q = router.run("family_query", text = text.trim())
        val ai = if ((q.status == "ok" || q.status == "draft") && q.source == "device") q.data!!.jsonObject.let { LookFor(it.s("name") ?: "", it.i("age"), it.s("place")) }.takeIf { it.name.isNotBlank() } else null
        val p = ai ?: parseLookFor(text)
        val rules = shared.defaults["nameRules"]!!.jsonObject.mapValues { it.value.jsonArray.map { x -> x.jsonPrimitive.content } }
        var variants = offlineVariants(p.name, rules); var by = "offline"
        if (signal.value == "back") {
            val v = router.run("name_variants", text = p.name)
            if (v.status == "ok") { variants = (v.data!!.jsonObject.strings("variants").filter { it.isNotBlank() && !it.equals(p.name, true) } + variants).distinct(); by = "cloud" }
        }
        val local = people.filter { it.confirmed }.map { PublicRec(it.id, it.name, it.age, shelterId.value, "safe") }
        val remote = if (signal.value == "back") runCatching { adapters.sync.list("public").filter { it["name"] is String && it["hidden"] != true }
            .map { PublicRec(it["id"].toString(), it["name"].toString(), (it["age"] as? Number)?.toInt(), it["shelterId"].toString(), it["status"].toString()) } }.getOrDefault(emptyList()) else emptyList()
        return SmartFamily(p.name, p.age, p.place, if (ai != null) "device" else "rules", variants, by, searchVariants(p.name, variants, p.age, local + remote))
    }

    // F3 family search: this shelter only (minimal fields, D6)
    fun familySearch(q: String) = familySearch(q, people.filter { it.confirmed }.map { PublicRec(it.id, it.name, it.age, shelterId.value, "safe") })

    // F2 offline keyword cards
    fun keywordCard(text: String) = keywordIcons(text, shared.keywords)

    // F11 outbox + scout
    fun enqueue(type: String, collection: String, payload: Map<String, Any?>, docId: String? = null) { val id = java.util.UUID.randomUUID().toString(); jobs += UiJob(id, type, "$collection/${docId ?: id}", payload, System.currentTimeMillis()) }
    suspend fun scoutCheck() {
        val s = scoutStep(scoutRun, adapters.connectivity.check(), rules.scoutSuccessesNeeded)
        scoutRun = s.run; signal.value = s.state
        if (s.state != "no_signal") lastSignalAt.value = System.currentTimeMillis()
        if (s.alert) scoutAlert.value = true
        if (s.state == "back") { flush(); rerunPending(); syncFamily(); syncPickups() }
    }
    // QR courier (6.10 level 2): small structured JSON only, never photos.
    fun exportChunks(size: Int = 700): List<String> = chunk(JsonArray(jobs.filter { it.status.state != "sent" }.map { j ->
        buildJsonObject { put("id", j.id); put("type", j.type); put("path", j.path); put("createdAt", j.createdAt); put("payload", toJson(j.payload)) }
    }).toString(), size)

    /** Courier import is a trust boundary: well-formed jobs only, skip ids we already have. */
    fun importChunks(chunks: List<String>): Int {
        val list = join(chunks)?.let { runCatching { Json.parseToJsonElement(it) as? JsonArray }.getOrNull() } ?: return 0
        val have = jobs.map { it.id }.toSet()
        val fresh = list.mapNotNull { it as? JsonObject }.filter { it.s("id") != null && it.s("type") != null && it.s("path") != null && it.s("id") !in have }
            .map { UiJob(it.s("id")!!, it.s("type")!!, it.s("path")!!, (it["payload"] as? JsonObject)?.let(::fromJson) as? Map<String, Any?> ?: emptyMap(), it["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L) }
        jobs += fresh
        return fresh.size
    }

    private var flushing = false
    /** One flush at a time; a job that hangs (Firestore waits for the server) times out so it can't block the pickup behind it. */
    suspend fun flush() {
        if (flushing) return
        flushing = true
        try {
            for (j in orderJobs(jobs.filter { it.status.state == "waiting" || it.status.state == "failed" }, rules.queuePriority, { it.type }, { it.createdAt })) {
                if (j.status.state == "failed") j.status = transition(j.status, "retry")
                j.status = transition(transition(j.status, "start"), "send")
                val ok = runCatching { kotlinx.coroutines.withTimeout(JOB_TIMEOUT_MS) { adapters.sync.put(j.path, j.payload) } }.isSuccess
                j.status = transition(j.status, if (ok) "ack" else "fail")
            }
        } finally { flushing = false }
        val copy = jobs.toList(); jobs.clear(); jobs.addAll(copy) // refresh Compose
    }
}

internal fun toJson(v: Any?): JsonElement = when (v) {
    null -> JsonNull
    is JsonElement -> v
    is Map<*, *> -> JsonObject(v.entries.associate { it.key.toString() to toJson(it.value) })
    is Iterable<*> -> JsonArray(v.map(::toJson))
    is Number -> JsonPrimitive(v)
    is Boolean -> JsonPrimitive(v)
    else -> JsonPrimitive(v.toString())
}
internal fun fromJson(e: JsonElement): Any? = when (e) {
    is JsonNull -> null
    is JsonObject -> e.mapValues { fromJson(it.value) }
    is JsonArray -> e.map(::fromJson)
    is JsonPrimitive -> if (e.isString) e.content else e.booleanOrNull ?: e.longOrNull ?: e.doubleOrNull
}

object FeatureFactory { fun create(adapters: Adapters, shared: Shared) = Features(adapters, shared) }
