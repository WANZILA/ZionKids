//// <app/src/main/java/com/example/zionkids/domain/sync/HydrateAttendanceOnceWorker.kt>
//
//package com.example.zionkids.domain.sync
//
//import android.content.Context
//import androidx.hilt.work.HiltWorker
//import androidx.work.CoroutineWorker
//import androidx.work.Data
//import androidx.work.WorkerParameters
//import com.example.zionkids.core.di.AttendanceRef // swap if your qualifier differs
//import com.example.zionkids.data.local.dao.AttendanceDao
//import com.example.zionkids.data.model.Attendance
//import com.google.firebase.firestore.CollectionReference
//import com.google.firebase.firestore.FirebaseFirestore
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//import dagger.hilt.EntryPoint
//import dagger.hilt.InstallIn
//import dagger.hilt.android.EntryPointAccessors
//import dagger.hilt.components.SingletonComponent
//import kotlinx.coroutines.tasks.await
//import timber.log.Timber
//
//@HiltWorker
//class HydrateAttendanceOnceWorker @AssistedInject constructor(
//    @Assisted appContext: Context,
//    @Assisted params: WorkerParameters,
//    @AttendanceRef private val attendanceRef: CollectionReference,
//    private val attendanceDao: AttendanceDao,
//    private val firestore: FirebaseFirestore // kept for symmetry / future use
//) : CoroutineWorker(appContext, params) {
//
//    companion object {
//        const val KEY_ATTENDANCE_ID = "attendance_id"
//
//        fun inputFor(attendanceId: String): Data = Data.Builder()
//            .putString(KEY_ATTENDANCE_ID, attendanceId)
//            .build()
//    }
//
//    @EntryPoint
//    @InstallIn(SingletonComponent::class)
//    interface Deps {
//        @AttendanceRef fun attendanceRef(): CollectionReference
//        fun attendanceDao(): AttendanceDao
//        fun firestore(): FirebaseFirestore
//    }
//
//    constructor(appContext: Context, params: WorkerParameters) : this(
//        appContext,
//        params,
//        EntryPointAccessors.fromApplication(appContext, Deps::class.java).attendanceRef(),
//        EntryPointAccessors.fromApplication(appContext, Deps::class.java).attendanceDao(),
//        EntryPointAccessors.fromApplication(appContext, Deps::class.java).firestore()
//    )
//
//    override suspend fun doWork(): Result {
//        val id = inputData.getString(KEY_ATTENDANCE_ID)
//        if (id.isNullOrBlank()) {
//            Timber.w("HydrateAttendanceOnceWorker: missing attendance id")
//            return Result.success() // no-op
//        }
//
//        return try {
//            // 1) Fetch remote once
//            val snap = attendanceRef.document(id).get().await()
//            val remote = snap.toObject(Attendance::class.java)?.copy(
//                attendanceId = id,   // normalize id from doc
//                isDirty = false      // remote truth is clean
//            )
//
//            if (remote == null) {
//                Timber.i("HydrateAttendanceOnceWorker: attendance %s not found remotely", id)
//                return Result.success()
//            }
//
//            // 2) Fetch local once (expect a dao method analogous to EventDao#getOnce)
//            val local = attendanceDao.getOnce(id)
//
//            // 3) Naive resolve: server wins (adjust if you add a proper resolver)
//            val merged = remote
//
//            // 4) Upsert locally
//            attendanceDao.upsert(merged)
//
//            Result.success()
//        } catch (t: Throwable) {
//            Timber.e(t, "HydrateAttendanceOnceWorker failed for id=%s", id)
//            Result.retry()
//        }
//    }
//}

package com.example.zionkids.core.sync.attendance

import com.example.zionkids.core.di.AttendanceRef // If your qualifier is @AttendancesRef, switch import + annotation.
import com.example.zionkids.data.local.dao.AttendanceDao
import com.example.zionkids.data.model.Attendance
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrateAttendanceOnce @Inject constructor(
    @AttendanceRef private val attendanceRef: CollectionReference,
    private val attendanceDao: AttendanceDao
) {
    suspend operator fun invoke(pageSize: Int = 500, maxPages: Int = 50) = withContext(Dispatchers.IO) {
        var page = 0
        var lastUpdated: Timestamp? = null
        do {
            val q = if (lastUpdated == null) {
                attendanceRef
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(pageSize.toLong())
            } else {
                attendanceRef
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .startAfter(lastUpdated!!)
                    .limit(pageSize.toLong())
            }

            val snap = q.get().await()
            val items = snap.documents.mapNotNull { it.toObject(Attendance::class.java) }
            if (items.isEmpty()) break

            // Server truth on pull → mark clean
            attendanceDao.upsertAll(items.map { it.copy(isDirty = false) })

            lastUpdated = items.last().updatedAt
            page++
        } while (items.size >= pageSize && page < maxPages)
    }
}
