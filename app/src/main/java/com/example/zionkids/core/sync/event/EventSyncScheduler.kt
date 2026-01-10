// <app/src/main/java/com/example/zionkids/core/sync/event/EventSyncScheduler.kt>
// /// CHANGED: Added Timber logging for all enqueues (success + errors).
// /// CHANGED: Import EventSyncWorker and EventCascadeDeleteWorker explicitly.
// /// CHANGED: Guard against blank IDs in cascade delete with a warning log.

package com.example.zionkids.core.sync.event

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
//import com.example.zionkids.core.sync.EventCascadeDeleteWorker   // /// CHANGED
import timber.log.Timber                                        // /// CHANGED
import java.util.concurrent.TimeUnit

// /// CHANGED: Ensure we import the correct worker package (your worker lives in core.sync.event).
import com.example.zionkids.core.sync.event.EventSyncWorker     // /// CHANGED

// EventSyncScheduler.kt
object EventSyncScheduler {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // EventSyncScheduler.kt
    fun enqueueNow(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("event_sync_now")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // run ASAP if quota available
            .build()

        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "event_sync_queue",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EventSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("event_sync_now")
                .build()
        )

//        WorkManager.getInstance(ctx).enqueueUniqueWork(
//            "event_sync_queue",
//            ExistingWorkPolicy.REPLACE, // ← don’t get stuck behind old chains
//            req
//        )
    }

//    fun enqueueNow(ctx: Context) {
//        val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
//            .setConstraints(constraints)
//            .addTag("event_sync_now")
//            .build()
//        WorkManager.getInstance(ctx).enqueueUniqueWork(
//            "event_sync_queue",
//            ExistingWorkPolicy.APPEND,
//            req
//        )
//    }

    fun enqueuePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<EventSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "event_sync_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}

//object EventSyncScheduler {
//    private const val UNIQUE_PERIODIC = "event_sync_periodic"
//
//    private val constraints = Constraints.Builder()
//        .setRequiredNetworkType(NetworkType.CONNECTED)
//        .setRequiresBatteryNotLow(true)
//        .build()
//
//    fun enqueuePeriodic(context: Context) {
//        val req = PeriodicWorkRequestBuilder<EventSyncWorker>(30, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .build()
//        runCatching {
//            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                UNIQUE_PERIODIC,
//                ExistingPeriodicWorkPolicy.UPDATE, // Keeping your choice; consider KEEP to avoid cadence reset.
//                req
//            )
//        }.onSuccess {
//            Timber.i("EventSyncScheduler: periodic EventSyncWorker scheduled (30m, policy=UPDATE)")
//        }.onFailure { e ->
//            Timber.e(e, "EventSyncScheduler: failed to schedule periodic EventSyncWorker")
//        }
//    }
//
//    fun enqueueNow(context: Context) {
//        val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
//            .setConstraints(constraints)
//            .build()
//        runCatching {
//            WorkManager.getInstance(context).enqueueUniqueWork(
//                "event_sync_now",
//                ExistingWorkPolicy.APPEND, // Queue behind any existing chain; switch to REPLACE for “run-now”.
//                req
//            )
//        }.onSuccess {
//            Timber.i("EventSyncScheduler: one-off EventSyncWorker enqueued (policy=APPEND)")
//        }.onFailure { e ->
//            Timber.e(e, "EventSyncScheduler: failed to enqueue one-off EventSyncWorker")
//        }
//    }
//
//    /** Fire-and-forget cascade delete for a single childId (eventId); runs when constraints met. */
//    fun enqueueCascadeDelete(context: Context, childId: String) {
//        if (childId.isBlank()) {
//            Timber.w("EventSyncScheduler: enqueueCascadeDelete called with blank id; ignoring")
//            return
//        }
//
//        val input = Data.Builder()
//            .putString(EventCascadeDeleteWorker.KEY_CHILD_ID, childId)
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<EventCascadeDeleteWorker>()
//            .setConstraints(constraints)
//            .setInputData(input)
//            .build()
//
//        runCatching {
//            WorkManager.getInstance(context).enqueueUniqueWork(
//                "event_cascade_delete_$childId",
//                ExistingWorkPolicy.REPLACE, // Latest delete wins for this id
//                req
//            )
//        }.onSuccess {
//            Timber.i("EventSyncScheduler: cascade delete enqueued for eventId=%s", childId)
//        }.onFailure { e ->
//            Timber.e(e, "EventSyncScheduler: failed to enqueue cascade delete for eventId=%s", childId)
//        }
//    }
//}

//// <app/src/main/java/com/example/zionkids/core/sync/event/EventSyncScheduler.kt>
//// /// CHANGED: Keep your simplified scheduler; comment-only minimal diffs.
//// /// CHANGED: Use EventCascadeDeleteWorker consistently (import + references).
//// /// CHANGED: Notes on Existing*WorkPolicy choices (comments only; behavior unchanged).
//
//package com.example.zionkids.core.sync.event
//
//// /// CHANGED: Use the Event cascade delete worker (matches references below).
////import com.example.zionkids.core.sync.EventCascadeDeleteWorker
//
//// <app/src/main/java/com/example/zionkids/core/sync/ChildrenSyncScheduler.kt>
//// /// CHANGED: new scheduler to enqueue periodic + one-off syncs; WorkManager-friendly, minimal surface
//
//import android.content.Context
//import androidx.work.Constraints
//import androidx.work.Data
//import androidx.work.ExistingPeriodicWorkPolicy
//import androidx.work.ExistingWorkPolicy
//import androidx.work.NetworkType
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.PeriodicWorkRequestBuilder
//import androidx.work.WorkManager
////import com.example.zionkids.domain.sync.EventSyncWorker
//import java.util.concurrent.TimeUnit
//
//object EventSyncScheduler {
//    private const val UNIQUE_PERIODIC = "event_sync_periodic"
//
//    private val constraints = Constraints.Builder()
//        .setRequiredNetworkType(NetworkType.CONNECTED)
//        .setRequiresBatteryNotLow(true)
//        .build()
//
//    fun enqueuePeriodic(context: Context) {
//        val req = PeriodicWorkRequestBuilder<EventSyncWorker>(30, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            UNIQUE_PERIODIC,
//            ExistingPeriodicWorkPolicy.UPDATE, // /// CHANGED: Keeping your choice. Tip: many apps prefer KEEP to avoid resetting cadence.
//            req
//        )
//    }
//
//    fun enqueueNow(context: Context) {
//        val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "event_sync_now",
//            ExistingWorkPolicy.APPEND, // /// CHANGED: Keeping your choice to queue behind any existing chain; use REPLACE if you want “run-now and cancel previous”.
//            req
//        )
//    }
//
//    /** Fire-and-forget cascade delete for a single childId (works offline; runs when constraints met). */
//    fun enqueueCascadeDelete(context: Context, childId: String) {
//        val input = Data.Builder()
//            .putString(EventCascadeDeleteWorker.KEY_CHILD_ID, childId) // /// CHANGED: Event* worker key; param name unchanged to avoid breaking callers.
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<EventCascadeDeleteWorker>()
//            .setConstraints(constraints) // NetworkType.CONNECTED, etc.
//            .setInputData(input)
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "event_cascade_delete_$childId",
//            ExistingWorkPolicy.REPLACE, // /// CHANGED: Keep REPLACE so the latest delete wins for this id.
//            req
//        )
//    }
//
//
//}
