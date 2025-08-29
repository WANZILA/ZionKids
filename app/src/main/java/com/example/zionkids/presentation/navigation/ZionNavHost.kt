package com.example.zionkids.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.zionkids.presentation.screens.AdminDashboardScreen
import com.example.zionkids.presentation.screens.children.ChildDetailsScreen
import com.example.zionkids.presentation.screens.ChildFormScreen
import com.example.zionkids.presentation.screens.LoginScreen
import com.example.zionkids.presentation.screens.attendance.AttendanceDashboardScreen
import com.example.zionkids.presentation.screens.attendance.AttendanceRosterScreen
//import com.example.zionkids.presentation.screens.children.ChildFormScreen
import com.example.zionkids.presentation.screens.children.ChildrenDashboardScreen
import com.example.zionkids.presentation.screens.children.ChildrenListScreen
import com.example.zionkids.presentation.screens.events.EventDashboardScreen
import com.example.zionkids.presentation.screens.events.EventDetailsScreen
import com.example.zionkids.presentation.screens.events.EventFormScreen
import com.example.zionkids.presentation.screens.events.EventListScreen
import com.example.zionkids.presentation.screens.splash.SplashScreen

@Composable
fun ZionAppNavHost(
    navController: NavHostController = rememberNavController()
) {
    // Define routes where the bottom nav should be shown
    val bottomNavRoutes = listOf(
        Screen.AdminDashboard.route,
        Screen.ChildrenDashboard.route,
        Screen.EventsDashboard.route,
        Screen.AttendanceDashboard.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavRoutes.any { currentRoute?.startsWith(it) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    toLogin = { navController.navigate(Screen.Login.route) },
                    toAdmin = { navController.navigate(Screen.AdminDashboard.route) }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    toAdminDashboard = { navController.navigate(Screen.AdminDashboard.route) }
                )
            }

            /***
             *
             * Admin Dashboard
             */
            // Bottom nav screens
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    toChildrenList = {
                        navController.navigate(Screen.ChildrenList.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    toEventsList = {
                        navController.navigate(Screen.EventsList.route) {
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    toAddEvent = {},
                    toAddChild = {
                        navController.navigate(Screen.ChildForm.newChild()){
                            popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }


            /**
             *
             *
             *
             * children routes
             *
             *
             * **/
            //child dashboard
            composable(Screen.ChildrenDashboard.route) {
                ChildrenDashboardScreen(
                    toChildrenList = {
                        navController.navigate(Screen.ChildrenList.route) {
                            popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                                     },
                    toAddChild = {
                        navController.navigate(Screen.ChildForm.newChild()){
                            popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                                 },
                )
            }

            //children list
            composable(Screen.ChildrenList.route) {
                ChildrenListScreen(
                    toChildForm = {
                        navController.navigate(Screen.ChildForm.newChild()){
                            popUpTo(Screen.ChildrenList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    navigateUp = {
                        navController.navigate(Screen.ChildrenDashboard.route){
                            popUpTo(Screen.ChildrenList.route){ inclusive = true}
                            launchSingleTop = true
                        }
                    },
                    onChildClick = { childIdArg ->
//                        navController.navigate(Screen.ChildForm.edit(childIdArg))
                        navController.navigate(Screen.ChildDetails.createRoute(childIdArg)){
                            popUpTo(Screen.ChildrenList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                                                }
                )
            }
            // Add Child
            composable(Screen.ChildForm.route,
                    arguments = listOf(navArgument("childId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId")
                ChildFormScreen(
                    childIdArg = childId,
                    onFinished = { childIdArg ->
                        navController.navigate(Screen.ChildDetails.createRoute(childIdArg)) {
                            popUpTo(Screen.ChildForm.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSave = {
                        navController.navigate(Screen.ChildForm.route)
                    },
                    toList = {
                        navController.navigate(Screen.ChildrenList.route){
                            popUpTo(Screen.ChildForm.route){ inclusive = true}
                            launchSingleTop = true
                        }
                    },
                    navigateUp = {
                        navController.navigate(Screen.ChildrenDashboard.route){
                            popUpTo(Screen.ChildForm.route){ inclusive = true}
                            launchSingleTop = true
                        }
                    }
                )
            }

            /**
             * Child details
             */
            composable(
                route = Screen.ChildDetails.route,
                arguments = listOf(navArgument("childId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
                ChildDetailsScreen(
                    childIdArg = childId,
                    onEdit = { childIdArg ->
                        navController.navigate(Screen.ChildForm.edit(childIdArg)){
                            popUpTo(Screen.ChildrenList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    navigateUp = {
                        navController.popBackStack()
                    },

                )
               // val childId = backStackEntry.arguments?.getString("childId")
              //  ChildDetailsScreen(childIdArg = childId)
            }


            /***
             *
             * Events
             */

            composable(Screen.EventsDashboard.route) {
                EventDashboardScreen(
                    toEventDetails = { eventIdArg ->
                        navController.navigate(Screen.EventDetails.createRoute(eventIdArg)) {
                            popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAddEvent = {
                        navController.navigate(Screen.EventForm.newEvent()) {
                            popUpTo(Screen.EventForm.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    toEventList = {
                        navController.navigate(Screen.EventsList.route) {
                            popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },

                    onOpenEvent = { eventIdArg ->
                        navController.navigate(Screen.EventDetails.createRoute(eventIdArg)) {
                            popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    toEventForm = {
                        navController.navigate( Screen.EventForm.newEvent()){
                            popUpTo( Screen.EventsDashboard.route){ inclusive = true }
                            launchSingleTop = true
                        }
                    }
//                    onEditEvent = { eventIdArg ->
////                        navController.navigate(Screen.EventsList.route) {
////                            popUpTo(Screen.EventsDashboard.route) { inclusive = true }
////                            launchSingleTop = true
////                        }
//                    },
//                    toEventList = {
//                        navController.navigate(Screen.EventsList.route) {
//                            popUpTo(Screen.EventsDashboard.route) { inclusive = true }
//                            launchSingleTop = true
//                        }
//                    }
                )
            }

            // Event Form
            composable(Screen.EventForm.route,
                arguments = listOf(navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                EventFormScreen(
                    eventIdArg = eventId,
                    onFinished = { eventIdArg ->
                        navController.navigate(Screen.EventDetails.createRoute(eventIdArg)) {
                            popUpTo(Screen.EventForm.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    navigateUp = {
                        // close the form, don't push a new screen
                        navController.navigate(Screen.EventsDashboard.route){
                            popUpTo(Screen.EventForm.route) { inclusive = true  }
                            launchSingleTop = true
                        }
                    }
                )
            }

            /**
             * Event List
             */
            //Event list
            composable(Screen.EventsList.route) {
                EventListScreen(
                    toEventForm = {
                        navController.navigate(Screen.EventForm.newEvent()){
                            popUpTo(Screen.EventsList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    navigateUp = {
                        navController.navigate(Screen.EventsDashboard.route){
                            popUpTo(Screen.EventsList.route){ inclusive = true}
                            launchSingleTop = true
                        }
                    },
                    onEventClick = { eventIdArg ->
                        navController.navigate(Screen.EventDetails.createRoute(eventIdArg)) {
                            popUpTo(Screen.EventsList.route) { inclusive = true }
                            launchSingleTop = true
                        }
                 }
                )
            }

            composable(
                route = Screen.EventDetails.route,
                arguments = listOf(navArgument("eventId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                EventDetailsScreen(
                    eventIdArg = eventId,
                    onEdit = { eventIdArg ->
                        navController.navigate(Screen.EventForm.editEvent(eventIdArg)){
                            popUpTo(Screen.ChildrenList.route) { inclusive = true }
                            launchSingleTop = true

                        }
                    },
                    toAttendanceRoster = { eventIdArg, adminId ->
                        navController.navigate(Screen.AttendanceRoster.createRoute(eventIdArg)){
                            popUpTo(Screen.EventDetails.route) { inclusive = true }
                            launchSingleTop = true

                        }

                    },
                    navigateUp = {
                        navController.navigate(Screen.EventsList.route){
                            popUpTo(Screen.EventDetails.route) { inclusive = true }
                            launchSingleTop = true

                        }
                    },

                    )

            }


            /**
             * Attendance
             *
             */

            /**
             * Attandance dashboard
             */

            composable(Screen.AttendanceDashboard.route){
                AttendanceDashboardScreen(
                    navigateUp = { TODO() },
                    onContactGuardian = { },

                )
            }
            composable(
                route = Screen.AttendanceRoster.route,
                arguments = listOf(navArgument("eventId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                AttendanceRosterScreen(
                    eventId = eventId,
                    adminId = "0",

                    navigateUp = {
                        navController.navigate(Screen.EventsList.route){
                            popUpTo(Screen.EventDetails.route) { inclusive = true }
                            launchSingleTop = true

                        }
                    },

                    )

            }



        }
    }
}
