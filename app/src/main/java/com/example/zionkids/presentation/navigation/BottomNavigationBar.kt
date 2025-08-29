package com.example.zionkids.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoSim
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val items = listOf(
        Screen.AdminDashboard,
        Screen.ChildrenDashboard,
        Screen.EventsDashboard,
        Screen.AttendanceDashboard,
//        Screen.Profile,
//        Screen.Profile
    )

    NavigationBar {
        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            Surface(
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f), // spread evenly across the bar
                shape = RoundedCornerShape(20),
                //applies the background color
//                color = if (selected) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                color =  Color.Transparent,
                tonalElevation = if (selected) 1.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (screen) {
                            is Screen.AdminDashboard -> Icons.Default.Home
                            is Screen.ChildrenDashboard -> Icons.Default.Person
                            is Screen.EventsDashboard -> Icons.Default.AddTask
                            else -> Icons.Default.PeopleAlt
                        },
                        modifier = Modifier.size(24.dp),
                        contentDescription = screen.route,
                        tint = if (selected)
                           // MaterialTheme.colorScheme.onSecondaryContainer
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = screen.route.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = if (selected)
                           // MaterialTheme.colorScheme.onSecondaryContainer
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
