package com.example.zionkids.presentation.viewModels.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Event
import com.example.zionkids.domain.repositories.online.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val repo: EventsRepository
) : ViewModel() {

    private val _events = Channel<EventDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface EventDetailsEvent {
        data object Deleted : EventDetailsEvent
        data class Error(val msg: String) : EventDetailsEvent
    }

    /**
     * Holds the loaded [Event]. The Event model itself uses Firebase **Timestamp**
     * for `eventDate`, `createdAt`, and `updatedAt` — no millis here.
     */
    data class Ui(
        val loading: Boolean = true,
        val event: Event? = null,
        val error: String? = null,
        val deleting: Boolean = false,
        val deleted: Boolean = false
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    fun load(eventId: String) = viewModelScope.launch {
        _ui.update { Ui(loading = true) }
        runCatching { repo.getEventFast(eventId) }
            .onSuccess { e ->
                _ui.update {
                    it.copy(
                        loading = false,
                        event = e,
                        error = if (e == null) "Event not found" else null,
                        deleted = false,
                        deleting = false
                    )
                }
            }
            .onFailure { ex ->
                _ui.update { it.copy(loading = false, error = ex.message ?: "Failed to load") }
            }
    }

    /**
     * Optimistic delete: emits a Deleted event immediately and enqueues Firestore delete.
     * Firestore handles offline persistence & sync; timestamps remain on the Event model.
     */
    fun deleteEventOptimistic() {
        val id = _ui.value.event?.eventId ?: return
        // Emit navigation/deletion signal right away
        viewModelScope.launch { _events.send(EventDetailsEvent.Deleted) }
        // Queue delete (offline-friendly); surface any immediate exception as a UI event
        runCatching { repo.enqueueDelete(id) }
            .onFailure { e ->
                viewModelScope.launch {
                    _events.send(EventDetailsEvent.Error("Delete failed: ${e.message}"))
                }
            }
    }

    fun refresh(eventId: String) = load(eventId)
}
