package org.nobodymissing.domain

// Signal Scout (6.10): "back" only after N real checks succeed in a row; alert once per recovery.
data class ScoutStep(val run: Int, val state: String, val alert: Boolean)
data class ScoutRun(val states: List<String>, val alertAt: List<Int>)

fun scoutStep(run: Int, ok: Boolean, needed: Int): ScoutStep {
    val next = if (ok) run + 1 else 0
    return ScoutStep(next, if (next == 0) "no_signal" else if (next >= needed) "back" else "maybe", next == needed)
}

fun scoutStates(checks: List<Boolean>, needed: Int): ScoutRun {
    var run = 0
    val states = mutableListOf<String>(); val alerts = mutableListOf<Int>()
    checks.forEachIndexed { i, ok -> val s = scoutStep(run, ok, needed); run = s.run; states += s.state; if (s.alert) alerts += i }
    return ScoutRun(states, alerts)
}
