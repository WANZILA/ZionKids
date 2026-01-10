package com.example.zionkids.core.sync.attendance


import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// /// CHANGED: Ensure we import the correct worker package.

object AttendanceSyncScheduler {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "attendance_sync_queue",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("attendance_sync_now")
                .build()
        )
    }

    fun enqueuePeriodic(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "attendance_sync_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }
}
