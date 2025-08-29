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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
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

    // search query
    private val _query = MutableStateFlow("")
    fun onSearchQueryChange(q: String) { _query.value = q.trim() }

    fun load(eventId: String, limit: Long = 300) {
        viewModelScope.launch {
            // single-shot event fetch wrapped as a flow
            val eventFlow = flow { emit(eventRepo.getEventFast(eventId)) }

            combine(
                childrenRepo.streamAllNotGraduated(),
                attendanceRepo.streamAttendanceForEvent(eventId),
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
            checkedAt = nowTs,                                      // Timestamp ✅
            createdAt = rosterChild.attendance?.createdAt ?: nowTs, // Timestamp ✅
            updatedAt = nowTs                                       // Timestamp ✅
        )
        attendanceRepo.enqueueUpsertAttendance(att)
    }

    /** Toggle PRESENT/ABSENT — uses Timestamp throughout ✅ */
    fun toggleAttendance(eventId: String, rosterChild: RosterChild, adminId: String) {
        val nowTs = Timestamp.now()
        val newStatus =
            if (rosterChild.present) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT

        val att = Attendance(
            attendanceId = "${eventId}_${rosterChild.child.childId}",
            childId = rosterChild.child.childId,
            eventId = eventId,
            adminId = adminId,
            status = newStatus,
            notes = rosterChild.attendance?.notes ?: "",
            checkedAt = nowTs,                                      // Timestamp ✅
            createdAt = rosterChild.attendance?.createdAt ?: nowTs, // Timestamp ✅
            updatedAt = nowTs                                       // Timestamp ✅
        )
        // Offline-first enqueue
        attendanceRepo.enqueueUpsertAttendance(att)
    }
}
