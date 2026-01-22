package com.example.zionkids.data.local.projection

import com.example.zionkids.data.model.AttendanceStatus
import com.google.firebase.Timestamp

data class EventAttendanceRow(
    val eventId: String,
    val title: String,
    val eventDate: Timestamp,
    val teamName: String,
    val location: String,
    val status: AttendanceStatus,
    val notes: String
)
