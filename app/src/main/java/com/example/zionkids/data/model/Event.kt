package com.example.zionkids.data.model

import com.google.firebase.Timestamp

data class Event(
    val eventId: String = "",
    val title: String = "",
    val eventDate: Timestamp = Timestamp.now(),
    val teamName: String = "",
    val teamLeaderNames: String = "",
    val leaderTelephone1: String = "",
    val leaderTelephone2: String = "",
    val leaderEmail: String = "",
    val location: String = "",
    val eventStatus: EventStatus = EventStatus.SCHEDULED,
    val notes: String = "",
    val adminId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)


enum class EventStatus { SCHEDULED, ACTIVE, DONE }
