//package com.example.zionkids.presentation.screens.migrationToolKit
//
//import kotlinx.coroutines.withContext
//import org.json.JSONArray
//import java.io.BufferedReader
//import java.io.InputStreamReader
//import android.content.Context
//import android.os.Build
//import androidx.annotation.RequiresApi
//import com.google.firebase.FirebaseApp
//import com.google.firebase.FirebaseOptions
//import com.google.firebase.firestore.*
//import kotlinx.coroutines.tasks.await
//import android.net.Uri
//import kotlinx.coroutines.Dispatchers
//import org.json.JSONObject
//import java.io.OutputStream
//
///**
// * Zion Kids cache migrator:
// * - Reads docs from local CACHE (old/locked project)
// * - Copies them into another Firestore project you control
// *
// * How it works:
// *  - oldDb() uses the default Firestore instance and DISABLES NETWORK so we only read cache.
// *  - initTargetDb(...) creates a second FirebaseApp for your target project.
// *  - copyAllToTarget(...) loops through ROOTS and optional SUBCOLLS and writes in safe batches.
// */
//object ZionKidsCacheMigrator {
//
//    /** 🔧 EDIT if needed: your top-level collection names in Zion Kids. */
//    private val ROOTS: List<String> = listOf(
//        "children",
//        "events",
//        "attendances",
//        "users"
//        // add more if you use them: "regions", "streets", "guardians", "consecutiveAttendance", ...
//    )
//
//    /**
//     * 🔧 Optional: known subcollections under each root.
//     * Example if you have event rosters at events/{eventId}/roster:
//     *   "events" to listOf("roster")
//     * Example if you have notes at children/{childId}/notes:
//     *   "children" to listOf("notes")
//     */
//    private val SUBCOLLS: Map<String, List<String>> = mapOf(
//        // "events" to listOf("roster"),
//        // "children" to listOf("notes")
//    )
//
//    /** Default (old/locked) Firestore — network disabled so reads are from CACHE only. */
//    fun oldDb(): FirebaseFirestore {
//        val db = FirebaseFirestore.getInstance()
//        db.disableNetwork()
//        return db
//    }
//
//    /** Create a second Firestore bound to your TARGET project (children-of-zion). */
//    fun initTargetDb(
//        context: Context,
//        projectId: String,
//        applicationId: String, // looks like 1:...:android:...
//        apiKey: String,
//        appName: String = "targetApp"
//    ): FirebaseFirestore {
//        val options = FirebaseOptions.Builder()
//            .setProjectId(projectId)
//            .setApplicationId(applicationId)
//            .setApiKey(apiKey)
//            .build()
//
//        val app = try {
//            FirebaseApp.getInstance(appName)
//        } catch (_: IllegalStateException) {
//            FirebaseApp.initializeApp(context, options, appName)
//        }
//        return FirebaseFirestore.getInstance(app)
//    }
//
//    /** Helpers forcing CACHE reads. */
//    private suspend fun Query.getFromCache(): QuerySnapshot =
//        get(Source.CACHE).await()
//
//    private suspend fun DocumentReference.getFromCache(): DocumentSnapshot =
//        get(Source.CACHE).await()
//
//    /** Copy one document and its declared subcollections. */
//    private suspend fun copyDocWithSubcollections(
//        newDb: FirebaseFirestore,
//        srcDoc: DocumentSnapshot,
//        batcher: BatchWriter
//    ) {
//        val data = srcDoc.data ?: return
//        // Same path on target (keeps IDs and structure).
//        val dstRef = newDb.document(srcDoc.reference.path)
//        batcher.set(dstRef, data)
//
//        // Copy declared subcollections, if any
//        val root = srcDoc.reference.path.substringBefore('/') // e.g., "events"
//        val subs = SUBCOLLS[root] ?: emptyList()
//        for (sub in subs) {
//            val subSnap = srcDoc.reference.collection(sub).getFromCache()
//            for (subDoc in subSnap.documents) {
//                val subData = subDoc.data ?: continue
//                val dstSub = newDb.document(subDoc.reference.path)
//                batcher.set(dstSub, subData)
//            }
//        }
//    }
//
//    /**
//     * 👉 Public entry: copy everything we can see in CACHE into the target project.
//     * Provide a simple logger lambda to surface progress in your UI.
//     */
//    @RequiresApi(Build.VERSION_CODES.KITKAT)
//    suspend fun copyAllToTarget(
//        context: Context,
//        projectId: String,
//        applicationId: String,
//        apiKey: String,
//        log: (String) -> Unit = {}
//    ) {
//        val srcDb = oldDb() // CACHE-only
//        val dstDb = initTargetDb(context, projectId, applicationId, apiKey)
//        val batcher = BatchWriter(dstDb)
//
//        for (coll in ROOTS) {
//            log("Copying collection: $coll ...")
//            val snap = srcDb.collection(coll).getFromCache()
//            log("Found ${snap.size()} docs in cache for $coll")
//            for (doc in snap.documents) {
//                copyDocWithSubcollections(dstDb, doc, batcher)
//            }
//        }
//
//        batcher.flush()
//        log("Copy complete ✅")
//    }
//
//    /** Batches writes safely under the 500-ops limit. */
//    private class BatchWriter(private val db: FirebaseFirestore) {
//        private var batch = db.batch()
//        private var count = 0
//        private val limit = 450 // headroom for safety
//
//        suspend fun set(ref: DocumentReference, data: Map<String, Any?>) {
//            batch.set(ref, data)
//            count++
//            if (count >= limit) commitReset()
//        }
//
//        suspend fun flush() {
//            if (count > 0) commitReset()
//        }
//
//        private suspend fun commitReset() {
//            batch.commit().await()
//            batch = db.batch()
//            count = 0
//        }
//    }
//
//
//    // Put inside object ZionKidsCacheMigrator { ... }
//    suspend fun exportAllToJsonl(
//        context: Context,
//        outputUri: Uri,
//        log: (String) -> Unit = {}
//    ) {
//        val db = oldDb() // cache-only
//        withContext(Dispatchers.IO) {
//            val os: OutputStream = context.contentResolver.openOutputStream(outputUri)
//                ?: error("Could not open output stream")
//            os.bufferedWriter().use { writer ->
//                fun writeLine(s: String) { writer.append(s).append('\n') }
//
//                for (coll in ROOTS) {
//                    log("Exporting $coll ...")
//                    val snap = db.collection(coll).getFromCache()
//                    log("Found ${snap.size()} docs in $coll")
//                    for (doc in snap.documents) {
//                        val data = doc.data ?: emptyMap<String, Any?>()
//                        // main doc
//                        writeLine(
//                            JSONObject()
//                                .put("path", doc.reference.path)
//                                .put("id", doc.id)
//                                .put("data", JSONObject(data))
//                                .toString()
//                        )
//                        // declared subcollections, if any
//                        val root = coll
//                        val subs = SUBCOLLS[root] ?: emptyList()
//                        for (sub in subs) {
//                            val subSnap = doc.reference.collection(sub).get(Source.CACHE).await()
//                            for (subDoc in subSnap.documents) {
//                                val subData = subDoc.data ?: emptyMap<String, Any?>()
//                                writeLine(
//                                    JSONObject()
//                                        .put("path", subDoc.reference.path) // e.g. events/123/roster/abc
//                                        .put("id", subDoc.id)
//                                        .put("data", JSONObject(subData))
//                                        .toString()
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        log("Export complete ✅")
//    }
//
//
//    // Restore into the TARGET project you control
//    suspend fun restoreFromJsonl(
//        context: Context,
//        inputUri: Uri,
//        projectId: String,
//        applicationId: String,
//        apiKey: String,
//        log: (String) -> Unit = {}
//    ) {
//        val dstDb = initTargetDb(context, projectId, applicationId, apiKey)
//        val batcher = BatchWriter(dstDb)
//
//        withContext(Dispatchers.IO) {
//            context.contentResolver.openInputStream(inputUri).use { input ->
//                requireNotNull(input) { "Could not open input stream" }
//                BufferedReader(InputStreamReader(input)).useLines { lines ->
//                    var count = 0
//                    lines.forEach { line ->
//                        if (line.isBlank()) return@forEach
//                        val json = JSONObject(line)
//                        val path = json.getString("path")      // e.g., "children/abc123"
//                        val dataObj = json.getJSONObject("data")
//
//                        val map = jsonToMap(dataObj)
//                        val ref = dstDb.document(path)
//                        // write
//                        batcher.set(ref, map)
//                        count++
//                        if (count % 1000 == 0) {
//                            log("Queued $count docs...")
//                        }
//                    }
//                }
//            }
//        }
//        batcher.flush()
//        log("Restore complete ✅")
//    }
//
//    /** Convert JSONObject/JSONArray to nested Map/List for Firestore set() */
//    private fun jsonToAny(any: Any?): Any? = when (any) {
//        is JSONObject -> jsonToMap(any)
//        is JSONArray -> (0 until any.length()).map { i -> jsonToAny(any.get(i)) }
//        JSONObject.NULL -> null
//        else -> any
//    }
//
//    private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
//        val out = mutableMapOf<String, Any?>()
//        val it = obj.keys()
//        while (it.hasNext()) {
//            val k = it.next()
//            out[k] = jsonToAny(obj.get(k))
//        }
//        return out
//    }
//
//    // ADD inside ZionMigrator
//
//    /**
//     * Toggle `force` on /appConfig/mobile of the DEFAULT app (the app this build is using).
//     * Optional: set a custom forceMessage, latestVersionCode, and downloadUrl.
//     */
//    suspend fun setForceUpdateDefaultAlt(
//        context: Context,
//        force: Boolean,
//        message: String? = null,
//        latestVersionCode: String? = null,
//        downloadUrl: String? = null,
//        log: (String) -> Unit = {}
//    ) {
//        val db = FirebaseFirestore.getInstance() // default project
//        setForceUpdateInternal(db, force, message, latestVersionCode, downloadUrl, log)
//    }
//
//
//
//    /**
//     * Toggle `force` on /appConfig/mobile of a TARGET project (uses your appId/apiKey fields).
//     */
//
//    // === Admin toggles for /appConfig/mobile ===
//
//    // If you ever want to act on a target project (the fields on your screen), call this:
//    // ADD inside object ZionMigrator (no removals)
//
//// Toggle `force` on /appConfig/mobile for the DEFAULT app (this build's Firebase app)
//    suspend fun setForceUpdateDefault(
//        context: Context,
//        force: Boolean,
//        message: String? = null,
//        latestVersionCode: String? = null,  // your AppUpdateConfig stores version codes as String
//        downloadUrl: String? = null,
//        log: (String) -> Unit = {}
//    ) {
//        val db = FirebaseFirestore.getInstance()
//        setForceUpdateInternal(db, force, message, latestVersionCode, downloadUrl, log)
//    }
//
//    // Toggle `force` on /appConfig/mobile for a TARGET project (from the text fields)
//    suspend fun setForceUpdateOnTarget(
//        context: Context,
//        projectId: String,
//        applicationId: String,
//        apiKey: String,
//        force: Boolean,
//        message: String? = null,
//        latestVersionCode: String? = null,
//        downloadUrl: String? = null,
//        log: (String) -> Unit = {}
//    ) {
//        val db = initTargetDb(context, projectId, applicationId, apiKey)
//        setForceUpdateInternal(db, force, message, latestVersionCode, downloadUrl, log)
//    }
//
//    // Shared writer
//    private suspend fun setForceUpdateInternal(
//        db: FirebaseFirestore,
//        force: Boolean,
//        message: String? = null,
//        latestVersionCode: String? = null,
//        downloadUrl: String? = null,
//        log: (String) -> Unit
//    ) {
//        val ref = db.collection("appConfig").document("mobile")
//        val payload = mutableMapOf<String, Any>(
//            "force" to force,
//            "updatedAt" to FieldValue.serverTimestamp()
//        )
//        message?.let { payload["forceMessage"] = it }
//        latestVersionCode?.let { payload["latestVersionCode"] = it }  // keep as String to match your model
//        downloadUrl?.let { payload["downloadUrl"] = it }
//
//        ref.set(payload, SetOptions.merge()).await()
//        log(if (force) "Force update ENABLED ✅" else "Force update CLEARED ✅")
//    }
//
//
//
//}
