package com.example.zionkids.data.model

import com.google.firebase.Timestamp

data class Attendance(
    val attendanceId: String = "",
    val childId: String = "",
    val eventId: String  = "",
    val adminId: String = "",
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val notes: String = "",

    // Timestamps all through ✅
    val checkedAt: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),

    // snapshot fields (optional)
)

enum class AttendanceStatus { ABSENT, PRESENT, EXCUSED }
