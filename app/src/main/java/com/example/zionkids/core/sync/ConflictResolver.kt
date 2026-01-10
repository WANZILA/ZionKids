package com.example.zionkids.core.sync

// <app/src/main/java/com/example/zionkids/core/sync/ConflictResolver.kt>
// /// CHANGED: new tiny, shared conflict resolver used by worker/listener
//package com.example.zionkids.core.sync

import com.example.zionkids.data.model.Child
import com.example.zionkids.domain.repositories.offline.OfflineChildrenRepository


// Top-level helper so you can call resolveChild(local, remote) directly
fun resolveChild(local: Child?, remote: Child?): Child {
    // If there's no remote, keep local as-is (stay dirty so it will push).
    if (remote == null) return requireNotNull(local) { "resolveChild: both local and remote are null" }

    // If no local, accept remote (coming from server) and mark clean.
    if (local == null) return remote.copy(isDirty = false)

    val lv = local.version
    val rv = remote.version
    return when {
        // 1) Higher version wins
        rv > lv -> remote.copy(isDirty = false)
        rv < lv -> local

        // 2) Same version: compare updatedAt
        remote.updatedAt.toDate().time > local.updatedAt.toDate().time -> remote.copy(isDirty = false)
        remote.updatedAt.toDate().time < local.updatedAt.toDate().time -> local

        // 3) Still tied: keep local if it’s dirty (let it push), else remote (clean)
        else -> if (local.isDirty) local else remote.copy(isDirty = false)
    }
}

// Extension so existing calls like `offlineRepo.resolveChild(local, remote)` still compile.
fun OfflineChildrenRepository.resolveChild(local: Child?, remote: Child?): Child =
    resolveChild(local, remote)