package org.nobodymissing.features

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.nobodymissing.domain.JobStatus
import org.nobodymissing.domain.PickupRecord
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** Everything a shelter must not lose when Android kills the app. Photos (batch) are never saved, same as web. */
class Saved(
    val people: ArrayList<UiPerson>, val meds: ArrayList<UiMed>, val jobs: ArrayList<UiJob>, val round: HashMap<String, String>,
    val field: ArrayList<UiField>, val announcements: ArrayList<UiAnnouncement>, val help: ArrayList<UiHelp>,
    val familyGroup: FamilyGroup?, val myPickup: Pair<PickupRecord, String>?, val warning: ActiveWarning?,
    val lang: String, val shelterId: String, val privateMode: Boolean,
    val day: Long, val device: Int, val cloud: Int,
) : java.io.Serializable { companion object { private const val serialVersionUID = 1L } }

fun Features.snapshot() = Saved(ArrayList(people), ArrayList(meds), ArrayList(jobs.map { it.copy(status = it.status.copy()) }), HashMap(round),
    ArrayList(field), ArrayList(announcements), ArrayList(help), familyGroup.value, myPickup.value, warning.value, lang.value, shelterId.value, privateMode.value,
    today(), router.deviceCount, router.cloudCount)

/** The live counter says "Today", so it starts again each day. */
private fun today() = java.time.LocalDate.now().toEpochDay()

fun Features.restore(s: Saved) {
    people.clear(); people += s.people; meds.clear(); meds += s.meds; round.clear(); round += s.round
    field.clear(); field += s.field; announcements.clear(); announcements += s.announcements; help.clear(); help += s.help
    // A job cut off mid-send was not confirmed by the server: send it again (writes are idempotent by doc id).
    jobs.clear(); jobs += s.jobs.map { if (it.status.state == "processing" || it.status.state == "sending") it.copy(status = JobStatus("waiting", it.status.retries)) else it }
    familyGroup.value = s.familyGroup; myPickup.value = s.myPickup; warning.value = s.warning
    lang.value = s.lang; shelterId.value = s.shelterId; privateMode.value = s.privateMode
    if (s.day == today()) { router.deviceCount = s.device; router.cloudCount = s.cloud }
}

/** Write to a temp file then rename, so a kill mid-write keeps the last good copy. */
fun Features.saveTo(file: File, s: Saved = snapshot()) = runCatching {
    val tmp = File(file.path + ".tmp")
    ObjectOutputStream(tmp.outputStream().buffered()).use { it.writeObject(s) }
    tmp.renameTo(file)
}.onFailure { android.util.Log.w("NobodyMissing", "save failed", it) }

/** False when there is nothing (or nothing readable, e.g. after an app update changed a class): start fresh. */
fun Features.loadFrom(file: File): Boolean = file.exists() && runCatching {
    ObjectInputStream(file.inputStream().buffered()).use { restore(it.readObject() as Saved) }
}.isSuccess

/** Save shortly after any change: snapshotFlow re-runs (and emits a fresh copy) whenever a state it read changes. */
@OptIn(FlowPreview::class)
fun Features.autoSave(file: File, scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) = scope.launch {
    snapshotFlow { snapshot() }.drop(1).debounce(300).collect { s -> launch(Dispatchers.IO) { saveTo(file, s) } }
}
