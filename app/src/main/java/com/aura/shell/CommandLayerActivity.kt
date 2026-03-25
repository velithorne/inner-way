package com.aura.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aura.shell.command.CommandLayerViewModel
import com.aura.shell.command.CommandLayerViewModelFactory
import com.aura.shell.command.CommandSideEffect
import com.aura.shell.command.LauncherDrawerIntent
import com.aura.shell.ui.command.CommandLayerScreen
import com.aura.shell.ui.theme.AuraShellTheme
import com.aura.shell.voice.SpeechInputManager
import com.aura.shell.voice.VoiceSurfaceState
import kotlinx.coroutines.launch

class CommandLayerActivity : ComponentActivity() {

    private val viewModel: CommandLayerViewModel by viewModels {
        CommandLayerViewModelFactory(application)
    }

    private lateinit var speech: SpeechInputManager

    override fun onResume() {
        super.onResume()
        LaunchActivityProvider.attach(this)
    }

    override fun onPause() {
        LaunchActivityProvider.detach(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speech = SpeechInputManager(applicationContext)
        enableEdgeToEdge()

        val startListeningFromLaunch = intent.getBooleanExtra(EXTRA_START_LISTENING, false)

        lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> viewModel.onForegroundChanged(true)
                    Lifecycle.Event.ON_PAUSE -> viewModel.onForegroundChanged(false)
                    else -> {}
                }
            },
        )

        setContent {
            AuraShellTheme {
                val state by viewModel.uiState.collectAsState()
                val focusRequester = remember { FocusRequester() }
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        startVoiceCapture()
                    } else {
                        viewModel.setVoiceState(
                            VoiceSurfaceState.Error(
                                "Microphone access is off. You can still type commands below.",
                            ),
                        )
                    }
                }

                val passiveMicLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        viewModel.setPassiveHandsFreeEnabled(true)
                    } else {
                        viewModel.setPassiveHandsFreeEnabled(false)
                    }
                }

                fun onPassiveToggle(enabled: Boolean) {
                    if (!enabled) {
                        viewModel.setPassiveHandsFreeEnabled(false)
                        return
                    }
                    if (hasMicPermission()) {
                        viewModel.setPassiveHandsFreeEnabled(true)
                    } else {
                        passiveMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
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
                                    is CommandSideEffect.FinishAfterGoHome -> {
                                        finish()
                                    }
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(startListeningFromLaunch) {
                    if (startListeningFromLaunch) {
                        when {
                            hasMicPermission() -> startVoiceCapture()
                            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                                viewModel.setVoiceState(VoiceSurfaceState.PermissionNeeded)
                            }
                            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        focusRequester.requestFocus()
                        keyboard?.show()
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
                    onHistoryPick = { entry ->
                        viewModel.applyHistoryEntry(entry)
                        focusRequester.requestFocus()
                    },
                    onAppPick = { pkg ->
                        viewModel.launchApp(pkg)
                    },
                    onRecentPick = { pkg ->
                        viewModel.launchApp(pkg)
                    },
                    onMicClick = {
                        when {
                            hasMicPermission() -> startVoiceCapture()
                            state.voice is VoiceSurfaceState.PermissionNeeded -> {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                                viewModel.setVoiceState(VoiceSurfaceState.PermissionNeeded)
                            }
                            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onVoiceCancel = {
                        speech.stopListening()
                        viewModel.setVoiceState(VoiceSurfaceState.Idle)
                    },
                    onPermissionRetry = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onPassiveHandsFreeChange = { onPassiveToggle(it) },
                    onBack = { finish() },
                )
            }
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun startVoiceCapture() {
        viewModel.onTapMicStarted()
        if (!speech.isAvailable()) {
            viewModel.setVoiceState(
                VoiceSurfaceState.Error("Voice input isn’t available. Type your command instead."),
            )
            return
        }
        viewModel.setVoiceState(VoiceSurfaceState.Listening)
        speech.startListening(
            onReady = { viewModel.setVoiceState(VoiceSurfaceState.Listening) },
            onPartialResult = { partial -> viewModel.onInputChange(partial) },
            onFinalResult = { text ->
                viewModel.setVoiceState(VoiceSurfaceState.Processing)
                viewModel.applySpeechTranscriptAndSubmit(text)
            },
            onError = { msg -> viewModel.setVoiceState(VoiceSurfaceState.Error(msg)) },
        )
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

    override fun onDestroy() {
        speech.stopListening()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_START_LISTENING = "com.aura.shell.extra.START_LISTENING"
    }
}
