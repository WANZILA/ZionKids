package com.example.zionkids.domain.repositories.online

import com.example.zionkids.core.di.AttendanceRef
import com.example.zionkids.core.di.ChildrenRef
import com.example.zionkids.data.mappers.toChildren
import com.example.zionkids.data.mappers.toFirestoreMapPatch
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.Reply
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ChildrenSnapshot(
    val children: List<Child>,
    val fromCache: Boolean,        // true = served from local cache (offline or warming up)
    val hasPendingWrites: Boolean  // true = local changes not yet synced
)

interface ChildrenRepository {
    suspend fun getChildFast(id: String): Child?
    fun streamChildren(): Flow<List<Child>>
    suspend fun upsert(child: Child, isNew: Boolean): String
    suspend fun getAll(): List<Child>
    suspend fun getAllNotGraduated(): List<Child>
    fun streamAllNotGraduated(): Flow<ChildrenSnapshot>
//    suspend fun deleteChild(id: String)
// (optional) fire-and-forget offline version
   suspend fun deleteChildAndAttendances(childId: String)

    fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot>

    fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot>

    suspend fun getByEducationPreference(pref: EducationPreference): List<Child>

}

@Singleton
class ChildrenRepositoryImpl @Inject constructor(
    @ChildrenRef private val childrenRef: CollectionReference,
    @AttendanceRef private val attendanceRef: CollectionReference
) : ChildrenRepository {

    private fun Query.toChildrenSnapshotFlow(): Flow<ChildrenSnapshot> = callbackFlow {
        val reg = this@toChildrenSnapshotFlow.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val docs = snap ?: return@addSnapshotListener
            val children = docs.documents.mapNotNull { it.toObject(Child::class.java) }
            trySend(
                ChildrenSnapshot(
                    children = children,
                    fromCache = docs.metadata.isFromCache,
                    hasPendingWrites = docs.metadata.hasPendingWrites()
                )
            )
        }
        awaitClose { reg.remove() }
    }
    /* ------------ Reads ------------ */

    override suspend fun getAll(): List<Child> =
        childrenRef.get().await().toChildren()

    override fun streamChildren(): Flow<List<Child>> = callbackFlow {
        val q = childrenRef
            .orderBy("fName", ) // Timestamp
            .orderBy("lName", ) // Timestamp

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toChildren()
            trySend(list).isSuccess
        }

        awaitClose { registration.remove() }
    }

    override suspend fun getChildFast(id: String): Child? {
        val doc = childrenRef.document(id)
        // 1) Try CACHE first (instant if available)
        try {
            val cache = doc.get(Source.CACHE).await()
            cache.toObject(Child::class.java)?.let { return it }
        } catch (_: Exception) {
            // cache miss — fall back to server
        }
        // 2) SERVER for fresh data
        val server = doc.get(Source.SERVER).await()
        return server.toObject(Child::class.java)
    }

    /* ------------ Create / Update ------------ */

    override suspend fun upsert(child: Child, isNew: Boolean): String {
        val id = child.childId
        require(id.isNotBlank()) { "childId required (generate one before saving)" }

        val docRef = childrenRef.document(id)

        // Start from your PATCH map (already Timestamp-based) and make sure audit/search fields are present.
        val patch = child.toFirestoreMapPatch().toMutableMap()

        // Always update updatedAt with a Timestamp
        patch["updatedAt"] = Timestamp.now()

        // Ensure graduated field is present for queries (string enum in Firestore)
        if (!patch.containsKey("graduated")) patch["graduated"] = Reply.NO.name

        // On create only: set createdAt and childId
        if (isNew) {
//            patch["createdAt"] = child.createdAt.takeIf { it != null } ?: Timestamp.now()
//            patch["createdAt"] = Timestamp.now()
            patch["childId"] = id
        }

        // Normalized name search (handy for prefix search)
        fun buildNameSearch(f: String, l: String): String =
            (f.trim() + " " + l.trim()).lowercase()
        patch["nameSearch"] = buildNameSearch(child.fName, child.lName)

        // Fire-and-sync (works offline; merges on server)
        docRef.set(patch, SetOptions.merge()).await()

        return id
    }

    /* ------------ Convenience queries ------------ */

    override suspend fun getAllNotGraduated(): List<Child> =
        childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("fName")
            .orderBy( "lName")
//            .orderBy("updatedAt", Query.Direction.DESCENDING) // Timestamp
//            .orderBy("createdAt", Query.Direction.DESCENDING) // Timestamp
            .get()
            .await()
            .toChildren()

    override fun streamAllNotGraduated(): Flow<ChildrenSnapshot> = callbackFlow {
        val q = childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("fName")
            .orderBy( "lName")
//            .orderBy("updatedAt", Query.Direction.DESCENDING)
//            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toChildren()
            val meta = snap.metadata
            val fromCache = meta.isFromCache
            val hasLocalWrites = meta.hasPendingWrites()
            trySend(ChildrenSnapshot(list, fromCache, hasLocalWrites)).isSuccess
        }

        awaitClose { registration.remove() }
    }

    /* ------------ Deletes ------------ */



    override suspend fun deleteChildAndAttendances(childId: String) {
        val db = childrenRef.firestore

        // delete the child immediately
        childrenRef.document(childId).delete()

        // listener registration placeholder
        var registration: ListenerRegistration? = null

        registration = db.collectionGroup("attendance")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                if (snap.isEmpty) {
                    // no more attendances ⇒ stop listening
                    registration?.remove()
                    return@addSnapshotListener
                }

                val chunks = snap.documents.chunked(450)
                chunks.forEach { chunk ->
                    db.runBatch { b -> chunk.forEach { b.delete(it.reference) } }
                }
            }
    }

    override fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot> =
        childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .whereEqualTo("educationPreference", pref.name) // enums saved as strings
            .orderBy("fName")
            .orderBy( "lName")
            .toChildrenSnapshotFlow()

    override fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot> =
        combine(
            streamByEducationPreference(pref),
            streamAllNotGraduated()
        ) { filtered, all ->
            if (filtered.fromCache && filtered.children.isEmpty() && all.children.isNotEmpty()) {
                filtered.copy(
                    children = all.children.filter { it.educationPreference == pref },
                    fromCache = true // stay honest about cache origin
                )
            } else filtered
        }

    override suspend fun getByEducationPreference(pref: EducationPreference): List<Child> {
        return try {
            val snapshot = childrenRef
                .whereEqualTo("graduated", false)
                .whereEqualTo("educationPreference", pref.name) // store enums as strings
                .orderBy("fName") // optional, but consistent with your streams
                .get()
                .await()  // from kotlinx-coroutines-play-services

            snapshot.documents.mapNotNull { it.toObject(Child::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }





}

