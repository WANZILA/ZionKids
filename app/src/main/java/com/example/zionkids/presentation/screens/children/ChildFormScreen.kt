@file:Suppress("NAME_SHADOWING")

package com.example.zionkids.presentation.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zionkids.data.model.*
import com.example.zionkids.presentation.screens.widgets.PickerDialog
import com.example.zionkids.presentation.viewModels.children.ChildFormUiState
import com.example.zionkids.presentation.viewModels.children.ChildFormViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

/* ----------------------------------------------------------
 * Top-level Screen
 * ---------------------------------------------------------- */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildFormScreen(
    childIdArg: String?,               // null = create, not null = edit
    onFinished: (String) -> Unit,      // called after successful save
    onSave: () -> Unit,
    navigateUp: () -> Unit,
    toList: () -> Unit,
    vm: ChildFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ui = vm.ui
    val step = vm.step
    val state = vm.ui
    val snackbarHostState = remember { SnackbarHostState() }
    val savingOrLoading = state.saving || state.loading

    LaunchedEffect(childIdArg) {
        if (childIdArg.isNullOrBlank()) vm.ensureNewIdIfNeeded() else vm.loadForEdit(childIdArg)
    }


//    LaunchedEffect(Unit) {
//        vm.events.collectLatest { ev ->
//            when (ev) {
//                is ChildFormViewModel.ChildFormEvent.Saved -> {
//                    snackbarHostState.showSnackbar("Saved: ${ev.id}")
//                    onFinished(ev.id)
//                }
//                is ChildFormViewModel.ChildFormEvent.Error ->
//                    snackbarHostState.showSnackbar(ev.msg)
//            }
//        }
//    }
    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            when (ev) {
                is ChildFormViewModel.ChildFormEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
                is ChildFormViewModel.ChildFormEvent.Saved -> {
                    // show snackbar first (optional), then navigate/finish
                    onFinished(ev.id)
                    snackbarHostState.showSnackbar("Saved ")
                    
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
                        Icon(
                            Icons.Default.ArrowCircleLeft, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                        )
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
                    if (vm.validateStep(step) ){
                        vm.finish()
                        navigateUp()
                    }
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
                RegistrationStatus.SPONSORSHIP    -> StepSponsorship(uiState = ui, vm = vm)
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

/* ----------------------------------------------------------
 * Nav bar & step header
 * ---------------------------------------------------------- */

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
        RegistrationStatus.SPONSORSHIP to "Sponsorship",
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

/* ----------------------------------------------------------
 * Step 1 — Basic Info
 * ---------------------------------------------------------- */
/* ----------------------------------------------------------
 * Step 1 — Basic Info
 * ---------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepBasicInfo(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        val display = vm.ui.street.trim().ifBlank { "Tap to choose Street" }
        var showSkillsDialog by remember {  mutableStateOf(false) }

        // First name (required)
        AppTextField(
            value = uiState.fName,
            onValueChange = vm::onFirstName,
            label = "First name*",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.fName.isNotEmpty()) {
                    IconButton(onClick = { vm.onFirstName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.fNameError,
//            supportingText = if (uiState.fNameError == null) "2–30 letters, spaces, . ’ -" else null
        )

        // Last name (required)
        AppTextField(
            value = uiState.lName,
            onValueChange = vm::onLastName,
            label = "Last name*",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.lName.isNotEmpty()) {
                    IconButton(onClick = { vm.onLastName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.lNameError,
//            supportingText = if (uiState.lNameError == null) "2–30 letters, spaces, . ’ -" else null
        )

        // Other name (optional)
        AppTextField(
            value = uiState.oName,
            onValueChange = vm::onOtherName,
            label = "Other name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            trailingIcon = {
                if (uiState.oName.isNotEmpty()) {
                    IconButton(onClick = { vm.onOtherName("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            }
        )

        EnumDropdown(
            title = "Gender",
            selected = uiState.gender,
            values = Gender.values().toList(),
            onSelected = vm::onGender,
            labelFor = ::labelForGender,
            iconFor = ::iconForGender
        )
        // Age
        AppTextField(
            value = uiState.ageText,
            onValueChange = vm::onAge,
            label = "Age (0–25)",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            leadingIcon = { Icon(Icons.Outlined.Badge, null) },
            trailingIcon = {
                if (uiState.ageText.isNotEmpty()) {
                    IconButton(onClick = { vm.onAge("") }) {
                        Icon(Icons.Outlined.Clear, "Clear")
                    }
                }
            },
            errorText = uiState.ageError,
//            supportingText = if (uiState.ageError == null) "Leave blank if unknown" else null
        )

        // DOB
        AppDateField(
            label = "Date of Birth",
            value = uiState.dob,
            onChanged = vm::onDob,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DOB verified")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.dobVerified, onCheckedChange = vm::onDobVerified)
        }

        // Street
//        AppTextField(
//            value = uiState.street,
//            onValueChange = vm::onStreet,
//            label = "Street / Nearby landmark",
//            modifier = Modifier.fillMaxWidth(),
//            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
//            leadingIcon = { Icon(Icons.Outlined.Home, null) },
//            trailingIcon = {
//                if (uiState.street.isNotEmpty()) {
//                    IconButton(onClick = { vm.onStreet("") }) {
//                        Icon(Icons.Outlined.Clear, "Clear")
//                    }
//                }
//            },
//            errorText = uiState.streetError,
//        )

        AppTextField(
            value = display,
            onValueChange = { /* read-only */ },
            label = "Street ",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSkillsDialog = true },
            leadingIcon = { Icon(Icons.Outlined.PanTool, null) },
            readOnly = true,   // user can’t type
            enabled = false     // but the whole field is tappable

        )

        if (showSkillsDialog) {
            PickerDialog(
                title = "Select Street",
                feature = vm.streetPicker,
                onPicked = { vm.onStreetPicked (it); showSkillsDialog = false },
                onDismiss = { showSkillsDialog = false }
            )
        }

        EnumDropdown(
            title = "Accepted Jesus?",
            selected = uiState.acceptedJesus,
            values = Reply.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(acceptedJesus = it) },
            labelFor = ::labelForReply,
            iconFor = ::iconForReply
        )
        // Invited by (enum)
        EnumDropdown(
            title = "Invited by",
            selected = uiState.invitedBy,
            values = Individual.values().toList(),
            onSelected = vm::onInvitedBy,
            labelFor = ::labelForIndividual,
            iconFor = ::iconForIndividual
        )

        // Invited-by details
//        AppTextField(
//            value = uiState.invitedByIndividualId,
//            onValueChange = { vm.ui = vm.ui.copy(invitedByIndividualId = it) },
//            label = "Invited by (ID)",
//            modifier = Modifier.fillMaxWidth(),
//            leadingIcon = { Icon(Icons.Outlined.Badge, null) }
//        )
        if(uiState.invitedBy.name == Individual.OTHER.toString()){
            AppTextField(
                value = uiState.invitedByTypeOther,
                onValueChange = { vm.ui = vm.ui.copy(invitedByTypeOther = it) },
                label = "Invited by (Other - text)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
            )
        }

        // Education preference (enum)
        EnumDropdown(
            title = "Education preference",
            selected = uiState.educationPreference,
            values = EducationPreference.values().toList(),
            onSelected = vm::onEduPref,
            labelFor = ::labelForEducationPreference,
            iconFor = ::iconForEducationPreference
        )

        EnumDropdown(
            title = "Resettlement preference",
            selected = uiState.resettlementPreference,
            values = ResettlementPreference.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(resettlementPreference = it) },
            labelFor = ::labelForResettlementPreference,
            iconFor = ::iconForResettlementPreference
        )
    }
}

/* ----------------------------------------------------------
 * Step 2 — Background
 * ---------------------------------------------------------- */
@Composable
private fun StepBackground(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTextField(
            value = uiState.reasonLeftHome,
            onValueChange = { vm.ui = vm.ui.copy(reasonLeftHome = it) },
            label = "Reason left home",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Info, null) }
        )


        AppDateField(
            label = "Left home date",
            value = uiState.leftHomeDate,
            onChanged = { vm.ui = vm.ui.copy(leftHomeDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

    }
}

/* ----------------------------------------------------------
 * Step 3 — Education
 * ---------------------------------------------------------- */
@Composable
private fun StepEducation(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()

    val display = vm.ui.technicalSkills.trim().ifBlank { "Tap to choose skill" }
    var showTechSkillDialog by remember {  mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if(uiState.educationPreference.name == EducationPreference.SKILLING.toString()){
//            AppTextField(
//                value = display,
//                onValueChange = { vm.ui = vm.ui.copy(technicalSkills = it) },
//                label = "Select skill",
//                modifier = Modifier.fillMaxWidth(),
//                leadingIcon = { Icon(Icons.Outlined.PanTool, null) }
//            )
            AppTextField(
                value = display,
                onValueChange = { /* read-only */ },
                label = "Select skill",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTechSkillDialog = true },
                leadingIcon = { Icon(Icons.Outlined.PanTool, null) },
                readOnly = true,   // user can’t type
                enabled = false     // but the whole field is tappable

            )

            if (showTechSkillDialog) {
                PickerDialog(
                    title = "Select skill",
                    feature = vm.technicalSkillsPicker,
                    onPicked = { vm.onTechnicalSkillsPicked(it); showTechSkillDialog = false },
                    onDismiss = { showTechSkillDialog = false }
                )
            }
        }

        EnumDropdown(
            title = "Educational Level",
            selected = uiState.educationLevel,
            values = EducationLevel.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(educationLevel = it) },
            labelFor = ::labelForEducationLevel,
            iconFor = ::iconForEducationLevel
        )

        if(
            uiState.educationLevel.name == EducationLevel.NURSERY.toString() ||
            uiState.educationLevel.name == EducationLevel.PRIMARY.toString() ||
            uiState.educationLevel.name == EducationLevel.SECONDARY.toString()
            ){

            AppTextField(
                value = uiState.lastClass,
                onValueChange = { vm.ui = vm.ui.copy(lastClass = it) },
                label = "Last class",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.School, null) }
            )
            AppTextField(
                value = uiState.previousSchool,
                onValueChange = { vm.ui = vm.ui.copy(previousSchool = it) },
                label = "Previous school",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.School, null) }
            )

            EnumDropdown(
                title = "Who was your Sponsor",
                selected = uiState.formerSponsor,
                values = Relationship.values().toList(),
                onSelected = { vm.ui = vm.ui.copy(formerSponsor = it) },
                labelFor = ::labelForRelationship,
                iconFor = ::iconForRelationship
            )

            if(uiState.formerSponsor.name == Relationship.OTHER.toString()){
                AppTextField(
                    value = uiState.formerSponsorOther,
                    onValueChange = { vm.ui = vm.ui.copy(formerSponsorOther = it) },
                    label = "Who was your sponsor",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.School, null) }
                )
            }
            AppTextField(
                value = uiState.reasonLeftSchool,
                onValueChange = { vm.ui = vm.ui.copy(reasonLeftSchool = it) },
                label = "Reason left school",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Info, null) }
            )
        }


//



    }
}

/* ----------------------------------------------------------
 * Step 4 — Family / Resettlement / Sponsorship
 * ---------------------------------------------------------- */
@Composable
private fun StepFamily(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Resettlement", style = MaterialTheme.typography.titleMedium)

        AppDateField(
            label = "Leave street date",
            value = uiState.leaveStreetDate, // <-- renamed
            onChanged = { vm.ui = vm.ui.copy(leaveStreetDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Resettled")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.resettled, onCheckedChange = { vm.ui = vm.ui.copy(resettled = it) })
        }
        AppDateField(
            label = "Resettlement date",
            value = uiState.resettlementDate,
            onChanged = { vm.ui = vm.ui.copy(resettlementDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        // Location
        AppTextField(uiState.region, { vm.ui = vm.ui.copy(region = it) }, "Region",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
        AppTextField(uiState.district, { vm.ui = vm.ui.copy(district = it) }, "District",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
        AppTextField(uiState.county, { vm.ui = vm.ui.copy(county = it) }, "County",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
        AppTextField(uiState.subCounty, vm::onSubCounty, "Sub-county",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
        AppTextField(uiState.parish, { vm.ui = vm.ui.copy(parish = it) }, "Parish",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })
        AppTextField(uiState.village, { vm.ui = vm.ui.copy(village = it) }, "Village",
            Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Place, null) })

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Primary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName1,
            last = uiState.memberLName1,
            onFirst = { vm.ui = vm.ui.copy(memberFName1 = it) },
            onLast  = { vm.ui = vm.ui.copy(memberLName1 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship1,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship1 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )

        PhoneRow(
            a = uiState.telephone1a,
            b = uiState.telephone1b,
            onA = { vm.ui = vm.ui.copy(telephone1a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone1b = it.filter { ch -> ch.isDigit() }) }
        )

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Secondary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName2,
            last = uiState.memberLName2,
            onFirst = { vm.ui = vm.ui.copy(memberFName2 = it) },
            onLast  = { vm.ui = vm.ui.copy(memberLName2 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship2,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship2 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )
        PhoneRow(
            a = uiState.telephone2a,
            b = uiState.telephone2b,
            onA = { vm.ui = vm.ui.copy(telephone2a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone2b = it.filter { ch -> ch.isDigit() }) }
        )

        Divider(Modifier.padding(vertical = 8.dp))
        Text("Tertiary contact", style = MaterialTheme.typography.titleMedium)
        NameRow(
            first = uiState.memberFName3,
            last = uiState.memberLName3,
            onFirst = { vm.ui = vm.ui.copy(memberFName3 = it) },
            onLast  = { vm.ui = vm.ui.copy(memberLName3 = it) }
        )
        EnumDropdown(
            title = "Relationship",
            selected = uiState.relationship3,
            values = Relationship.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(relationship3 = it) },
            labelFor = ::labelForRelationship,
            iconFor = ::iconForRelationship
        )
//        PhoneRow(
//            a = uiState.telephone3a,
//            onA = { vm.ui = vm.ui.copy(telephone3a = it.filter { ch -> ch.isDigit() }) },
//        )
        PhoneRow(
            a = uiState.telephone3a,
            b = uiState.telephone3b,
            onA = { vm.ui = vm.ui.copy(telephone3a = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(telephone3b = it.filter { ch -> ch.isDigit() }) }
        )
    }
}



/* ----------------------------------------------------------
 * Step 5 — Family / Resettlement / Sponsorship
 * ---------------------------------------------------------- */
@Composable
private fun StepSponsorship(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("Sponsorship", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sponsored for education")
            Spacer(Modifier.width(12.dp))
            Switch(checked = uiState.sponsoredForEducation, onCheckedChange = vm::onSponsored)
        }


        AppTextField(
            value = uiState.sponsorFName,
            onValueChange = { vm.ui = vm.ui.copy(sponsorFName = it) },
            label = "Sponsor first name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Person, null) }
        )
        AppTextField(
            value = uiState.sponsorLName,
            onValueChange = { vm.ui = vm.ui.copy(sponsorLName = it) },
            label = "Sponsor last name",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Person, null) }
        )
        PhoneRow(
            a = uiState.sponsorTelephone1,
            b = uiState.sponsorTelephone2,
            onA = { vm.ui = vm.ui.copy(sponsorTelephone1  = it.filter { ch -> ch.isDigit() }) },
            onB = { vm.ui = vm.ui.copy(sponsorTelephone2  = it.filter { ch -> ch.isDigit() }) }
        )

        AppTextField(
            value = uiState.sponsorEmail,
            onValueChange = { vm.ui = vm.ui.copy(sponsorEmail = it) },
            label = "Sponsor email",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.Email, null) }
        )
        AppTextField(
            value = uiState.sponsorNotes,
            onValueChange = vm::onSponsorNotes,
            label = "Sponsor notes",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
        )
     }
}

/* ----------------------------------------------------------
 * Step 6 — Spiritual
 * ---------------------------------------------------------- */
@Composable
private fun StepSpiritual(uiState: ChildFormUiState, vm: ChildFormViewModel) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
//        EnumDropdown(
//            title = "Accepted Jesus?",
//            selected = uiState.acceptedJesus,
//            values = Reply.values().toList(),
//            onSelected = { vm.ui = vm.ui.copy(acceptedJesus = it) },
//            labelFor = ::labelForReply,
//            iconFor = ::iconForReply
//        )
        EnumDropdown(
            title = "Who prayed with them?",
            selected = uiState.whoPrayed,
            values = Individual.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(whoPrayed = it) },
            labelFor = ::labelForIndividual,
            iconFor = ::iconForIndividual
        )
        if(uiState.whoPrayed == Individual.OTHER){
            AppTextField(
                value = uiState.whoPrayedOther,
                onValueChange = { vm.ui = vm.ui.copy(whoPrayedOther = it) },
                label = "Who prayed (Other - text)",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.EditNote, null) }
            )
        }

        AppDateField(
            label = "Spiritual decision date",
            value = uiState.acceptedJesusDate,
            onChanged = { vm.ui = vm.ui.copy(acceptedJesusDate = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )

        EnumDropdown(
            title = "Class Group",
            selected = uiState.classGroup,
            values = ClassGroup.values().toList(),
            onSelected = { vm.ui = vm.ui.copy(classGroup = it) },
            labelFor = ::labelForClassGroup,
            iconFor = ::iconForClassGroup
        )

        AppDateField(
            label = "Registered On",
            value = uiState.createdAt,
            onChanged = { vm.ui = vm.ui.copy(createdAt = it) },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) }
        )
        AppTextField(
            value = uiState.outcome,
            onValueChange = { vm.ui = vm.ui.copy(outcome = it) },
            label = "Notes / outcome",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) },
            maxLines = 5
        )
//    when a child has completed a skilling or school and they are working
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Graduated")
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = uiState.graduated == Reply.YES,
                onCheckedChange = vm::onGraduated
            )
        }


        AppTextField(
            value = uiState.generalComments,
            onValueChange = { vm.ui = vm.ui.copy(generalComments = it) },
            label = "General comments",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Outlined.EditNote, null) },
            maxLines = 5
        )
    }
}

/* ----------------------------------------------------------
 * Step 7 — Complete
 * ---------------------------------------------------------- */
@Composable
private fun StepComplete(uiState: ChildFormUiState) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.childId.isNotBlank())
            Text("Ready to save updates for #${uiState.childId}")
        else
            Text("Review all details, then tap Save")
    }
}


/* ----------------------------------------------------------
 * Reusables — Name, Phone, Date, EnumDropdown
 * ---------------------------------------------------------- */

@Composable
private fun NameRow(first: String, last: String, onFirst: (String) -> Unit, onLast: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            value = first,
            onValueChange = onFirst,
            label = "First name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
        )
        AppTextField(
            value = last,
            onValueChange = onLast,
            label = "Last name",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
        )
    }
}

@Composable
private fun PhoneRow(
    a: String,
    b: String,
    onA: (String) -> Unit,
    onB: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppTextField(
            value = a,
            onValueChange = onA, // callers can filter digits if desired
            label = "Phone 1",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            trailingIcon = {
                if (a.isNotEmpty()) {
                    IconButton(onClick = { onA("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
        AppTextField(
            value = b,
            onValueChange = onB,
            label = "Phone 2",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            trailingIcon = {
                if (b.isNotEmpty()) {
                    IconButton(onClick = { onB("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDateField(
    label: String,
    value: com.google.firebase.Timestamp?,
    onChanged: (com.google.firebase.Timestamp?) -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var show by rememberSaveable { mutableStateOf(false) }

    // Read-only text field that shows the current DOB
    AppTextField(
        value = formatDate(value),
        onValueChange = { /* read-only */ },
        label = label,
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        leadingIcon = leadingIcon,
        trailingIcon = {
            IconButton(onClick = { show = true }) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
            }
        }
    )

    if (show) {
        // 🚀 Create DatePickerState *inside* the dialog so it’s fresh each time you open it,
        // seeded from the latest DOB.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.toDate()?.time
        )

        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    onChanged(millis?.let { com.google.firebase.Timestamp(java.util.Date(it)) })
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = true)
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun AppDateField(
//    label: String,
//    value: com.google.firebase.Timestamp?,
//    onChanged: (com.google.firebase.Timestamp?) -> Unit,
//    leadingIcon: @Composable (() -> Unit)? = null
//) {
//    var show by rememberSaveable { mutableStateOf(false) }
//    val state = rememberDatePickerState(initialSelectedDateMillis = value?.toDate()?.time)
//
//    // ✅ Sync ONLY when the dialog is closed; don't override the user's live selection.
//    LaunchedEffect(value, show) {
//        if (!show) {
//            state.selectedDateMillis = value?.toDate()?.time
//        }
//    }
//
//    AppTextField(
//        value = formatDate(value),
//        onValueChange = { /* read-only */ },
//        label = label,
//        modifier = Modifier.fillMaxWidth(),
//        readOnly = true,
//        leadingIcon = leadingIcon,
//        trailingIcon = {
//            IconButton(onClick = { show = true }) {
//                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
//            }
//        }
//    )
//
//    if (show) {
//        DatePickerDialog(
//            onDismissRequest = { show = false },
//            confirmButton = {
//                TextButton(onClick = {
//                    val millis = state.selectedDateMillis
//                    onChanged(millis?.let { com.google.firebase.Timestamp(java.util.Date(it)) })
//                    show = false
//                }) { Text("OK") }
//            },
//            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
//        ) {
//            DatePicker(state = state, showModeToggle = true)
//        }
//    }
//}

/* -------- EnumDropdown with icons + friendly labels -------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String = { it.name },
    iconFor: (T) -> ImageVector? = { null }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = labelFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { v ->
                DropdownMenuItem(
                    text = {
                        Row {
                            iconFor(v)?.let {
                                Icon(it, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(labelFor(v))
                        }
                    },
                    onClick = {
                        onSelected(v)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ---------------- Icon + Label mappers for your enums ---------------- */

private fun iconForIndividual(v: Individual): ImageVector = when (v) {
    Individual.UNCLE -> Icons.Outlined.Person
    Individual.AUNTY -> Icons.Outlined.Female
    Individual.CHILD -> Icons.Outlined.Face
    Individual.OTHER -> Icons.Outlined.Group
}
private fun labelForIndividual(v: Individual): String = when (v) {
    Individual.UNCLE -> "Uncle"
    Individual.AUNTY -> "Aunty"
    Individual.CHILD -> "Child"
    Individual.OTHER -> "Other"
}

private fun iconForEducationPreference(v: EducationPreference): ImageVector = when (v) {
    EducationPreference.SCHOOL -> Icons.Outlined.School
    EducationPreference.SKILLING -> Icons.Outlined.Build
    EducationPreference.NONE -> Icons.Outlined.Block
}
private fun labelForEducationPreference(v: EducationPreference): String = when (v) {
    EducationPreference.SCHOOL -> "School"
    EducationPreference.SKILLING -> "Skilling"
    EducationPreference.NONE -> "None"
}

private fun iconForReply(v: Reply): ImageVector = when (v) {
    Reply.YES -> Icons.Outlined.CheckCircle
    Reply.NO -> Icons.Outlined.Cancel
}
private fun labelForReply(v: Reply): String = when (v) {
    Reply.YES -> "Yes"
    Reply.NO -> "No"
}

private fun labelForClassGroup(v: ClassGroup): String = when (v) {
    ClassGroup.SERGEANT -> "Sergeant: 3-5"
    ClassGroup.LIEUTENANT -> "Lieutenant: 6-9"
    ClassGroup.CAPTAIN -> "Captain: 10-12"
    ClassGroup.GENERAL -> "General: 13-25"
}

private fun iconForClassGroup(v: ClassGroup): ImageVector = when (v) {
    ClassGroup.SERGEANT -> Icons.Outlined.SpatialAudioOff
    ClassGroup.LIEUTENANT -> Icons.Outlined.Mood
    ClassGroup.CAPTAIN-> Icons.Outlined.Badge
    ClassGroup.GENERAL -> Icons.Outlined.ShutterSpeed
}

private fun labelForGender(v: Gender): String = when (v) {
    Gender.MALE -> "Male"
    Gender.FEMALE -> "Female"
}

private fun iconForGender(v: Gender): ImageVector = when (v) {
    Gender.MALE -> Icons.Outlined.Man
    Gender.FEMALE -> Icons.Outlined.Woman
}

private fun labelForResettlementPreference(v: ResettlementPreference): String = when (v) {
    ResettlementPreference.DIRECT_HOME ->  "Direct Home"
    ResettlementPreference.TEMPORARY_HOME -> "Temporary Home"
    ResettlementPreference.OTHER -> "Other"
//    Gender.FEMALE -> "Female"
}

private fun iconForResettlementPreference(v: ResettlementPreference): ImageVector = when (v) {
    ResettlementPreference.DIRECT_HOME ->  Icons.Outlined.Home
    ResettlementPreference.TEMPORARY_HOME -> Icons.Outlined.Hotel
    ResettlementPreference.OTHER -> Icons.Outlined.OtherHouses
}

private fun iconForRelationship(v: Relationship): ImageVector = when (v) {
    Relationship.NONE -> Icons.Outlined.HorizontalRule
    Relationship.PARENT -> Icons.Outlined.EmojiPeople
    Relationship.UNCLE -> Icons.Outlined.Person
    Relationship.AUNTY -> Icons.Outlined.Woman
    Relationship.OTHER -> Icons.Outlined.Group
}
private fun labelForRelationship(v: Relationship): String = when (v) {
    Relationship.NONE -> "None"
    Relationship.PARENT -> "Parent/Guardian"
    Relationship.UNCLE -> "Uncle"
    Relationship.AUNTY -> "Aunty"
    Relationship.OTHER -> "Other"
}

private fun iconForEducationLevel(v: EducationLevel): ImageVector = when (v) {
    EducationLevel.NONE -> Icons.Outlined.NoAccounts
    EducationLevel.NURSERY -> Icons.Outlined.BabyChangingStation
    EducationLevel.PRIMARY -> Icons.Outlined.ChildCare
    EducationLevel.SECONDARY -> Icons.Outlined.Person
}
private fun labelForEducationLevel(v: EducationLevel): String = when (v) {
    EducationLevel.NONE -> "None"
    EducationLevel.NURSERY -> "Nursery"
    EducationLevel.PRIMARY -> "Primary"
    EducationLevel.SECONDARY -> "Secondary"
}

@Suppress("unused")
private fun iconForStatus(v: RegistrationStatus): ImageVector = when (v) {
    RegistrationStatus.BASICINFOR -> Icons.Outlined.Badge
    RegistrationStatus.BACKGROUND -> Icons.Outlined.Description
    RegistrationStatus.EDUCATION -> Icons.Outlined.School
    RegistrationStatus.FAMILY -> Icons.Outlined.Home
    RegistrationStatus.SPONSORSHIP -> Icons.Outlined.Paid
    RegistrationStatus.SPIRITUAL -> Icons.Outlined.FavoriteBorder
    RegistrationStatus.COMPLETE -> Icons.Outlined.CheckCircle
}
@Suppress("unused")
private fun labelForStatus(v: RegistrationStatus): String = when (v) {
    RegistrationStatus.BASICINFOR -> "Basic"
    RegistrationStatus.BACKGROUND -> "Background"
    RegistrationStatus.EDUCATION -> "Education"
    RegistrationStatus.FAMILY -> "Family"
    RegistrationStatus.SPONSORSHIP -> "Sponsorship"
    RegistrationStatus.SPIRITUAL -> "Spiritual"
    RegistrationStatus.COMPLETE -> "Complete"
}

/* ----------------------------------------------------------
 * Reusable AppTextField (colors + icons + extras)
 * ---------------------------------------------------------- */

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    supportingText: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = errorText != null,
        supportingText = {
            when {
                errorText != null -> Text(errorText, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText)
            }
        },
        textStyle = textStyle,
        minLines = minLines,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            errorCursorColor = MaterialTheme.colorScheme.error,
            disabledTextColor = MaterialTheme.colorScheme.onSurface, //  Prevent fade
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

/* ----------------------------------------------------------
 * MinSdk-24-safe Timestamp -> String formatter
 * ---------------------------------------------------------- */

private fun formatDate(ts: Timestamp?): String {
    if (ts == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(ts.toDate())
}
