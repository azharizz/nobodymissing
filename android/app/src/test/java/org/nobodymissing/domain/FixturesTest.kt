package org.nobodymissing.domain

import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

/** Contract tests: the same shared/fixtures as web (Vitest). Working dir is android/app. */
class FixturesTest {
    private fun load(path: String) = Json.parseToJsonElement(File("../../shared/$path").readText()).jsonObject
    private val rules = Rules.from(load("rules/defaults.json"))
    private val keywords = Keywords.from(load("rules/keywords.json"))
    private val drugs = Drug.listFrom(load("demo/drugs.json"))
    private fun cases(file: String, key: String = "cases") = load("fixtures/$file")[key]!!.jsonArray.map { it.jsonObject }
    private val JsonObject.input get() = this["input"]!!
    private val JsonObject.expected get() = this["expected"]!!
    private fun JsonObject.name() = this["name"]?.jsonPrimitive?.content ?: input.toString()

    @Test fun priority() {
        for (c in cases("priority.json")) {
            val i = c.input.jsonObject
            val r = priority(PriorityInput.from(i["person"]!!.jsonObject), i.s("event")!!, rules.fieldPriority)
            val e = c.expected.jsonObject
            assertEquals(c.name(), PriorityResult(e.i("score")!!, e.s("tier")!!, e.strings("why")), r)
        }
        for (c in cases("priority.json", "sortCases")) {
            val i = c.input.jsonObject
            val rows = i["people"]!!.jsonArray.map { it.jsonObject }.map { FieldRow(it.s("id")!!, it.d("distanceM"), PriorityInput.from(it["person"]!!.jsonObject)) }
            assertEquals(c.name(), c.expected.jsonArray.map { it.jsonPrimitive.content }, sortFieldList(rows, i.s("event")!!, rules.fieldPriority).map { it.id })
        }
    }

    @Test fun drugMatcher() {
        for (c in cases("drugMatcher.json")) {
            val i = c.input.jsonObject; val e = c.expected.jsonObject
            val want = DrugMatch(e.s("status")!!, e.s("generic"), e.strings("candidates"), e.strings("lookAlikes"), e.b("strengthKnown"), true)
            assertEquals(c.name(), want, matchDrug(i.s("text")!!, i.s("strength"), drugs))
        }
        for (c in cases("drugMatcher.json", "substitutionCases")) {
            val i = c.input.jsonObject
            assertEquals(c.name(), c.expected.jsonPrimitive.content, substitutionLevel(Med.from(i["prescribed"]!!.jsonObject), Med.from(i["available"]!!.jsonObject)))
        }
    }

    @Test fun schedule() {
        for (c in cases("schedule.json")) {
            val s = buildSchedule(c.input.jsonObject["cards"]!!.jsonArray.map { Card.from(it.jsonObject) }, rules)
            val e = c.expected.jsonObject
            assertEquals(c.name(), e["counts"]!!.jsonObject.mapValues { it.value.jsonPrimitive.int }, s.counts)
            assertEquals(c.name(), e["slot12"]!!.jsonArray.map { it.jsonObject.let { o -> SlotEntry(o.s("personId")!!, o.s("generic")!!, o.s("doseAsWritten")) } }, s.slots["12:00"])
            assertEquals(c.name(), e["alerts"]!!.jsonArray.map { it.jsonObject.let { o -> Alert(o.s("personId")!!, o.s("generic")!!, o.s("type")!!, o.i("supplyDays")) } }, s.alerts)
            assertEquals(c.name(), e["shortages"]!!.jsonArray.map { it.jsonObject.let { o -> Shortage(o.s("generic")!!, o.s("strength"), o.i("people")!!) } }, s.shortages)
            assertEquals(c.name(), e.i("unscheduled"), s.unscheduled)
        }
        for (c in cases("schedule.json", "requestCases")) {
            val shelters = c.input.jsonObject["shelters"]!!.jsonArray.map { it.jsonObject }.map { o -> o.s("shelterId")!! to o["items"]!!.jsonArray.map { RequestItem.from(it.jsonObject) } }
            val e = c.expected.jsonObject
            assertEquals(c.name(), CombinedRequest(e.strings("fromShelters"), e["items"]!!.jsonArray.map { RequestItem.from(it.jsonObject) }), combineRequests(shelters))
        }
    }

    @Test fun dedupe() {
        for (c in cases("dedupe.json", "normalizeCases")) assertEquals(c.name(), c.expected.jsonPrimitive.content, normalizeName(c.input.jsonPrimitive.content))
        for (c in cases("dedupe.json", "groupCases")) {
            val recs = c.input.jsonObject["records"]!!.jsonArray.map { it.jsonObject.let { o -> Rec(o.s("id")!!, o.s("name")!!, o.i("age")) } }
            assertEquals(c.name(), c.expected.jsonArray.map { g -> g.jsonArray.map { it.jsonPrimitive.content } }, groupDuplicates(recs))
        }
        for (c in cases("dedupe.json", "searchCases")) {
            val i = c.input.jsonObject
            val recs = i["records"]!!.jsonArray.map { it.jsonObject.let { o -> PublicRec(o.s("id")!!, o.s("name")!!, o.i("age"), o.s("shelterId")!!, o.s("status")!!, o.b("hidden") ?: false) } }
            val want = c.expected.jsonArray.map { it.jsonObject.let { o -> PublicHit(o.s("name")!!, o.s("ageBand")!!, o.s("shelterId")!!, o.s("status")!!) } }
            assertEquals(c.name(), want, familySearch(i.s("query")!!, recs))
        }
    }

    @Test fun queue() {
        for (c in cases("queue.json", "transitionCases")) {
            val events = c.input.jsonObject.strings("events")
            val e = c.expected.jsonObject
            if (e.b("error") == true) assertThrows(c.name(), IllegalStateException::class.java) { applyEvents(events) }
            else assertEquals(c.name(), JobStatus(e.s("state")!!, e.i("retries")!!), applyEvents(events))
        }
        for (c in cases("queue.json", "orderCases")) {
            val jobs = c.input.jsonObject["jobs"]!!.jsonArray.map { it.jsonObject.let { o -> QueuedJob(o.s("id")!!, o.s("type")!!, o.d("createdAt")!!.toLong()) } }
            assertEquals(c.name(), c.expected.jsonArray.map { it.jsonPrimitive.content }, orderJobs(jobs, rules.queuePriority).map { it.id })
        }
        for (c in cases("queue.json", "progressCases")) {
            val e = c.expected.jsonObject
            assertEquals(Progress(e.i("sent")!!, e.i("total")!!, e.i("waiting")!!), progress(c.input.jsonObject.strings("states")))
        }
    }

    @Test fun router() {
        for (c in cases("router.json")) {
            val i = c.input.jsonObject; val x = i["ctx"]!!.jsonObject; val e = c.expected.jsonObject
            val ctx = RouterCtx(x.b("onDeviceAvailable")!!, x.b("online")!!, x.b("privateMode")!!, x.b("staffApprovedCloud")!!)
            assertEquals(c.name(), Decision(e.s("action")!!, e.s("mode")), decide(i.s("task")!!, ctx, rules.routerPolicy, rules.accuracyBarPoints))
        }
        for (c in cases("router.json", "guardCases")) {
            val i = c.input.jsonObject
            assertEquals(c.name(), c.expected.jsonPrimitive.boolean, isOnDeviceReady(i.b("apiPresent")!!, i.s("availability")!!))
        }
    }

    @Test fun keywordIcons() {
        for (c in cases("keywordIcons.json")) {
            val e = c.expected.jsonObject
            assertEquals(c.name(), IconResult(e.strings("icons"), e.strings("times"), e.s("place")), keywordIcons(c.input.jsonPrimitive.content, keywords))
        }
    }

    @Test fun reachList() {
        for (c in cases("reachList.json")) {
            val i = c.input.jsonObject; val e = c.expected.jsonObject
            val people = i["people"]!!.jsonArray.map { it.jsonObject.let { o -> ReachPerson(o.s("id")!!, o.i("age"), o.strings("flags")) } }
            val want = ReachResult(e["list"]!!.jsonArray.map { it.jsonObject.let { o -> ReachEntry(o.s("id")!!, o.strings("reasons"), o.b("told")!!) } }, e.i("remaining")!!)
            assertEquals(want, reachList(people, i.strings("told"), rules))
        }
    }

    @Test fun schemaValidation() {
        val schemas = listOf("roster", "careCard", "announcementCard", "fieldReport", "request", "translation").associateWith { load("schemas/$it.json") }
        for (c in cases("schemaValidation.json")) {
            val i = c.input.jsonObject
            assertEquals(c.name(), c.expected.jsonObject.b("valid"), parseAndValidate(i.s("raw")!!, schemas[i.s("schema")]!!).valid)
        }
    }

    @Test fun rosterMapping() {
        for (c in cases("rosterMapping.json")) {
            val i = c.input.jsonObject
            val rows = i["rows"]!!.jsonArray.map { RosterRow.from(it.jsonObject) }
            val want = c.expected.jsonArray.map { it.jsonObject.let { o -> PersonDraft(o.s("nameAsWritten")!!, o.i("age"), o.s("sex"), o.strings("suggestedFlags"), o.s("notes"), o.b("needsReview")!!, o.s("inferredBy")!!, o.s("confirmedBy")) } }
            assertEquals(want, mapRosterRows(rows, i.s("source")!!, rules))
        }
    }

    @Test fun scout() {
        for (c in cases("scout.json")) {
            val checks = c.input.jsonObject["checks"]!!.jsonArray.map { it.jsonPrimitive.boolean }
            val e = c.expected.jsonObject
            assertEquals(ScoutRun(e.strings("states"), e["alertAt"]!!.jsonArray.map { it.jsonPrimitive.int }), scoutStates(checks, rules.scoutSuccessesNeeded))
        }
    }

    @Test fun courier() {
        for (c in cases("courier.json")) {
            val i = c.input.jsonObject
            assertEquals(c.expected.jsonArray.map { it.jsonPrimitive.content }, chunk(i.s("payload")!!, i.i("size")!!))
        }
        for (c in cases("courier.json", "joinCases")) {
            assertEquals(c.expected.jsonPrimitive.contentOrNull, join(c.input.jsonArray.map { it.jsonPrimitive.content }))
        }
    }

    @Test fun regionPack() {
        val pack = Pack.from(load("demo/region-aceh.json"))
        for (c in cases("regionPack.json", "handCases")) {
            val i = c.input.jsonObject
            assertEquals(c.name(), c.expected.jsonPrimitive.doubleOrNull, handAt(pack, i.d("lat")!!, i.d("lng")!!))
        }
        for (c in cases("regionPack.json", "higherCases")) {
            val i = c.input.jsonObject; val e = c.expected.jsonObject
            assertEquals(HigherPlace(e.s("id")!!, e.s("name")!!, e.i("distanceM")!!.toLong(), e.s("direction")!!, e.d("handGain")!!),
                nearestHigher(pack, i.d("lat")!!, i.d("lng")!!, i.s("event")!!, load("rules/defaults.json").d("higherMinGainM")!!))
        }
    }

    @Test fun hybridContracts() {
        val fx = load("fixtures/hybrid.json")
        for (c in fx["medCheckInput"]!!.jsonArray.map { it.jsonObject }) {
            val meds = c["input"]!!.jsonObject["meds"]!!.jsonArray.map { it.jsonObject }.map { Triple(it.s("nameAsWritten")!!, it.s("strength"), it.s("generic")) }
            val out = medCheckInput(meds)
            assertEquals(c.name(), c["expected"]!!.jsonObject["meds"]!!.jsonArray.map { it.jsonObject.let { e -> MedLine(e.s("name")!!, e.s("strength")) } }, out)
            assertEquals(false, out.toString().contains("Hasan") || out.toString().contains("Bed 4") || out.toString().contains("base64"))
        }
        for (c in fx["coarseLocation"]!!.jsonArray.map { it.jsonObject }) {
            val i = c["input"]!!.jsonObject; val e = c["expected"]!!.jsonObject
            assertEquals(e.d("lat")!! to e.d("lng")!!, coarseLocation(i.d("lat")!!, i.d("lng")!!, i.i("decimals")!!))
        }
        for (c in fx["crossCheck"]!!.jsonArray.map { it.jsonObject }) {
            val items = c["input"]!!.jsonObject["items"]!!.jsonArray.map { it.jsonObject.let { x -> x.s("name")!! to x.s("generic") } }
            assertEquals(c.name(), c["expected"]!!.jsonArray.map { it.jsonObject.let { e -> Cross(e.s("name")!!, e.s("offline"), e.s("status")!!) } }, crossCheck(items, drugs))
        }
        for (c in fx["groundedPlaces"]!!.jsonArray.map { it.jsonObject }) {
            val i = c["input"]!!.jsonObject
            val names = i["places"]!!.jsonArray.map { it.jsonObject.s("name")!! }
            val chunks = i["chunks"]!!.jsonArray.map { it.jsonObject.let { x -> GroundedChunk(x.s("title"), x.s("uri"), x.s("placeId")) } }
            assertEquals(c.name(), c["expected"]!!.jsonArray.map { it.jsonObject.let { e -> e.s("name")!! to e.s("uri") } }, groundedPlaces(names, chunks).map { (k, ch) -> names[k] to ch.uri })
        }
        for (c in fx["askKit"]!!.jsonArray.map { it.jsonObject }) {
            val i = c["input"]!!.jsonObject; val r = i["result"]!!.jsonObject
            val e = c["expected"] as? JsonObject
            assertEquals(c.name(), e?.let { it.s("answer")!! to it.s("cardId")!! }, askKitAnswer(r.s("cardId"), r.s("answer")!!, i.strings("cardIds")))
        }
    }
}
