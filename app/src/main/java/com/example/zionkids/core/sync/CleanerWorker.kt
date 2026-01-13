// app/src/main/java/com/example/zionkids/core/sync/CleanerWorker.kt
// /// CHANGED: Fix fallback constructor (missing comma + wrong arg order).
// /// CHANGED: Use Timestamp(cutoffSeconds, cutoffNanos) to avoid Date ctor mismatch.
// /// CHANGED: Add logs + keep behavior the same.

package com.example.zionkids.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.dao.EventDao
import com.google.firebase.Timestamp
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val childDao: ChildDao,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao
) : CoroutineWorker(appContext, params) {

    companion object {
        // retention window for tombstones
        const val KEY_RETENTION_DAYS = "retentionDays"
        private const val DEFAULT_RETENTION_DAYS = 0L

        // /// CHANGED: output keys so UI can read how many rows were deleted
        const val OUT_DELETED_CHILDREN = "deleted_children"
        const val OUT_DELETED_ATTENDANCES = "deleted_attendances"
        const val OUT_DELETED_EVENTS = "deleted_events"
        const val OUT_DELETED_TOTAL = "deleted_total"
    }


    // ---- Fallback path when HiltWorkerFactory isn’t used ----
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun childDao(): ChildDao
        fun attendanceDao(): AttendanceDao
        fun eventDao(): EventDao
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).eventDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).attendanceDao()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val retentionDays =
                inputData.getLong(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS).coerceAtLeast(0L)

            val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays)

            // Firestore Timestamp uses seconds+nanos (not java.util.Date in your imports here)
            val cutoffSeconds = cutoffMs / 1000L
            val cutoffNanos = ((cutoffMs % 1000L) * 1_000_000L).toInt()
            val cutoff = Timestamp(cutoffSeconds, cutoffNanos)

            Timber.i("CleanerWorker: start retentionDays=%d cutoff=%s", retentionDays, cutoff)

            // Clean tables that have tombstones (isDeleted/isDirty/deletedAt)
            val deletedChildren = childDao.hardDeleteOldTombstones(cutoff)
            val deletedAttendances = attendanceDao.hardDeleteOldTombstones(cutoff)
            val deletedEvents = eventDao.hardDeleteOldTombstones(cutoff)

            val deletedTotal = deletedChildren + deletedAttendances + deletedEvents

            Timber.i(
                "CleanerWorker: deleted children=%d attendances=%d events=%d total=%d",
                deletedChildren, deletedAttendances, deletedEvents, deletedTotal
            )

            val out = androidx.work.Data.Builder()
                .putInt(OUT_DELETED_CHILDREN, deletedChildren)
                .putInt(OUT_DELETED_ATTENDANCES, deletedAttendances)
                .putInt(OUT_DELETED_EVENTS, deletedEvents)
                .putInt(OUT_DELETED_TOTAL, deletedTotal)
                .build()

            Timber.i("CleanerWorker: done")
            Result.success(out)

        } catch (t: Throwable) {
            Timber.e(t, "CleanerWorker failed")
            Result.retry()
        }
    }
}
