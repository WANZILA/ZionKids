package com.example.zionkids.presentation.screens.children

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zionkids.data.model.Child
import com.example.zionkids.presentation.viewModels.children.ChildDetailsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailsScreen(
    childIdArg: String,
    onEdit: (String) -> Unit,
    navigateUp: () -> Unit,           // call navController::popBackStack from the caller
    vm: ChildDetailsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // load once
    LaunchedEffect(childIdArg) { vm.load(childIdArg) }

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is ChildDetailsViewModel.Event.Deleted -> navigateUp()
                is ChildDetailsViewModel.Event.Error -> snackbarHostState.showSnackbar(ev.msg)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(ui.child?.fName?.trim() ?: "Child details")
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    val child = ui.child
                    if (child != null) {
                        IconButton(onClick = { onEdit(child.childId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { vm.refresh(child.childId) }, enabled = !ui.deleting) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showConfirm = true }, enabled = !ui.deleting) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                        IconButton(onClick = navigateUp) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    "Error: ${ui.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ui.child != null -> DetailsContent(ui.child!!)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete child") },
            text = { Text("Are you sure you want to delete ${ui.child?.fName} ${ui.child?.lName}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !ui.deleting,
                    onClick = {
                        showConfirm = false
                        vm.deleteChildOptimistic()
                    }
                ) {
                    if (ui.deleting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailsContent(child: Child) {
    var openBasic by rememberSaveable { mutableStateOf(true) }
    var openBackground by rememberSaveable { mutableStateOf(false) }
    var openEducation by rememberSaveable { mutableStateOf(false) }
    var openResettlement by rememberSaveable { mutableStateOf(false) }
    var openMembers by rememberSaveable { mutableStateOf(false) }
    var openSpiritual by rememberSaveable { mutableStateOf(false) }
    var openStatus by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CollapsibleSection("Basic Info", openBasic, { openBasic = !openBasic }) {
                Field("First name", child.fName)
                Field("Last name", child.lName)
                Field("Other name", child.oName ?: "-")
                Field("Age", child.age.takeIf { it > 0 }?.toString() ?: "-")
                Field("Street", child.street.ifBlank { "-" })
                Field("Invited by", child.invitedBy.name)
                Field("Invited by type", child.invitedByType.ifBlank { "-" })
                Field("Education preference", child.educationPreference.name)
            }
        }

        item {
            CollapsibleSection("Background Info", openBackground, { openBackground = !openBackground }) {
                Field("Left home date", child.leftHomeDate?.asHuman() ?: "-")
                Field("Reason left home", child.reasonLeftHome ?: "-")
                Field("Left street date", child.leftStreetDate?.asHuman() ?: "-")
            }
        }

        item {
            CollapsibleSection("Education Info", openEducation, { openEducation = !openEducation }) {
                Field("Last class", child.lastClass ?: "-")
                Field("Previous school", child.previousSchool ?: "-")
                Field("Reason left school", child.reasonLeftSchool ?: "-")
            }
        }

        item {
            CollapsibleSection("Family Resettlement", openResettlement, { openResettlement = !openResettlement }) {
                Field("Home preference", child.homePreference.name)
                Field("Go home date", child.goHomeDate?.asHuman() ?: "-")
                Field("Region", child.region ?: "-")
                Field("District", child.district ?: "-")
                Field("County", child.county ?: "-")
                Field("Sub-county", child.subCounty ?: "-")   // <-- renamed
                Field("Parish", child.parish ?: "-")
                Field("Village", child.village ?: "-")
            }
        }

        item {
            CollapsibleSection("Family Members", openMembers, { openMembers = !openMembers }) {
                MemberBlock(1, child.memberFName1, child.memberLName1, child.relationship1.name, child.telephone1a, child.telephone1b) // <-- renamed
                Divider()
                MemberBlock(2, child.memberFName2, child.memberLName2, child.relationship2.name, child.telephone2a, child.telephone2b) // <-- renamed
                Divider()
                MemberBlock(3, child.memberFName3, child.memberLName3, child.relationship3.name, child.telephone3a, child.telephone3b) // <-- renamed
            }
        }

        item {
            CollapsibleSection("Spiritual Info", openSpiritual, { openSpiritual = !openSpiritual }) {
                Field("Accepted Jesus", child.acceptedJesus.name)
                Field("Accepted Jesus date", child.acceptedJesusDate?.asHuman() ?: "-")
                Field("Who prayed", child.whoPrayed.name)
                Field("Outcome", child.outcome ?: "-")
            }
        }

        item {
            CollapsibleSection("Status", openStatus, { openStatus = !openStatus }) {
                Field("Registration step", child.registrationStatus.name)
                Field("Graduated", child.graduated.name)
                Field("Created at", child.createdAt.asHuman())
                Field("Updated at", child.updatedAt.asHuman())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Divider()
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MemberBlock(
    idx: Int,
    first: String?, last: String?,
    relation: String,
    phoneA: String?, phoneB: String?
) {
    Text("Member $idx", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Field("Name", "${first ?: "-"} ${last ?: ""}".trim().ifBlank { "-" })
    Field("Relationship", relation)
    Field("Phone (A)", phoneA ?: "-")
    Field("Phone (B)", phoneB ?: "-")
}

private fun Long.asHuman(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this)
}
