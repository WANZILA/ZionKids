package com.example.zionkids.presentation.viewModels.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Attendance
import com.example.zionkids.data.model.AttendanceStatus
import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.online.AttendanceRepository
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.EventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class RosterChild(
    val child: Child,
    val attendance: Attendance? = null,
    val present: Boolean = false
)

data class AttendanceRosterUiState(
    val loading: Boolean = true,
    val children: List<RosterChild> = emptyList(),
    val eventTitle: String? = null,
    val eventDate: Timestamp = Timestamp.now(),
    val error: String? = null,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false
)

@HiltViewModel
class AttendanceRosterViewModel @Inject constructor(
    private val childrenRepo: ChildrenRepository,
    private val attendanceRepo: AttendanceRepository,
    private val eventRepo: EventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AttendanceRosterUiState())
    val ui: StateFlow<AttendanceRosterUiState> = _ui.asStateFlow()

    private val _query = MutableStateFlow("")
    fun onSearchQueryChange(q: String) { _query.value = q.trim() }

    // one-off UI events (snackbar)
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    sealed class UiEvent {
        data class Saved(val pendingSync: Boolean) : UiEvent()
    }

    // bulk mode flag
    private val _bulkMode = MutableStateFlow(false)
    val bulkMode: StateFlow<Boolean> = _bulkMode.asStateFlow()

    // Cancel duplicate loads
    private var loadJob: Job? = null

    fun load(eventId: String, limit: Long = 300) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val eventFlow = flow { emit(runCatching { eventRepo.getEventFast(eventId) }.getOrNull()) }

            val attendanceFlow = attendanceRepo.streamAttendanceForEvent(eventId)
            val smoothAttendanceFlow =
                _bulkMode.flatMapLatest { bulk -> if (bulk) attendanceFlow.debounce(120) else attendanceFlow }

            combine(
                childrenRepo.streamAllNotGraduated(),
                smoothAttendanceFlow,
                eventFlow,
                _query.debounce(200)
            ) { childrenSnap, attendanceSnap, eventSnap, q ->
                val attMap = attendanceSnap.attendance.associateBy { it.childId }
                val merged = childrenSnap.children.map { child ->
                    val att = attMap[child.childId]
                    RosterChild(
                        child = child,
                        attendance = att,
                        present = att?.status == AttendanceStatus.PRESENT
                    )
                }

                val filtered = if (q.isBlank()) merged else {
                    val needle = q.lowercase()
                    merged.filter { rc ->
                        val full = "${rc.child.fName} ${rc.child.lName}".trim().lowercase()
                        full.contains(needle)
                    }
                }

                AttendanceRosterUiState(
                    loading = false,
                    children = filtered,
                    eventTitle = eventSnap?.title,
                    eventDate = eventSnap!!.eventDate,
                    error = null,
                    isOffline = childrenSnap.fromCache || attendanceSnap.fromCache,
                    isSyncing = childrenSnap.hasPendingWrites || attendanceSnap.hasPendingWrites
                )
            }
                .onStart { _ui.value = AttendanceRosterUiState(loading = true) }
                .catch { e ->
                    _ui.value = AttendanceRosterUiState(
                        loading = false,
                        error = e.message ?: "Failed to load roster"
                    )
                }
                .collect { state -> _ui.value = state }
        }
    }

    /** Notes updates for ABSENT children — uses Timestamp throughout ✅ */
    fun updateNotes(eventId: String, rosterChild: RosterChild, adminId: String, notes: String) {
        val nowTs = Timestamp.now()
        val att = Attendance(
            attendanceId = "${eventId}_${rosterChild.child.childId}",
            childId = rosterChild.child.childId,
            eventId = eventId,
            adminId = adminId,
            status = AttendanceStatus.ABSENT,
            notes = notes,
            checkedAt = nowTs,
            createdAt = rosterChild.attendance?.createdAt ?: nowTs,
            updatedAt = nowTs
        )
        attendanceRepo.enqueueUpsertAttendance(att)
        _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
    }

    /** Toggle PRESENT/ABSENT — uses Timestamp throughout ✅ */
    fun toggleAttendance(eventId: String, rosterChild: RosterChild, adminId: String) {
        val nowTs = Timestamp.now()
        val newStatus = if (rosterChild.present) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
        val att = Attendance(
            attendanceId = "${eventId}_${rosterChild.child.childId}",
            childId = rosterChild.child.childId,
            eventId = eventId,
            adminId = adminId,
            status = newStatus,
            notes = rosterChild.attendance?.notes ?: "",
            checkedAt = nowTs,
            createdAt = rosterChild.attendance?.createdAt ?: nowTs,
            updatedAt = nowTs
        )
        attendanceRepo.enqueueUpsertAttendance(att)
        _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
    }

    // --- BULK API (Present / Absent) → single snapshot via WriteBatch ---
    fun markAllPresent(eventId: String, adminId: String) =
        markAllInternalBatch(eventId, adminId, AttendanceStatus.PRESENT)

    fun markAllAbsent(eventId: String, adminId: String) =
        markAllInternalBatch(eventId, adminId, AttendanceStatus.ABSENT)

//    private fun markAllInternalBatch(eventId: String, adminId: String, status: AttendanceStatus) {
//        val kids = _ui.value.children
//        if (kids.isEmpty()) return
//
//        _bulkMode.value = true
//
//        // Build 'existing' map for skipping no-op updates
//        val existing = kids.associate { it.child.childId to it.attendance }
//
//        attendanceRepo
//            .markAllInBatchChunked(
//                eventId = eventId,
//                adminId = adminId,
//                children = kids.map { it.child },
//                existing = existing,
//                status = status
//            )
//            .addOnCompleteListener {
//                _events.tryEmit(UiEvent.Saved(pendingSync = _ui.value.isOffline || _ui.value.isSyncing))
//                _bulkMode.value = false
//            }
//            .addOnFailureListener {
//                // still fine offline; local writes are pending
//                _events.tryEmit(UiEvent.Saved(pendingSync = true))
//                _bulkMode.value = false
//            }
//    }


    private fun markAllInternalBatch(eventId: String, adminId: String, status: AttendanceStatus) {
        val kids = _ui.value.children
        if (kids.isEmpty()) return
        _ui.value = AttendanceRosterUiState(
            loading = true,

        )
        _bulkMode.value = true

        viewModelScope.launch {
            try {
                val targets = kids.filter { rc -> rc.attendance?.status != status }
                if (targets.isEmpty()) {
                    _events.tryEmit(UiEvent.Saved(pendingSync = false))
                    return@launch
                }

                val existing = kids.associate { it.child.childId to it.attendance }

                val task = attendanceRepo.markAllInBatchChunked(
                    eventId = eventId,
                    adminId = adminId,
                    children = targets.map { it.child },
                    existing = existing,
                    status = status
                )

                _events.tryEmit(UiEvent.Saved(pendingSync = true))

                // Await completion so busy stays true while work runs
                try {
                    task?.await()
                    _events.tryEmit(UiEvent.Saved(pendingSync = false))
                } catch (e: Exception) {
                    _events.tryEmit(UiEvent.Saved(pendingSync = true))
                }
            } finally {
                _bulkMode.value = false
            }
        }
    }

}
