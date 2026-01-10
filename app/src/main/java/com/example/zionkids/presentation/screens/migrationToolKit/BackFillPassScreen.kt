@file:Suppress("RedundantSuspendModifier", "FunctionName", "UnusedImport")

package com.example.zionkids.presentation.screens.migrationToolKit

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * BackFillPassScreen
 *
 * Same UI shell as your original:
 *  • Batch size field (default 480; under Firestore's 500 write limit)
 *  • Run button (disabled while job is running)
 *  • Live stats + streaming logs
 *
 * What it does (R/A/C/E):
 *  • Adds ONLY missing sync fields: isDirty=false, isDeleted=false, version=0L
 *    to children, attendances, events (merge-only; never clobbers existing keys)
 *  • Normalizes children.street to canonical casing if it contains known areas
 *  • Paged reads by documentId(); safe, batched writes; idempotent
 */
@Composable
fun BackFillPassScreen() {
    val app = LocalContext.current.applicationContext as Application
    val vm: BackFillPassVM = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackFillPassVM(app) as T
            }
        }
    )

    val logs: List<String> = vm.logs
    val isRunning by vm.isRunning
    val stats by vm.stats

    var batchSizeText by remember { mutableStateOf(TextFieldValue("480")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = batchSizeText,
                onValueChange = { batchSizeText = it },
                label = { Text("Batch size (max 490)") },
                singleLine = true,
                modifier = Modifier.width(200.dp)
            )
            Button(
                enabled = !isRunning,
                onClick = {
                    val bs = batchSizeText.text.toIntOrNull() ?: 480
                    vm.runBackfill(batchSize = bs.coerceIn(1, 490))
                }
            ) { Text(if (isRunning) "Running…" else "Run Backfill") }
        }

        // Stats line (children / attendances / events + normalized street count)
        Text(
            "Progress — " +
                    "children: ${stats.childrenScanned}/${stats.childrenQueued}/${stats.childrenCommitted} | " +
                    "attend: ${stats.attendScanned}/${stats.attendQueued}/${stats.attendCommitted} | " +
                    "events: ${stats.eventsScanned}/${stats.eventsQueued}/${stats.eventsCommitted} | " +
                    "street normalized: ${stats.streetNormalized}"
        )

        Divider()
        Text("Logs")
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            reverseLayout = true
        ) {
            items(logs.asReversed()) { line -> Text(line) }
        }
    }
}

/* ----------------------------- ViewModel ---------------------------------- */

/**
 * BackFillPassVM
 *
 * Packs all R/A/C/E backfill logic inside the ViewModel (no new top-level objects).
 * Phases:
 *  1) children  — add missing sync fields; normalize street
 *  2) attendances — add missing sync fields
 *  3) events — add missing sync fields
 */
class BackFillPassVM(app: Application) : AndroidViewModel(app) {

    data class Stats(
        val childrenScanned: Int = 0,
        val childrenQueued: Int = 0,
        val childrenCommitted: Int = 0,
        val attendScanned: Int = 0,
        val attendQueued: Int = 0,
        val attendCommitted: Int = 0,
        val eventsScanned: Int = 0,
        val eventsQueued: Int = 0,
        val eventsCommitted: Int = 0,
        val streetNormalized: Int = 0
    )

    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> get() = _isRunning

    private val _stats = mutableStateOf(Stats())
    val stats: State<Stats> get() = _stats

    private fun log(msg: String) {
        _logs.add(msg)
        if (_logs.size > 2000) repeat(200) { if (_logs.isNotEmpty()) _logs.removeAt(0) }
    }

    /**
     * Entry point: runs all phases with safe batching + merge semantics.
     */
    fun runBackfill(batchSize: Int = 480) {
        if (_isRunning.value) return
        _isRunning.value = true
        _logs.clear()
        _stats.value = Stats()

        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                log("Backfill started… 🧩")
                runChildrenPhase(db, batchSize)
                runAttendancesPhase(db, batchSize)
                runEventsPhase(db, batchSize)
                log("All phases done. ✅")
            } catch (t: Throwable) {
                log("Backfill error: ${t.message ?: t.toString()}")
                Log.e("BackFillPassVM", "Failed", t)
            } finally {
                _isRunning.value = false
            }
        }
    }

    /* ----------------------------- Phases -------------------------------- */

    private suspend fun runChildrenPhase(db: FirebaseFirestore, batchSize: Int) = withContext(Dispatchers.IO) {
        log("Phase 1: Children — add missing sync fields + normalize street…")
        val batcher = BatchWriter(db, maxOps = batchSize)

        var scanned = 0
        var queued = 0
        var committed = 0
        var normalized = 0
        var lastId: String? = null

        while (true) {
            var q: Query = db.collection("children")
                .orderBy(FieldPath.documentId())
                .limit(2_000)
            if (lastId != null) q = q.startAfter(lastId)

            val snap = try { q.get().await() } catch (e: Exception) {
                log("Children read failed: ${e.message}"); break
            }
            if (snap.isEmpty) break

            for (doc in snap.documents) {
                scanned++

                val existing = doc.data ?: emptyMap<String, Any?>()
                val toAdd = missingSyncDefaults(existing)        // add only absent keys
                val toUpdate = streetNormalizationUpdate(existing) // canonicalize street if contains known area

                if (toAdd.isNotEmpty() || toUpdate.isNotEmpty()) {
                    val payload = HashMap<String, Any?>(toAdd.size + toUpdate.size)
                    payload.putAll(toAdd)
                    payload.putAll(toUpdate)

                    batcher.set(doc.reference, payload, SetOptions.merge())
                    queued++
                    if (toUpdate.isNotEmpty()) normalized++

                    if (batcher.size() >= batcher.maxOps) {
                        batcher.flush()
                        committed += batcher.lastCommitCount
                        _stats.value = _stats.value.copy(
                            childrenScanned = scanned,
                            childrenQueued = queued,
                            childrenCommitted = committed,
                            streetNormalized = normalized
                        )
                        log("Children — scanned:$scanned queued:$queued committed:$committed normalized:$normalized")
                    }
                }

                _stats.value = _stats.value.copy(
                    childrenScanned = scanned,
                    childrenQueued = queued,
                    childrenCommitted = committed,
                    streetNormalized = normalized
                )
            }

            lastId = snap.documents.last().id
            log("Children scanned: $scanned (paging…)")
        }

        if (batcher.size() > 0) {
            batcher.flush()
            committed += batcher.lastCommitCount
            _stats.value = _stats.value.copy(
                childrenCommitted = committed,
                streetNormalized = normalized
            )
        }
        log("Phase 1 done — Children: scanned:$scanned queued:$queued committed:$committed normalized:$normalized")
    }

    private suspend fun runAttendancesPhase(db: FirebaseFirestore, batchSize: Int) = withContext(Dispatchers.IO) {
        log("Phase 2: Attendances — add missing sync fields…")
        val batcher = BatchWriter(db, maxOps = batchSize)

        var scanned = 0
        var queued = 0
        var committed = 0
        var lastId: String? = null

        while (true) {
            var q: Query = db.collection("attendances")
                .orderBy(FieldPath.documentId())
                .limit(2_000)
            if (lastId != null) q = q.startAfter(lastId)

            val snap = try { q.get().await() } catch (e: Exception) {
                log("Attendances read failed: ${e.message}"); break
            }
            if (snap.isEmpty) break

            for (doc in snap.documents) {
                scanned++

                val existing = doc.data ?: emptyMap<String, Any?>()
                val toAdd = missingSyncDefaults(existing) // add only absent keys

                if (toAdd.isNotEmpty()) {
                    batcher.set(doc.reference, toAdd, SetOptions.merge())
                    queued++

                    if (batcher.size() >= batcher.maxOps) {
                        batcher.flush()
                        committed += batcher.lastCommitCount
                        _stats.value = _stats.value.copy(
                            attendScanned = scanned,
                            attendQueued = queued,
                            attendCommitted = committed
                        )
                        log("Attendances — scanned:$scanned queued:$queued committed:$committed")
                    }
                }

                _stats.value = _stats.value.copy(
                    attendScanned = scanned,
                    attendQueued = queued,
                    attendCommitted = committed
                )
            }

            lastId = snap.documents.last().id
            log("Attendances scanned: $scanned (paging…)")
        }

        if (batcher.size() > 0) {
            batcher.flush()
            committed += batcher.lastCommitCount
            _stats.value = _stats.value.copy(attendCommitted = committed)
        }
        log("Phase 2 done — Attendances: scanned:$scanned queued:$queued committed:$committed")
    }

    private suspend fun runEventsPhase(db: FirebaseFirestore, batchSize: Int) = withContext(Dispatchers.IO) {
        log("Phase 3: Events — add missing sync fields…")
        val batcher = BatchWriter(db, maxOps = batchSize)

        var scanned = 0
        var queued = 0
        var committed = 0
        var lastId: String? = null

        while (true) {
            var q: Query = db.collection("events")
                .orderBy(FieldPath.documentId())
                .limit(2_000)
            if (lastId != null) q = q.startAfter(lastId)

            val snap = try { q.get().await() } catch (e: Exception) {
                log("Events read failed: ${e.message}"); break
            }
            if (snap.isEmpty) break

            for (doc in snap.documents) {
                scanned++

                val existing = doc.data ?: emptyMap<String, Any?>()
                val toAdd = missingSyncDefaults(existing) // add only absent keys

                if (toAdd.isNotEmpty()) {
                    batcher.set(doc.reference, toAdd, SetOptions.merge())
                    queued++

                    if (batcher.size() >= batcher.maxOps) {
                        batcher.flush()
                        committed += batcher.lastCommitCount
                        _stats.value = _stats.value.copy(
                            eventsScanned = scanned,
                            eventsQueued = queued,
                            eventsCommitted = committed
                        )
                        log("Events — scanned:$scanned queued:$queued committed:$committed")
                    }
                }

                _stats.value = _stats.value.copy(
                    eventsScanned = scanned,
                    eventsQueued = queued,
                    eventsCommitted = committed
                )
            }

            lastId = snap.documents.last().id
            log("Events scanned: $scanned (paging…)")
        }

        if (batcher.size() > 0) {
            batcher.flush()
            committed += batcher.lastCommitCount
            _stats.value = _stats.value.copy(eventsCommitted = committed)
        }
        log("Phase 3 done — Events: scanned:$scanned queued:$queued committed:$committed")
    }

    /* ---------------------------- Helpers ------------------------------- */

    /** Return only the sync defaults that are currently absent. */
    private fun missingSyncDefaults(existing: Map<String, Any?>): Map<String, Any?> {
        val out = HashMap<String, Any?>(3)
        if (!existing.containsKey("isDirty")) out["isDirty"] = false
        if (!existing.containsKey("isDeleted")) out["isDeleted"] = false
        if (!existing.containsKey("version")) out["version"] = 0L
        return out
    }

    /**
     * If 'street' contains any known area token (case-insensitive),
     * return a patch that sets its canonical casing. Example:
     *  "kisenyi zone 5" → "Kisenyi"
     */
    private fun streetNormalizationUpdate(existing: Map<String, Any?>): Map<String, Any?> {
        val raw = (existing["street"] as? String)?.trim().orEmpty()
        if (raw.isEmpty()) return emptyMap()

        val canonicalAreas = listOf(
            "Katwe", "Owino", "Kisenyi", "Nakivubo", "Nakasero",
            "Old Taxi Park", "New Taxi Park", "Bakuli", "Bwaise",
            "Fly Over", "Arua Park", "Katanga", "Kamwokya"
        )

        val lower = raw.lowercase(Locale.getDefault())
        val canonical = canonicalAreas.firstOrNull { area ->
            lower.contains(area.lowercase(Locale.getDefault()))
        } ?: return emptyMap()

        return if (raw != canonical) mapOf("street" to canonical) else emptyMap()
    }

    /* ------------------------- Mini BatchWriter -------------------------- */

    /**
     * Small internal batch helper so we don't introduce new files/types.
     * Keeps writes <500 per commit (default 480) and exposes commit counts.
     */
    private class BatchWriter(
        private val db: FirebaseFirestore,
        val maxOps: Int = 480
    ) {
        private var batch = db.batch()
        private var ops = 0
        var lastCommitCount: Int = 0
            private set

        fun size(): Int = ops

        fun set(
            ref: com.google.firebase.firestore.DocumentReference,
            data: Map<String, Any?>,
            setOptions: SetOptions
        ) {
            batch.set(ref, data, setOptions)
            ops++
        }

        suspend fun flush() {
            if (ops == 0) return
            try {
                batch.commit().await()
                lastCommitCount = ops
            } finally {
                batch = db.batch()
                ops = 0
            }
        }
    }
}
