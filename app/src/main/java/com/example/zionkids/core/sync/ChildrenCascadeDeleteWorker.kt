// app/src/main/java/com/example/zionkids/core/sync/ChildrenCascadeDeleteWorker.kt
package com.example.zionkids.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.core.di.ChildrenRef
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
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

@HiltWorker
class ChildrenCascadeDeleteWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @ChildrenRef private val childrenRef: CollectionReference,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object { const val KEY_CHILD_ID = "childId" }

    // ---- Fallback path when HiltWorkerFactory isn’t used (mirror of ChildrenSyncWorker) ----
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        @ChildrenRef fun childrenRef(): CollectionReference
        fun firestore(): FirebaseFirestore
    }

    // Default WM factory will look for (Context, WorkerParameters). Delegate via EntryPoint.
    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childrenRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    // app/src/main/java/com/example/zionkids/core/sync/ChildrenCascadeDeleteWorker.kt

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val childId = inputData.getString(KEY_CHILD_ID).orEmpty()
        Timber.d("CascadeWorker: start childId=%s", childId)
        if (childId.isBlank()) return@withContext Result.failure()

        try {
            // Log the exact path we’re using
            Timber.d("CascadeWorker: childrenRef.path = %s", childrenRef.path)

            // 0) Confirm EXISTS on SERVER before delete (helps diagnose wrong collection / id mismatch)
            val before = childrenRef.document(childId).get(com.google.firebase.firestore.Source.SERVER).await()
            Timber.d("CascadeWorker: existsBefore=%s (docPath=%s/%s)", before.exists(), childrenRef.path, childId)

            // 1) Delete the child doc (this succeeds even if doc missing; that’s OK)
            childrenRef.document(childId).delete().await()

            // 2) Re-check on SERVER to verify it’s gone
            val after = childrenRef.document(childId).get(com.google.firebase.firestore.Source.SERVER).await()
            Timber.d("CascadeWorker: existsAfter=%s (should be false)", after.exists())

            // If it still exists, likely rules blocked a real delete but didn’t throw (rare),
            // or we deleted the wrong path. Force a failure to see logs and retry.
            if (after.exists()) {
                Timber.e("CascadeWorker: delete appears to have failed (existsAfter=true)")
                return@withContext Result.retry()
            }

            Timber.d("CascadeWorker: success for %s", childId)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CascadeWorker failed for %s", childId)
            Result.retry()
        }
    }

}
