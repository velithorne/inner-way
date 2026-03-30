package com.collide.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.collide.app.CollideApplication
import com.collide.app.ui.archive.ArchiveScreen
import com.collide.app.ui.archive.ArchiveViewModel
import com.collide.app.ui.collider.ColliderScreen
import com.collide.app.ui.collider.ColliderViewModel
import com.collide.app.ui.eventdetail.EventDetailScreen
import com.collide.app.ui.eventdetail.EventDetailViewModel
import com.collide.app.ui.home.HomeScreen
import com.collide.app.ui.home.HomeViewModel
import com.collide.app.ui.settings.SettingsScreen
import com.collide.app.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val COLLIDER = "collider"
    const val ARCHIVE = "archive"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val SETTINGS = "settings"

    fun eventDetail(eventId: Long) = "event_detail/$eventId"
}

@Composable
fun CollideNavGraph(
    navController: NavHostController,
    application: CollideApplication,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {

        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(application.eventRepository)
            )
            HomeScreen(
                viewModel = vm,
                onNavigateToCollider = { navController.navigate(Routes.COLLIDER) },
                onNavigateToArchive = { navController.navigate(Routes.ARCHIVE) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.COLLIDER) {
            val vm: ColliderViewModel = viewModel(
                factory = ColliderViewModel.Factory(application.eventRepository, application.settingsStore)
            )
            ColliderScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ARCHIVE) {
            val vm: ArchiveViewModel = viewModel(
                factory = ArchiveViewModel.Factory(application.eventRepository)
            )
            ArchiveScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },
                onEventClick = { eventId -> navController.navigate(Routes.eventDetail(eventId)) }
            )
        }

        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: return@composable
            val vm: EventDetailViewModel = viewModel(
                factory = EventDetailViewModel.Factory(application.eventRepository, eventId)
            )
            EventDetailScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(application.settingsStore)
            )
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
