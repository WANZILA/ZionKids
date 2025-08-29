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
