package com.example.zionkids.core.sync

// <app/src/main/java/com/example/zionkids/domain/sync/HydrateChildrenOnce.kt>
// /// CHANGED: new one-shot hydrator that pulls Firestore -> Room via ChildDao; run on login/app start to fill the table now.

//package com.example.zionkids.domain.sync

import com.example.zionkids.core.di.ChildrenRef
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.model.Child
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrateChildrenOnce @Inject constructor(
    @ChildrenRef private val childrenRef: CollectionReference,
    private val childDao: ChildDao
) {
    suspend operator fun invoke(pageSize: Int = 500, maxPages: Int = 50) = withContext(Dispatchers.IO) {
        var page = 0
        var lastUpdated: com.google.firebase.Timestamp? = null
        do {
            val q = if (lastUpdated == null) {
                childrenRef.orderBy("updatedAt", Query.Direction.ASCENDING).limit(pageSize.toLong())
            } else {
                childrenRef.orderBy("updatedAt", Query.Direction.ASCENDING)
                    .startAfter(lastUpdated!!)
                    .limit(pageSize.toLong())
            }
            val snap = q.get().await()
            val items = snap.documents.mapNotNull { it.toObject(Child::class.java) }
            if (items.isEmpty()) break

            // Server is authoritative on pull
            childDao.upsertAll(items.map { it.copy(isDirty = false) })

            lastUpdated = items.last().updatedAt
            page++
        } while (items.size >= pageSize && page < maxPages)
    }
}
