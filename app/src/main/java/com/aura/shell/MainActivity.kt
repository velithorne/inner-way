package com.aura.shell

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.compose.material3.SnackbarHostState
import com.aura.shell.command.LauncherDrawerIntent
import com.aura.shell.knowledge.KnowledgeActivity
import com.aura.shell.ui.home.HomeScreen
import com.aura.shell.ui.home.HomeViewModel
import com.aura.shell.ui.home.HomeViewModelFactory
import com.aura.shell.ui.home.PinnedCardUi
import com.aura.shell.ui.theme.AuraShellTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(application)
    }

    override fun onResume() {
        super.onResume()
        LaunchActivityProvider.attach(this)
        homeViewModel.refreshRecents()
    }

    override fun onPause() {
        LaunchActivityProvider.detach(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDrawerIntent(intent)
        enableEdgeToEdge()
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> homeViewModel.onForegroundChanged(true)
                    Lifecycle.Event.ON_PAUSE -> homeViewModel.onForegroundChanged(false)
                    else -> {}
                }
            },
        )
        setContent {
            AuraShellTheme {
                val vm = homeViewModel
                val state by vm.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        vm.setPassiveHandsFreeEnabled(true)
                    } else {
                        vm.setPassiveHandsFreeEnabled(false)
                        scope.launch {
                            snackbarHostState.showSnackbar("Microphone access is needed for hands-free. Toggle on again after allowing.")
                        }
                    }
                }

                LaunchedEffect(state.personalizationNavNonce) {
                    if (state.personalizationNavNonce == null) return@LaunchedEffect
                    startActivity(
                        Intent(this@MainActivity, CommandLayerActivity::class.java).apply {
                            putExtra(CommandLayerActivity.EXTRA_OPEN_PERSONALIZATION, true)
                        },
                    )
                    vm.consumePersonalizationNavRequest()
                }

                LaunchedEffect(state.knowledgeNavNonce) {
                    val req = state.knowledgeNavNonce ?: return@LaunchedEffect
                    startActivity(
                        Intent(this@MainActivity, KnowledgeActivity::class.java).apply {
                            putExtra(KnowledgeActivity.EXTRA_ROUTE, req.first)
                        },
                    )
                    vm.consumeKnowledgeNavRequest()
                }

                fun onPassiveToggle(enabled: Boolean) {
                    if (!enabled) {
                        vm.setPassiveHandsFreeEnabled(false)
                        return
                    }
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        vm.setPassiveHandsFreeEnabled(true)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                HomeScreen(
                    state = state,
                    knowledgePreview = state.latestKnowledgePreview,
                    onOpenKnowledge = {
                        startActivity(
                            Intent(this@MainActivity, KnowledgeActivity::class.java).apply {
                                putExtra(KnowledgeActivity.EXTRA_ROUTE, "list")
                            },
                        )
                    },
                    passiveHandsFreeEnabled = state.passiveHandsFreeEnabled,
                    onPassiveHandsFreeChange = { onPassiveToggle(it) },
                    handsFree = state.handsFree,
                    drawerRequest = state.drawerRequest,
                    onDrawerRequestConsumed = { vm.consumeDrawerRequest() },
                    onAppClick = { pkg -> vm.onAppLaunch(pkg) },
                    onCommandBarClick = {
                        startActivity(Intent(this@MainActivity, CommandLayerActivity::class.java))
                    },
                    onMicClick = {
                        startActivity(
                            Intent(this@MainActivity, CommandLayerActivity::class.java).apply {
                                putExtra(CommandLayerActivity.EXTRA_START_LISTENING, true)
                            },
                        )
                    },
                    onPinnedClick = { card: PinnedCardUi ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "${card.title}: pinned modules arrive in a later phase.",
                            )
                        }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDrawerIntent(intent)
    }

    private fun handleDrawerIntent(intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            LauncherDrawerIntent.ACTION_OPEN_APP_DRAWER -> homeViewModel.requestDrawer(expand = true)
            LauncherDrawerIntent.ACTION_CLOSE_APP_DRAWER -> homeViewModel.requestDrawer(expand = false)
        }
    }
}
