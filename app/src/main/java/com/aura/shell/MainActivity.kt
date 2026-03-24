package com.aura.shell

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraShellTheme {
                val vm = homeViewModel
                val state by vm.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                HomeScreen(
                    state = state,
                    onAppClick = { pkg -> vm.onAppLaunch(pkg) },
                    onCommandBarClick = {
                        startActivity(Intent(this@MainActivity, CommandLayerActivity::class.java))
                    },
                    onMicClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Voice layer coming in Phase 3",
                            )
                        }
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

    override fun onResume() {
        super.onResume()
        homeViewModel.refreshRecents()
    }
}
