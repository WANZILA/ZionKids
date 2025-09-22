package com.example.zionkids.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.Event
import com.example.zionkids.data.model.EventStatus
import com.example.zionkids.data.model.Reply
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.EventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUi(
    val loading: Boolean = true,
    val error: String? = null,

    // KPIs
    val childrenTotal: Int = 0,
    val childrenNewThisMonth: Int = 0,
    val childrenGraduated: Int = 0,
    val sponsored: Int = 0,
    val resettled: Int = 0,
    val toBeResettled: Int = 0,
//    val childrenStale: Int = 0,
    val eventsToday: Int = 0,
    val eventsActiveNow: Int = 0,
    val acceptedChrist: Int = 0,
    val yetToAcceptChrist: Int = 0,

    // Lists / distributions
    val happeningToday: List<Event> = emptyList(),               // next 3 by time
    val eduDist: Map<EducationPreference, Int> = emptyMap(),
    val regionTop: List<Pair<String, Int>> = emptyList(),
    val streetTop: List<Pair<String, Int>> = emptyList()
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val childrenRepo: ChildrenRepository,
    private val eventsRepo: EventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                childrenRepo.streamChildren(),     // Flow<List<Child>>
                eventsRepo.streamEventSnapshots()  // Flow<List<Event>>
            ) { children, events ->
                compute(children, events)
            }
                .onStart { _ui.value = _ui.value.copy(loading = true, error = null) }
                .catch { e -> _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed to load") }
                .collect { state -> _ui.value = state.copy(loading = false, error = null) }
        }
    }

    // ----- Timestamp helpers -----
    private fun Timestamp?.millisOrZero(): Long = this?.toDate()?.time ?: 0L
    private fun Event.timeMillis(): Long = this.eventDate.toDate().time

    private fun compute(children: List<Child>, events: List<Event>): HomeUi {
        val nowMillis = System.currentTimeMillis()
        val sod = startOfDay(nowMillis)
        val eod = endOfDay(nowMillis)
        val (mStart, mEnd) = monthBounds(nowMillis)

        // KPIs (all based on Firestore Timestamp fields)
        val childrenTotal = children.size
        val childrenNewThisMonth = children.count { c ->
            val created = c.createdAt.millisOrZero()
            created in mStart..mEnd
        }
        val childrenGraduated = children.count { it.graduated == Reply.YES }
        val sponsored = children.count{ it.sponsoredForEducation   }
        val resettled = children.count{ it.resettled }
        val toBeResettled = children.count{ !it.resettled }
//        val acceptedChrist = children.count { it.acceptedJesus == Reply.YES }
        val acceptedChrist = children.count { c ->
            c.acceptedJesus == Reply.YES &&
                    c.acceptedJesusDate?.toDate()?.time?.let { it in mStart..mEnd } == true
        }

        val yetToAcceptChrist = children.count { c ->
            c.acceptedJesus == Reply.NO
        }

//        val childrenStale = children.count { c ->
//            val updated = c.updatedAt.millisOrZero()
//            updated < nowMillis - THIRTY_DAYS
//        }

        // Events — Timestamp throughout
        val eventsToday = events.count { e -> e.timeMillis() in sod..eod }
        val eventsActiveNow = events.count { it.eventStatus == EventStatus.ACTIVE }

        val happeningToday = events
            .filter { e -> e.timeMillis() in sod..eod }
            .sortedBy { it.timeMillis() }
            .take(4)

        // Distributions
        val eduDist = children.groupingBy { it.educationPreference }.eachCount()

        val regionTop = children
            .groupingBy { it.region ?: "Unknown" }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }

        val streetTop = children
            .groupingBy { it.region ?: "Unknown" }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key to it.value }


        return HomeUi(
            loading = false,
            error = null,
            childrenTotal = childrenTotal,
            childrenNewThisMonth = childrenNewThisMonth,
            childrenGraduated = childrenGraduated,
//            childrenStale = childrenStale,
            eventsToday = eventsToday,
            eventsActiveNow = eventsActiveNow,
            sponsored = sponsored,
            resettled = resettled,
            toBeResettled = toBeResettled,
            happeningToday = happeningToday,
            acceptedChrist = acceptedChrist,
            yetToAcceptChrist = yetToAcceptChrist,
            eduDist = eduDist,
            regionTop = regionTop,
            streetTop = streetTop,
        )
    }

    // ----- Calendar math stays in millis -----
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
