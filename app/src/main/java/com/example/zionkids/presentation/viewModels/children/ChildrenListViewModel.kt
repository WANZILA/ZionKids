package com.example.zionkids.presentation.viewModels.children

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.example.zionkids.domain.repositories.online.ChildrenSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildrenListUiState(
    val loading: Boolean = true,
    val children: List<Child> = emptyList(),
    val isOffline: Boolean = false,   // served from cache
    val isSyncing: Boolean = false,   // local writes pending
    val error: String? = null
)
private const val TAG = "ChildrenListViewModel"
@HiltViewModel
class ChildrenListViewModel @Inject constructor(
    private val repo: ChildrenRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ChildrenListUiState())
    val ui: StateFlow<ChildrenListUiState> = _ui.asStateFlow()

    private val _query = MutableStateFlow("")
    fun onSearchQueryChange(q: String) { _query.value = q }

    // holds the latest full snapshot from Firestore
    private var latestSnap: ChildrenSnapshot =
        ChildrenSnapshot(emptyList(), fromCache = true, hasPendingWrites = false)

    init {
        observeStream()   // Firestore -> latestSnap
        observeQuery()    // Search    -> filter latestSnap
    }

    private fun observeStream(limit: Long = 300) {
        viewModelScope.launch {
            repo.streamAllNotGraduated()
                .onStart { _ui.value = ChildrenListUiState(loading = true) }
                .catch { e ->
                    android.util.Log.e(TAG, "stream error", e)
                    _ui.value = _ui.value.copy(loading = false, error = e.message)
                }
                .collect { snap ->
                    latestSnap = snap
                    pushFiltered()  // re-apply current query
                }
        }
    }

    private fun observeQuery() {
        viewModelScope.launch {
            _query
                //.debounce(200) // optional
                .map { it.trim().lowercase() }
                .distinctUntilChanged()
                .collect {
                    pushFiltered()
                }
        }
    }

    private fun pushFiltered() {
        val q = _query.value.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            latestSnap.children
        } else {
            latestSnap.children.filter { c ->
                val full = "${c.fName} ${c.lName}".trim().lowercase()
                full.contains(q)
            }
        }
        _ui.value = ChildrenListUiState(
            loading = false,
            children = filtered,
            isOffline = latestSnap.fromCache,
            isSyncing = latestSnap.hasPendingWrites,
            error = null
        )
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true)
                repo.getAllNotGraduated() // warms cache; stream will emit next
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message)
            } finally {
                _ui.value = _ui.value.copy(loading = false)
            }
        }
    }

    companion object { private const val TAG = "ChildrenListViewModel" }
}
