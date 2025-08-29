package com.example.zionkids.presentation.screens.children

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zionkids.data.model.Child
import com.example.zionkids.presentation.viewModels.children.ChildrenListViewModel

import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenListScreen(
    toChildForm: () -> Unit,
    navigateUp: () -> Unit,
    onChildClick: (String) -> Unit = {},
    vm: ChildrenListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ui.error) {
        ui.error?.let { Log.d("ChildrenListScreen", "Cliff here: $it") }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Not Graduated")
                        Spacer(Modifier.width(8.dp))
//                        if (ui.isOffline) AssistChip(
//                            onClick = {},
//                            label = { Text("Offline") }
//                        )
//                        if (ui.isSyncing) AssistChip(
//                            onClick = {},
//                            label = { Text("Syncing…") }
//                        )
                    }
                },
                actions = {

                        IconButton(onClick = { vm.refresh() }, ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = toChildForm, ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add")
                        }
                        IconButton(onClick = navigateUp) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }

                }
                ,
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {

            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    vm.onSearchQueryChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("Search by name…") },
                singleLine = true
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    ui.error != null -> Text("Failed to load: ${ui.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center))
                    ui.children.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
                    else -> ChildrenList(items = ui.children, onChildClick = onChildClick)
                }
            }
        }
    }
}

@Composable
private fun ChildrenList(
    items: List<Child>,
    onChildClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.childId }) { child ->
            ChildRow(child = child, onClick = { onChildClick(child.childId) })
        }
    }
}

@Composable
private fun ChildRow(
    child: Child,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "${child.fName} ${child.lName}".trim(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Updated: ${child.updatedAt.asHuman()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            // You can show more fields here as needed
        }
    }
}

private fun Long.asHuman(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this)
}
