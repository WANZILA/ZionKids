package com.example.zionkids.data.local.seed

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.core.sync.attendance.AttendanceSyncScheduler
import com.example.zionkids.data.local.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AttendencesSeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // 1) Seed only if attendances table is empty
            AttendanceSeedLoader.seedIfAttendancesEmpty(applicationContext, db)

            // 2) Immediately enqueue your normal Firestore pull/push pipeline
            // (this will apply any changes since the seed)
            AttendanceSyncScheduler.enqueuePullNow(applicationContext)
            AttendanceSyncScheduler.enqueuePushNow(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
