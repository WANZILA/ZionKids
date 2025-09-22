package com.example.zionkids.presentation.viewModels.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.AttendanceStatus
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.Event
import com.example.zionkids.domain.repositories.online.AttendanceRepository
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.EventsRepository
import com.example.zionkids.core.Utils.Network.NetworkMonitorUtil
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class ChildHistory(
    val child: Child,
    val lastEvents: List<AttendanceStatus>,   // newest first (size up to N)
    val consecutiveAbsences: Int              // streak from most recent backwards
)

data class AttendanceDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val eventTitle: String = "Attendance",
    val eventDateText: String = "",
    val totalChildren: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val presentPct: Int = 0,
    val absentPct: Int = 0,
    val recentEventTrend: List<Pair<String, Int>> = emptyList(),
    val topAttendees: List<Child> = emptyList(),
    val frequentAbsentees: List<Child> = emptyList(),
    val consecutiveAbsenceAlerts: List<ChildHistory> = emptyList(),
    val notesSummaryTop: List<Pair<String, Int>> = emptyList(),
    val childHistories: List<ChildHistory> = emptyList()
)

@HiltViewModel
class AttendanceDashboardViewModel @Inject constructor(
    private val childrenRepo: ChildrenRepository,
    private val attendanceRepo: AttendanceRepository,
    private val eventsRepo: EventsRepository,
    private val networkMonitor: NetworkMonitorUtil,
) : ViewModel() {

    private val _selectedEventId = MutableStateFlow<String?>(null)

    data class Ui(
        val loading: Boolean = true,
        val error: String? = null,
        val isOffline: Boolean = false,
        val isSyncing: Boolean = false,
        val events: List<Event> = emptyList(),
        val selectedEventId: String? = null,
        val selectedEventTitle: String = "Attendance",
        val selectedEventDateText: String = "",
        val total: Int = 0,
        val present: Int = 0,
        val absent: Int = 0,
        val presentPct: Int = 0,
        val absentPct: Int = 0,
        val trend: List<Pair<String,Int>> = emptyList(),
        val alerts: List<ChildHistory> = emptyList(),
        val notesTop: List<Pair<String,Int>> = emptyList(),
        val topAttendees: List<Child> = emptyList(),
        val frequentAbsentees: List<Child> = emptyList(),
        val histories: List<ChildHistory> = emptyList()
    )
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    init {
        // Load events list and auto-select first
        viewModelScope.launch {
            eventsRepo.streamEventSnapshots()  // ordered DESC by eventDate in repo
                .onStart { _ui.update { it.copy(loading = true) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message) } }
                .collect { events ->
                    _ui.update { it.copy(events = events) }
                    if (_selectedEventId.value == null && events.isNotEmpty()) {
                        _selectedEventId.value = events.first().eventId
                    }
                }
        }

        // When an event is selected, compute the dashboard
        viewModelScope.launch {
            combine(
                childrenRepo.streamAllNotGraduated(),
                _selectedEventId.filterNotNull().flatMapLatest { id ->
                    combine(
                        flow { emit(eventsRepo.getEventFast(id)) },
                        attendanceRepo.streamAttendanceForEvent(id)
                    ) { ev, attSnap -> ev to attSnap }
                },
                networkMonitor.isOnline
            ) { childrenSnap, (event, attSnap), isOnline ->

                val children = childrenSnap.children
                val att = attSnap.attendance
                val present = att.count { it.status == AttendanceStatus.PRESENT }
                val absent = att.count { it.status == AttendanceStatus.ABSENT }
//                val total = children.size
                val total = present + absent
//                val absent = (total - present).coerceAtLeast(0)
                val presentPct = pct(present, total)
                val absentPct = pct(absent, total)



                // notes summary
//                val notesTop = att.filter { it.status == AttendanceStatus.ABSENT }
//                    .map { it.notes.trim() }
//                    .filter { it.isNotEmpty() }
//                    .groupingBy { normalize(it) }.eachCount()
//                    .entries.sortedByDescending { it.value }.take(5)
//                    .map { it.key to it.value }

                // --- Use Timestamp for event times ---
//                fun Event.timeMillis(): Long = this.eventDate.toDate().time

                // trend (last 7 events present counts)
//                val recent = _ui.value.events.take(4)
                val recent = _ui.value.events.take(3)
                val trend = recent.map { ev ->
                    val count = attendanceRepo.getAttendanceOnce(ev.eventId)
                        .count { it.status == AttendanceStatus.PRESENT }
                    label(ev.title.trim().take(15)) to count
                }




                // Status flags
                val offlineHeuristic =
                    (childrenSnap.fromCache && !childrenSnap.hasPendingWrites) &&
                            (attSnap.fromCache && !attSnap.hasPendingWrites)
                val syncing = childrenSnap.hasPendingWrites || attSnap.hasPendingWrites
                val isOffline = !isOnline || offlineHeuristic

                Ui(
                    loading = false,
                    error = null,
                    isOffline = isOffline,
                    isSyncing = syncing,
                    events = _ui.value.events,
                    selectedEventId = _selectedEventId.value,
                    selectedEventTitle = event?.title ?: "Attendance",
                    selectedEventDateText = event?.eventDate?.let { fullLabel(it) } ?: "",
                    total = total,
                    present = present,
                    absent = absent,
                    presentPct = presentPct,
                    absentPct = absentPct,
                    trend = trend,
//                    alerts = alerts,
//                    notesTop = notesTop,
//                    topAttendees = topAttendees,
//                    frequentAbsentees = frequentAbsentees,
//                    histories = histories
                )
            }
                .catch { e ->
                    _ui.update { it.copy(loading = false, error = e.message) }
                }
                .collect { s -> _ui.value = s }
        }
    }

    fun onSelectEvent(id: String) { _selectedEventId.value = id }

    // helpers
    private fun pct(n: Int, d: Int) = if (d <= 0) 0 else ((n.toDouble() / d) * 100).toInt()

//    private fun consecutiveAbsencesFromNewest(statuses: List<AttendanceStatus>): Int {
//        var c = 0
//        for (i in statuses.indices.reversed()) if (statuses[i] == AttendanceStatus.ABSENT) c++ else break
//        return c
//    }

//    private fun normalize(s: String) =
//        s.lowercase().replace(Regex("\\s+"), " ").replace(Regex("[^a-z0-9 \\-]"), "").trim()

    private fun label(ts: String): String {
        return ts
    }


    // ----- Timestamp-based labels -----
//    private fun label(ts: Timestamp): String =
//        SimpleDateFormat("MMM d", Locale.getDefault()).format(ts.toDate())

    private fun fullLabel(ts: Timestamp): String =
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(ts.toDate())
}
