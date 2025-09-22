package com.example.zionkids.presentation.viewModels.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.RegistrationStatus
import com.example.zionkids.data.model.Reply
import com.example.zionkids.domain.repositories.online.ChildrenRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
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
    val streetTop: List<Pair<String, Int>> = emptyList(),
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
            repo.streamChildren()
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") } }
                .collect { list -> _ui.update { compute(list) } }
        }
    }

    // ---- Timestamp helpers ----
    private fun Timestamp?.millisOrZero(): Long = this?.toDate()?.time ?: 0L

    private fun compute(all: List<Child>): ChildrenSummaryUi {
        val nowMillis = System.currentTimeMillis()
        val (startMonth, endMonth) = monthBounds(nowMillis)

        val total = all.size
        val newThisMonth = all.count { c ->
            val created = c.createdAt.millisOrZero()
            created in startMonth..endMonth
        }
        val graduated = all.count { it.graduated == Reply.YES }
        val sponsored = all.count { it.sponsoredForEducation }
        val reunited = all.count { it.resettled }
        val inProgram = all.count { it.registrationStatus != RegistrationStatus.COMPLETE }

        val staleLimit = nowMillis - THIRTY_DAYS
        val staleUpdates = all.count { c -> c.updatedAt.millisOrZero() < staleLimit }

        val ages = all.mapNotNull { a -> a.age.takeIf { it > 0 } }
        val avgAge = if (ages.isNotEmpty()) ages.average() else 0.0

        val eduDist = all.groupingBy { it.educationPreference }.eachCount()
//        val regionTop = all
//            .groupingBy { it.region ?: "Unknown" }
//            .eachCount()
//            .entries
//            .sortedByDescending { it.value }
//            .take(3)
//            .map { it.key to it.value }
//        val streetTop = all
//            .groupingBy { it.street ?: "Unknown" }
//            .eachCount()
//            .entries
//            .sortedByDescending { it.value }
//            .take(3)
//            .map { it.key to it.value }
        // Regions: prefer child.region; skip blanks; normalize casing/whitespace
        val regionsNorm = all.mapNotNull { child ->
            child.region.normalizeOrNull()
        }
        val regionTop = topN(regionsNorm, 3)

// Streets: prefer child.street, then child.address?.street; skip blanks; normalize
        val streetsNorm = all.mapNotNull { child ->
            (child.street ?: child?.street).normalizeOrNull()
        }
        val streetTop = topN(streetsNorm, 3)

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
            streetTop = streetTop,
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

    // --- helpers (put inside the ViewModel) ---
    private fun String?.normalizeOrNull(): String? {
        if (this == null) return null
        val t = this.trim().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return null
        // Title-case (Makindye, Kisenyi, etc.)
        return t.lowercase(Locale.getDefault())
            .split(' ')
            .joinToString(" ") { w -> w.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) } }
    }

    private fun topN(names: List<String>, n: Int = 3): List<Pair<String, Int>> =
        names.groupingBy { it }
            .eachCount()
            .toList()
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second } // count desc
                    .thenBy { it.first }                             // name asc (tie-break)
            )
            .take(n)


    companion object {
        private const val THIRTY_DAYS: Long = 30L * 24 * 60 * 60 * 1000
    }
}
