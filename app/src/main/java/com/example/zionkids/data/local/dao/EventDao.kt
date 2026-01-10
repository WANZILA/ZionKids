// <app/src/main/java/com/example/zionkids/data/local/dao/EventDao.kt>
// /// CHANGED: Add loadDirtyBatch(limit) used by EventSyncWorker (alias of existing dirty query).
// /// CHANGED: Add markBatchPushed(ids, newVersion, newUpdatedAt: Timestamp) to clear dirty & bump version/time.
// /// CHANGED: Update softDelete to accept Firestore Timestamp (avoid Long) to keep Timestamp-only flow.
// /// CHANGED: Keep all existing APIs; no renames, minimal diff.

package com.example.zionkids.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.zionkids.data.model.Event
import kotlinx.coroutines.flow.Flow
import com.google.firebase.Timestamp  // /// CHANGED: use Firestore Timestamp in DAO params

@Dao
interface EventDao {

    // --- Observability / Paging (unchanged) ---
    @Query("""
        SELECT * FROM events
        WHERE isDeleted = 0
        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
    """)
    fun pagingActive(): PagingSource<Int, Event>

    // /// CHANGED: Simple LIKE-based search across common fields; uses existing indices on title/teamName.
    @Query("""
        SELECT * FROM events
        WHERE isDeleted = 0 AND (
            title      LIKE :needle ESCAPE '\' COLLATE NOCASE OR
            teamName   LIKE :needle ESCAPE '\' COLLATE NOCASE OR
            location   LIKE :needle ESCAPE '\' COLLATE NOCASE
        )
        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
    """)
    fun pagingSearch(needle: String): PagingSource<Int, Event>

    @Query("""
        SELECT * FROM events
        --WHERE isDeleted = 0
        ORDER BY eventDate DESC  --, createdAt DESC, updatedAt DESC
    """)
    fun observeAllActive(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    fun observeById(id: String): Flow<Event?>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    suspend fun getOnce(id: String): Event?

    // --- Delta pulls (unchanged; Room will convert Timestamp fields via TypeConverters) ---
    @Query("""
        SELECT * FROM events
        WHERE (updatedAt > :afterUpdatedAtMillis) OR (version > :afterVersion)
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun getSince(afterUpdatedAtMillis: Long, afterVersion: Long, limit: Int): List<Event>

    // --- Dirty rows for push (existing; keep) ---
    @Query("""
        SELECT * FROM events
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun getDirty(limit: Int): List<Event>

    // /// CHANGED: Add alias used by EventSyncWorker; delegates to the same SQL.
//    @Query("""
//        SELECT * FROM events
//        WHERE isDirty = 1
//        ORDER BY updatedAt ASC
//        LIMIT :limit
//    """)
//    suspend fun loadDirtyBatch(limit: Int): List<Event>
    @Query("""SELECT * FROM events WHERE isDirty = 1 ORDER BY updatedAt ASC LIMIT :limit""")
    suspend fun loadDirtyBatch(limit: Int): List<Event>

    @Query("SELECT COUNT(*) FROM events WHERE isDirty = 1")
    fun observeDirtyCount(): Flow<Int>

    // --- Upserts (unchanged) ---
    @Upsert
    suspend fun upsertAll(items: List<Event>)
//    @Upsert
//    suspend fun upsertAll(children: List<Child>)


    @Upsert
    suspend fun upsertOne(item: Event)

    @Query("UPDATE events SET isDirty = :dirty WHERE eventId IN (:ids)")
    suspend fun setDirty(ids: List<String>, dirty: Boolean)

    // /// CHANGED: softDelete now accepts Firestore Timestamp to keep Timestamp-only flow.
    @Query("UPDATE events SET isDeleted = 1, isDirty = 1, updatedAt = :now WHERE eventId = :id")
    suspend fun softDelete(id: String, now: Timestamp)

    // Hard delete (unchanged)
    @Query("DELETE FROM events WHERE eventId = :id")
    suspend fun hardDelete(id: String)

    // /// CHANGED: markBatchPushed used by EventSyncWorker to finalize a successful push.
    // ///          Sets isDirty=false, bumps version, and writes updatedAt as Firestore Timestamp.
    @Query("""UPDATE events SET
              isDirty = 0,
              version = :newVersion,
              updatedAt = :newUpdatedAt
            WHERE eventId IN (:ids)""")
    suspend fun markBatchPushed(
        ids: List<String>,
        newVersion: Long,
        newUpdatedAt: Timestamp
    )
//    @Query("""
//        UPDATE events
//        SET isDirty = 0,
//            version  = :newVersion,
//            updatedAt = :newUpdatedAt
//        WHERE eventId IN (:ids)
//    """)
//    suspend fun markBatchPushed(
//        ids: List<String>,
//        newVersion: Long,
//        newUpdatedAt: Timestamp
//    )
}

//package com.example.zionkids.data.local.dao
//
//// <app/src/main/java/com/example/zionkids/data/local/dao/EventDao.kt>
//// /// NEW: Room DAO for events with Paging3, delta queries, bulk upserts, and dirty management.
//// + Added paging queries ordered by eventDate/updatedAt/createdAt
//// + Added delta fetch by (updatedAt/version)
//// + Added markDirty/softDelete helpers for offline-first writes
////package com.example.zionkids.data.local.dao
//
//import androidx.paging.PagingSource
//import androidx.room.Dao
//import androidx.room.Query
//import androidx.room.Upsert
//import com.example.zionkids.data.model.Event
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface EventDao {
//
//    // + Paging source for smooth lists (active only)
//    @Query("""
//        SELECT * FROM events
//        WHERE isDeleted = 0
//        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
//    """)
//    fun pagingActive(): PagingSource<Int, Event>
//
//    // + Stream all active events (for simple flows / snapshots)
//    @Query("""
//        SELECT * FROM events
//        WHERE isDeleted = 0
//        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
//    """)
//    fun observeAllActive(): Flow<List<Event>>
//
//    // + Observe one
//    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
//    fun observeById(id: String): Flow<Event?>
//
//    // + Get once (fast path)
//    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
//    suspend fun getOnce(id: String): Event?
//
//    // + Delta pulls by updatedAt/version (millis + scalar). Keep small page sizes at call site.
//    @Query("""
//        SELECT * FROM events
//        WHERE (updatedAt > :afterUpdatedAtMillis) OR (version > :afterVersion)
//        ORDER BY updatedAt ASC
//        LIMIT :limit
//    """)
//    suspend fun getSince(afterUpdatedAtMillis: Long, afterVersion: Long, limit: Int): List<Event>
//
//    // + Dirty rows for push (batch ≤ 500)
//    @Query("""
//        SELECT * FROM events
//        WHERE isDirty = 1
//        ORDER BY updatedAt ASC
//        LIMIT :limit
//    """)
//    suspend fun getDirty(limit: Int): List<Event>
//
//    // + Count dirty (for metadata)
//    @Query("SELECT COUNT(*) FROM events WHERE isDirty = 1")
//    fun observeDirtyCount(): Flow<Int>
//
//    // + Bulk upsert
//    @Upsert
//    suspend fun upsertAll(items: List<Event>)
//
//    // + Single upsert
//    @Upsert
//    suspend fun upsertOne(item: Event)
//
//    // + Mark dirty/clean
//    @Query("UPDATE events SET isDirty = :dirty WHERE eventId IN (:ids)")
//    suspend fun setDirty(ids: List<String>, dirty: Boolean)
//
//    // + Soft delete (isDeleted=1, isDirty=1, updatedAt = :nowMillis)
//    @Query("UPDATE events SET isDeleted = 1, isDirty = 1, updatedAt = :nowMillis WHERE eventId = :id")
//    suspend fun softDelete(id: String, nowMillis: Long)
//
//    // + Hard delete (use sparingly)
//    @Query("DELETE FROM events WHERE eventId = :id")
//    suspend fun hardDelete(id: String)
//}
