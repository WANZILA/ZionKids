@file:Suppress("NAME_SHADOWING")

package com.example.zionkids.presentation.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.example.zionkids.core.utils.DatesUtils
import com.example.zionkids.data.model.*
import com.example.zionkids.presentation.viewModels.children.ChildFormUiState
import com.example.zionkids.presentation.viewModels.children.ChildFormViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildFormScreen(
    childIdArg: String?,               // null = create, not null = edit
    onFinished: (String) -> Unit,      // called after successful save
    onSave: () -> Unit,
    navigateUp: () -> Unit,
    toList:() -> Unit,
    vm: ChildFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ui = vm.ui
    val step = vm.step
    val state = vm.ui
    val snackbarHostState = remember { SnackbarHostState() }
    val savingOrLoading = state.saving || state.loading

    LaunchedEffect(childIdArg) {
        if (childIdArg.isNullOrBlank()) {
            vm.ensureNewIdIfNeeded()
        } else {
            vm.loadForEdit(childIdArg)
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is ChildFormViewModel.ChildFormEvent.Saved -> {
                    snackbarHostState.showSnackbar("Saved: ${ev.id}")
                    onFinished(ev.id)
//                    navigateUp()
                }
                is ChildFormViewModel.ChildFormEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    if (childIdArg.isNullOrBlank()) Text("Register Child") else Text("Edit Child")
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = toList) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            StepNavBar(
                step = step,
                onBack = { vm.goBack() },
                onNext = {
                    if (step == RegistrationStatus.SPIRITUAL) {
                        vm.jumpToStep(RegistrationStatus.COMPLETE)
                        vm.finish()
                        onFinished(vm.ui.childId)
                    } else vm.goNext()
                },
                onSave = {
                    vm.finish()
                    navigateUp()
                },
                nextLabel = if (step == RegistrationStatus.SPIRITUAL) "Finish" else "Next",
                backEnabled = step != RegistrationStatus.BASICINFOR,
                actionsEnabled = !savingOrLoading
            )
        }
    ) { inner ->
        Column(
            modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            StepHeader(step = step, onStepClicked = vm::jumpToStep)
            Spacer(Modifier.height(12.dp))

            when (step) {
                RegistrationStatus.BASICINFOR -> StepBasicInfo(uiState = ui, vm = vm)
                RegistrationStatus.BACKGROUND -> StepBackground(uiState = ui, vm = vm)
                RegistrationStatus.EDUCATION  -> StepEducation(uiState = ui, vm = vm)
                RegistrationStatus.FAMILY     -> StepFamily(uiState = ui, vm = vm)
                RegistrationStatus.SPIRITUAL  -> StepSpiritual(uiState = ui, vm = vm)
                RegistrationStatus.COMPLETE   -> StepComplete(uiState = ui)
            }

            if (ui.loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            ui.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StepNavBar(
    step: RegistrationStatus,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    nextLabel: String,
    backEnabled: Boolean,
    actionsEnabled: Boolean
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onSave, enabled = actionsEnabled) { Text("Save") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack, enabled = actionsEnabled && backEnabled) { Text("Back") }
            Button(onClick = onNext, enabled = actionsEnabled) { Text(nextLabel) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepHeader(
    step: RegistrationStatus,
    onStepClicked: (RegistrationStatus) -> Unit
) {
    val steps = listOf(
        RegistrationStatus.BASICINFOR to "Basic",
        RegistrationStatus.BACKGROUND to "Background",
        RegistrationStatus.EDUCATION to "Education",
        RegistrationStatus.FAMILY to "Family",
        RegistrationStatus.SPIRITUAL to "Spiritual"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEach { (s, label) ->
            FilterChip(
                selected = step == s,
                onClick = { onStepClicked(s) },
                label = { Text(label) }
            )
        }
    }
}

private fun RegistrationStatus.readable(): String = when (this) {
    RegistrationStatus.BASICINFOR -> "Basic Info"
    RegistrationStatus.BACKGROUND -> "Background"
    RegistrationStatus.EDUCATION  -> "Education"
    RegistrationStatus.FAMILY     -> "Family"
    RegistrationStatus.SPIRITUAL  -> "Spiritual"
    RegistrationStatus.COMPLETE   -> "Complete"
}

// -------------------- STEP 1 --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepBasicInfo(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Picker
        var showDatePicker by remember { mutableStateOf(false) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = uiState.dob)

        OutlinedTextField(
            value = uiState.fName,
            onValueChange = vm::onFirstName,
            label = { Text("First name*") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.lName,
            onValueChange = vm::onLastName,
            label = { Text("Last name*") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.oName ?: "",
            onValueChange = vm::onOtherName,
            label = { Text("Other name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.ageText,
            onValueChange = vm::onAge,
            label = { Text("Age (0–25)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // NEW: DOB & verified
        // Date
        OutlinedTextField(
            value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DatesUtils.formatDate(uiState.dob)
            } else {
                uiState.dob.toString() // fallback, or use SimpleDateFormat
            },
            onValueChange = { /* read-only via picker */ },
            readOnly = true,
            label = { Text("Date (ms since epoch)") },
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val picked = dateState.selectedDateMillis
                        if (picked != null) vm.onDob(picked)
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DOB verified")
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = uiState.dobVerified,
                onCheckedChange = vm::onDobVerified
            )
        }

        OutlinedTextField(
            value = uiState.street,
            onValueChange = vm::onStreet,
            label = { Text("Street / Nearby landmark") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        EnumDropdown(
            title = "Invited by",
            selected = uiState.invitedBy,
            values = Individual.values().toList(),
            onSelected = vm::onInvitedBy
        )
        EnumDropdown(
            title = "Education preference",
            selected = uiState.educationPreference,
            values = EducationPreference.values().toList(),
            onSelected = vm::onEduPref
        )
    }
}

// -------------------- STEP 2 --------------------
@Composable
private fun StepBackground(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.reasonLeftHome ?: "",
            onValueChange = { vm.ui = vm.ui.copy(reasonLeftHome = it) },
            label = { Text("Reason left home") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.reasonLeftSchool ?: "",
            onValueChange = { vm.ui = vm.ui.copy(reasonLeftSchool = it) },
            label = { Text("Reason left school") },
            modifier = Modifier.fillMaxWidth()
        )
        EnumDropdown(
            title = "Wants to go home?",
            selected = uiState.homePreference,
            values = Reply.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(homePreference = it) }
        )
    }
}

// -------------------- STEP 3 --------------------
@Composable
private fun StepEducation(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = uiState.lastClass ?: "",
            onValueChange = { vm.ui = vm.ui.copy(lastClass = it) },
            label = { Text("Last class") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.previousSchool ?: "",
            onValueChange = { vm.ui = vm.ui.copy(previousSchool = it) },
            label = { Text("Previous school") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -------------------- STEP 4 --------------------
@Composable
private fun StepFamily(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // New: Resettlement fields
        Text("Resettlement", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.region ?: "",
            onValueChange = { vm.ui = vm.ui.copy(region = it) },
            label = { Text("Region") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.district ?: "",
            onValueChange = { vm.ui = vm.ui.copy(district = it) },
            label = { Text("District") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.county ?: "",
            onValueChange = { vm.ui = vm.ui.copy(county = it) },
            label = { Text("County") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.subCounty ?: "",
            onValueChange = { vm.onSubCounty(it) },
            label = { Text("Sub-county") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.parish ?: "",
            onValueChange = { vm.ui = vm.ui.copy(parish = it) },
            label = { Text("Parish") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.village ?: "",
            onValueChange = { vm.ui = vm.ui.copy(village = it) },
            label = { Text("Village") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Text("Program", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reunited with family")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.reunitedWithFamily, onCheckedChange = vm::onReunited)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sponsored for education")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.sponsoredForEducation, onCheckedChange = vm::onSponsored)
        }
        OutlinedTextField(
            value = uiState.sponsorId ?: "",
            onValueChange = vm::onSponsorId,
            label = { Text("Sponsor ID (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.sponsorNotes ?: "",
            onValueChange = vm::onSponsorNotes,
            label = { Text("Sponsor notes") },
            modifier = Modifier.fillMaxWidth()
        )

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Primary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName1 ?: "",
            last = uiState.memberLName1 ?: "",
            onFirst = { vm.ui = vm.ui.copy(memberFName1 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName1 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship1,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship1 = it) }
        )
        PhoneRow(
            a = uiState.telephone1a ?: "",
            b = uiState.telephone1b ?: "",
            onA = { vm.ui = vm.ui.copy(telephone1a = it.filter { it.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone1b = it.filter { it.isDigit() }) }
        )

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Secondary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName2 ?: "",
            last = uiState.memberLName2 ?: "",
            onFirst = { vm.ui = vm.ui.copy(memberFName2 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName2 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship2,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship2 = it) }
        )
        PhoneRow(
            a = uiState.telephone2a ?: "",
            b = uiState.telephone2b ?: "",
            onA = { vm.ui = vm.ui.copy(telephone2a = it.filter { it.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone2b = it.filter { it.isDigit() }) }
        )

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Tertiary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName3 ?: "",
            last = uiState.memberLName3 ?: "",
            onFirst = { vm.ui = vm.ui.copy(memberFName3 = it) },
            onLast = { vm.ui = vm.ui.copy(memberLName3 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship3,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship3 = it) }
        )
        PhoneRow(
            a = uiState.telephone3a ?: "",
            b = uiState.telephone3b ?: "",
            onA = { vm.ui = vm.ui.copy(telephone3a = it.filter { it.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone3b = it.filter { it.isDigit() }) }
        )
    }
}

// -------------------- STEP 5 --------------------
@Composable
private fun StepSpiritual(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EnumDropdown(
            title = "Accepted Jesus?",
            selected = uiState.acceptedJesus,
            values = Reply.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(acceptedJesus = it) }
        )
        EnumDropdown(
            title = "Who prayed with them?",
            selected = uiState.whoPrayed,
            values = Individual.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(whoPrayed = it) }
        )
        OutlinedTextField(
            value = uiState.outcome ?: "",
            onValueChange = { vm.ui = vm.ui.copy(outcome = it) },
            label = { Text("Notes / outcome") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -------------------- STEP 6 --------------------
@Composable
private fun StepComplete(uiState: ChildFormUiState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.childId.isNotBlank())
            Text("Ready to save updates for #${uiState.childId}")
        else
            Text("Review all details, then tap Save")
    }
}

// -------------------- SMALL REUSABLES --------------------
@Composable
private fun NameRow(first: String, last: String, onFirst: (String) -> Unit, onLast: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = first,
            onValueChange = onFirst,
            label = { Text("First name") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = last,
            onValueChange = onLast,
            label = { Text("Last name") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PhoneRow(a: String, b: String, onA: (String) -> Unit, onB: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = a,
            onValueChange = onA,
            label = { Text("Phone 1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = b,
            onValueChange = onB,
            label = { Text("Phone 2") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.name) },
                    onClick = {
                        onSelected(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
