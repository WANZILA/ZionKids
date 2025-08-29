package com.example.zionkids.presentation.viewModels.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _events = kotlinx.coroutines.channels.Channel<Event>(kotlinx.coroutines.channels.Channel.BUFFERED)
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
        val deleted: Boolean = false
    )

    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    fun load(childId: String) = viewModelScope.launch {
        _ui.value = Ui(loading = true)
        runCatching { repo.getChildFast(childId) }
            .onSuccess { c -> _ui.value = _ui.value.copy(loading = false, child = c, error = if (c == null) "Child not found" else null) }
            .onFailure { e -> _ui.value = _ui.value.copy(loading = false, error = e.message ?: "Failed to load") }
    }

//    fun deleteChild() = viewModelScope.launch {
//        val id = _ui.value.child?.childId ?: return@launch
//        _ui.value = _ui.value.copy(deleting = true, error = null)
//        runCatching { repo.deleteChild(id) }    // queued offline, syncs later
//            .onSuccess { _ui.value = _ui.value.copy(deleting = false, deleted = true) }
//            .onFailure { e -> _ui.value = _ui.value.copy(deleting = false, error = e.message ?: "Delete failed") }
//    }
fun deleteChildOptimistic() {
    val id = _ui.value.child?.childId ?: return
    // 1) Emit navigation event immediately
    viewModelScope.launch { _events.send(Event.Deleted) }

    // 2) Queue the delete (no await); it will sync when online
    try {
        repo.enqueueDelete(id)
    } catch (e: Exception) {
        viewModelScope.launch { _events.send(Event.Error("Delete queue failed: ${e.message}")) }
    }
}
    fun refresh(childId: String) {
        // KISS: just reuse your existing loader
        load(childId)
    }
}
