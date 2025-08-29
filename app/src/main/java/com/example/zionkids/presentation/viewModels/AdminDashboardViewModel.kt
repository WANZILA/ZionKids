package com.example.zionkids.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.Event
import com.example.zionkids.data.model.EventStatus
import com.example.zionkids.data.model.RegistrationStatus
import com.example.zionkids.data.model.Reply
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class HomeUi(
    val loading: Boolean = true,
    val error: String? = null,
    // KPIs
    val childrenTotal: Int = 0,
    val childrenNewThisMonth: Int = 0,
    val childrenGraduated: Int = 0,
    val childrenStale: Int = 0,
    val eventsToday: Int = 0,
    val eventsActiveNow: Int = 0,
    // Lists
    val happeningToday: List<Event> = emptyList(), // next 3 by time
    val eduDist: Map<EducationPreference, Int> = emptyMap(),
    val regionTop: List<Pair<String, Int>> = emptyList()
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val childrenRepo: ChildrenRepository,
    private val eventsRepo: EventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    init {
        // Combine both streams so dashboard updates in real time.
        viewModelScope.launch {
            combine(
                childrenRepo.streamChildren(),          // Flow<List<Child>>
                eventsRepo.streamEventSnapshots()       // Flow<List<Event>>
            ) { children, events ->
                compute(children, events)
            }
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") } }
                .collect { state -> _ui.value = state.copy(loading = false, error = null) }
        }
    }

    private fun compute(children: List<Child>, events: List<Event>): HomeUi {
        val now = System.currentTimeMillis()
        val sod = startOfDay(now)
        val eod = endOfDay(now)
        val (mStart, mEnd) = monthBounds(now)

        val childrenTotal = children.size
        val childrenNewThisMonth = children.count { it.createdAt in mStart..mEnd }
        val childrenGraduated = children.count { it.graduated == Reply.YES }
        val childrenStale = children.count { it.updatedAt < now - THIRTY_DAYS }

        // --- Use Timestamp all through for events ---
        fun Event.timeMillis(): Long = this.eventDate.toDate().time

        val eventsToday = events.count { it.timeMillis() in sod..eod }
        val eventsActiveNow = events.count { it.eventStatus == EventStatus.ACTIVE }

        val happeningToday = events
            .filter { it.timeMillis() in sod..eod }
            .sortedBy { it.timeMillis() }
            .take(3)

        val eduDist = children.groupingBy { it.educationPreference }.eachCount()

        val regionTop = children
            .groupingBy { it.region ?: "Unknown" }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return HomeUi(
            loading = false,
            error = null,
            childrenTotal = childrenTotal,
            childrenNewThisMonth = childrenNewThisMonth,
            childrenGraduated = childrenGraduated,
            childrenStale = childrenStale,
            eventsToday = eventsToday,
            eventsActiveNow = eventsActiveNow,
            happeningToday = happeningToday,
            eduDist = eduDist,
            regionTop = regionTop
        )
    }

    private fun startOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun endOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        timeInMillis
    }

    private fun monthBounds(time: Long): Pair<Long, Long> {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        c.add(Calendar.MILLISECOND, -1)
        return start to c.timeInMillis
    }

    companion object {
        private const val THIRTY_DAYS: Long = 30L * 24 * 60 * 60 * 1000
    }
}
