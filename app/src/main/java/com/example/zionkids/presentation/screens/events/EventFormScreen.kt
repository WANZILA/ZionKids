package com.example.zionkids.presentation.screens.events

import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zionkids.core.utils.DatesUtils
import com.example.zionkids.data.model.EventStatus
import com.example.zionkids.presentation.viewModels.events.EventFormViewModel
import com.example.zionkids.presentation.viewModels.events.EventFormUIState
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventIdArg: String?,
    onFinished: (String) -> Unit,
    navigateUp: () -> Unit,
    vm: EventFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ui by vm.ui.collectAsState()  // ✅ this is valid now

    val snackbarHostState = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is EventFormViewModel.EventFormEvent.Saved -> {
                    onFinished(ev.id)
                    navigateUp()
                }
                is EventFormViewModel.EventFormEvent.Error -> {
                    // maybe show Snackbar
                }
            }
        }
    }

    LaunchedEffect(eventIdArg) {
        if (eventIdArg.isNullOrBlank()) vm.ensureNewIdIfNeeded() else vm.loadForEdit(eventIdArg)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (eventIdArg.isNullOrBlank()) "Create Event" else "Edit Event") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { vm.save()
                        },
                        enabled = !(ui.loading || ui.saving)
                    ) { Text("Save") }
                }
            }
        }
    ) { inner ->
        Column(
            modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            EventFields(ui = ui, vm = vm)

            ui.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(64.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFields(
    ui: EventFormUIState,
    vm: EventFormViewModel
) {
    OutlinedTextField(
        value = ui.title ?: "",
        onValueChange = vm::onTitle,
        label = { Text("Title*") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )

    EventDateField(
        ui = ui,
        onDatePicked = vm::onDatePicked
    )

    EnumDropdown(
        title = "Status",
        selected = ui.eventStatus,
        values = EventStatus.values().toList(),
        onSelected = vm::onStatus
    )

    OutlinedTextField(
        value = ui.location ?: "",
        onValueChange = vm::onLocation,
        label = { Text("Location") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = ui.notes ?: "",
        onValueChange = vm::onNotes,
        label = { Text("Notes") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateField(ui: EventFormUIState, onDatePicked: (Timestamp) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = ui.eventDate.toDate().time // Timestamp → millis for M3 picker
    )

    OutlinedTextField(
        value = if (Build.VERSION.SDK_INT >= VERSION_CODES.O)
            DatesUtils.formatDate(ui.eventDate.toDate().time)
        else
            ui.eventDate.toDate().time.toString(),
        onValueChange = { /* read-only */ },
        readOnly = true,
        label = { Text("Event Date") },
        trailingIcon = { TextButton(onClick = { showDatePicker = true }) { Text("Pick") } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        onDatePicked(Timestamp(Date(millis))) // millis → Timestamp ✅
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.name) },
                    onClick = { onSelected(v); expanded = false }
                )
            }
        }
    }
}
