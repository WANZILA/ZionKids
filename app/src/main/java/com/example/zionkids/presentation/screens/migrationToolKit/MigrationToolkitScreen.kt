package com.example.zionkids.migration

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import com.example.zionkids.presentation.screens.widgets.BulkConfirmDialog

/* =============================== */
/* ======== EDIT THESE =========== */
/* =============================== */
private val ROOTS = listOf(
    "children",
    "events",
    "attendance",
    "users"
    // add: "regions", "streets", ...
)

private val SUBCOLLS: Map<String, List<String>> = mapOf(
    // "events" to listOf("roster"),
    // "children" to listOf("notes")
)

/* =============================== */
/* ======== UI SCREEN ============ */
/* =============================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrationToolkitScreen(
    navigateUp: () -> Unit,
    defaultProjectId: String = "children-of-zion",
//    defaultApplicationId: String = "1:83231010718:web:300a4b2bf8bc8df183308f",
//    defaultApiKey: String = "AIzaSyD7RgsmdJoH3NFL2xgkuj4QcmUJI3Ukcjw"
    defaultApplicationId: String = "1:83231010718:web:300a4b2bf8bc8df18330",
    defaultApiKey: String = "AIzaSyD7RgsmdJoH3NFL2xgkuj4QcmUJI3Ukc",
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectId by remember { mutableStateOf(defaultProjectId) }
    var applicationId by remember { mutableStateOf(defaultApplicationId) }
    var apiKey by remember { mutableStateOf(defaultApiKey) }

    var log by remember { mutableStateOf("Ready.") }
    var running by remember { mutableStateOf(false) }

    // Confirmation dialogs
    var confirmCopy by remember { mutableStateOf(false) }
    var confirmExport by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    // Shared confirm dialog state
    var showConfirm by remember { mutableStateOf(false) }
    var confirmMessage by remember { mutableStateOf("") }
    var pendingConfirm: (() -> Unit)? by remember { mutableStateOf(null) }

    //for download url
    var downloadUrlText by remember {
        mutableStateOf("https://appdistribution.firebase.google.com")}
    // ADD — only overwrite once (so you don't clobber what the admin is typing)
    var initialUrlLoaded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val ref = FirebaseFirestore.getInstance()
            .collection("appConfig")
            .document("mobile")

        val reg = ref.addSnapshotListener { snap, _ ->
            val url = snap?.getString("downloadUrl")
            // On first open (or first snapshot), if Firestore has a URL, use it.
            if (!initialUrlLoaded && !url.isNullOrBlank()) {
                downloadUrlText = url
                initialUrlLoaded = true
            }
        }

        onDispose { reg.remove() }
    }


    // File creators/pickers
    val createDoc =
        rememberLauncherForActivityResult(CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                running = true
                scope.launch {
                    try {
                        ZionMigrator.exportAllToJsonl(context, uri) { msg -> log = msg }
                    } catch (e: Exception) {
                        log = "Export error: ${e.message}"
                    } finally {
                        running = false
                    }
                }
            } else {
                log = "Export canceled."
            }
        }

    val openDoc = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            running = true
            scope.launch {
                try {
                    ZionMigrator.restoreFromJsonl(
                        context = context,
                        inputUri = uri,
                        projectId = projectId,
                        applicationId = applicationId,
                        apiKey = apiKey
                    ) { msg -> log = msg }
                } catch (e: Exception) {
                    log = "Restore error: ${e.message}"
                } finally {
                    running = false
                }
            }
        } else {
            log = "Restore canceled."
        }
    }

    val scroll = rememberScrollState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        },
//        snackbarHost = { SnackbarHost(snackbarHostState) },

    ) { inner ->

        Column(
            modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Zion Kids — Migration Toolkit", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

//        OutlinedTextField(
//            value = projectId, onValueChange = { projectId = it },
//            label = { Text("Project ID (target)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(8.dp))
//        OutlinedTextField(
//            value = applicationId, onValueChange = { applicationId = it },
//            label = { Text("Application ID (target)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
//        )
//        Spacer(Modifier.height(8.dp))
//        OutlinedTextField(
//            value = apiKey, onValueChange = { apiKey = it },
//            label = { Text("Web API Key (target)") }, singleLine = true,
//            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
//        )

            Spacer(Modifier.height(20.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !running && apiKey.isNotBlank(),
                    onClick = { confirmCopy = true }
                ) { Text("Copy Cache → Project") }

                Button(
                    enabled = !running,
                    onClick = { confirmExport = true }
                ) { Text("Export Cache → File") }

                Button(
//                enabled = !running && apiKey.isNotBlank(),
                    enabled = false,
                    onClick = { confirmRestore = true }
                ) { Text("Restore File → Project") }



                /*** For forcing an update  **/
                Spacer(Modifier.height(8.dp))

// ADD — paste/update the download link here
                OutlinedTextField(
                    value = downloadUrlText,
                    onValueChange = { downloadUrlText = it },
                    label = { Text("Download URL (Play/App Distribution)") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )


                Button(
                    enabled = !running,
                    onClick = {
                        confirmMessage =
                            "Force Update? All users will be blocked until they update."
                        pendingConfirm = {
                            running = true
                            scope.launch {
                                try {
                                    ZionMigrator.setForceUpdateDefault(
                                        context = context,
                                        force = true,
                                        message = "An important update is required. Please update to continue.",
                                        downloadUrl = downloadUrlText.takeIf { it.isNotBlank() }      ) { msg -> log = msg }
                                } catch (e: Exception) {
                                    log = "Force update error: ${e.message}"
                                } finally {
                                    running = false
                                }
                            }
                        }
                        showConfirm = true
                    }

                ) { Text("Force Update (All Users)") }


                /*** For setting the url for the download link  **/

                Button(
                    enabled = !running,
                    onClick = {
                        confirmMessage =
                            "Set Latest to THIS app version? Older versions will get a soft prompt."
                        pendingConfirm = {
                            running = true
                            scope.launch {
                                try {
                                    ZionMigrator.setLatestToInstalled(
                                        context = context,
                                         downloadUrl = downloadUrlText.takeIf { it.isNotBlank() } ) { msg -> log = msg }
                                } catch (e: Exception) {
                                    log = "Set latest error: ${e.message}"
                                } finally {
                                    running = false
                                }
                            }
                        }
                        showConfirm = true
                    }

                ) { Text("Set Latest = This App") }

                Button(
                    enabled = !running,
                    onClick = {
                        confirmMessage =
                            "Set Minimum to THIS app version? All older versions will be blocked."
                        pendingConfirm = {
                            running = true
                            scope.launch {
                                try {
                                    ZionMigrator.setMinToInstalled(context) { msg -> log = msg }
                                } catch (e: Exception) {
                                    log = "Set min error: ${e.message}"
                                } finally {
                                    running = false
                                }
                            }
                        }
                        showConfirm = true
                    }

                ) { Text("Set Min = This App") }


                Button(
                    enabled = !running,
                    onClick = {
                        confirmMessage =
                            "Clear Force Update? Users above the minimum version will be allowed."
                        pendingConfirm = {
                            running = true
                            scope.launch {
                                try {
                                    ZionMigrator.setForceUpdateDefault(
                                        context = context,
                                        force = false,
                                        message = null,
                                        downloadUrl = downloadUrlText.takeIf { it.isNotBlank() }
                                    ) { msg -> log = msg }
                                } catch (e: Exception) {
                                    log = "Clear force error: ${e.message}"
                                } finally {
                                    running = false
                                }
                            }
                        }
                        showConfirm = true
                    }

                ) { Text("Clear Force Update") }

            }

            Spacer(Modifier.height(16.dp))
            Text(log)
        }

        /* --------- Confirmations --------- */

        if (confirmCopy) {
            AlertDialog(
                onDismissRequest = { confirmCopy = false },
                title = { Text("Confirm Copy") },
                text = {
                    Text(
                        "This will write cached data into:\n" +
                                "• Project ID: $projectId\n" +
                                "• App ID: $applicationId\n\n" +
                                "Only documents currently in your device cache will be copied.\nProceed?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmCopy = false
                        // ✅ use the scope/context captured above (no remember/LocalContext here)
                        if (!running) {
                            running = true
                            scope.launch {
                                try {
                                    ZionMigrator.copyAllToTarget(
                                        context = context,
                                        projectId = projectId,
                                        applicationId = applicationId,
                                        apiKey = apiKey
                                    ) { msg -> log = msg }
                                } catch (e: Exception) {
                                    log = "Copy error: ${e.message}"
                                } finally {
                                    running = false
                                }
                            }
                        }
                    }) { Text("Yes, copy") }
                },
                dismissButton = { TextButton(onClick = { confirmCopy = false }) { Text("Cancel") } }
            )
        }

        if (confirmExport) {
            AlertDialog(
                onDismissRequest = { confirmExport = false },
                title = { Text("Confirm Export") },
                text = { Text("This will export all cached documents to a .jsonl file. Proceed?") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmExport = false
                        // ✅ just trigger the launcher; no composable calls here
                        createDoc.launch("zionkids-export.jsonl")
                    }) { Text("Yes, export") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        confirmExport = false
                    }) { Text("Cancel") }
                }
            )
        }

        if (confirmRestore) {
            AlertDialog(
                onDismissRequest = { confirmRestore = false },
                title = { Text("Confirm Restore") },
                text = {
                    Text(
                        "This will read a .jsonl backup and write documents into:\n" +
                                "• Project ID: $projectId\n" +
                                "• App ID: $applicationId\n\n" +
                                "Existing docs at the same paths will be overwritten.\nProceed?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmRestore = false
                        // ✅ just open the picker; no composable APIs here
                        openDoc.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }) { Text("Yes, restore") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        confirmRestore = false
                    }) { Text("Cancel") }
                }
            )
        }

        BulkConfirmDialog(
            show = showConfirm,
            onDismiss = {
                showConfirm = false
                pendingConfirm = null
            },
            onConfirm = {
                val action = pendingConfirm
                showConfirm = false
                pendingConfirm = null
                action?.invoke()
            },
            message = confirmMessage
        )

    }

}
/* =========================================== */
/* ============ MIGRATOR LOGIC =============== */
/* =========================================== */

object ZionMigrator {

    private fun oldDb(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        db.disableNetwork() // force CACHE reads from the locked project
        return db
    }

    private fun initTargetDb(
        context: Context,
        projectId: String,
        applicationId: String,
        apiKey: String,
        appName: String = "targetApp"
    ): FirebaseFirestore {
        val options = FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .build()

        val app = try {
            FirebaseApp.getInstance(appName)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(context, options, appName)
        }
        return FirebaseFirestore.getInstance(app)
    }

    private suspend fun Query.getFromCache(): QuerySnapshot =
        get(Source.CACHE).await()

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    suspend fun copyAllToTarget(
        context: Context,
        projectId: String,
        applicationId: String,
        apiKey: String,
        log: (String) -> Unit = {}
    ) {
        val srcDb = oldDb() // cache-only
        val dstDb = initTargetDb(context, projectId, applicationId, apiKey)
        val batcher = BatchWriter(dstDb)

        for (coll in ROOTS) {
            log("Copying collection: $coll ...")
            val snap = srcDb.collection(coll).getFromCache()
            log("Found ${snap.size()} docs in cache for $coll")
            for (doc in snap.documents) {
                val data = doc.data ?: continue
                batcher.set(dstDb.document(doc.reference.path), data)

                val subs = SUBCOLLS[coll] ?: emptyList()
                for (sub in subs) {
                    val subSnap = doc.reference.collection(sub).getFromCache()
                    for (subDoc in subSnap.documents) {
                        val subData = subDoc.data ?: continue
                        batcher.set(dstDb.document(subDoc.reference.path), subData)
                    }
                }
            }
        }
        batcher.flush()
        log("Copy complete ✅")
    }

    suspend fun exportAllToJsonl(
        context: Context,
        outputUri: Uri,
        log: (String) -> Unit = {}
    ) {
        val db = oldDb()
        withContext(Dispatchers.IO) {
            val os: OutputStream = context.contentResolver.openOutputStream(outputUri)
                ?: error("Unable to open output stream")
            os.bufferedWriter().use { writer ->
                fun writeLine(s: String) = writer.append(s).append('\n')

                for (coll in ROOTS) {
                    log("Exporting $coll ...")
                    val snap = db.collection(coll).getFromCache()
                    log("Found ${snap.size()} docs in $coll")
                    for (doc in snap.documents) {
                        val data = doc.data ?: emptyMap<String, Any?>()
                        writeLine(
                            JSONObject()
                                .put("path", doc.reference.path)
                                .put("id", doc.id)
                                .put("data", JSONObject(data))
                                .toString()
                        )
                        val subs = SUBCOLLS[coll] ?: emptyList()
                        for (sub in subs) {
                            val subSnap = doc.reference.collection(sub).get(Source.CACHE).await()
                            for (subDoc in subSnap.documents) {
                                val subData = subDoc.data ?: emptyMap<String, Any?>()
                                writeLine(
                                    JSONObject()
                                        .put("path", subDoc.reference.path)
                                        .put("id", subDoc.id)
                                        .put("data", JSONObject(subData))
                                        .toString()
                                )
                            }
                        }
                    }
                }
            }
        }
        log("Export complete ✅")
    }

    suspend fun restoreFromJsonl(
        context: Context,
        inputUri: Uri,
        projectId: String,
        applicationId: String,
        apiKey: String,
        log: (String) -> Unit = {}
    ) {
        val dstDb = initTargetDb(context, projectId, applicationId, apiKey)
        val batcher = BatchWriter(dstDb)

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(inputUri).use { input ->
                requireNotNull(input) { "Could not open input stream" }
                BufferedReader(InputStreamReader(input)).useLines { lines ->
                    var count = 0
                    lines.forEach { line ->
                        if (line.isBlank()) return@forEach
                        val json = JSONObject(line)
                        val path = json.getString("path")
                        val dataObj = json.getJSONObject("data")
                        val map = jsonToMap(dataObj)
                        batcher.set(dstDb.document(path), map)
                        count++
                        if (count % 1000 == 0) log("Queued $count docs...")
                    }
                }
            }
        }
        batcher.flush()
        log("Restore complete ✅")
    }

    private fun jsonToAny(any: Any?): Any? = when (any) {
        is JSONObject -> jsonToMap(any)
        is JSONArray -> (0 until any.length()).map { i -> jsonToAny(any.get(i)) }
        JSONObject.NULL -> null
        else -> any
    }

    private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            out[k] = jsonToAny(obj.get(k))
        }
        return out
    }

    private class BatchWriter(private val db: FirebaseFirestore) {
        private var batch = db.batch()
        private var count = 0
        private val limit = 450

        suspend fun set(ref: DocumentReference, data: Map<String, Any?>) {
            batch.set(ref, data)
            count++
            if (count >= limit) commitReset()
        }

        suspend fun flush() {
            if (count > 0) commitReset()
        }

        private suspend fun commitReset() {
            batch.commit().await()
            batch = db.batch()
            count = 0
        }
    }

    // ADD inside object ZionMigrator (no removals)

    // Toggle `force` on /appConfig/mobile for the DEFAULT app (this build's Firebase app)
    suspend fun setForceUpdateDefault(
        context: Context,
        force: Boolean,
        message: String? = null,
        latestVersionCode: String? = null,  // your AppUpdateConfig stores version codes as String
        downloadUrl: String? = null,
        log: (String) -> Unit = {}
    ) {
        val db = FirebaseFirestore.getInstance()
        setForceUpdateInternal(db, force, message, latestVersionCode, downloadUrl, log)
    }

    // Toggle `force` on /appConfig/mobile for a TARGET project (from the text fields)
    suspend fun setForceUpdateOnTarget(
        context: Context,
        projectId: String,
        applicationId: String,
        apiKey: String,
        force: Boolean,
        message: String? = null,
        latestVersionCode: String? = null,
        downloadUrl: String? = null,
        log: (String) -> Unit = {}
    ) {
        val db = initTargetDb(context, projectId, applicationId, apiKey)
        setForceUpdateInternal(db, force, message, latestVersionCode, downloadUrl, log)
    }

    // Shared writer
    private suspend fun setForceUpdateInternal(
        db: FirebaseFirestore,
        force: Boolean,
        message: String? = null,
        latestVersionCode: String? = null,
        downloadUrl: String? = null,
        log: (String) -> Unit
    ) {
        val ref = db.collection("appConfig").document("mobile")
        val payload = mutableMapOf<String, Any>(
            "force" to force,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        message?.let { payload["forceMessage"] = it }
        latestVersionCode?.let { payload["latestVersionCode"] = it }  // keep as String to match your model
        downloadUrl?.let { payload["downloadUrl"] = it }

        ref.set(payload, SetOptions.merge()).await()
        log(if (force) "Force update ENABLED ✅" else "Force update CLEARED ✅")
    }

    //for setting the version numbers
    // === Simple admin actions: set gate to the version installed on this device ===

    // Get the installed app's versionCode as Int
    private fun installedVersionCode(ctx: Context): Int {
        val pm = ctx.packageManager
        val pkg = pm.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toInt() else @Suppress("DEPRECATION") pkg.versionCode
    }

    /** Set latestVersionCode to the version installed on THIS device. */
    suspend fun setLatestToInstalled(
        context: Context,
        downloadUrl: String? = null,
        log: (String) -> Unit = {}
    ) {
        val latest = installedVersionCode(context).toString()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("appConfig").document("mobile")
        val payload = mutableMapOf<String, Any>(
            "latestVersionCode" to latest,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        downloadUrl?.let { payload["downloadUrl"] = it }
        ref.set(payload, SetOptions.merge()).await()
        log("latestVersionCode set to $latest ✅")
    }

    /** Set minVersionCode to the version installed on THIS device (hard cutover). */
    suspend fun setMinToInstalled(
        context: Context,
        log: (String) -> Unit = {}
    ) {
        val min = installedVersionCode(context).toString()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("appConfig").document("mobile")
        ref.set(
            mapOf(
                "minVersionCode" to min,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        log("minVersionCode set to $min ✅")
    }


}
