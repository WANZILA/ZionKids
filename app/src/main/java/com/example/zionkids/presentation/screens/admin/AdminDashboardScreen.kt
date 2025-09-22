package com.example.zionkids.presentation.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zionkids.R
import com.example.zionkids.data.model.Event
import com.example.zionkids.presentation.components.action.ZionKidAppTopBar
//import com.example.zionkids.presentation.screens.admin.StatCard
//import com.example.zionkids.presentation.screens.children.StatCard
import com.example.zionkids.presentation.viewModels.AdminDashboardViewModel
import com.example.zionkids.presentation.viewModels.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    toChildrenList: () -> Unit,
    toEventsList: () -> Unit,
    toAccepted: () -> Unit,
    toYetAccept: () -> Unit,
    toResettled: () -> Unit,
    toBeResettled: () -> Unit,
    vm: AdminDashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            ZionKidAppTopBar(
                canNavigateBack = false,
                navigateUp = { /* no-op on home */ },
                txtLabel = stringResource(R.string.home),
            )
        },
    ) { inner ->
        when {
            ui.loading -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Error: ${ui.error}", color = MaterialTheme.colorScheme.error) }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KPI rows
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total", ui.childrenTotal.toString(), Modifier.weight(1f), onClick = toChildrenList)
                        StatCard("New This Month", ui.childrenNewThisMonth.toString(), Modifier.weight(1f))
                       }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Graduated", ui.childrenGraduated.toString(), Modifier.weight(1f))
                        StatCard("Sponsored", ui.sponsored.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Accepted Christ", ui.acceptedChrist.toString(),  onClick = toAccepted,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard("Yet to Accept", ui.yetToAcceptChrist.toString(), onClick = toYetAccept,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                         StatCard("Resettled", ui.resettled.toString(), onClick = toResettled , modifier = Modifier.weight(1f))
                         StatCard("To Resettle", ui.toBeResettled.toString(),onClick = toBeResettled , modifier =  Modifier.weight(1f))

//                         StatCard("Events Today", ui.eventsToday.toString(), Modifier.weight(1f), onClick = toEventsList)

                    }
                }
//                item {
//                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                        StatCard("Active Now", ui.eventsActiveNow.toString(), Modifier.weight(1f))
//                    }
//                }


                // Happening Today
                item {
                    SectionCard("Happening Today") {
                        if (ui.happeningToday.isEmpty()) {
                            Text("No events today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ui.happeningToday.forEach { e ->
                                    EventRowSmall(e, onOpen = toEventsList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------- Reusable bits ---------- */

@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String = "",
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

//@Composable
//private fun DistributionRow(label: String, count: Int, total: Int) {
//    Column {
//        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//            Text(label, style = MaterialTheme.typography.bodyMedium)
//            Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
//        }
//        LinearProgressIndicator(
//            progress = (count / total.toFloat()).coerceIn(0f, 1f),
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(6.dp)
//        )
//    }
//}

@Composable
private fun EventRowSmall(event: Event, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    event.title.ifBlank { "Untitled Event" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    event.eventDate.asTimeAndDate(), // Timestamp → formatted string
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.location.isNotBlank()) {
                    Text(
                        event.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AssistChip(onClick = onOpen, label = { Text(event.eventStatus.name.first().toString()) })
        }
    }
}

/* ---------- Timestamp formatting ---------- */

private fun Timestamp.asTimeAndDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this.toDate()) // uses the device's locale/timezone
}

// (Optional: keep the Long version if you still use it elsewhere)
private fun Long.asTimeAndDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}
