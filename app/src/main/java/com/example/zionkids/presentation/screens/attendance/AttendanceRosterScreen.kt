//package com.example.zionkids.presentation.screens.attendance
package com.example.zionkids.presentation.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.example.zionkids.presentation.viewModels.attendance.AttendanceRosterViewModel
import com.example.zionkids.presentation.viewModels.attendance.RosterChild
import androidx.compose.foundation.layout.Arrangement // needed for spacedBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRosterScreen(
    eventId: String,
    adminId: String,
    navigateUp: () -> Unit,
    vm: AttendanceRosterViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    // load once for this event
    LaunchedEffect(eventId) { vm.load(eventId) }

    // local text for the field only; VM owns actual filter via onSearchQueryChange()
    var query by rememberSaveable { mutableStateOf("") }

    // list is already filtered by the VM
    val visible = ui.children
    val total = visible.size
    val presentCount = visible.count { it.present }
    val absentCount = total - presentCount

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Attendance")
                        if (total > 0) {
                            Text(
                                "$presentCount / $total present",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ui.isOffline) {
                            AssistChip(onClick = {}, label = { Text("Offline") })
                            Spacer(Modifier.width(8.dp))
                        }
                        // if (ui.isSyncing) { AssistChip(onClick = {}, label = { Text("Syncing…") }); Spacer(Modifier.width(8.dp)) }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row {
                    Text(
                        text = ui.eventTitle ?: "Loading…",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Search -> delegate to VM for debounced filtering
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

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text(
                        text = ui.error ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    visible.isEmpty() -> Text(
                        "No matches",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                                    label = { Text("Present: $presentCount") }
                                )
                                AssistChip(
                                    onClick = {},
                                    leadingIcon = { Icon(Icons.Outlined.Circle, null) },
                                    label = { Text("Absent: $absentCount") }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Total: $total") }
                                )
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(visible, key = { it.child.childId }) { rc ->
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

    // Local source of truth for the text field
    var localNotes by rememberSaveable(rosterChild.child.childId) {
        mutableStateOf(rosterChild.attendance?.notes.orEmpty())
    }

    // If Firestore updates while NOT editing, sync local value
    LaunchedEffect(rosterChild.attendance?.notes) {
        if (!isEditing) {
            localNotes = rosterChild.attendance?.notes.orEmpty()
        }
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
                        text = "${rosterChild.child.fName} ${rosterChild.child.lName}".trim(),
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
                    onClick = onToggle,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (rosterChild.present) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Present")
                        Spacer(Modifier.width(6.dp))
                        Text("Present")
                    } else {
                        Icon(Icons.Outlined.Circle, contentDescription = "Absent")
                        Spacer(Modifier.width(6.dp))
                        Text("Absent")
                    }
                }
            }

            if (!rosterChild.present) {
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = localNotes,                       // ← local value
                    onValueChange = { localNotes = it },      // ← update local only
                    placeholder = { Text("Reason for absence…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isEditing = it.isFocused },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onNotesChange(localNotes)           // ← push to VM once
                            keyboardController?.hide()
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
