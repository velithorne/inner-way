package com.velithorne.innerway.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.velithorne.innerway.services.SensorPollingService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application),
        )[VelithorneViewModel::class.java]

        setContent {
            VelithorneApp(viewModel = viewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        SensorPollingService.start(this)
    }

    override fun onStop() {
        SensorPollingService.stop(this)
        super.onStop()
    }
}
