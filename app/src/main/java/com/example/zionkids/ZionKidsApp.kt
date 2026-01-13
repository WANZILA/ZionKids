// <app/src/main/java/com/example/zionkids/ZionKidsApp.kt>
// /// CHANGED: set Firestore persistent cache settings in Application.onCreate(), before any Firestore usage

package com.example.zionkids

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.zionkids.core.sync.ChildrenSyncScheduler
//import com.example.zionkids.core.sync.event.HydrateEventsOnce
import com.example.zionkids.data.local.dao.ChildDao
import com.example.zionkids.data.local.db.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

// >>> FIXED: use KTX imports
//import com.google.firebase.ktx.Firebase
//import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

import timber.log.Timber

import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.content.pm.ApplicationInfo
import com.example.zionkids.core.sync.CleanerScheduler
import com.example.zionkids.core.sync.SyncCoordinatorScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.firestore

@HiltAndroidApp
class ZionKidsApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
//    @Inject lateinit var hydrateChildrenOnce: HydrateChildrenOnce
//    @Inject lateinit var hydrateEventsOnce: HydrateEventsOnce
//    @Inject lateinit var hydrateAttendanceOnce: HydrateAttendanceOnce

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DbEntryPoint {
        fun appDb(): AppDatabase
        fun childDao(): ChildDao
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // Firestore persistent cache BEFORE any Firestore access
        runCatching {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(500L * 1024 * 1024) // 500MB
                .build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            Firebase.firestore.firestoreSettings = settings

            // --- versionName / versionCode without BuildConfig ---
            val pm = applicationContext.packageManager
            val pkg = applicationContext.packageName
            val pInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            val versionName = pInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (pInfo.longVersionCode).toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            // --- buildType without BuildConfig ---
            val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val buildType = if (isDebug) "debug" else "release"

            // Crashlytics keys
            // Collect automatically in release; flip to true in debug if you want to test
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!isDebug)

            Firebase.crashlytics.setCustomKey("version_name", versionName)
            Firebase.crashlytics.setCustomKey("version_code", versionCode)
            Firebase.crashlytics.setCustomKey("build_type", buildType)

            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            Firebase.crashlytics.setCustomKey("device_name", deviceName)
            Firebase.crashlytics.setCustomKey("brand", Build.BRAND)
            Firebase.crashlytics.setCustomKey("device_code", Build.DEVICE)   // e.g., KF6
            Firebase.crashlytics.setCustomKey("product", Build.PRODUCT)
            Firebase.crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
            Firebase.crashlytics.setCustomKey("android_release", Build.VERSION.RELEASE)

            // Attach/clear user on auth state changes
            Firebase.auth.addAuthStateListener { auth ->
                val u = auth.currentUser
                if (u != null) {
                    Firebase.crashlytics.setUserId(u.uid)
                    Firebase.crashlytics.setCustomKey("user_name", (u.displayName ?: "unknown").take(64))
                    val emailHash = u.email?.let { sha256(it) } ?: ""
                    Firebase.crashlytics.setCustomKey("user_email_hash", emailHash)
                    Firebase.crashlytics.setCustomKey("user_alias", (u.email?.substringBefore('@') ?: "guest").take(32))
                } else {
                    Firebase.crashlytics.setUserId("")
                    Firebase.crashlytics.setCustomKey("user_name", "")
                    Firebase.crashlytics.setCustomKey("user_email_hash", "")
                    Firebase.crashlytics.setCustomKey("user_alias", "")
                }
            }
        }

        // Prewarm DB so Database Inspector shows it as OPEN
        val ep = EntryPointAccessors.fromApplication(this, DbEntryPoint::class.java)
        val db = ep.appDb()
        db.openHelper.writableDatabase.query("SELECT 1")

        // One-shot hydrate to fill Room quickly (non-blocking)
//        CoroutineScope(Dispatchers.IO).launch {
//            runCatching {
//                hydrateChildrenOnce(pageSize = 100, maxPages = 20)
//                hydrateEventsOnce(pageSize = 100, maxPages = 20)
//                hydrateAttendanceOnce(pageSize = 100, maxPages = 20)
//            }
//        }

        // Schedule ongoing sync
        ChildrenSyncScheduler.enqueuePeriodicPush(this)
        ChildrenSyncScheduler.enqueuePeriodicPull(this)

// CHANGED: pull immediately once at startup
//        ChildrenSyncScheduler.enqueuePushNow(this)
        // DEBUG: run immediately so you see logs / data hydration now
        ChildrenSyncScheduler.enqueuePullNow(this)
        CleanerScheduler.enqueuePeriodic(this, retentionDays = 30L)
//        CleanerScheduler.enqueueNow(appContext)
        SyncCoordinatorScheduler.enqueuePullAllPeriodic(this, cleanerRetentionDays = 30L)
        SyncCoordinatorScheduler.enqueuePushAllNow(this, cleanerRetentionDays = 30L)

        SyncCoordinatorScheduler.enqueuePushAllPeriodic(this, cleanerRetentionDays = 30L)



//        ChildrenSyncScheduler.enqueuePeriodicPush(this)
////        ChildrenSyncScheduler.enqueuePushNow(this)
//        ChildrenSyncScheduler.enqueuePeriodicPull(this)
//        EventSyncScheduler.enqueuePeriodicPush(this)
//        EventSyncScheduler.enqueuePushNow(this)
//        AttendanceSyncScheduler.enqueuePushNow(this)
//        AttendanceSyncScheduler.enqueuePeriodicPush(this)
    }

    companion object {
        private fun sha256(text: String): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            return md.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}

//// <app/src/main/java/com/example/zionkids/ZionKidsApp.kt>
//// /// CHANGED: set Firestore persistent cache settings in Application.onCreate(), before any Firestore usage
//
//package com.example.zionkids
//
//import android.app.Application
//import androidx.hilt.work.HiltWorkerFactory
//import androidx.work.Configuration
//import com.example.zionkids.core.sync.ChildrenSyncScheduler
//import com.example.zionkids.core.sync.HydrateChildrenOnce
//import com.example.zionkids.core.sync.attendance.AttendanceSyncScheduler
//import com.example.zionkids.core.sync.attendance.HydrateAttendanceOnce
//import com.example.zionkids.core.sync.event.EventSyncScheduler
//import com.example.zionkids.core.sync.event.EventSyncWorker
//import com.example.zionkids.core.sync.event.HydrateEventsOnce
//import com.example.zionkids.data.local.dao.ChildDao
//import com.example.zionkids.data.local.db.AppDatabase
////import com.example.zionkids.domain.sync.HydrateAttendanceOnceWorker
//import dagger.hilt.EntryPoint
//import dagger.hilt.InstallIn
//import dagger.hilt.android.EntryPointAccessors
//import dagger.hilt.android.HiltAndroidApp
//import dagger.hilt.components.SingletonComponent
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//// /// CHANGED: Firestore settings imports (moved from Activity)
//import com.google.firebase.Firebase
//import com.google.firebase.crashlytics.crashlytics
////import com.google.firebase.firestore.BuildConfig
////import com.example.zionkids.BuildConfig
//import com.google.firebase.firestore.FirebaseFirestoreSettings
//import com.google.firebase.firestore.PersistentCacheSettings
//import com.google.firebase.firestore.firestore
//import timber.log.Timber
//
//import android.content.pm.PackageManager
//import android.os.Build
//import android.content.pm.PackageInfo
//import android.content.pm.PackageManager.PackageInfoFlags
//import android.content.pm.ApplicationInfo
////import com.google.firebase.Firebase
//import com.google.firebase.crashlytics.crashlytics
//
//
//@HiltAndroidApp
//class ZionKidsApp : Application(), Configuration.Provider {
//
//    @Inject lateinit var workerFactory: HiltWorkerFactory
//    @Inject lateinit var hydrateChildrenOnce: HydrateChildrenOnce
//
//    @Inject lateinit var  hydrateEventsOnce : HydrateEventsOnce
//
//    @Inject lateinit var hydrateAttendanceOnce : HydrateAttendanceOnce
//
//
//    override val workManagerConfiguration: Configuration
//        get() = Configuration.Builder()
//            .setWorkerFactory(workerFactory)
//            .build()
//
//    @EntryPoint
//    @InstallIn(SingletonComponent::class)
//    interface DbEntryPoint {
//        fun appDb(): AppDatabase
//        fun childDao(): ChildDao
//    }
//
//    override fun onCreate() {
//        super.onCreate()
////        Timber.d("WorkerFactory type = ${workerFactory::class.qualifiedName}")
//        Timber.plant(Timber.DebugTree())
//        // /// CHANGED: configure Firestore cache BEFORE any Firestore access
//        runCatching {
//            val cacheSettings = PersistentCacheSettings.newBuilder()
//                .setSizeBytes(500L * 1024 * 1024) // 500MB
//                .build()
//            val settings = FirebaseFirestoreSettings.Builder()
//                .setLocalCacheSettings(cacheSettings)
//                .build()
//            Firebase.firestore.firestoreSettings = settings
//
//// --- versionName / versionCode without BuildConfig ---
//
//
//            val pm = applicationContext.packageManager
//            val pkg = applicationContext.packageName
//            val pInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                pm.getPackageInfo(pkg, PackageInfoFlags.of(0))
//            } else {
//                @Suppress("DEPRECATION")
//                pm.getPackageInfo(pkg, 0)
//            }
//
//            val versionName = pInfo.versionName ?: "unknown"
//            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                (pInfo.longVersionCode).toInt()
//            } else {
//                @Suppress("DEPRECATION")
//                pInfo.versionCode
//            }
//
//// --- buildType without BuildConfig ---
//            val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
//            val buildType = if (isDebug) "debug" else "release"
//
//// Tag Crashlytics
//            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
//            Firebase.crashlytics.setCustomKey("version_name", versionName)
//            Firebase.crashlytics.setCustomKey("version_code", versionCode)
//            Firebase.crashlytics.setCustomKey("build_type", buildType)
//            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
//            Firebase.crashlytics.setCustomKey("device_name", deviceName)
//            Firebase.crashlytics.setCustomKey("brand", Build.BRAND)
//            Firebase.crashlytics.setCustomKey("device_code", Build.DEVICE)   // e.g., KF6
//            Firebase.crashlytics.setCustomKey("product", Build.PRODUCT)
//            Firebase.crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
//            Firebase.crashlytics.setCustomKey("android_release", Build.VERSION.RELEASE)
//            // Optional: enable logs in debug if you ever disable via manifest flags later
//            Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
//        }
////        Timber.d("WorkerFactory type = ${workerFactory::class.qualifiedName}")
//
//        // Prewarm DB so Database Inspector shows it as OPEN
//        val ep = EntryPointAccessors.fromApplication(this, DbEntryPoint::class.java)
//        val db = ep.appDb()
//        db.openHelper.writableDatabase.query("SELECT 1")
//
//        // One-shot hydrate to fill Room quickly (non-blocking)
//        CoroutineScope(Dispatchers.IO).launch {
//            //100
//            runCatching {
//                hydrateChildrenOnce(pageSize = 100, maxPages = 20)
//                hydrateEventsOnce(pageSize = 100, maxPages = 20)
//                hydrateAttendanceOnce(pageSize = 100, maxPages = 20)
//            }
//        }
////        Timber.d("WorkerFactory type = ${workerFactory::class.qualifiedName}")
//
//        // Schedule ongoing sync
//        ChildrenSyncScheduler.enqueuePeriodicPush(this)
//        ChildrenSyncScheduler.enqueuePushNow(this)
//        EventSyncScheduler.enqueuePeriodicPush(this)
//        EventSyncScheduler.enqueuePushNow(this)
//        AttendanceSyncScheduler.enqueuePushNow(this)
//        AttendanceSyncScheduler.enqueuePeriodicPush(this)
////        AttendanceSyncScheduler.
////        Timber.plant(Timber.DebugTree())
////        ChildrenSyncScheduler.enqueueCascadeDelete(this)
////        Timber.d("WorkerFactory type = ${workerFactory::class.qualifiedName}")
//
//    }
//}
