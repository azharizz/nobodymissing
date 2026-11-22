package org.nobodymissing.demo

import org.nobodymissing.domain.parseCheckin
import org.nobodymissing.domain.parseLookFor
import org.nobodymissing.domain.parsePickup
import org.nobodymissing.ports.GroundingPlace
import org.nobodymissing.ports.InferenceRequest
import org.nobodymissing.ports.InferenceResult

/** DEMO ONLY: canned AI replies when there is no Firebase project (same as web demo/demoReply.ts). Badges still say which path answered. */
object DemoReply {
    fun reply(r: InferenceRequest): InferenceResult {
        val source = if (r.mode.endsWith("cloud")) "cloud" else "device"
        val t = r.text ?: ""
        val time = Regex("""(\d{1,2})[.:](\d{2})""").find(t)?.value?.replace('.', ':')
        val raw = when (r.task) {
            "roster_read" -> """{"rows":[{"name":"Teuku Hasballah","age":79,"sex":"M","flags":["hard_of_hearing","wheelchair"],"notes":"tidak bisa dengar, pakai kursi roda","confidence":0.86},{"name":"Cut Nurlaila","age":34,"sex":"F","flags":["pregnant"],"notes":null,"confidence":0.93},{"name":"M. Rizki","age":7,"sex":"M","flags":[],"notes":null,"confidence":0.62}]}"""
            "care_card_read" -> """{"meds":[{"nameAsWritten":"Amlodipin 5 mg","strength":"5 mg","doseAsWritten":"1 x 1 tablet pagi","timesPerDay":1,"qtyLeft":2,"confidence":0.88},{"nameAsWritten":"Lantus","strength":"100 units/ml","doseAsWritten":"10 unit malam","timesPerDay":1,"qtyLeft":1,"confidence":0.9}],"conditions":["hypertension","diabetes"],"allergies":[]}"""
            "field_report" -> """{"people":[{"name":"${t.substringBefore(',').take(40).replace("\"", "")}","age":${Regex("""\b(\d{2})\b""").find(t)?.value ?: "null"},"mobility":${if (Regex("can't walk|tidak bisa jalan|bedridden", RegexOption.IGNORE_CASE).containsMatchIn(t)) 3 else 1},"medicalNeeds":${if (t.contains("insulin", true)) "[\"insulin\"]" else "[]"}}]}"""
            "announcement", "announcement_id" -> """{"cards":[{"icon":"${when { Regex("water|air", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "water"; Regex("medic|obat", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "medicine"; else -> "food" }}","title":"${t.split(Regex("[.,!]"))[0].take(30).replace("\"", "")}","time":${time?.let { "\"$it\"" } ?: "null"},"place":null}]}"""
            "med_check" -> """{"items":[{"name":"insulin glargine","generic":"insulin glargine","alsoKnownAs":["Lantus","Basaglar"],"swaps":["insulin detemir","NPH insulin"],"cantMiss":true,"note":"Keep cool; never skip."},{"name":"amlodipine","generic":"amlodipine","alsoKnownAs":["Norvasc","Tensivask"],"swaps":["nifedipine (long-acting)"],"cantMiss":false,"note":null}],"interactions":[]}"""
            "find_open" -> """{"places":[{"name":"Apotek Sehat Keluarga","kind":"pharmacy","distanceKm":1.2,"openNow":true,"hours":"until 21:00"},{"name":"Puskesmas Kota (demo)","kind":"health post","distanceKm":2.4,"openNow":true,"hours":"24 hours"}]}"""
            "help_triage" -> """{"kind":"${when { Regex("insulin|obat|medic", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "medicine"; Regex("sick|sakit|demam|fever|pain|nyeri", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "sick"; else -> "other" }}","urgent":${Regex("chest|dada|breath|napas", RegexOption.IGNORE_CASE).containsMatchIn(t)},"summary":"${t.take(80).replace("\"", "")}"}"""
            "family_query" -> parseLookFor(t).let { """{"name":"${it.name.replace("\"", "")}","age":${it.age ?: "null"},"place":${it.place?.let { p -> "\"${p.replace("\"", "")}\"" } ?: "null"}}""" }
            "name_variants" -> """{"variants":[${if (t.contains(' ')) "\"${t.replace(" ", "").replace("\"", "")}\"" else if (t.length > 6) "\"${t.take(3)} ${t.drop(3).replace("\"", "")}\"" else ""}]}"""
            "warning_plain" -> """{"hazard":"${if (Regex("banjir|flood|debit air", RegexOption.IGNORE_CASE).containsMatchIn(t)) "flood" else "other"}","level":"${if (Regex("segera|evakuasi|evacuate", RegexOption.IGNORE_CASE).containsMatchIn(t)) "go_now" else "prepare"}","plain":"The river is rising fast. Leave the riverside now and go to higher ground.","action":"Go to higher ground now"}"""
            "warning_latest" -> """{"hazard":"flood","level":"go_now","plain":"BPBD says the Tamiang river is still rising. People near the river must leave now.","action":"Go to higher ground","source":"BPBD Aceh Tamiang (demo)","issuedAt":"07:30"}"""
            "pickup_request" -> parsePickup(t, emptyMap(), mapOf("3" to listOf("tidak bisa jalan", "can't walk", "bedridden"), "2" to listOf("kursi roda", "wheelchair"))).let { """{"who":"${it.who.replace("\"", "")}","mobility":${maxOf(1, it.mobility)},"needs":${if (t.contains("insulin", true)) "[\"insulin\"]" else "[]"},"people":1}""" }
            "checkin_parse" -> parseCheckin(t, mapOf("hard_of_hearing" to listOf("dengar", "hearing"))).let { """{"name":"${it.name.replace("\"", "")}","age":${it.age ?: "null"},"flags":${if (it.flags.isEmpty()) "[]" else "[\"hard_of_hearing\"]"}}""" }
            "ask_kit" -> {
                // Demo: pick the notice sharing a word with the question (the real model reads them in any language).
                val words = t.lowercase().split(Regex("\\W+")).filter { it.length > 3 && it !in setOf("when", "what", "where", "does", "will", "there") }
                val hit = Regex("""id=(\S+) text=([^\n]*)""").findAll(r.prompt).firstOrNull { m -> words.any { m.groupValues[2].lowercase().contains(it) } }
                val intent = when {
                    Regex("^(hi|halo|hello|hai|hey|thanks|terima kasih|makasih)\\b", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "greeting"
                    Regex("tidak bisa (jalan|keluar)|can't (walk|get out)|jemput|pickup", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "pickup"
                    Regex("keluarga|family|anak saya|my (mother|father|son|daughter|child)", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "family"
                    Regex("sakit|sick|obat|medicine|tolong|help", RegexOption.IGNORE_CASE).containsMatchIn(t) -> "help"
                    hit != null -> "notice"
                    else -> "unknown"
                }
                if (intent == "notice") """{"intent":"notice","cardId":"${hit!!.groupValues[1]}","answer":"${hit.groupValues[2].replace(Regex(" \\(said:.*$"), "").replace("\"", "")}"}""" else """{"intent":"$intent","cardId":null,"answer":""}"""
            }
            else -> "{}"
        }
        val places = if (r.task == "warning_latest") listOf(GroundingPlace("BPBD Aceh Tamiang (demo)", "https://bpbd.acehtamiangkab.go.id/", null)) else if (r.task == "find_open") listOf(GroundingPlace("Apotek Sehat Keluarga", "https://maps.google.com/?q=Apotek+Sehat+Keluarga", null), GroundingPlace("Puskesmas Kota (demo)", "https://maps.google.com/?q=Puskesmas", null)) else emptyList()
        return InferenceResult(raw, source, places)
    }
}
