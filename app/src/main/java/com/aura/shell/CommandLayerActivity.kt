package com.aura.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aura.shell.command.CommandLayerViewModel
import com.aura.shell.command.CommandLayerViewModelFactory
import com.aura.shell.command.CommandSideEffect
import com.aura.shell.command.LauncherDrawerIntent
import com.aura.shell.ui.command.CommandLayerScreen
import com.aura.shell.ui.theme.AuraShellTheme
import kotlinx.coroutines.launch

class CommandLayerActivity : ComponentActivity() {

    private val viewModel: CommandLayerViewModel by viewModels {
        CommandLayerViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraShellTheme {
                val state by viewModel.uiState.collectAsState()
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }

                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModel.sideEffects.collect { effect ->
                                when (effect) {
                                    is CommandSideEffect.OpenAppDrawerAndFinish -> {
                                        sendDrawerIntent(LauncherDrawerIntent.ACTION_OPEN_APP_DRAWER)
                                    }
                                    is CommandSideEffect.CloseAppDrawerAndFinish -> {
                                        sendDrawerIntent(LauncherDrawerIntent.ACTION_CLOSE_APP_DRAWER)
                                    }
                                }
                            }
                        }
                    }
                }

                CommandLayerScreen(
                    state = state,
                    focusRequester = focusRequester,
                    onInputChange = viewModel::onInputChange,
                    onSubmit = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        viewModel.submitCommand()
                    },
                    onHistoryPick = { line ->
                        viewModel.applyHistoryLine(line)
                        focusRequester.requestFocus()
                    },
                    onAppPick = { pkg ->
                        viewModel.launchApp(pkg)
                    },
                    onRecentPick = { pkg ->
                        viewModel.launchApp(pkg)
                    },
                    onBack = { finish() },
                )
            }
        }
    }

    private fun sendDrawerIntent(action: String) {
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            this.action = action
            addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        startActivity(intent)
        finish()
    }
}
