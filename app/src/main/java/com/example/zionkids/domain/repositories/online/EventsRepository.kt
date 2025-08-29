package com.example.zionkids.domain.repositories.online

import com.example.zionkids.core.Utils.GenerateId
import com.example.zionkids.core.di.EventsRef
import com.example.zionkids.data.mappers.toEvents
import com.example.zionkids.data.model.Event
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class EventSnapshot(
    val events: List<Event>,
    val fromCache: Boolean,
    val hasPendingWrites: Boolean
)

interface EventsRepository {
    suspend fun createOrUpdateEvent(event: Event, isNew: Boolean): String
    fun streamEvents(): Flow<EventSnapshot>               // list + metadata
    fun streamEventSnapshots(): Flow<List<Event>>         // just the list
    suspend fun getEventFast(id: String): Event?
    fun enqueueDelete(id: String)
}

@Singleton
class EventsRepositoryImpl @Inject constructor(
    @EventsRef private val eventsRef: CollectionReference
) : EventsRepository {

    override suspend fun getEventFast(id: String): Event? {
        val doc = eventsRef.document(id)
        // Try cache first
        try {
            val cache = doc.get(com.google.firebase.firestore.Source.CACHE).await()
            cache.toObject(Event::class.java)?.let { return it.copy(eventId = id) }
        } catch (_: Exception) { /* cache miss */ }
        // Fallback to server
        val server = doc.get(com.google.firebase.firestore.Source.SERVER).await()
        return server.toObject(Event::class.java)?.copy(eventId = id)
    }

    override suspend fun createOrUpdateEvent(event: Event, isNew: Boolean): String {
        val id = event.eventId.ifBlank { GenerateId.generateId("event") }
        val nowTs = Timestamp.now()

        // IMPORTANT: field names match Event model: eventDate / createdAt / updatedAt
        val patch = mutableMapOf<String, Any>(
            "eventId" to id,
            "adminId" to event.adminId,
            "title" to event.title,
            "eventDate" to event.eventDate,               // Timestamp ✅
            "eventStatus" to event.eventStatus.name,
            "location" to event.location,
            "notes" to event.notes,
            "updatedAt" to nowTs                           // Timestamp ✅
        )
        if (isNew) patch["createdAt"] = nowTs             // Timestamp ✅

        eventsRef.document(id).set(patch, SetOptions.merge()).await()
        return id
    }

    // Emits list + metadata
    override fun streamEvents() = callbackFlow<EventSnapshot> {
        val q = eventsRef
            .orderBy("eventDate", Query.Direction.DESCENDING)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toEvents() // mapper returns List<Event> and fills eventId
            val meta = snap.metadata
            trySend(
                EventSnapshot(
                    events = list,
                    fromCache = meta.isFromCache,
                    hasPendingWrites = meta.hasPendingWrites()
                )
            ).isSuccess
        }

        awaitClose { registration.remove() }
    }

    // Emits just the list
    override fun streamEventSnapshots() = callbackFlow<List<Event>> {
        val q = eventsRef
            .orderBy("eventDate", Query.Direction.DESCENDING)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration = q.addSnapshotListener { snap, err ->
            if (err != null) {
                cancel("Firestore listener error", err)
                return@addSnapshotListener
            }
            val list = snap!!.toEvents()
            trySend(list).isSuccess
        }

        awaitClose { registration.remove() }
    }

    override fun enqueueDelete(id: String) {
        // queued offline; syncs when online
        eventsRef.document(id).delete()
    }
}
