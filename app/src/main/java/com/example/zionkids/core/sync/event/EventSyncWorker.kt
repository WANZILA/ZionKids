// <app/src/main/java/com/example/zionkids/core/sync/event/EventSyncWorker.kt>
// /// CHANGED: Added Timber logs (start/end, counts, samples, phase timings) without changing logic.

package com.example.zionkids.core.sync.event

import com.example.zionkids.domain.sync.ChildrenSyncWorker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.core.di.EventsRef
import com.example.zionkids.data.local.dao.EventDao
import com.example.zionkids.data.mappers.toFirestoreMapPatch
//import com.example.zionkids.domain.sync.ChildrenSyncWorker.WorkerDeps
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.Query // /// CHANGED: not used
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.system.measureTimeMillis // /// CHANGED: for timing logs

// EventSyncWorker.kt
@HiltWorker
class EventSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @EventsRef private val eventRef: CollectionReference,
    private val eventDao: EventDao,
    private val firestore: FirebaseFirestore
//    @Assisted appContext: Context,
//    @Assisted params: WorkerParameters,
//    @EventsRef private val eventRef: CollectionReference,
//    private val eventDao: EventDao
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEvents {
        @EventsRef fun eventRef(): CollectionReference
        fun eventDao(): EventDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerEvents::class.java).eventRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerEvents::class.java).eventDao(),
                EntryPointAccessors.fromApplication(appContext, ChildrenSyncWorker.WorkerDeps::class.java).firestore()
    )


    override suspend fun doWork(): Result = try {
        Timber.i("EventSyncWorker: starting…")
        val dirty = eventDao.loadDirtyBatch(450)
        Timber.i("EventSyncWorker: loaded dirty=%d", dirty.size)

        if (dirty.isEmpty()) {
            Timber.i("EventSyncWorker: nothing to push, success")
            Result.success()
        } else {
            Timber.i(
                "EventSyncWorker: writing to collection='%s' (proj=%s, app=%s)",
                eventRef.path,
                eventRef.firestore.app.options.projectId,
                eventRef.firestore.app.name
            )

            val now = com.google.firebase.Timestamp.now()

            // 2) PRE-FETCH existence OUTSIDE the batch (legal)
            val existsMap = mutableMapOf<String, Boolean>()
            for (ev in dirty) {
                val snap = eventRef.document(ev.eventId).get(Source.SERVER).await()
                existsMap[ev.eventId] = snap.exists()
            }

//            val remoteSnap = eventRef.document(ev.eventId).get().await()
            eventRef.firestore.runBatch { b ->
                dirty.forEach { ev ->
                    val doc = eventRef.document(ev.eventId)
//                    val remoteSnap = eventRef.document(ev.eventId).get().await()
                    val patch = ev.toFirestoreMapPatch().toMutableMap().apply {
                        this["eventId"]   = ev.eventId
                        this["updatedAt"] = now
                        this["version"]   = ev.version
                        if (existsMap[ev.eventId] == false && ev.createdAt != null) {
                            this["createdAt"] = ev.createdAt // only on first publish
                        }
//                        if (!remoteSnap.exists() && ev.createdAt != null) this["createdAt"] = ev.createdAt
                        if (ev.isDeleted) this["isDeleted"] = true
                    }
                    if (ev.isDeleted) b.delete(doc) else b.set(doc, patch, SetOptions.merge())
                }
            }.await()
            val checkId = dirty.first().eventId
            val snap = eventRef.document(checkId).get(Source.SERVER).await()
            Timber.i("EventSyncWorker: server check id=%s exists=%s dataKeys=%s",
                checkId, snap.exists(), snap.data?.keys?.joinToString(","))

            Timber.i("EventSyncWorker: batch write OK (ops=%d)", dirty.size)

            val maxVersion = dirty.maxOf { it.version }
            eventDao.markBatchPushed(dirty.map { it.eventId }, newVersion = maxVersion, newUpdatedAt = now)
            Timber.i("EventSyncWorker: marked clean (ids=%s)", dirty.joinToString { it.eventId })

            Result.success()
        }
    } catch (t: Throwable) {
        Timber.e(t, "EventSyncWorker failed")
        Result.retry()
    }

}
