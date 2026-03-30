package com.collide.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Collider : Screen("collider")
    object Archive : Screen("archive")
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object Settings : Screen("settings")
}
