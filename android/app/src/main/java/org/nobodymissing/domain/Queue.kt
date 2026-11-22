package org.nobodymissing.domain

// Outbox job queue state machine + sync priority (6.10).
data class JobStatus(val state: String, val retries: Int) : java.io.Serializable
data class QueuedJob(val id: String, val type: String, val createdAt: Long)
data class Progress(val sent: Int, val total: Int, val waiting: Int)

private val NEXT = mapOf(
    "start" to (listOf("waiting") to "processing"),
    "send" to (listOf("processing") to "sending"),
    "ack" to (listOf("sending") to "sent"),
    "fail" to (listOf("processing", "sending") to "failed"),
    "retry" to (listOf("failed") to "waiting"),
)

fun transition(j: JobStatus, ev: String): JobStatus {
    val (from, to) = NEXT[ev] ?: error("unknown event $ev")
    check(j.state in from) { "invalid $ev from ${j.state}" }
    return JobStatus(to, j.retries + if (ev == "retry") 1 else 0)
}

fun applyEvents(events: List<String>, start: JobStatus = JobStatus("waiting", 0)) = events.fold(start, ::transition)

fun <T : Any> orderJobs(jobs: List<T>, prio: Map<String, Int>, type: (T) -> String, createdAt: (T) -> Long): List<T> {
    val p = { t: String -> prio[t] ?: prio.getValue("other") }
    return jobs.sortedWith(compareBy<T> { p(type(it)) }.thenBy { createdAt(it) })
}
fun orderJobs(jobs: List<QueuedJob>, prio: Map<String, Int>) = orderJobs(jobs, prio, { it.type }, { it.createdAt })

fun progress(states: List<String>): Progress {
    val sent = states.count { it == "sent" }
    return Progress(sent, states.size, states.size - sent)
}
