package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CashFlowApp
import com.example.ui.CashFlowViewModel
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CashFlowTheme

class MainActivity : ComponentActivity() {

    private var viewModelRef: CashFlowViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CashFlowViewModel = viewModel()
            viewModelRef = viewModel

            val appThemeMode by viewModel.appThemeMode.collectAsState()
            val isDarkTheme = when (appThemeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CashFlowTheme(darkTheme = isDarkTheme) {
                LaunchedEffect(intent) {
                    handleIntent(intent)
                }
                CashFlowApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra("OPEN_SCREEN") == "HUD") {
            viewModelRef?.setSelectedTab(0)
        }
    }
}
