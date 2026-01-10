// <app/src/main/java/com/example/zionkids/data/local/dao/AttendanceDao.kt>
package com.example.zionkids.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.zionkids.data.model.Attendance
import com.example.zionkids.data.model.AttendanceStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // ---------- Upserts ----------
    @Upsert
    suspend fun upsert(a: Attendance)

    @Upsert
    suspend fun upsertAll(list: List<Attendance>)

    // ---------- Single reads ----------
    @Query("SELECT * FROM attendances WHERE attendanceId = :id LIMIT 1")
    suspend fun getOnce(id: String): Attendance?

    // DAO
    @Query("SELECT * FROM attendances WHERE eventId = :eventId AND childId = :childId LIMIT 1")
    suspend fun getOneForChildAtEvent(eventId: String, childId: String): Attendance?


    // ---------- Lists (one-shot) ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId
        ORDER BY updatedAt DESC
    """)
    suspend fun getByEvent(eventId: String): List<Attendance>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId AND status = :status
        ORDER BY updatedAt DESC
    """)
    suspend fun getByEventAndStatus(eventId: String, status: AttendanceStatus): List<Attendance>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND childId = :childId
        ORDER BY updatedAt DESC
    """)
    suspend fun getForChild(childId: String): List<Attendance>

    // ---------- Streams (for UI) ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId
        ORDER BY updatedAt DESC
    """)
    fun observeByEvent(eventId: String): Flow<List<Attendance>>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND childId = :childId
        ORDER BY updatedAt DESC
    """)
    fun observeByChild(childId: String): Flow<List<Attendance>>

    // ---------- Sync helpers ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun loadDirtyBatch(limit: Int): List<Attendance>

    @Query("""
        UPDATE attendances
        SET isDirty = 0,
            version  = :newVersion,
            updatedAt = :newUpdatedAt
        WHERE attendanceId IN (:ids)
    """)
    suspend fun markBatchPushed(ids: List<String>, newVersion: Long, newUpdatedAt: Timestamp)

    @Query("""
        UPDATE attendances
        SET isDirty = 1,
            version  = version + 1,
            updatedAt = :now
        WHERE attendanceId = :id
    """)
    suspend fun markDirty(id: String, now: Timestamp)

    @Query("""
        UPDATE attendances
        SET isDeleted = 1,
            isDirty = 1,
            version  = version + 1,
            updatedAt = :now
        WHERE attendanceId = :id
    """)
    suspend fun softDelete(id: String, now: Timestamp)

    // ---------- Handy counters / diagnostics ----------
    @Query("SELECT COUNT(*) FROM attendances WHERE isDirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendances WHERE isDeleted = 0 AND eventId = :eventId")
    fun observeCountForEvent(eventId: String): Flow<Int>


    /** Total non-graduated children registered on/before cutoffMs. */
    @Query("""
        SELECT COUNT(*) 
        FROM children c
        WHERE COALESCE(c.graduated, 'NO') = 'NO'
          AND (c.createdAt IS NULL OR c.createdAt <= :cutoffMs)
    """)
    suspend fun countTotalEligibleByCutoff(cutoffMs: Long): Int

    /** selects the count of the children based on the eventDate **/
    /** Present count for an event among eligible (non-grad, registered on/before cutoffMs). */
    @Query("""
        SELECT COUNT(DISTINCT a.childId)
        FROM attendances a
        JOIN children c ON c.childId = a.childId
        WHERE a.eventId = :eventId
          AND a.status = :presentStatus
          AND COALESCE(c.graduated, 'NO') = 'NO'
          AND (c.createdAt IS NULL OR c.createdAt <= :cutoffMs)
    """)
    suspend fun countPresentEligibleForEvent(
        eventId: String,
        presentStatus: String,   // e.g., "PRESENT" if you store enum name
        cutoffMs: Long
    ): Int

    // --- Optional quick diagnostics (handy while verifying) ---
    @Query("SELECT COUNT(*) FROM children")
    suspend fun countAllChildren(): Int

    @Query("SELECT COUNT(*) FROM children WHERE COALESCE(graduated,0)=0")
    suspend fun countAllNonGraduated(): Int
}
