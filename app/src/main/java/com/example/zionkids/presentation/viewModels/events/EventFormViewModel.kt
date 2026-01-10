// <app/src/main/java/com/example/zionkids/presentation/viewModels/events/EventFormViewModel.kt>
// /// CHANGED: Consume OfflineEventsRepository (Room-first) instead of online EventsRepository.
// /// CHANGED: loadForEdit() reads from Room via repo.getEventFast(id).
// /// CHANGED: save() writes to Room via repo.createOrUpdateEvent(...), marking isDirty for sync.

package com.example.zionkids.presentation.viewModels.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.utils.FormValidatorUtil
import com.example.zionkids.core.Utils.GenerateId
import com.example.zionkids.core.sync.event.EventSyncScheduler
import com.example.zionkids.core.sync.event.EventSyncWorker
import com.example.zionkids.data.model.Event
import com.example.zionkids.data.model.EventStatus
import com.example.zionkids.domain.repositories.offline.OfflineEventsRepository   // /// CHANGED
//import com.example.zionkids.domain.sync.ChildrenSyncWorker
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventFormViewModel @Inject constructor(
    private val repo: OfflineEventsRepository,   // /// CHANGED: inject offline repo
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(EventFormUIState())
    val ui: StateFlow<EventFormUIState> = _ui.asStateFlow()

    private val _events = Channel<EventFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface EventFormEvent {
        data class Saved(val id: String) : EventFormEvent
        data class Error(val msg: String) : EventFormEvent
    }

    // ---- setters for fields ----
    fun onTitle(v: String) { _ui.value = _ui.value.copy(title = v) }
    fun onTeamName(v: String) { _ui.value = _ui.value.copy(teamName = v) }
    fun onTeamLeaderNames(v: String) { _ui.value = _ui.value.copy(teamLeaderNames = v) }
    fun onLeaderTelephone1(v: String) { _ui.value = _ui.value.copy(leaderTelephone1 = v) }
    fun onLeaderTelephone2(v: String) { _ui.value = _ui.value.copy(leaderTelephone2 = v) }
    fun onLeaderEmail(v: String) { _ui.value = _ui.value.copy(leaderEmail = v) }
    fun onDatePicked(ts: Timestamp) { _ui.value = _ui.value.copy(eventDate = ts) }
    fun onDatePickedMillis(millis: Long?) {
        _ui.value = _ui.value.copy(
            eventDate = millis?.let { Timestamp(it / 1000, ((it % 1000).toInt()) * 1_000_000) }
                ?: _ui.value.eventDate
        )
    }
    fun onLocation(v: String) { _ui.value = _ui.value.copy(location = v) }
    fun onNotes(v: String) { _ui.value = _ui.value.copy(notes = v) }
    fun onAdminId(v: String) { _ui.value = _ui.value.copy(adminId = v) }
    fun onStatus(v: EventStatus) { _ui.value = _ui.value.copy(eventStatus = v) }

    fun ensureNewIdIfNeeded() {
        val curr = _ui.value
        if (curr.eventId.isBlank()) {
            val now = Timestamp.now()
            _ui.value = curr.copy(
                eventId = GenerateId.generateId("event"),
                createdAt = now,
                updatedAt = now,
                isNew = true
            )
        }
    }

    // ---- load existing (Room-first) ----
    fun loadForEdit(eventId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        val existing = repo.getEventFast(eventId)   // /// CHANGED: read from offline repo
        _ui.value = if (existing != null) {
            _ui.value.from(existing).copy(loading = false, isNew = false)
        } else {
            _ui.value.copy(loading = false, error = "Event not found")
        }
    }

    /** Final save (create or update). Emits Saved(id) on success. */
    fun save() = viewModelScope.launch {
        _ui.value = _ui.value.copy(saving = true, error = null)

        val curr = _ui.value
        val titleRes    = FormValidatorUtil.validateName(curr.title)
        val locationRes = FormValidatorUtil.validateName(curr.location)
        val teamRes     = FormValidatorUtil.validateName(curr.teamName)

        val hasInvalid = listOf(titleRes, locationRes, teamRes).any { !it.isValid }
        if (hasInvalid) {
            _ui.value = curr.copy(
                saving = false,
                error = "Please fix the highlighted fields.",
                title = titleRes.value,             titleError = titleRes.error,
                location = locationRes.value,       locationError = locationRes.error,
                teamName = teamRes.value,           teamNameError = teamRes.error,
            )
            _events.trySend(EventFormEvent.Error("Missing or invalid fields"))
            return@launch
        }

        // Ensure ID for new event
        val ensured = _ui.value
        val finalId = if (ensured.eventId.isBlank()) {
            GenerateId.generateId("event").also {
                _ui.value = ensured.copy(eventId = it, isNew = true)
            }
        } else ensured.eventId

        val nowTs = Timestamp.now()
        val event = buildEvent(_ui.value, id = finalId, nowTs = nowTs)

        runCatching { repo.createOrUpdateEvent(event, isNew = _ui.value.isNew) }  // /// CHANGED
            .onSuccess {
                _ui.value = _ui.value.copy(
                    saving = false,
                    eventId = finalId,
                    isNew = false,
                    createdAt = nowTs,
                    updatedAt = nowTs
                )
                val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("event_sync_now")
                    .build()

                WorkManager.getInstance(appContext).enqueueUniqueWork(
//                    "children-push-once",
//                    ExistingWorkPolicy.REPLACE,
                    "event_sync_queue",
                    ExistingWorkPolicy.APPEND,
                    req
                )
                EventSyncScheduler.enqueueNow(appContext)
                _events.trySend(EventFormEvent.Saved(finalId))
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Failed to save")
                _events.trySend(EventFormEvent.Error("Failed to save"))
            }
    }

    // ---- helpers ----
    private fun buildEvent(state: EventFormUIState, id: String, nowTs: Timestamp): Event =
        Event(
            eventId = id,
            title = state.title ?: "",
            eventDate = state.eventDate,
            teamName = state.teamName,
            teamLeaderNames = state.teamLeaderNames,
            leaderTelephone1 = state.leaderTelephone1,
            leaderTelephone2 = state.leaderTelephone2,
            leaderEmail = state.leaderEmail,
            location = state.location ?: "",
            eventStatus = state.eventStatus,
            notes = state.notes ?: "",
            adminId = state.adminId ?: "",
            createdAt = state.createdAt ?: nowTs, // Timestamp ✅
            updatedAt = nowTs                      // Timestamp ✅
        )

    private fun EventFormUIState.from(e: Event) = copy(
        eventId = e.eventId,
        title = e.title,
        eventDate = e.eventDate,
        teamName = e.teamName,
        teamLeaderNames = e.teamLeaderNames,
        leaderTelephone1 = e.leaderTelephone1,
        leaderTelephone2 = e.leaderTelephone2,
        leaderEmail = e.leaderEmail,
        location = e.location,
        eventStatus = e.eventStatus,
        notes = e.notes,
        adminId = e.adminId,
        createdAt = e.createdAt,      // Timestamp ✅
        updatedAt = e.updatedAt       // Timestamp ✅
    )
}

data class EventFormUIState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    // per-field errors
    val titleError: String? = null,
    val locationError: String? = null,
    val teamNameError: String? = null,
    val teamLeaderNameError: String? = null,

    val eventId: String = "",
    val title: String? = null,
    val eventDate: Timestamp = Timestamp.now(),
    val teamName: String = "",
    val teamLeaderNames: String = "",
    val leaderTelephone1: String = "",
    val leaderTelephone2: String = "",
    val leaderEmail: String = "",
    val location: String? = null,
    val eventStatus: EventStatus = EventStatus.SCHEDULED,
    val notes: String? = null,
    val adminId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

//package com.example.zionkids.presentation.viewModels.events
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.core.utils.FormValidatorUtil
//import com.example.zionkids.core.Utils.GenerateId
//import com.example.zionkids.data.model.Event
//import com.example.zionkids.data.model.EventStatus
//import com.example.zionkids.domain.repositories.online.EventsRepository
//import com.google.firebase.Timestamp
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.receiveAsFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class EventFormViewModel @Inject constructor(
//    private val repo: EventsRepository
//) : ViewModel() {
//
//    private val _ui = MutableStateFlow(EventFormUIState())
//    val ui: StateFlow<EventFormUIState> = _ui.asStateFlow()
//
//    private val _events = Channel<EventFormEvent>(Channel.BUFFERED)
//    val events = _events.receiveAsFlow()
//
//    sealed interface EventFormEvent {
//        data class Saved(val id: String) : EventFormEvent
//        data class Error(val msg: String) : EventFormEvent
//    }
//
////    // ---- setters for fields ----
//    fun onTitle(v: String) {
//        _ui.value = _ui.value.copy(title = v)
//    }
//
//
//
//    fun onTeamName(v: String) {
//        _ui.value = _ui.value.copy(teamName = v)
//    }
//
//    fun onTeamLeaderNames(v: String) {
//        _ui.value = _ui.value.copy(teamLeaderNames = v)
//    }
//
//    fun onLeaderTelephone1(v: String) {
//        _ui.value = _ui.value.copy(leaderTelephone1 = v)
//    }
//
//    fun onLeaderTelephone2(v: String) {
//        _ui.value = _ui.value.copy(leaderTelephone2 = v)
//    }
//
//    fun onLeaderEmail(v: String) {
//        _ui.value = _ui.value.copy(leaderEmail = v)
//    }
//
//    fun onDatePicked(ts: Timestamp) {
//        _ui.value = _ui.value.copy(eventDate = ts)
//    }
//
//    fun onDatePickedMillis(millis: Long?) {
//        _ui.value = _ui.value.copy(
//            eventDate = millis?.let { Timestamp(it / 1000, ((it % 1000).toInt()) * 1_000_000) }
//                ?: _ui.value.eventDate
//        )
//    }
//
//    fun onLocation(v: String) {
//        _ui.value = _ui.value.copy(location = v)
//    }
//
//    fun onNotes(v: String) {
//        _ui.value = _ui.value.copy(notes = v)
//    }
//
//    fun onAdminId(v: String) {
//        _ui.value = _ui.value.copy(adminId = v)
//    }
//
//    fun onStatus(v: EventStatus) {
//        _ui.value = _ui.value.copy(eventStatus = v)
//    }
//
//    fun ensureNewIdIfNeeded() {
//        val curr = _ui.value
//        if (curr.eventId.isBlank()) {
//            val now = Timestamp.now()
//            _ui.value = curr.copy(
//                eventId = GenerateId.generateId("event"),
//                createdAt = now,
//                updatedAt = now,
//                isNew = true
//            )
//        }
//    }
//
//    // ---- load existing ----
//    fun loadForEdit(eventId: String) = viewModelScope.launch {
//        _ui.value = _ui.value.copy(loading = true, error = null)
//        val existing = repo.getEventFast(eventId)
//        _ui.value = if (existing != null) {
//            _ui.value.from(existing).copy(loading = false, isNew = false)
//        } else {
//            _ui.value.copy(loading = false, error = "Event not found")
//        }
//    }
//
//    /** Final save (create or update). Emits Saved(id) on success. */
//    fun save() = viewModelScope.launch {
//        _ui.value = _ui.value.copy(saving = true, error = null)
//
//        val curr = _ui.value
//
//        val titleRes    = FormValidatorUtil.validateName(curr.title)
//        val locationRes = FormValidatorUtil.validateName(curr.location)
//        val teamRes     = FormValidatorUtil.validateName(curr.teamName)
//
//        val hasInvalid = listOf(titleRes, locationRes, teamRes).any { !it.isValid }
//        if (hasInvalid) {
//            _ui.value = curr.copy(
//                saving = false,
//                error = "Please fix the highlighted fields.",
//                // push cleaned values + errors to UI so fields show normalized text and error messages
//                title = titleRes.value,             titleError = titleRes.error,
//                location = locationRes.value,       locationError = locationRes.error,
//                teamName = teamRes.value,           teamNameError = teamRes.error,
////                teamLeaderNames = teamLeaderNameRes.value,  teamLeaderNameError = teamLeaderNameRes.error
//
//                // If using DOB:
//                // dobText = if (dobRes.isValid) FormValidatorUtil.formatDate(dobRes.value) else curr.dobText,
//                // dobError = dobRes.error
//            )
//            _events.trySend(EventFormEvent.Error("Missing or invalid fields"))
//            return@launch
//        }
//
//
//        // Ensure ID for new event
//        val ensured = _ui.value
//        val finalId = if (ensured.eventId.isBlank()) {
//            GenerateId.generateId("event").also {
//                _ui.value = ensured.copy(eventId = it, isNew = true)
//            }
//        } else ensured.eventId
//
//        val nowTs = Timestamp.now()
//        val event = buildEvent(_ui.value, id = finalId, nowTs = nowTs)
//
//        runCatching { repo.createOrUpdateEvent(event, isNew = _ui.value.isNew) }
//            .onSuccess {
//                _ui.value = _ui.value.copy(
//                    saving = false,
//                    eventId = finalId,
//                    isNew = false,
//                    updatedAt = nowTs
//                )
//                _events.trySend(EventFormEvent.Saved(finalId))
//            }
//            .onFailure { e ->
//                _ui.value = _ui.value.copy(saving = false, error = e.message ?: "Failed to save")
//                _events.trySend(EventFormEvent.Error("Failed to save"))
//            }
//    }
//
//    // ---- helpers ----
//    private fun buildEvent(state: EventFormUIState, id: String, nowTs: Timestamp): Event =
//        Event(
//            eventId = id,
//            title = state.title ?: "",
//            eventDate = state.eventDate,
//            teamName = state.teamName,
//            teamLeaderNames = state.teamLeaderNames,
//            leaderTelephone1 = state.leaderTelephone1,
//            leaderTelephone2 = state.leaderTelephone2,
//            leaderEmail = state.leaderEmail,
//            location = state.location ?: "",
//            eventStatus = state.eventStatus,
//            notes = state.notes ?: "",
//            adminId = state.adminId ?: "",
//            createdAt = state.createdAt ?: nowTs, // Timestamp ✅
//            updatedAt = nowTs                      // Timestamp ✅
//        )
//
//    private fun EventFormUIState.from(e: Event) = copy(
//        eventId = e.eventId,
//        title = e.title,
//        eventDate = e.eventDate,
//        teamName = e.teamName,
//        teamLeaderNames = e.teamLeaderNames,
//        leaderTelephone1 = e.leaderTelephone1,
//        leaderTelephone2 = e.leaderTelephone2,
//        leaderEmail = e.leaderEmail,
//        location = e.location,
//        eventStatus = e.eventStatus,
//        notes = e.notes,
//        adminId = e.adminId,
//        createdAt = e.createdAt,      // Timestamp ✅
//        updatedAt = e.updatedAt       // Timestamp ✅
//    )
//}
//
//data class EventFormUIState(
//    val loading: Boolean = false,
//    val saving: Boolean = false,
//    val error: String? = null,
//    val isNew: Boolean = true,
//
//    // per-field errors
//    val titleError: String? = null,
//    val locationError: String? = null,
//    val teamNameError: String? = null,
//    val teamLeaderNameError: String? = null,
//
//
//    val eventId: String = "",
//    val title: String? = null,
//    val eventDate: Timestamp = Timestamp.now(),
//    val teamName: String = "",
//    val teamLeaderNames: String = "",
//    val leaderTelephone1: String = "",
//    val leaderTelephone2: String = "",
//    val leaderEmail: String = "",
//    val location: String? = null,
//    val eventStatus: EventStatus = EventStatus.SCHEDULED,
//    val notes: String? = null,
//    val adminId: String? = null,
//    val createdAt: Timestamp? = null,
//    val updatedAt: Timestamp? = null
//)
