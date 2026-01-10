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

// /// CHANGED: Paging compose imports (non-breaking)
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems

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

    // /// CHANGED: collect paging + search count (keeps old pipeline intact as fallback)
    val pagedChildren: LazyPagingItems<com.example.zionkids.data.model.Child> =
        vm.childrenPaging.collectAsLazyPagingItems()
    val searchCount by vm.searchCount.collectAsStateWithLifecycle(initialValue = 0)

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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { /* unchanged */ }
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

            // /// CHANGED: tiny helper row with live count for current search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = {}, label = { Text("Matches: $searchCount") })
                if (total > 0) {
                    AssistChip(onClick = {}, label = { Text("P: ${visible.count { it.present }}") })
                    AssistChip(onClick = {}, label = { Text("A: ${visible.size - visible.count { it.present }}") })
                }
            }

            // Stats chips (one line, small) — unchanged
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
                    // /// CHANGED: Prefer paged list when it has items; else fallback to previous non-paged pipeline
                    pagedChildren.itemCount > 0 -> {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pagedChildren.itemCount) { index ->
                                val child = pagedChildren[index]
                                if (child != null) {
                                    // Map current child → RosterChild using ui snapshot (O(n) search kept small by paging 50/page)
                                    val rc = ui.children.firstOrNull { it.child.childId == child.childId }
                                        ?: RosterChild(child = child, attendance = null, present = false)

                                    AttendanceRow(
                                        rosterChild = rc,
                                        onToggle = { vm.toggleAttendance(eventId, rc, adminId) },
                                        onNotesChange = { notes -> vm.updateNotes(eventId, rc, adminId, notes) }
                                    )
                                } else {
                                    // simple placeholder while paging loads
                                    ElevatedCard(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { Text("Loading…") }
                                    }
                                }
                            }

                            // Optional: list end spacer so FAB/snackbars don’t cover last row
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                    visible.isEmpty() && query.isNotBlank() ->
                        Text("No matches for “$query”.", Modifier.align(Alignment.Center))
                    visible.isEmpty() ->
                        Text("No children to show.", Modifier.align(Alignment.Center))
                    else -> {
                        val listState = rememberLazyListState()
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
