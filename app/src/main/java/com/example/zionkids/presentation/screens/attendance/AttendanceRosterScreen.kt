package com.example.zionkids.presentation.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.example.zionkids.presentation.screens.events.dayFmt
import com.example.zionkids.presentation.screens.widgets.BulkConfirmDialog
import com.example.zionkids.presentation.viewModels.attendance.AttendanceRosterViewModel
import com.example.zionkids.presentation.viewModels.attendance.RosterChild
import com.example.zionkids.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRosterScreen(
    eventId: String,
    adminId: String,
    navigateUp: () -> Unit,
    vm: AttendanceRosterViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bulkBusy by vm.bulkMode.collectAsStateWithLifecycle()
    val authUi by authVM.ui.collectAsStateWithLifecycle()

    // load once
    LaunchedEffect(eventId) { vm.load(eventId) }

    // snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is AttendanceRosterViewModel.UiEvent.Saved -> {
                    snackbarHostState.showSnackbar(if (ev.pendingSync) "Saved (pending sync)" else "Saved")
                }
            }
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    val visible = ui.children
    val total = visible.size
    val presentCount = visible.count { it.present }
    val absentCount = total - presentCount

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var confirmAction: (() -> Unit)? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Attendance")
//                        if (total > 0) {
//                            Text(
//                                "$presentCount / $total present",
//                                style = MaterialTheme.typography.labelMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        modifier = Modifier.padding(end = 8.dp)
//                    ) {
//                        if (ui.isOffline) AssistChip(onClick = {}, label = { Text("Offline data") })
//                        if (ui.isSyncing) AssistChip(onClick = {}, label = { Text("Syncing…") })
//                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = ui.eventTitle ?: "Loading…",
                        style = MaterialTheme.typography.displaySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(ui.eventDate) ?: "Loading…",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    vm.onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search children…") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                )
            )

            // Bulk bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if(authUi.perms.canMakeAllPresent){
                    Button(
                        onClick = {
                            dialogMessage = "Are you sure you want to make ALL children present?"
                            confirmAction = { vm.markAllPresent(eventId, adminId) }
                            showDialog = true
                        },
                        enabled = !ui.loading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (ui.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Mark Present")
                        }
                    }
                }


                if(authUi.perms.canMakeAllAbsent){
                    OutlinedButton(
                        onClick = {
                            dialogMessage = "Are you sure you want to make ALL children absent?"
                            confirmAction = { vm.markAllAbsent(eventId, adminId) }
                            showDialog = true
                        },
                        enabled = !ui.loading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (ui.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Mark Absent")
                        }
                    }
                }
            }


            BulkConfirmDialog(
                show = showDialog,
                onDismiss = {
//                    if (!bulkBusy)
                    showDialog = false  },
                onConfirm = {
                    confirmAction?.invoke()
                    showDialog = false
                },
                message = dialogMessage
            )
//            if (bulkBusy) {
//                LinearProgressIndicator(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 12.dp, vertical = 4.dp)
//                )
//            }

            // Stats chips (one line, small)
            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                        label = { Text("P: $presentCount") }
                    )
                    AssistChip(
                        onClick = {},
                        leadingIcon = { Icon(Icons.Outlined.Circle, null) },
                        label = { Text("A: $absentCount") }
                    )
                    AssistChip(onClick = {}, label = { Text("All: $total") })
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        text = ui.error ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    visible.isEmpty() && query.isNotBlank() ->
                        Text("No matches for “$query”.", Modifier.align(Alignment.Center))
                    visible.isEmpty() ->
                        Text("No children to show.", Modifier.align(Alignment.Center))
                    else -> {
                        val listState = rememberLazyListState()


////
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                visible,
                                key = { "${it.child.childId}:${it.attendance?.attendanceId ?: ""}" },
                                contentType = { "row" }
                            ) { rc ->
                                AttendanceRow(
                                    rosterChild = rc,
                                    onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
                                    onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(
    rosterChild: RosterChild,
    onToggle: () -> Unit,
    onNotesChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }

    var localNotes by rememberSaveable(rosterChild.child.childId) {
        mutableStateOf(rosterChild.attendance?.notes.orEmpty())
    }

    // KISS: editor visible only when Absent; reopen via "Add/Edit note" button
    var showNotes by remember {
        mutableStateOf(!rosterChild.present && rosterChild.attendance?.notes.isNullOrBlank())
    }

    LaunchedEffect(rosterChild.attendance?.notes) {
        if (!isEditing) localNotes = rosterChild.attendance?.notes.orEmpty()
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rosterChild.child.fName.trim(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = rosterChild.child.lName.trim(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val line2 = buildString {
                        if (rosterChild.child.street.isNotBlank())
                            append("Street: ${rosterChild.child.street}")
                    }
                    if (line2.isNotBlank()) {
                        Text(
                            text = line2,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        onToggle()
                        // if switching to Absent, open editor if no note yet; if to Present, hide editor
                        showNotes = !rosterChild.present && localNotes.isBlank()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (rosterChild.present) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Present")
                        Spacer(Modifier.width(6.dp)); Text("Present")
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "Absent")
                        Spacer(Modifier.width(6.dp)); Text("Absent")
                    }
                }
            }

            if (!rosterChild.present) {
                Spacer(Modifier.height(8.dp))

                // KISS toggle button for editor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showNotes = !showNotes }) {
                        Text(if (showNotes) "Hide note" else if (localNotes.isBlank()) "Add note" else "Edit note")
                    }
                }

                if (showNotes) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = localNotes,
                        onValueChange = { localNotes = it },
                        placeholder = { Text("Reason for absence…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                val lostFocus = !it.isFocused && isEditing
                                isEditing = it.isFocused
                                if (lostFocus) {
                                    onNotesChange(localNotes)   // save on blur
                                    showNotes = false           // hide after blur
                                }
                            },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onNotesChange(localNotes)       // save on Done
                                keyboardController?.hide()
                                showNotes = false               // hide after Done
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )
                }
            }
        }
    }
}


private fun formatDate(ts: Timestamp): String = dayFmt.format(ts.toDate())
private val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

//package com.example.zionkids.presentation.screens.attendance
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.outlined.CheckCircle
//import androidx.compose.material.icons.outlined.Circle
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
//import androidx.compose.ui.focus.onFocusChanged
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.example.zionkids.data.model.AttendanceStatus
//import com.example.zionkids.presentation.viewModels.attendance.AttendanceRosterViewModel
//import com.example.zionkids.presentation.viewModels.attendance.RosterChild
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AttendanceRosterScreen(
//    eventId: String,
//    adminId: String,
//    navigateUp: () -> Unit,
//    vm: AttendanceRosterViewModel = hiltViewModel()
//) {
//    val ui by vm.ui.collectAsStateWithLifecycle()
//
//    // load once for this event
//    LaunchedEffect(eventId) { vm.load(eventId) }
//
//    val snackbarHostState = remember { SnackbarHostState() }
//    val scope = rememberCoroutineScope()
//
//    // listen for one-off events
//    LaunchedEffect(Unit) {
//        vm.events.collect { ev ->
//            when (ev) {
//                is AttendanceRosterViewModel.UiEvent.Saved -> {
//                    val msg = if (ev.pendingSync) "Saved (pending sync)" else "Saved"
//                    snackbarHostState.showSnackbar(message = msg)
//                }
//            }
//        }
//    }
//
//    // local text for the field only; VM owns actual filter via onSearchQueryChange()
//    var query by rememberSaveable { mutableStateOf("") }
//
//    // list is already filtered by the VM
//    val visible = ui.children
//    val total = visible.size
//    val presentCount = visible.count { it.present }
//    val absentCount = total - presentCount
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = {
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Text("Attendance")
//                        if (total > 0) {
//                            Text(
//                                "$presentCount / $total present",
//                                style = MaterialTheme.typography.labelMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    }
//                },
//                navigationIcon = {
//                    IconButton(onClick = navigateUp) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
////                    Row(
////                        verticalAlignment = Alignment.CenterVertically,
////                        horizontalArrangement = Arrangement.spacedBy(8.dp),
////                        modifier = Modifier.padding(end = 8.dp)
////                    ) {
////                        if (ui.isOffline) {
////                            AssistChip(onClick = {}, label = { Text("Offline data") })
////                        }
////                        if (ui.isSyncing) {
////                            AssistChip(onClick = {}, label = { Text("Syncing…") })
////                        }
////                    }
//                }
//            )
//        }
//    ) { inner ->
//        Column(
//            Modifier
//                .fillMaxSize()
//                .padding(inner)
//        ) {
//            ElevatedCard(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 16.dp, start = 12.dp, end = 12.dp)
//            ) {
//                Column(Modifier.padding(12.dp)) {
//                    Text(
//                        text = ui.eventTitle ?: "Loading…",
//                        style = MaterialTheme.typography.titleLarge,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                    if (total > 0) {
//                        Text(
//                            "$presentCount present • $absentCount absent • $total total",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//            }
//
//            // Search -> delegate to VM for debounced filtering
//            OutlinedTextField(
//                value = query,
//                onValueChange = {
//                    query = it
//                    vm.onSearchQueryChange(it)
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(12.dp),
//                placeholder = { Text("Search children…") },
//                singleLine = true,
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                )
//            )
//
//            // After the OutlinedTextField (search) …
//            BulkMarkBar(
//                onMarkAllPresent = { vm.markAllPresent(eventId, adminId) },
//                onMarkAllAbsent  = { vm.markAllAbsent(eventId, adminId) }
//            )
//            Spacer(Modifier.height(8.dp))
//
//            Box(Modifier.fillMaxSize()) {
//                when {
//                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
//                    ui.error != null -> Text(
//                        text = ui.error ?: "Error",
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                    visible.isEmpty() && query.isNotBlank() ->
//                        Text("No matches for “$query”.", Modifier.align(Alignment.Center))
//                    visible.isEmpty() ->
//                        Text("No children to show.", Modifier.align(Alignment.Center))
//                    else -> {
//                        val listState = rememberLazyListState()
//                        LazyColumn(
//                            state = listState,
//                            modifier = Modifier.fillMaxSize(),
//                            contentPadding = PaddingValues(12.dp),
//                            verticalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            items(
//                                visible,
//                                key = { "${it.child.childId}:${it.attendance?.attendanceId ?: ""}" }
//                            ) { rc ->
//                                AttendanceRow(
//                                    rosterChild = rc,
//                                    onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
//                                    onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) }
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun BulkMarkBar(
//    onMarkAllPresent: () -> Unit,
//    onMarkAllAbsent: () -> Unit
//) {
//    var confirm by remember { mutableStateOf<AttendanceStatus?>(null) }
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Button(onClick = { confirm = AttendanceStatus.PRESENT }) { Text("Mark all Present") }
//        OutlinedButton(onClick = { confirm = AttendanceStatus.ABSENT }) { Text("Mark all Absent") }
//    }
//
//    if (confirm != null) {
//        AlertDialog(
//            onDismissRequest = { confirm = null },
//            title = { Text("Apply to all?") },
//            text = { Text("This will set every child’s status to ${confirm!!.name}. You can still toggle individuals afterward.") },
//            confirmButton = {
//                TextButton(onClick = {
//                    if (confirm == AttendanceStatus.PRESENT) onMarkAllPresent() else onMarkAllAbsent()
//                    confirm = null
//                }) { Text("Apply") }
//            },
//            dismissButton = {
//                TextButton(onClick = { confirm = null }) { Text("Cancel") }
//            }
//        )
//    }
//}
//
//@Composable
//private fun AttendanceRow(
//    rosterChild: RosterChild,
//    onToggle: () -> Unit,
//    onNotesChange: (String) -> Unit
//) {
//    val keyboardController = LocalSoftwareKeyboardController.current
//    val focusRequester = remember { FocusRequester() }
//    var isEditing by remember { mutableStateOf(false) }
//
//    // local notes text
//    var localNotes by rememberSaveable(rosterChild.child.childId) {
//        mutableStateOf(rosterChild.attendance?.notes.orEmpty())
//    }
//
//    // show/hide notes editor (KISS: only for ABSENT; default hidden unless no note yet)
//    var showNotes by remember {
//        mutableStateOf(!rosterChild.present && rosterChild.attendance?.notes.isNullOrBlank())
//    }
//
//    // keep local notes in sync when Firestore updates and we're not editing
//    LaunchedEffect(rosterChild.attendance?.notes) {
//        if (!isEditing) localNotes = rosterChild.attendance?.notes.orEmpty()
//    }
//
//    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 10.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = "${rosterChild.child.fName} ${rosterChild.child.lName}".trim(),
//                        style = MaterialTheme.typography.titleMedium,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                    val line2 = buildString {
//                        if (rosterChild.child.street.isNotBlank())
//                            append("Street: ${rosterChild.child.street}")
//                    }
//                    if (line2.isNotBlank()) {
//                        Text(
//                            text = line2,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis
//                        )
//                    }
//                }
//
//                FilledTonalButton(
//                    onClick = {
//                        onToggle()
//                        // if switching to ABSENT, allow quick note add; if to PRESENT, hide notes
//                        showNotes = !rosterChild.present && localNotes.isBlank()
//                    },
//                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
//                ) {
//                    if (rosterChild.present) {
//                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Present")
//                        Spacer(Modifier.width(6.dp)); Text("Present")
//                    } else {
//                        Icon(Icons.Outlined.Circle, contentDescription = "Absent")
//                        Spacer(Modifier.width(6.dp)); Text("Absent")
//                    }
//                }
//            }
//
//            if (!rosterChild.present) {
//                Spacer(Modifier.height(8.dp))
//
//                // KISS: tiny button to open/close the editor
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    TextButton(onClick = { showNotes = !showNotes }) {
//                        Text(if (showNotes) "Hide note" else if (localNotes.isBlank()) "Add note" else "Edit note")
//                    }
//                }
//
//                if (showNotes) {
//                    Spacer(Modifier.height(6.dp))
//                    OutlinedTextField(
//                        value = localNotes,
//                        onValueChange = { localNotes = it },
//                        placeholder = { Text("Reason for absence…") },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .focusRequester(focusRequester)
//                            .onFocusChanged {
//                                val lostFocus = !it.isFocused && isEditing
//                                isEditing = it.isFocused
//                                if (lostFocus) {
//                                    onNotesChange(localNotes)   // save on blur
//                                    showNotes = false           // hide after blur
//                                }
//                            },
//                        singleLine = false,
//                        maxLines = 3,
//                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
//                        keyboardActions = KeyboardActions(
//                            onDone = {
//                                onNotesChange(localNotes)       // save on Done
//                                keyboardController?.hide()
//                                showNotes = false               // hide after Done
//                            }
//                        ),
//                        colors = OutlinedTextFieldDefaults.colors(
//                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
//                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
//                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
//                        )
//                    )
//                }
//            }
//        }
//    }
//}
//
//////package com.example.zionkids.presentation.screens.attendance
////package com.example.zionkids.presentation.screens.attendance
////
////import androidx.compose.foundation.layout.*
////import androidx.compose.foundation.lazy.LazyColumn
////import androidx.compose.foundation.lazy.items
////import androidx.compose.foundation.text.KeyboardActions
////import androidx.compose.foundation.text.KeyboardOptions
////import androidx.compose.material.icons.Icons
////import androidx.compose.material.icons.automirrored.outlined.ArrowBack
////import androidx.compose.material.icons.outlined.CheckCircle
////import androidx.compose.material.icons.outlined.Circle
////import androidx.compose.material3.*
////import androidx.compose.runtime.*
////import androidx.compose.runtime.saveable.rememberSaveable
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.focus.FocusRequester
////import androidx.compose.ui.focus.focusRequester
////import androidx.compose.ui.focus.onFocusChanged
////import androidx.compose.ui.platform.LocalSoftwareKeyboardController
////import androidx.compose.ui.text.input.ImeAction
////import androidx.compose.ui.text.style.TextOverflow
////import androidx.compose.ui.unit.dp
////import androidx.hilt.navigation.compose.hiltViewModel
////import androidx.lifecycle.compose.collectAsStateWithLifecycle
////import com.example.zionkids.presentation.viewModels.attendance.AttendanceRosterViewModel
////import com.example.zionkids.presentation.viewModels.attendance.RosterChild
////import androidx.compose.foundation.layout.Arrangement // needed for spacedBy
////import androidx.compose.material.icons.automirrored.filled.ArrowBack
////import androidx.compose.material.icons.filled.ArrowCircleLeft
////
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun AttendanceRosterScreen(
////    eventId: String,
////    adminId: String,
////    navigateUp: () -> Unit,
////    vm: AttendanceRosterViewModel = hiltViewModel()
////) {
////    val ui by vm.ui.collectAsStateWithLifecycle()
////
////    // load once for this event
////    LaunchedEffect(eventId) { vm.load(eventId) }
////
////    // local text for the field only; VM owns actual filter via onSearchQueryChange()
////    var query by rememberSaveable { mutableStateOf("") }
////
////    // list is already filtered by the VM
////    val visible = ui.children
////    val total = visible.size
////    val presentCount = visible.count { it.present }
////    val absentCount = total - presentCount
////
////    Scaffold(
////        topBar = {
////            CenterAlignedTopAppBar(
////                title = {
////                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
////                        Text("Attendance")
////                        if (total > 0) {
////                            Text(
////                                "$presentCount / $total present",
////                                style = MaterialTheme.typography.labelMedium,
////                                color = MaterialTheme.colorScheme.onSurfaceVariant
////                            )
////                        }
////                    }
////                },
////                navigationIcon = {
////                    IconButton(onClick = navigateUp) {
////                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
////                    }
////                },
////                actions = {
//////                    Row(verticalAlignment = Alignment.CenterVertically) {
//////                        if (ui.isOffline) {
//////                            AssistChip(onClick = {}, label = { Text("Offline") })
//////                            Spacer(Modifier.width(8.dp))
//////                        }
//////                        // if (ui.isSyncing) { AssistChip(onClick = {}, label = { Text("Syncing…") }); Spacer(Modifier.width(8.dp)) }
//////                    }
////                }
////            )
////        }
////    ) { inner ->
////        Column(
////            Modifier
////                .fillMaxSize()
////                .padding(inner)
////        ) {
////            ElevatedCard(
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .padding(top = 16.dp)
////            ) {
////                Row {
////                    Text(
////                        text = ui.eventTitle ?: "Loading…",
////                        style = MaterialTheme.typography.titleLarge,
////                        maxLines = 1,
////                        overflow = TextOverflow.Ellipsis
////                    )
////                }
////            }
////
////            // Search -> delegate to VM for debounced filtering
////            OutlinedTextField(
////                value = query,
////                onValueChange = {
////                    query = it
////                    vm.onSearchQueryChange(it)
////                },
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .padding(12.dp),
////                placeholder = { Text("Search children…") },
////                singleLine = true,
////                colors = OutlinedTextFieldDefaults.colors(
////                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
////                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
////                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
////                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
////                )
////            )
////
////            Box(Modifier.fillMaxSize()) {
////                when {
////                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
////                    ui.error != null -> Text(
////                        text = ui.error ?: "Error",
////                        color = MaterialTheme.colorScheme.error,
////                        modifier = Modifier.align(Alignment.Center)
////                    )
////                    visible.isEmpty() -> Text(
////                        "No matches",
////                        modifier = Modifier.align(Alignment.Center)
////                    )
////                    else ->
////                        Column(
////                            verticalArrangement = Arrangement.spacedBy(8.dp),
////                            horizontalAlignment = Alignment.CenterHorizontally
////                        ) {
////                            Row(
////                                horizontalArrangement = Arrangement.spacedBy(8.dp),
////                                verticalAlignment = Alignment.CenterVertically
////                            ) {
////                                AssistChip(
////                                    onClick = {},
////                                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
////                                    label = { Text("Present: $presentCount") }
////                                )
////                                AssistChip(
////                                    onClick = {},
////                                    leadingIcon = { Icon(Icons.Outlined.Circle, null) },
////                                    label = { Text("Absent: $absentCount") }
////                                )
////                            }
////                            Row(
////                                horizontalArrangement = Arrangement.spacedBy(8.dp),
////                                verticalAlignment = Alignment.CenterVertically
////                            ) {
////                                AssistChip(
////                                    onClick = {},
////                                    label = { Text("Total: $total") }
////                                )
////                            }
////
////                            LazyColumn(
////                                modifier = Modifier.fillMaxSize(),
////                                contentPadding = PaddingValues(12.dp),
////                                verticalArrangement = Arrangement.spacedBy(8.dp)
////                            ) {
////                                items(visible, key = { it.child.childId }) { rc ->
////                                    AttendanceRow(
////                                        rosterChild = rc,
////                                        onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
////                                        onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) }
////                                    )
////                                }
////                            }
////                        }
////                }
////            }
////        }
////    }
////}
////
////@Composable
////private fun AttendanceRow(
////    rosterChild: RosterChild,
////    onToggle: () -> Unit,
////    onNotesChange: (String) -> Unit
////) {
////    val keyboardController = LocalSoftwareKeyboardController.current
////    val focusRequester = remember { FocusRequester() }
////    var isEditing by remember { mutableStateOf(false) }
////
////    // Local source of truth for the text field
////    var localNotes by rememberSaveable(rosterChild.child.childId) {
////        mutableStateOf(rosterChild.attendance?.notes.orEmpty())
////    }
////
////    // If Firestore updates while NOT editing, sync local value
////    LaunchedEffect(rosterChild.attendance?.notes) {
////        if (!isEditing) {
////            localNotes = rosterChild.attendance?.notes.orEmpty()
////        }
////    }
////
////    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
////        Column(
////            modifier = Modifier
////                .fillMaxWidth()
////                .padding(horizontal = 12.dp, vertical = 10.dp)
////        ) {
////
////            Row(
////                modifier = Modifier.fillMaxWidth(),
////                verticalAlignment = Alignment.CenterVertically,
////                horizontalArrangement = Arrangement.spacedBy(12.dp)
////            ) {
////                Column(modifier = Modifier.weight(1f)) {
////                    Text(
////                        text = "${rosterChild.child.fName} ${rosterChild.child.lName}".trim(),
////                        style = MaterialTheme.typography.titleMedium,
////                        maxLines = 1,
////                        overflow = TextOverflow.Ellipsis
////                    )
////                    val line2 = buildString {
////                        if (rosterChild.child.street.isNotBlank())
////                            append("Street: ${rosterChild.child.street}")
////                    }
////                    if (line2.isNotBlank()) {
////                        Text(
////                            text = line2,
////                            style = MaterialTheme.typography.bodySmall,
////                            color = MaterialTheme.colorScheme.onSurfaceVariant,
////                            maxLines = 1,
////                            overflow = TextOverflow.Ellipsis
////                        )
////                    }
////                }
////
////                FilledTonalButton(
////                    onClick = onToggle,
////                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
////                ) {
////                    if (rosterChild.present) {
////                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Present")
////                        Spacer(Modifier.width(6.dp))
////                        Text("Present")
////                    } else {
////                        Icon(Icons.Outlined.Circle, contentDescription = "Absent")
////                        Spacer(Modifier.width(6.dp))
////                        Text("Absent")
////                    }
////                }
////            }
////
////            if (!rosterChild.present) {
////                Spacer(Modifier.height(8.dp))
////
////                OutlinedTextField(
////                    value = localNotes,                       // ← local value
////                    onValueChange = { localNotes = it },      // ← update local only
////                    placeholder = { Text("Reason for absence…") },
////                    modifier = Modifier
////                        .fillMaxWidth()
////                        .focusRequester(focusRequester)
////                        .onFocusChanged { isEditing = it.isFocused },
////                    singleLine = false,
////                    maxLines = 3,
////                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
////                    keyboardActions = KeyboardActions(
////                        onDone = {
////                            onNotesChange(localNotes)           // ← push to VM once
////                            keyboardController?.hide()
////                        }
////                    ),
////                    colors = OutlinedTextFieldDefaults.colors(
////                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
////                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
////                        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
////                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
////                    )
////                )
////            }
////        }
////    }
////}
