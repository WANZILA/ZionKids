// com/example/zionkids/core/di/FirestoreModule.kt
package com.example.zionkids.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    @ChildrenRef
    fun provideChildrenCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("children")

    @Provides
    @Singleton
    @EventsRef
    fun provideEventsCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("events")

    @Provides
    @Singleton
    @AttendanceRef
    fun provideAttendanceRef(db: FirebaseFirestore): CollectionReference =
        db.collection("attendances")
}

//package com.example.zionkids.core.di
//// com/example/zionkids/core/di/FirestoreModule.kt
////package com.example.zionkids.core.di
//
//import com.google.firebase.firestore.CollectionReference
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.ktx.firestore
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object FirestoreModule {
//
//    @Provides
//    @Singleton
//    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance() // or Firebase.firestore
//
//    @Provides
//    @Singleton
//    @ChildrenRef
//    fun provideChildrenCollection(db: FirebaseFirestore): CollectionReference =
//        db.collection("children")
//
//    @Provides
//    @Singleton
//    @EventsRef
//    fun provideEventsCollection(db: FirebaseFirestore): CollectionReference =
//        db.collection("events")
//}
//
////import com.google.firebase.firestore.FirebaseFirestore
////import dagger.Module
////import dagger.Provides
////import dagger.hilt.InstallIn
////import dagger.hilt.components.SingletonComponent
////import javax.inject.Singleton
////
////@Module
////@InstallIn(SingletonComponent::class)
////object FirebaseModule {
////
////    @Provides
////    @Singleton
////    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
////}
//
