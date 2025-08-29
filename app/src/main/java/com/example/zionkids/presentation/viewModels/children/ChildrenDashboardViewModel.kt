package com.example.zionkids.presentation.viewModels.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.RegistrationStatus
import com.example.zionkids.data.model.Reply
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ChildrenSummaryUi(
    val loading: Boolean = true,
    val error: String? = null,
    val total: Int = 0,
    val newThisMonth: Int = 0,
    val graduated: Int = 0,
    val sponsored: Int = 0,
    val reunited: Int = 0,
    val inProgram: Int = 0,          // registration incomplete
    val avgAge: Double = 0.0,
    val eduDist: Map<EducationPreference, Int> = emptyMap(),
    val regionTop: List<Pair<String, Int>> = emptyList(),
    val staleUpdates: Int = 0        // updatedAt older than 30 days
)

@HiltViewModel
class ChildrenDashboardViewModel @Inject constructor(
    private val repo: ChildrenRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ChildrenSummaryUi())
    val ui: StateFlow<ChildrenSummaryUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.streamChildren()  // 👈 was getAll()
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") } }
                .collect { list -> _ui.update { compute(list) } }
        }
    }


    private fun compute(all: List<Child>): ChildrenSummaryUi {
        val now = System.currentTimeMillis()
        val (startMonth, endMonth) = monthBounds(now)
        val total = all.size
        val newThisMonth = all.count { it.createdAt in startMonth..endMonth }
        val graduated = all.count { it.graduated == Reply.YES }
        val sponsored = all.count { it.sponsoredForEducation }
        val reunited = all.count { it.reunitedWithFamily }
        val inProgram = all.count { it.registrationStatus != RegistrationStatus.COMPLETE }
        val staleLimit = now - 30L * 24 * 60 * 60 * 1000
        val staleUpdates = all.count { it.updatedAt < staleLimit }

        val ages = all.mapNotNull { it.age.takeIf { a -> a > 0 } }
        val avgAge = if (ages.isNotEmpty()) ages.average() else 0.0

        val eduDist = all.groupingBy { it.educationPreference }.eachCount()
        val regionTop = all
            .groupingBy { it.region ?: "Unknown" }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return ChildrenSummaryUi(
            loading = false,
            error = null,
            total = total,
            newThisMonth = newThisMonth,
            graduated = graduated,
            sponsored = sponsored,
            reunited = reunited,
            inProgram = inProgram,
            avgAge = "%.1f".format(Locale.getDefault(), avgAge).toDoubleOrNull() ?: 0.0,
            eduDist = eduDist,
            regionTop = regionTop,
            staleUpdates = staleUpdates
        )
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
        val end = c.timeInMillis
        return start to end
    }
}
