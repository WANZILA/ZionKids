package com.example.zionkids.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp



@Keep
 @Entity(tableName = "attendances",
     indices = [
         Index("eventId"),
         Index("childId"),
         Index("updatedAt"),
         Index(value = ["eventId", "status"]),
         Index(value = ["isDirty", "updatedAt"])
     ])
data class Attendance(
    @PrimaryKey val attendanceId: String = "",
    val childId: String = "",
    val eventId: String  = "",
    val adminId: String = "",
    val status: AttendanceStatus = AttendanceStatus.ABSENT,
    val notes: String = "",

    // sync & lifecycle

    val isDeleted: Boolean = false,     // tombstone for soft delete
    val isDirty: Boolean = true,        // Room-only usage; still stored for queries/observability
    val version: Long = 0,              // monotonic version (local increments; server-authoritative on push)
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val checkedAt: Timestamp = Timestamp.now()


)

enum class AttendanceStatus { ABSENT, PRESENT, EXCUSED }

//package com.example.zionkids.data.model
//
//import com.google.firebase.Timestamp
//
//data class Attendance(
//    val attendanceId: String = "",
//    val childId: String = "",
//    val eventId: String  = "",
//    val adminId: String = "",
//    val status: AttendanceStatus = AttendanceStatus.ABSENT,
//    val notes: String = "",
//
//    // Timestamps all through ✅
//    val checkedAt: Timestamp = Timestamp.now(),
//    val createdAt: Timestamp = Timestamp.now(),
//    val updatedAt: Timestamp = Timestamp.now(),
//
//    // snapshot fields (optional)
//)
//
//enum class AttendanceStatus { ABSENT, PRESENT, EXCUSED }
