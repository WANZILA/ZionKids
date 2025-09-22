package com.example.zionkids.data.model

import com.google.firebase.Timestamp

enum class AssignedRole { ADMIN, LEADER, VOLUNTEER, VIEWER,  SPONSOR   }

data class UserProfile(
    val uid: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val userRole: AssignedRole = AssignedRole.VOLUNTEER,
    val failedAttempts: Int = 0,
    val disabled: Boolean = false,
    val deletedUser: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

/***
 * Permissions we’ll use (simple & practical)
 *
 * ADMIN: everything (manage users, children, events, attendance, config).
 *
 * LEAD: create/update children & events; create/update attendance; delete attendance.
 *
 * VOLUNTEER: read children & events only; create/update attendance.
 *
 * VIEWER: read all .
 *
 * SPONSOR: read children & events only).
 */
//class Access(private val me: UserProfile?) {
//    fun has(r: Role) = me?.roles?.contains(r) == true && me.disabled == false
//}
