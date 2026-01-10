package com.example.zionkids.domain.sync



// <app/src/main/java/com/example/zionkids/domain/sync/ChildrenSyncWorker.kt>
// /// CHANGED: add tiny conflict resolver (prefer higher version, else newer updatedAt; keep local if equal and dirty);
// /// CHANGED: use resolver to merge pulled remote docs with local before upsertAll


import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.core.di.ChildrenRef
import com.example.zionkids.core.sync.resolveChild
//import com.example.zionkids.core.sync.SyncPrefs
//import com.example.zionkids.core.sync.resolveChild
import com.example.zionkids.data.model.Child
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.mappers.toFirestoreMapPatch
//import com.example.zionkids.domain.repositories.offline.OfflineChildrenRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max

// app/src/main/java/com/example/zionkids/domain/sync/ChildrenSyncWorker.kt
@HiltWorker
class ChildrenSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @ChildrenRef private val childrenRef: CollectionReference,
    private val childDao: ChildDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    // ---- Fallback path for when HiltWorkerFactory isn't used ----
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        @ChildrenRef fun childrenRef(): CollectionReference
        fun childDao(): ChildDao
        fun firestore(): FirebaseFirestore
    }

    // This secondary ctor is what the default WM factory looks for.
    // It delegates to the primary assisted-inject ctor with deps pulled from Hilt.
    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childrenRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result {
        // ... your existing logic unchanged ...
        return try {
            // 1) collect dirty locals
            val dirty = childDao.loadDirtyBatch(limit = 500)
            if (dirty.isEmpty()) return Result.success()

            val now = com.google.firebase.Timestamp.now()

            // 2) pre-resolve against current remote docs (KISS: sequential; fine for ≤500)
            val toWrite = mutableListOf<Child>()
            for (local in dirty) {
                val remoteSnap = childrenRef.document(local.childId).get().await()
                val remote = remoteSnap.toObject(Child::class.java) // may be null
                val resolved = resolveChild(local, remote)
                // force audit fields for the write (server-side doc)
                toWrite += resolved.copy(updatedAt = now)
            }

            // 3) write resolved docs in a single batch (maps to avoid POJO edge cases)
            childrenRef.firestore.runBatch { b ->
                toWrite.forEach { child ->
                    val doc = childrenRef.document(child.childId)
                    val patch = child.toFirestoreMapPatch().toMutableMap().apply {
                        this["childId"]   = child.childId
                        this["updatedAt"] = now
                        this["version"]   = child.version
                    }
                    if (child.isDeleted) b.delete(doc) else b.set(doc, patch, com.google.firebase.firestore.SetOptions.merge())
                }
            }.await()

            // 4) mark local rows clean with the same timestamp; bump version to the max we just wrote
            val maxVersion = toWrite.maxOf { it.version }
            childDao.markBatchPushed(
                ids = dirty.map { it.childId },
                newVersion   = maxVersion,
                newUpdatedAt = now
            )

            Result.success()
        } catch (t: Throwable) {
            timber.log.Timber.e(t, "ChildrenSyncWorker failed")
            Result.retry()
        }

//            // 1) collect dirty locals
//            val dirty = childDao.loadDirtyBatch(limit = 500)
//            if (dirty.isEmpty()) return Result.success()
//
//
//
//            val now = Timestamp.now()
//            firestore.runBatch { b ->
//                dirty.forEach { c ->
//                    val doc = childrenRef.document(c.childId)
//                    if (c.isDeleted) b.delete(doc)
//                    else {
//                        val patch = c.toFirestoreMapPatch().toMutableMap().apply {
//                            this["childId"] = c.childId
//                            this["updatedAt"] = now
//                            this["version"]  = c.version
//                        }
//                        b.set(doc, patch, SetOptions.merge())
//                    }
//                }
//            }.await()
//
//            childDao.markBatchPushed(
//                ids = dirty.map { it.childId },
//                newVersion   = dirty.maxOf { it.version },
//                newUpdatedAt = now
//            )
//            Result.success()
//        } catch (t: Throwable) {
//            Timber.e(t, "ChildrenSyncWorker failed")
//            Result.retry()
//        }
    }
}
