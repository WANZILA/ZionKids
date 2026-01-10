package com.example.zionkids.core.sync.attendance

import com.example.zionkids.data.mappers.toFirestoreMapPatch

import kotlinx.coroutines.tasks.await

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.zionkids.core.di.AttendanceRef

import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.mappers.toFirestoreMapPatch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
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

@HiltWorker
class AttendanceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @AttendanceRef private val attRef: CollectionReference,
    private val attDao: AttendanceDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerAttendance {
        @AttendanceRef fun attRef(): CollectionReference
        fun attDao(): AttendanceDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerAttendance::class.java).attRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerAttendance::class.java).attDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerAttendance::class.java).firestore()
    )

    override suspend fun doWork(): Result = try {
        Timber.i("AttendanceSyncWorker: starting…")
        val dirty = attDao.loadDirtyBatch(450)
        Timber.i("AttendanceSyncWorker: loaded dirty=%d", dirty.size)

        if (dirty.isEmpty()) {
            Timber.i("AttendanceSyncWorker: nothing to push, success")
            Result.success()
        } else {
            Timber.i(
                "AttendanceSyncWorker: writing to collection='%s' (proj=%s, app=%s)",
                attRef.path,
                attRef.firestore.app.options.projectId,
                attRef.firestore.app.name
            )

            val now = Timestamp.now()

            // 1) PRE-FETCH existence (outside batch)
            val existsMap = mutableMapOf<String, Boolean>()
            for (a in dirty) {
                val snap = attRef.document(a.attendanceId).get(Source.SERVER).await()
                existsMap[a.attendanceId] = snap.exists()
            }

            // 2) BATCH write
            attRef.firestore.runBatch { b ->
                dirty.forEach { a ->
                    val doc = attRef.document(a.attendanceId)
                    val patch = a.toFirestoreMapPatch().toMutableMap().apply {
                        this["attendanceId"] = a.attendanceId
                        this["updatedAt"] = now
                        this["version"] = a.version
                        if (existsMap[a.attendanceId] == false) {
                            this["createdAt"] = a.createdAt
                        }
                    }
                    if (a.isDeleted) b.delete(doc) else b.set(doc, patch, SetOptions.merge())
                }
            }.await()

            // 3) Server check (mirror Event)
            val checkId = dirty.first().attendanceId
            val snap = attRef.document(checkId).get(Source.SERVER).await()
            Timber.i(
                "AttendanceSyncWorker: server check id=%s exists=%s dataKeys=%s",
                checkId, snap.exists(), snap.data?.keys?.joinToString(",")
            )

            // 4) Mark clean
            val maxVersion = dirty.maxOf { it.version }
            attDao.markBatchPushed(dirty.map { it.attendanceId }, maxVersion, now)
            Timber.i("AttendanceSyncWorker: marked clean (ids=%s)", dirty.joinToString { it.attendanceId })

            Result.success()
        }
    } catch (t: Throwable) {
        Timber.e(t, "AttendanceSyncWorker failed")
        Result.retry()
    }
}
