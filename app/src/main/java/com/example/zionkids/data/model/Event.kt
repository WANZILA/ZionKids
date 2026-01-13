package com.example.zionkids.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["title"]),
        Index(value = ["teamName"]),
        Index(value = ["eventDate"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isDirty"]),
        Index(value = ["isDeleted", "updatedAt"])
    ]
)
data class Event(
    @PrimaryKey val eventId: String = "",
    val eventParentId: String = "",
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
    val updatedAt: Timestamp = Timestamp.now(),
    //syn helpers
    val isDirty: Boolean = false,
    // Soft-deletes (tombstones) so Room remains source of truth and we can sync deletions.
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    // Cheap conflict resolution: prefer higher version (server increments), else newer updatedAt.
    val version: Long = 0L
)


enum class EventStatus { SCHEDULED, ACTIVE, DONE }
