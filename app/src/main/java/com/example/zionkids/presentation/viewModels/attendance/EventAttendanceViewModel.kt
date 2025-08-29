//package com.example.zionkids.presentation.viewModels.attendance
//
//// package com.example.zionkids.presentation.viewModels.events
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.zionkids.data.model.Attendance
//import com.example.zionkids.data.model.AttendanceStatus
//import com.example.zionkids.domain.repositories.online.AttendanceRepository
////import com.example.zionkids.domain.repositories.online.AttendanceRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//data class EventAttendanceUi(
//    val loading: Boolean = true,
//    val error: String? = null,
//    val list: List<Attendance> = emptyList(),
//    val present: Int = 0,
//    val absent: Int = 0,
//    val excused: Int = 0
//)
//
//@HiltViewModel
//class EventAttendanceViewModel @Inject constructor(
//    private val repo: AttendanceRepository
//) : ViewModel() {
//
//    private val _ui = MutableStateFlow(EventAttendanceUi())
//    val ui: StateFlow<EventAttendanceUi> = _ui.asStateFlow()
//
//    fun start(eventId: String) {
//        viewModelScope.launch {
//            repo.streamByEvent(eventId)
//                .onStart { _ui.update { it.copy(loading = true, error = null) } }
//                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load") } }
//                .collect { snap ->
//                    val list = snap.attendance
//                    _ui.update {
//                        it.copy(
//                            loading = false,
//                            list = list,
//                            present = list.count { a -> a.status == AttendanceStatus.PRESENT },
//                            absent  = list.count { a -> a.status == AttendanceStatus.ABSENT },
//                            excused = list.count { a -> a.status == AttendanceStatus.EXCUSED }
//                        )
//                    }
//                }
//        }
//    }
//
//    fun mark(eventId: String, childId: String, adminId: String, status: AttendanceStatus, notes: String = "") {
//        viewModelScope.launch {
//            repo.mark(
//                Attendance(
//                    attendanceId = "", // will be derived as eventId_childId
//                    eventId = eventId,
//                    childId = childId,
//                    adminId = adminId,
//                    status = status,
//                    notes = notes
//                )
//            )
//        }
//    }
//}
