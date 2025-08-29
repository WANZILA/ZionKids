package com.example.zionkids.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object AdminDashboard: Screen("home")
    object ChildrenDashboard : Screen("children")
//    object Profile : Screen("profile")
    object ChildForm : Screen("childForm?childId={childId}"){
        fun newChild() = "childForm"                   // no id -> create
        fun edit(id: String) = "childForm?childId=$id"
        //fun createRoute(childId: String) = "add_child/$childId"
    }

    object ChildrenList: Screen("children_list")
    object ChildDetails : Screen("child_details/{childId}") {
        fun createRoute(childId: String) = "child_details/$childId"
    }

    /**
     * Events
     * **/
    object  EventsDashboard: Screen("events")

    object EventForm: Screen("eventForm?eventId={eventId}"){
        fun newEvent() = "eventForm"                   // no id -> create
        fun editEvent(id: String) = "eventForm?eventId=$id"
        //fun createRoute(childId: String) = "add_child/$childId"
    }

    object EventsList: Screen("events_list")

    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: String) = "event_details/$eventId"
    }

    /**
     * Attendance
     * **/
    object  AttendanceDashboard: Screen("attend")

    /**
     * Attendance List
     */
    object  AttendanceRoster: Screen("attendance_roster/{eventId}") {
            fun createRoute(eventId: String) = "attendance_roster/$eventId"
    }

}
