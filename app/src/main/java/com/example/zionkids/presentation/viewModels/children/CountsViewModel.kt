package com.example.zionkids.presentation.viewModels.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

enum class CountMode { STREETS, REGIONS }

data class CountsUi(
    val loading: Boolean = true,
    val items: List<Pair<String, Int>> = emptyList(), // [name, count]
    val totalChildren: Int = 0,
    val uniqueKeys: Int = 0,
    val error: String? = null
)

@HiltViewModel
class CountsViewModel @Inject constructor(
    private val repo: ChildrenRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    val title: String get() = when (mode) {
        CountMode.STREETS -> "Streets"
        CountMode.REGIONS -> "Regions"
    }

    private val mode = runCatching {
        CountMode.valueOf(savedState.get<String>("mode") ?: "STREETS")
    }.getOrDefault(CountMode.STREETS)

    private val _ui = MutableStateFlow(CountsUi())
    val ui: StateFlow<CountsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.streamChildren()
                .debounce(100)
                .map { children ->
                    val items = when (mode) {
                        CountMode.STREETS -> children.countBy { it.street  }
                        CountMode.REGIONS -> children.countBy { it.region }
                    }
                    CountsUi(
                        loading = false,
                        items = items,
                        totalChildren = children.size,
                        uniqueKeys = items.size,
                        error = null
                    )
                }
                .flowOn(Dispatchers.Default)
                .onStart { _ui.value = CountsUi(loading = true) }
                .catch { e -> _ui.value = _ui.value.copy(loading = false, error = e.message) }
                .collect { next -> _ui.value = next }
        }
    }
}

// --- generic helper ---
private fun <T> List<T>.countBy(
    keyOf: (T) -> String?
): List<Pair<String, Int>> {
    fun String.normalize(): String {
        val t = this.trim().replace(Regex("\\s+"), " ")
        return t.lowercase(Locale.getDefault())
            .split(' ')
            .joinToString(" ") { w -> w.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
    }

    val names = this.mapNotNull { keyOf(it)?.normalize() }
    val counts = names.groupingBy { it }.eachCount()
    return counts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key to it.value }
}
