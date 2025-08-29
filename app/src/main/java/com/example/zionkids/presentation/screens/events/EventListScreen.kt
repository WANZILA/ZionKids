package com.example.zionkids.presentation.screens.events

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zionkids.R
import com.example.zionkids.data.model.Event
import com.example.zionkids.presentation.viewModels.events.EventListViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    toEventForm: () -> Unit,
    navigateUp: () -> Unit,
    onEventClick: (String) -> Unit = {},
    vm: EventListViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ui.error) {
        ui.error?.let { Log.d("EventListScreen", "Error: $it") }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.events))
                        Spacer(Modifier.width(8.dp))
                        // If you want offline/sync chips, add them back here.
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = toEventForm) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                },
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
                placeholder = { Text("Search by title…") },
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
                        "Failed to load: ${ui.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    ui.event.isEmpty() -> Text("No matches", modifier = Modifier.align(Alignment.Center))
                    else -> EventList(items = ui.event, onEventClick = onEventClick)
                }
            }
        }
    }
}

@Composable
private fun EventList(
    items: List<Event>,
    onEventClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.eventId }) { event ->
            EventRow(event = event, onClick = { onEventClick(event.eventId) })
        }
    }
}

@Composable
private fun EventRow(
    event: Event,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = event.title.ifBlank { "Untitled Event" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Updated: ${event.updatedAt.asHuman()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            // You can show more fields here as needed, e.g.:
            // Text("When: ${event.eventDate.asHuman()}")
        }
    }
}

/* ---------- Timestamp formatting (timestamps all through) ---------- */

private val humanFmt = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())

private fun Timestamp.asHuman(): String = humanFmt.format(this.toDate())
