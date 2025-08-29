package com.example.zionkids.domain.repositories.online

import com.example.zionkids.core.di.ChildrenRef
import com.example.zionkids.data.mappers.toChildOrNull
import com.example.zionkids.data.mappers.toChildren
import com.example.zionkids.data.mappers.toFirestoreMapFull
import com.example.zionkids.data.mappers.toFirestoreMapPatch
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.model.Reply
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.get

data class ChildrenSnapshot(
    val children: List<Child>,
    val fromCache: Boolean,        // true = served from local cache (offline or warming up)
    val hasPendingWrites: Boolean  // true = local changes not yet synced
)

interface ChildrenRepository {
//    suspend fun getChild(id: String): Child?
//    suspend fun upsert(child: Child)            // create or update
// Change the signature to return the id we saved (handy when new)
//suspend fun upsert(child: Child, isNew: Boolean): String
    suspend fun getChildFast(id: String): Child?
    fun streamChildren(): Flow<List<Child>>
    suspend fun upsert(child: Child, isNew: Boolean): String
    suspend fun getAll(): List<Child>
    suspend fun getAllNotGraduated(): List<Child>

//    limit: Long = 200
    fun streamAllNotGraduated(): Flow<ChildrenSnapshot>
//    fun streamNotGraduatedByNamePrefix(query: String, limit: Long = 200): Flow<ChildrenSnapshot> // ← add
suspend fun deleteChild(id: String)          // keep blocking variant if you still need it
    fun enqueueDelete(id: String)                // 👈 add this to the INTERFACE
}

@Singleton
class ChildrenRepositoryImpl @Inject constructor(
    @ChildrenRef private val childrenRef: CollectionReference
) : ChildrenRepository {

    override suspend fun getAll(): List<Child> =
        childrenRef.get().await().toChildren()

    override fun streamChildren() = callbackFlow<List<Child>> {
        val q = childrenRef
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)

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


    //    override suspend fun getChild(id: String): Child? =
//        childrenRef.document(id).get().await().toChildOrNull()
   override suspend fun getChildFast(id: String): Child? {
    val doc = childrenRef.document(id)
    // 1) Try CACHE immediately (instant when available)
    try {
        val cache = doc.get(com.google.firebase.firestore.Source.CACHE).await()
        cache.toObject(Child::class.java)?.let { return it }
    } catch (_: Exception) {
        // cache miss/disabled — fall through to server
    }
    // 2) Fallback to SERVER (may take longer but fresh)
    val server = doc.get(com.google.firebase.firestore.Source.SERVER).await()
    return server.toObject(Child::class.java)
}


    override suspend fun upsert(child: Child, isNew: Boolean): String {
        // Use provided id (must be non-blank!). Caller is responsible for generating id for new.
        val id = child.childId
        require(id.isNotBlank()) { "childId required (generate one before saving)" }

        val now = System.currentTimeMillis()
        val docRef = childrenRef.document(id)

        // Build a merge/patch map (use your toFirestoreMapPatch mapper if you added it)
        val patch = child.toFirestoreMapPatch().toMutableMap()

        // Fields required for your offline query/sort
        patch["updatedAt"] = now
        if (!patch.containsKey("graduated")) patch["graduated"] = Reply.NO.name  // "NO"

        // On create only (no read required!)
        if (isNew) {
            patch["createdAt"] = now
            patch["childId"] = id
        }

        // build normalized search field from current values
        fun buildNameSearch(f: String, l: String): String =
            (f.trim() + " " + l.trim()).lowercase()

        val fName = child.fName
        val lName = child.lName
        patch["nameSearch"] = buildNameSearch(fName, lName)

        // This enqueues while offline and syncs later
        docRef.set(patch, SetOptions.merge()).await()

        return id
    }


//    override suspend fun getAll(): List<Child> =
//        childrenRef.get().await().documents.mapNotNull { it.toObject(Child::class.java) }

    // Only children with graduated == "No", ordered by last update desc (then created desc)
    override suspend fun getAllNotGraduated(): List<Child> =
        childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toChildren()



    // ChildrenRepositoryImpl
    override fun streamAllNotGraduated() = callbackFlow {
        val q = childrenRef
            .whereEqualTo("graduated", Reply.NO.name)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
//            .limit(limit)

        // register the listener
        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                // Cancel the flow with a cause; this triggers awaitClose{} immediately.
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }

            // Normal emission
            val list = snap!!.toChildren() // your mapper
            val meta = snap.metadata
            val fromCache = meta.isFromCache()
            val hasLocalWrites = meta.hasPendingWrites()

            trySend(ChildrenSnapshot(list, fromCache, hasLocalWrites)).isSuccess
        }

        // IMPORTANT: this must be the *last* statement in the block
        awaitClose { registration.remove() }
    }

    override suspend fun deleteChild(id: String) {
        childrenRef.document(id).delete().await()
    }
    override fun enqueueDelete(id: String) {
        // fire-and-forget; works offline (queued locally)
        childrenRef.document(id).delete()
    }

}
