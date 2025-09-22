package com.example.zionkids.presentation.viewModels.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.online.ChildrenRepository
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
class ChildDetailsViewModel @Inject constructor(
    private val repo: ChildrenRepository
) : ViewModel() {

    // one-shot events (navigation/snacks)
    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    sealed interface Event {
        data object Deleted : Event
        data class Error(val msg: String) : Event
    }

    data class Ui(
        val loading: Boolean = true,
        val child: Child? = null,
        val error: String? = null,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val lastRefreshed: Timestamp? = null // purely informational
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    /** Loads child: tries cache first, then server (repo handles that). */
    fun load(childId: String) = viewModelScope.launch {
        _ui.value = Ui(loading = true)
        runCatching { repo.getChildFast(childId) }
            .onSuccess { c ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    child = c,
                    error = if (c == null) "Child not found" else null,
                    lastRefreshed = Timestamp.now()
                )
            }
            .onFailure { e ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load"
                )
            }
    }

    /** Fire-and-forget delete; emits navigation immediately, queues deletion (works offline). */
//    fun deleteChildOptimistic() {
//        val id = _ui.value.child?.childId ?: return
//        viewModelScope.launch { _events.send(Event.Deleted) }
//        try {
//            repo.enqueueCascadeDelete(id)
//        } catch (e: Exception) {
//            viewModelScope.launch {
//                _events.send(Event.Error("Delete queue failed: ${e.message}"))
//            }
//        }
//    }
//    fun deleteChildOptimistic() = viewModelScope.launch {
//        _ui.value = _ui.value.copy(deleting = true, error = null)
//        val id = _ui.value.child?.childId ?: return@launch
////        ui = ui.copy(deleting = true, error = null)
//        runCatching {
//            repo.deleteChildAndAttendances(id) // suspend version
//        }.onSuccess {
//            _ui.value = _ui.value.copy(deleting = false)
////            ui = ui.copy(deleting = false)
//            _events.trySend(ChildDetailsViewModel.Event.Deleted)
//        }.onFailure { e ->
//            _ui.value = _ui.value.copy(deleting = false, error = e.message ?: "Failed to delete")
////            ui = ui.copy(deleting = false, error = e.message ?: "Failed to delete")
//            _events.trySend(ChildDetailsViewModel.Event.Error("Failed to delete"))
//        }
//    }
    fun deleteChildOptimistic() = viewModelScope.launch {
        val id = _ui.value.child?.childId ?: return@launch
        _ui.value = _ui.value.copy(deleting = true, error = null)

        try {
            repo.deleteChildAndAttendances(id)   // suspends, awaits completion
            _events.trySend(ChildDetailsViewModel.Event.Deleted)  // include the id
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message ?: "Failed to delete")
            _events.trySend(ChildDetailsViewModel.Event.Error("Failed to delete: ${e.message ?: ""}".trim()))
        } finally {
            _ui.value = _ui.value.copy(deleting = false)
        }
    }

    /** Simple refresh (same behavior as load). */
    fun refresh(childId: String) {
        load(childId)
    }
}
