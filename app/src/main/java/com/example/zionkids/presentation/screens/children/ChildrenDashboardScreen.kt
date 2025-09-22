package com.example.zionkids.presentation.screens.children

//import ZionKidAppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zionkids.R
import com.example.zionkids.data.model.EducationPreference
import com.example.zionkids.presentation.components.action.ZionKidAppTopBar
import com.example.zionkids.presentation.viewModels.auth.AuthViewModel
//import com.example.zionkids.presentation.components.action.ZionKidAppTopBar
import com.example.zionkids.presentation.viewModels.children.ChildrenDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenDashboardScreen(
    toChildrenList: () -> Unit,
    toAddChild: () -> Unit,
    toChildrenByEducation: (EducationPreference) -> Unit,
    toReunited: () -> Unit,
    toAllRegions: () -> Unit,
    toAllStreets: () -> Unit,
    vm: ChildrenDashboardViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val authUi by authVM.ui.collectAsStateWithLifecycle()
    val canCreateChild = authUi.perms.canCreateChild

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            ZionKidAppTopBar(
                canNavigateBack = false,
                navigateUp = { /* no-op */ },
                txtLabel = stringResource(R.string.children),
            )
        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = toAddChild,
//                containerColor = MaterialTheme.colorScheme.secondary,
//                contentColor   = MaterialTheme.colorScheme.onSecondary
//            ) {
//                Icon(Icons.Default.Add, contentDescription = "Add")
//            }
//        }
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
                // KPI Cards
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total", ui.total.toString(), Modifier.weight(1f))
                        StatCard("Fully Registered", ui.inProgram.toString(), Modifier.weight(1f))
          }
                }


                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                       StatCard("Reunited", ui.reunited.toString(), Modifier.weight(1f), onClick = toReunited)
                        StatCard("Avg Age", ui.avgAge.toString(), Modifier.weight(1f))
//
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if(canCreateChild){
                            StatCard(
                                title = "Children",
                                value = "Add New",
                                modifier = Modifier.weight(1f),
                                onClick = toAddChild
                            )
                        }

                        StatCard(
                            title = "Children",
                            value = "View All",
                            modifier = Modifier.weight(1f),
                            onClick = toChildrenList
                        )
                  }

                }


                // Education distribution
                item {
                    SectionCard(title = "Education Preference") {
                        val total = ui.eduDist.values.sum().coerceAtLeast(1)
                        EducationPreference.values().forEach { pref ->
                            val count = ui.eduDist[pref] ?: 0
                            EducationPreferenceRow(
                                label = pref.name,
                                count = count,
                                total = total,
                                onClick = { toChildrenByEducation(pref) } // 👈 navigate with selected pref
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }


                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "All",
                            value = "Streets",
                            modifier = Modifier.weight(1f),
                            onClick = toAllStreets
                        )

                        StatCard(
                            title = "All",
                            value = "Regions",
                            modifier = Modifier.weight(1f),
                            onClick = toAllRegions
                        )
                    }

                }


            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier, sub: String = "") {
    ElevatedCard(modifier) {
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

/** Simple text + progress bar row (no extra libraries). */
@Composable
private fun DistributionRow(label: String, count: Int, total: Int) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = (count / total.toFloat()).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}

// Add this overload
@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String = "",
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },   // clickable only if passed
        enabled = onClick != null
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationPreferenceRow(
    label: String,
    count: Int,
    total: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        enabled = true
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("View:$count", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (count / total.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}
