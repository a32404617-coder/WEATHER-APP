package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.notification.StormNotificationManager
import com.example.ui.StormRadarApp
import com.example.ui.StormRadarViewModel
import com.example.ui.theme.StormRadarTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StormRadarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetTab = intent?.getStringExtra(StormNotificationManager.EXTRA_TARGET_TAB)
        val alertId = intent?.getStringExtra(StormNotificationManager.EXTRA_ALERT_ID)
        if (alertId != null) {
            val matching = viewModel.uiState.value.activeAlerts.find { it.id == alertId }
            viewModel.inspectAlert(matching)
        }

        setContent {
            StormRadarTheme {
                StormRadarApp(
                    viewModel = viewModel,
                    initialTab = targetTab
                )
            }
        }
    }
}
