// com/example/zionkids/core/di/Qualifiers.kt
package com.example.zionkids.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ChildrenRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EventsRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AttendanceRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UsersRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdminAuth

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeletedUsersRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LockedAccountsRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppUpdateRepositoryRef

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionCode






//package com.example.zionkids.core.di
//
////// com/example/zionkids/core/di/Qualifiers.kt
////package com.example.zionkids.core.di
//
//import javax.inject.Qualifier
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class ChildrenRef
//
//@Qualifier
//@Retention(AnnotationRetention.BINARY)
//annotation class EventsRef
