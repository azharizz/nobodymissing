package org.nobodymissing.domain

// Reach list per announcement (F8).
data class ReachPerson(val id: String, val age: Int?, val flags: List<String>)
data class ReachEntry(val id: String, val reasons: List<String>, val told: Boolean)
data class ReachResult(val list: List<ReachEntry>, val remaining: Int)

fun reachList(people: List<ReachPerson>, told: List<String>, r: Rules): ReachResult {
    val list = people.map { p ->
        ReachEntry(p.id, r.reachFlags.filter { it in p.flags } + if ((p.age ?: 0) >= r.reachAge) listOf("age ${r.reachAge}+") else emptyList(), p.id in told)
    }.filter { it.reasons.isNotEmpty() }
    return ReachResult(list, list.count { !it.told })
}
