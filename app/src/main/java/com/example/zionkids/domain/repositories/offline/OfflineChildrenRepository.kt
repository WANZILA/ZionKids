// <app/src/main/java/com/example/zionkids/domain/repositories/offline/OfflineChildrenRepository.kt>
// /// CHANGED: use the existing ChildrenSnapshot type from the online package to avoid duplicate classes; keep everything else the same.

package com.example.zionkids.domain.repositories.offline

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
//import com.example.zionkids.core.sync.CascadeDelete
import com.example.zionkids.core.sync.ChildrenCascadeDeleteWorker
import com.example.zionkids.core.sync.ChildrenSyncScheduler
//import com.example.zionkids.core.sync.ChildrenCascadeDeleteWorker
import com.example.zionkids.data.model.Child
//import com.example.zionkids.data.model.ChildDao
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.data.model.Reply
import com.google.firebase.Timestamp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.dao.KpiDao
import com.example.zionkids.domain.repositories.online.ChildrenSnapshot   // /// CHANGED: reuse existing snapshot model
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar

// /// CHANGED: remove local duplicate to prevent type conflicts with imports elsewhere
// data class ChildrenSnapshot(
//     val children: List<Child>,
//     val fromCache: Boolean,
//     val hasPendingWrites: Boolean
// )

interface OfflineChildrenRepository {
    suspend fun getChildFast(id: String): Child?
    fun streamChildren(): Flow<List<Child>>
    suspend fun upsert(child: Child, isNew: Boolean): String
    suspend fun getAll(): List<Child>
    suspend fun getAllNotGraduated(): List<Child>
    fun streamAllNotGraduated(): Flow<ChildrenSnapshot>
    //    suspend fun deleteChild(id: String)
    // (optional) fire-and-forget offline version
    suspend fun deleteChildAndAttendances(childId: String)

    fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot>

    fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot>

    suspend fun getByEducationPreference(pref: EducationPreference): List<Child>

    /// CHANGED: new micro-edit API
    suspend fun markDirty(id: String)

    // /// CHANGED: new API — hard delete locally + remote cascade via Worker
    suspend fun hardDeleteCascade(childId: String)

    //uses paging 3 and count the query on search
    fun pagedNotGraduated(needle: String, pageSize: Int = 50): Flow<PagingData<Child>>
    fun countNotGraduated(needle: String): Flow<Int>
}
//suspend fun KpiDao.inc(key: String, delta: Long) = bump(key, delta)
@Singleton
class OfflineChildrenRepositoryImpl @Inject constructor(
    private val childDao: ChildDao,
    private val kpiDao: KpiDao,
    @ApplicationContext private val appContext: Context
) : OfflineChildrenRepository {

    override suspend fun getChildFast(id: String): Child? =
        childDao.getById(id)

    override fun streamChildren(): Flow<List<Child>> =
        childDao.observeAllActive()

    override suspend fun upsert(child: Child, isNew: Boolean): String {
        val id = child.childId
        require(id.isNotBlank()) { "childId required (generate one before saving)" }

        val prev = childDao.getById(id)

        val now = Timestamp.now()
        val nextVersion = (child.version + 1).coerceAtLeast(1)

        // ✅ define next FIRST
        val next = child.copy(
            isDirty = true,
            isDeleted = false,
            updatedAt = now,
            version = nextVersion
        )

        // ✅ then use it for KPIs
        updateKpis(prev = prev, next = next, isDelete = false)

        // ✅ and persist the exact state we counted
        childDao.upsert(next)

        return id
    }

//    override suspend fun upsert(child: Child, isNew: Boolean): String {
//        val id = child.childId
//        require(id.isNotBlank()) { "childId required (generate one before saving)" }
//
//        val prev = childDao.getById(id)
//
//        val now = Timestamp.now()
//        val nextVersion = (child.version + 1).coerceAtLeast(1)
//
//        // /// CHANGED: Update KPIs before persisting (so counters reflect the write)
//        updateKpis(prev, next, isDelete = false)
////        updateKpis(next, prev, isDelete = true)
//
//        childDao.upsert(
//            child.copy(
//                isDirty = true,
//                isDeleted = false,
//                updatedAt = now,
//                version = nextVersion
//            )
//        )
//        return id
//    }

    override suspend fun getAll(): List<Child> =
        childDao.getAllActive()

    override suspend fun getAllNotGraduated(): List<Child> =
        childDao.getAllByGraduated(Reply.NO)

    override fun streamAllNotGraduated(): Flow<ChildrenSnapshot> =
        combine(
            childDao.observeAllByGraduated(Reply.NO),
            childDao.observeDirtyCount()
        ) { list, dirtyCount ->
            ChildrenSnapshot(
                children = list,
                fromCache = true,               // Room is the local cache / source of truth
                hasPendingWrites = dirtyCount > 0
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteChildAndAttendances(childId: String) {
        val prev = childDao.getById(childId) // /// CHANGED: fetch for KPI deltas
        // Offline-only: mark tombstone; any server-side cascade handled by future sync worker.
        childDao.softDelete(childId, Timestamp.now())
        prev?.let { updateKpis(prev = it, next = it.copy(isDeleted = true), isDelete = true) }
    }

    override fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot> =
        combine(
            childDao.observeByEducationPreference(pref),
            childDao.observeDirtyCount()
        ) { list, dirtyCount ->
            ChildrenSnapshot(
                children = list.filter { it.graduated == Reply.NO },
                fromCache = true,
                hasPendingWrites = dirtyCount > 0
            )
        }.flowOn(Dispatchers.IO)

    override fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot> =
        combine(
            streamByEducationPreference(pref),
            streamAllNotGraduated()
        ) { filtered, all ->
            if (filtered.children.isEmpty() && all.children.isNotEmpty()) {
                filtered.copy(
                    children = all.children.filter { it.educationPreference == pref },
                    fromCache = true
                )
            } else filtered
        }

    override suspend fun getByEducationPreference(pref: EducationPreference): List<Child> =
        childDao.getAllActive().filter { it.educationPreference == pref && it.graduated == Reply.NO }


    /// CHANGED: micro-edit — mark row dirty without fetching the whole entity
    override suspend fun markDirty(id: String) {
        childDao.markDirty(id, Timestamp.now())
    }

    // OfflineChildrenRepositoryImpl.kt
//    override suspend fun hardDeleteCascade(childId: String) {
//        childDao.hardDelete(childId)
//        CascadeDelete.child(appContext, childId)
//    }
    override suspend fun hardDeleteCascade(childId: String) {
        require(childId.isNotBlank()) { "childId is blank" }
        // /// CHANGED: fetch for KPI deltas before hard delete
        val prev = childDao.getById(childId)
        // /// CHANGED: reflect deletion in KPIs
        prev?.let { updateKpis(prev = it, next = it.copy(isDeleted = true), isDelete = true) }

        // OfflineChildrenRepositoryImpl.hardDeleteCascade(...)
        // OfflineChildrenRepositoryImpl.hardDeleteCascade(...)
        childDao.hardDelete(childId)

        val input = androidx.work.Data.Builder()
            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
            .build()

        val req = androidx.work.OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
            .setInputData(input)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            // optional, helps run quicker in foreground-like priority
            //.setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("cascade_delete")
            .build()

        androidx.work.WorkManager.getInstance(appContext).enqueueUniqueWork(
            "children_cascade_delete_$childId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            req
        )

//        val input = Data.Builder()
//            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
//            .setInputData(input)
//            .setConstraints(
//                Constraints.Builder()
//                    .setRequiredNetworkType(NetworkType.CONNECTED)
//                    .build()
//            )
//            .build()
//
//        WorkManager.getInstance(appContext).enqueueUniqueWork(
//            "children_cascade_delete_$childId",
//            ExistingWorkPolicy.REPLACE,
//            req
//        )

//        childDao.hardDelete(childId)
//        ChildrenSyncScheduler.enqueueCascadeDelete(appContext, childId)
    }

    // ---------- KPI helpers (no full-table scans) ----------
    // We count ACTIVE (not deleted) rows, to mirror your UI logic.
    private suspend fun updateKpis(prev: Child?, next: Child, isDelete: Boolean) {
        val wasActive = prev?.isDeleted == false
        val isActive = !next.isDeleted

        // childrenTotal: +1 on new active, -1 when an active child becomes deleted; no change on in-place updates.
        when {
            prev == null && isActive -> kpiDao.inc("childrenTotal", 1)
            wasActive && !isActive   -> kpiDao.inc("childrenTotal", -1)
        }

        // childrenNewThisMonth: +1 if newly inserted active AND createdAt is in current month; -1 if such a row is deleted
        val monthKey = currentMonthKey()
        if (prev == null && isActive && inCurrentMonth(next.createdAt)) {
            kpiDao.inc("childrenNewThisMonth:$monthKey", 1)
        } else if (wasActive && !isActive && inCurrentMonth(prev?.createdAt)) {
            kpiDao.inc("childrenNewThisMonth:$monthKey", -1)
        }

        // childrenGraduated: transition on graduated flag among active rows
        val prevGrad = wasActive && prev?.graduated == Reply.YES
        val nextGrad = isActive && next.graduated == Reply.YES
        when {
            !prevGrad && nextGrad -> kpiDao.inc("childrenGraduated", 1)
            prevGrad && !nextGrad -> kpiDao.inc("childrenGraduated", -1)
        }

        // resettled / toBeResettled: keep complementary tallies among active rows
        val prevRes = wasActive && (prev?.resettled == true)
        val nextRes = isActive && (next.resettled == true)
        when {
            !prevRes && nextRes -> {
                kpiDao.inc("resettled", 1)
                kpiDao.inc("toBeResettled", -1)
            }
            prevRes && !nextRes -> {
                kpiDao.inc("resettled", -1)
                kpiDao.inc("toBeResettled", 1)
            }
            prev == null && isActive -> {
                // new row
                if (nextRes) kpiDao.inc("resettled", 1) else kpiDao.inc("toBeResettled", 1)
            }
            wasActive && !isActive -> {
                // deletion: remove from the bucket it belonged to
                if (prevRes) kpiDao.inc("resettled", -1) else kpiDao.inc("toBeResettled", -1)
            }
        }

        // acceptedChrist: count YES with acceptedJesusDate in current month (monthly bucket)
        val prevAcc = wasActive && prev?.acceptedJesus == Reply.YES && inCurrentMonth(prev.acceptedJesusDate)
        val nextAcc = isActive && next.acceptedJesus == Reply.YES && inCurrentMonth(next.acceptedJesusDate)
        when {
            !prevAcc && nextAcc -> kpiDao.inc("acceptedChrist:$monthKey", 1)
            prevAcc && !nextAcc -> kpiDao.inc("acceptedChrist:$monthKey", -1)
        }

        // yetToAcceptChrist: active with acceptedJesus == NO
        val prevYet = wasActive && prev?.acceptedJesus == Reply.NO
        val nextYet = isActive && next.acceptedJesus == Reply.NO
        when {
            !prevYet && nextYet -> kpiDao.inc("yetToAcceptChrist", 1)
            prevYet && !nextYet -> kpiDao.inc("yetToAcceptChrist", -1)
        }
    }

    private fun inCurrentMonth(ts: Timestamp?): Boolean {
        if (ts == null) return false
        val c = Calendar.getInstance()
        val nowYear = c.get(Calendar.YEAR)
        val nowMonth = c.get(Calendar.MONTH)
        c.time = ts.toDate()
        return c.get(Calendar.YEAR) == nowYear && c.get(Calendar.MONTH) == nowMonth
    }

    private fun currentMonthKey(): String {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(y, m)
    }

    override fun pagedNotGraduated(needle: String, pageSize: Int): Flow<PagingData<Child>> =
        Pager(
            config = PagingConfig(
                pageSize = pageSize,
                prefetchDistance = pageSize / 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { childDao.pageNotGraduatedByName(needle.lowercase()) }
        ).flow

    override fun countNotGraduated(needle: String): Flow<Int> =
        childDao.countNotGraduatedByName(needle.lowercase())

//    override suspend fun hardDeleteCascade(childId: String) {
//        require(childId.isNotBlank()) { "childId is blank" }
//
//        // 1) Remove from Room immediately (UI updates now)
//        childDao.hardDelete(childId)
//
//        // 2) Kick a one-off worker to delete Firestore child + all attendances
//        val input = Data.Builder()
//            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
//            .setInputData(input)
//            .build()
//
//        WorkManager.getInstance(appContext).enqueueUniqueWork(
//            "children_cascade_delete_$childId",
//            ExistingWorkPolicy.APPEND,
//            req
//        )
//    }

//    override suspend fun hardDeleteCascade(childId: String) {
////         1) Purge from Room immediately (UI updates instantly)
//        childDao.hardDelete(childId)
//
//        // 2) Enqueue a one-shot Worker to delete Firestore child doc and attendance subcollection
//        val input = Data.Builder()
//            .putString(ChildrenCascadeDeleteWorker.KEY_CHILD_ID, childId)
//            .build()
//
//        val req = OneTimeWorkRequestBuilder<ChildrenCascadeDeleteWorker>()
//            .setInputData(input)
//            .build()
//
//        WorkManager.getInstance(appContext)
//            .enqueueUniqueWork(
//                "children_cascade_delete_$childId",
//                ExistingWorkPolicy.REPLACE,
//                req
//            )
//    }


}

//package com.example.zionkids.domain.repositories.offline
//
//// <app/src/main/java/com/example/zionkids/domain/repositories/online/ChildrenRepositoryOffline.kt>
//// /// CHANGED: new offline-first repository that uses Room (ChildDao) only; same interface, zero Firestore calls.
//// before → after (new file)
//
//
//import com.example.zionkids.data.model.Child
////import com.example.zionkids.data.model.ChildDao
//import com.example.zionkids.data.model.EducationPreference
//import com.example.zionkids.data.model.Reply
//import com.google.firebase.Timestamp
//import javax.inject.Inject
//import javax.inject.Singleton
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.flowOn
//
//import com.example.zionkids.data.local.dao.ChildDao
//
//
//data class ChildrenSnapshot(
//    val children: List<Child>,
//    val fromCache: Boolean,        // true = served from local cache (offline or warming up)
//    val hasPendingWrites: Boolean  // true = local changes not yet synced
//)
//
//interface OfflineChildrenRepository {
//    suspend fun getChildFast(id: String): Child?
//    fun streamChildren(): Flow<List<Child>>
//    suspend fun upsert(child: Child, isNew: Boolean): String
//    suspend fun getAll(): List<Child>
//    suspend fun getAllNotGraduated(): List<Child>
//    fun streamAllNotGraduated(): Flow<ChildrenSnapshot>
//    //    suspend fun deleteChild(id: String)
//// (optional) fire-and-forget offline version
//    suspend fun deleteChildAndAttendances(childId: String)
//
//    fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot>
//
//    fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot>
//
//    suspend fun getByEducationPreference(pref: EducationPreference): List<Child>
//
//
//}
//@Singleton
//class OfflineChildrenRepositoryImpl @Inject constructor(
//    private val childDao: ChildDao
//) : OfflineChildrenRepository {
//
//    override suspend fun getChildFast(id: String): Child? =
//        childDao.getById(id)
//
//    override fun streamChildren(): Flow<List<Child>> =
//        childDao.observeAllActive()
//
//    override suspend fun upsert(child: Child, isNew: Boolean): String {
//        val id = child.childId
//        require(id.isNotBlank()) { "childId required (generate one before saving)" }
//        val now = Timestamp.now()
//        val nextVersion = (child.version + 1).coerceAtLeast(1)
//        childDao.upsert(
//            child.copy(
//                isDirty = true,
//                isDeleted = false,
//                updatedAt = now,
//                version = nextVersion
//            )
//        )
//        return id
//    }
//
//    override suspend fun getAll(): List<Child> =
//        childDao.getAllActive()
//
//    override suspend fun getAllNotGraduated(): List<Child> =
//        childDao.getAllByGraduated(Reply.NO)
//
//    override fun streamAllNotGraduated(): Flow<ChildrenSnapshot> =
//        combine(
//            childDao.observeAllByGraduated(Reply.NO),
//            childDao.observeDirtyCount()
//        ) { list, dirtyCount ->
//            ChildrenSnapshot(
//                children = list,
//                fromCache = true,               // Room is the local cache / source of truth
//                hasPendingWrites = dirtyCount > 0
//            )
//        }.flowOn(Dispatchers.IO)
//
//    override suspend fun deleteChildAndAttendances(childId: String) {
//        // Offline-only: mark tombstone; any server-side cascade handled by future sync worker.
//        childDao.softDelete(childId, Timestamp.now())
//    }
//
//    override fun streamByEducationPreference(pref: EducationPreference): Flow<ChildrenSnapshot> =
//        combine(
//            childDao.observeByEducationPreference(pref),
//            childDao.observeDirtyCount()
//        ) { list, dirtyCount ->
//            ChildrenSnapshot(
//                children = list.filter { it.graduated == Reply.NO },
//                fromCache = true,
//                hasPendingWrites = dirtyCount > 0
//            )
//        }.flowOn(Dispatchers.IO)
//
//    override fun streamByEducationPreferenceResilient(pref: EducationPreference): Flow<ChildrenSnapshot> =
//        combine(
//            streamByEducationPreference(pref),
//            streamAllNotGraduated()
//        ) { filtered, all ->
//            if (filtered.children.isEmpty() && all.children.isNotEmpty()) {
//                filtered.copy(
//                    children = all.children.filter { it.educationPreference == pref },
//                    fromCache = true
//                )
//            } else filtered
//        }
//
//    override suspend fun getByEducationPreference(pref: EducationPreference): List<Child> =
//        childDao.getAllActive().filter { it.educationPreference == pref && it.graduated == Reply.NO }
//}
