package com.example.zionkids.presentation.viewModels.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.core.Utils.GenerateId
import com.example.zionkids.data.model.Event
import com.example.zionkids.data.model.EventStatus
import com.example.zionkids.domain.repositories.online.EventsRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventFormViewModel @Inject constructor(
    private val repo: EventsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(EventFormUIState())
    val ui: StateFlow<EventFormUIState> = _ui.asStateFlow()

    private val _events = Channel<EventFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface EventFormEvent {
        data class Saved(val id: String) : EventFormEvent
        data class Error(val msg: String) : EventFormEvent
    }

    // ---- setters for fields (StateFlow style) ----
    fun onTitle(v: String)        { _ui.value = _ui.value.copy(title = v) }
    fun onDatePicked(ts: Timestamp){ _ui.value = _ui.value.copy(eventDate = ts) }
    fun onLocation(v: String)     { _ui.value = _ui.value.copy(location = v) }
    fun onNotes(v: String)        { _ui.value = _ui.value.copy(notes = v) }
    fun onAdminId(v: String)      { _ui.value = _ui.value.copy(adminId = v) }
    fun onStatus(v: EventStatus)  { _ui.value = _ui.value.copy(eventStatus = v) }

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

    // ---- load existing ----
    fun loadForEdit(eventId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        val existing = repo.getEventFast(eventId)
        _ui.value = if (existing != null) {
            _ui.value.from(existing).copy(loading = false, isNew = false)
        } else {
            _ui.value.copy(loading = false, error = "Event not found")
        }
    }

    /** Final save */
    fun save() = viewModelScope.launch {
        val before = _ui.value
        _ui.value = before.copy(saving = true, error = null)

        val curr = _ui.value
        if (curr.title.isNullOrBlank()) {
            _ui.value = curr.copy(saving = false, error = "Title is required.")
            _events.trySend(EventFormEvent.Error("Missing required fields"))
            return@launch
        }
        if (curr.location.isNullOrBlank()) {
            _ui.value = curr.copy(saving = false, error = "Location is required.")
            _events.trySend(EventFormEvent.Error("Missing required fields"))
            return@launch
        }

        // Ensure ID if needed
        val ensured = _ui.value
        val finalId = if (ensured.eventId.isBlank()) {
            GenerateId.generateId("event").also {
                _ui.value = ensured.copy(eventId = it, isNew = true)
            }
        } else ensured.eventId

        val nowTs = Timestamp.now()
        val stateForBuild = _ui.value
        val event = buildEvent(stateForBuild, id = finalId, nowTs = nowTs)

        runCatching { repo.createOrUpdateEvent(event, isNew = stateForBuild.isNew) }
            .onSuccess {
                _ui.value = _ui.value.copy(saving = false, eventId = finalId, isNew = false, updatedAt = nowTs)
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
            eventId    = id,
            title      = state.title ?: "",
            eventDate  = state.eventDate,                 // Timestamp ✅
            location   = state.location ?: "",
            eventStatus= state.eventStatus,
            notes      = state.notes ?: "",
            adminId    = state.adminId ?: "",
            createdAt  = state.createdAt ?: nowTs,        // Timestamp ✅
            updatedAt  = nowTs                             // Timestamp ✅
        )

    private fun EventFormUIState.from(e: Event) = copy(
        eventId    = e.eventId,
        title      = e.title,
        eventDate  = e.eventDate,     // Timestamp ✅
        location   = e.location,
        eventStatus= e.eventStatus,
        notes      = e.notes,
        adminId    = e.adminId,
        createdAt  = e.createdAt,     // Timestamp ✅
        updatedAt  = e.updatedAt      // Timestamp ✅
    )
}

data class EventFormUIState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val isNew: Boolean = true,

    val eventId: String = "",
    val title: String? = null,
    val eventDate: Timestamp = Timestamp.now(),   // Timestamp in UI state ✅
    val location: String? = null,
    val eventStatus: EventStatus = EventStatus.SCHEDULED,
    val notes: String? = null,
    val adminId: String? = null,
    val createdAt: Timestamp? = null,             // Timestamp ✅
    val updatedAt: Timestamp? = null              // Timestamp ✅
)
