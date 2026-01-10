package com.example.zionkids.core.sync.attendance

import com.example.zionkids.data.model.Attendance


fun resolveAttendance(local: Attendance, remote: Attendance?): Attendance {
    if (remote == null) return local
    return when {
        local.version > remote.version -> local
        local.version < remote.version -> remote
        else -> when {
            local.updatedAt > remote.updatedAt -> local
            local.updatedAt < remote.updatedAt -> remote
            else -> if (local.isDirty) local else remote
        }
    }
}
