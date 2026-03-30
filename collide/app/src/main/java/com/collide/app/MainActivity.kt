package com.collide.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.collide.app.data.db.CollideDatabase
import com.collide.app.data.repository.EventRepository
import com.collide.app.data.settings.AppSettings
import com.collide.app.domain.engine.CodeColliderEngine
import com.collide.app.domain.engine.collision.RecipeSerializer
import com.collide.app.domain.engine.collision.TransformationEngine
import com.collide.app.domain.engine.normalize.CodeNormalizer
import com.collide.app.domain.engine.replay.EventReplayer
import com.collide.app.ui.archive.ArchiveScreen
import com.collide.app.ui.archive.ArchiveViewModel
import com.collide.app.ui.collider.ColliderScreen
import com.collide.app.ui.collider.ColliderViewModel
import com.collide.app.ui.eventdetail.EventDetailScreen
import com.collide.app.ui.eventdetail.EventDetailViewModel
import com.collide.app.ui.home.HomeScreen
import com.collide.app.ui.home.HomeViewModel
import com.collide.app.ui.navigation.Screen
import com.collide.app.ui.settings.SettingsScreen
import com.collide.app.ui.settings.SettingsViewModel
import com.collide.app.ui.theme.CollideTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Dependency setup — in a production app, use Hilt/DI framework
        val db = CollideDatabase.getInstance(this)
        val repo = EventRepository(db)
        val settings = AppSettings(this)
        val engine = CodeColliderEngine()
        val recipeSerializer = RecipeSerializer()
        val replayer = EventReplayer(
            normalizer = CodeNormalizer(),
            transformationEngine = TransformationEngine(),
            recipeSerializer = recipeSerializer
        )

        setContent {
            CollideTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel(factory = HomeViewModel.Factory(repo)),
                                onColliderClick = { navController.navigate(Screen.Collider.route) },
                                onArchiveClick = { navController.navigate(Screen.Archive.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) }
                            )
                        }

                        composable(Screen.Collider.route) {
                            ColliderScreen(
                                viewModel = viewModel(
                                    factory = ColliderViewModel.Factory(engine, repo, settings, recipeSerializer)
                                ),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Archive.route) {
                            ArchiveScreen(
                                viewModel = viewModel(factory = ArchiveViewModel.Factory(repo)),
                                onEventClick = { id ->
                                    navController.navigate(Screen.EventDetail.createRoute(id))
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.EventDetail.route) { backStackEntry ->
                            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                            EventDetailScreen(
                                eventId = eventId,
                                viewModel = viewModel(
                                    factory = EventDetailViewModel.Factory(repo, settings, replayer)
                                ),
                                onNavigateBack = { navController.popBackStack() },
                                onShare = { summary ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "COLLIDE Event Export")
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                    }
                                    startActivity(Intent.createChooser(intent, "Share Event"))
                                }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel(factory = SettingsViewModel.Factory(settings)),
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
