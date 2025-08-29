package com.example.zionkids.domain.repositories.online

import com.example.zionkids.core.Utils.isOfflineError
import com.example.zionkids.core.di.AttendanceRef
import com.example.zionkids.data.model.Attendance
import com.example.zionkids.data.model.AttendanceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AttendanceSnapshot(
    val attendance: List<Attendance>,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean
)

interface AttendanceRepository {
    suspend fun upsertAttendance(att: Attendance): String
    fun enqueueUpsertAttendance(att: Attendance)      // optimistic, no await
    fun streamAttendanceForEvent(eventId: String): Flow<AttendanceSnapshot>
    fun streamAttendanceForChild(childId: String): Flow<AttendanceSnapshot> // history

    /** One-shot fetch of all attendance rows for an event (cache → server). */
    suspend fun getAttendanceOnce(eventId: String): List<Attendance>

    /** Convenience: fetch a single child’s status for an event (or null if none). */
    suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus?
}

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    @AttendanceRef private val attendanceRef: CollectionReference
) : AttendanceRepository {

    private fun idFor(eventId: String, childId: String) = "${eventId}_${childId}"

    /** Writes using Timestamps throughout. */
    override suspend fun upsertAttendance(att: Attendance): String {
        val id = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val nowTs = Timestamp.now()

        val patch = mapOf(
            "attendanceId" to id,
            "childId"      to att.childId,
            "eventId"      to att.eventId,
            "adminId"      to att.adminId,
            "status"       to att.status.name,
            "notes"        to att.notes,
            "checkedAt"    to att.checkedAt,          // Timestamp ✅
            "updatedAt"    to nowTs,                  // Timestamp ✅
            "createdAt"    to att.createdAt           // Timestamp ✅ (preserve caller's value)
        )

        attendanceRef.document(id).set(patch, SetOptions.merge()).await()
        return id
    }

    /** Fire-and-forget; queued offline by Firestore SDK. */
    override fun enqueueUpsertAttendance(att: Attendance) {
        val id = att.attendanceId.ifBlank { idFor(att.eventId, att.childId) }
        val nowTs = Timestamp.now()
        attendanceRef.document(id).set(
            att.copy(
                attendanceId = id,
                updatedAt = nowTs,
                createdAt = att.createdAt
            ),
            SetOptions.merge()
        )
    }

    override fun streamAttendanceForEvent(eventId: String) = callbackFlow {
        // NOTE: whereEqualTo + orderBy may require a composite index; Firestore will log a link.
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .orderBy("updatedAt", Query.Direction.DESCENDING) // Timestamp field ✅

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) { cancel("attendance stream error", err); return@addSnapshotListener }
            val list = snap!!.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
            val meta = snap.metadata
            trySend(
                AttendanceSnapshot(
                    attendance = list,
                    fromCache = meta.isFromCache,          // Kotlin property (or use meta.isFromCache())
                    hasPendingWrites = meta.hasPendingWrites() // method
                )
            ).isSuccess
        }
        awaitClose { reg.remove() }
    }

    override fun streamAttendanceForChild(childId: String) = callbackFlow {
        val q = attendanceRef
            .whereEqualTo("childId", childId)
            .orderBy("updatedAt", Query.Direction.DESCENDING) // Timestamp field ✅

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) { cancel("attendance stream error", err); return@addSnapshotListener }
            val list = snap!!.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
            val meta = snap.metadata
            trySend(
                AttendanceSnapshot(
                    attendance = list,
                    fromCache = meta.isFromCache,
                    hasPendingWrites = meta.hasPendingWrites()
                )
            ).isSuccess
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getAttendanceOnce(eventId: String): List<Attendance> {
        val q = attendanceRef.whereEqualTo("eventId", eventId)

        // 1) Try CACHE first (works offline if previously synced)
        val cacheList = try {
            val cache = q.get(Source.CACHE).await()
            cache.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
        } catch (_: Exception) { emptyList() }

        // 2) Try SERVER; on offline fall back to cache
        return try {
            val server = q.get(Source.SERVER).await()
            server.documents.mapNotNull { d ->
                d.toObject(Attendance::class.java)?.copy(attendanceId = d.id)
            }
        } catch (e: Exception) {
            if (e.isOfflineError()) cacheList else throw e
        }
    }

    override suspend fun getStatusForChildOnce(childId: String, eventId: String): AttendanceStatus? {
        val q = attendanceRef
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("childId", childId)

        // CACHE first
        val cacheStatus = try {
            val cache = q.get(Source.CACHE).await()
            cache.documents.firstOrNull()?.toObject(Attendance::class.java)?.status
        } catch (_: Exception) { null }

        // SERVER if possible; otherwise keep cache value
        return try {
            val server = q.get(Source.SERVER).await()
            server.documents.firstOrNull()?.toObject(Attendance::class.java)?.status ?: cacheStatus
        } catch (_: Exception) {
            cacheStatus
        }
    }
}
