package com.example.zionkids.core.sync

// <app/src/main/java/com/example/zionkids/core/sync/ChildrenSyncScheduler.kt>
// /// CHANGED: new scheduler to enqueue periodic + one-off syncs; WorkManager-friendly, minimal surface
//package com.example.zionkids.core.sync

import com.example.zionkids.domain.sync.ChildrenSyncWorker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
//import com.example.zionkids.domain.sync.ChildrenSyncWorker
//import com.example.zionkids.domain.sync.ChildrenSyncWorker
import java.util.concurrent.TimeUnit

object ChildrenSyncScheduler {
    private const val UNIQUE_PERIODIC = "children_sync_periodic"
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun enqueuePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<ChildrenSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun enqueueNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<ChildrenSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "children_sync_now",
//            ExistingWorkPolicy.REPLACE,
            ExistingWorkPolicy.APPEND,
            req
        )
    }

    /** Fire-and-forget cascade delete for a single childId (works offline; runs when constraints met). */
    fun enqueueCascadeDelete(context: Context, childId: String) {
        val input = Data.Builder()
            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
            .build()

        val req = OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
            .setConstraints(constraints) // NetworkType.CONNECTED, etc.
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "children_cascade_delete_$childId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
//    fun enqueueCascadeDelete(context: Context, childId: String) {
//        require(childId.isNotBlank()) { "childId is blank" }
//
//        val input = Data.Builder()
//            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
//            .setConstraints(constraints)
//            .setInputData(input)
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "children_cascade_delete_$childId",
//            ExistingWorkPolicy.APPEND,
//            req
//        )
//    }
}
