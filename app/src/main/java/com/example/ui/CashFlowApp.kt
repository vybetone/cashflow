package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AiAnalyticsScreen
import com.example.ui.screens.BrokerConnectionScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.ScreenAnalysisHudScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TradeJournalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowApp(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingChart.collectAsState()

    val greenColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive PC / Tablet / Large Screen Layout with NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                NavigationRail(
                    containerColor = surfaceColor,
                    contentColor = textColor,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            Surface(
                                color = greenColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = greenColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "CASH FLOW",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                color = textColor
                            )
                            Surface(
                                color = Color(0xFF1E88E5).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.5f)),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Computer,
                                        contentDescription = null,
                                        tint = Color(0xFF90CAF9),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PC / DESKTOP",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF90CAF9)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("cashflow_side_nav")
                ) {
                    val railColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = greenColor,
                        selectedTextColor = greenColor,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = greenColor.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        icon = { Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = "Screen HUD") },
                        label = { Text("Screen HUD", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_screen_hud")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") },
                        label = { Text("Camera", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_camera")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        icon = { Icon(Icons.Filled.Book, contentDescription = "Journal") },
                        label = { Text("Journal", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_journal")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.setSelectedTab(3) },
                        icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Broker") },
                        label = { Text("Broker", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_broker")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.setSelectedTab(4) },
                        icon = { Icon(Icons.Filled.Psychology, contentDescription = "AI Assistant") },
                        label = { Text("AI Assistant", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_analytics")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 5,
                        onClick = { viewModel.setSelectedTab(5) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontFamily = FontFamily.Monospace) },
                        colors = railColors,
                        modifier = Modifier.testTag("nav_item_settings")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Status at bottom of rail
                    Surface(
                        color = if (isAnalyzing) Color(0xFFFFB300).copy(alpha = 0.2f) else greenColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnalyzing) Color(0xFFFFB300) else greenColor),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = if (isAnalyzing) "ANALYZING" else "QUANT ON",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isAnalyzing) Color(0xFFFFB300) else greenColor
                        )
                    }
                }

                // Main Content Pane for PC
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "REAL-TIME SCREEN HUD"
                                        1 -> "CAMERA CHART SCANNER"
                                        2 -> "QUANT TRADE JOURNAL"
                                        3 -> "BROKER BRIDGE & AUTOTRADE"
                                        4 -> "AI TRADING ASSISTANT & ANALYTICS"
                                        5 -> "SYSTEM SETTINGS"
                                        else -> "CASH FLOW AI"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = textColor
                                )

                                Surface(
                                    color = if (isAnalyzing) Color(0xFFFFB300).copy(alpha = 0.2f) else greenColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnalyzing) Color(0xFFFFB300) else greenColor),
                                    modifier = Modifier.padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = if (isAnalyzing) "ANALYZING CHART..." else "LIVE QUANT ACTIVE",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isAnalyzing) Color(0xFFFFB300) else greenColor
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = surfaceColor,
                            titleContentColor = textColor
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                        ) {
                            when (selectedTab) {
                                0 -> ScreenAnalysisHudScreen(viewModel = viewModel)
                                1 -> CameraScannerScreen(viewModel = viewModel)
                                2 -> TradeJournalScreen(viewModel = viewModel)
                                3 -> BrokerConnectionScreen(viewModel = viewModel)
                                4 -> AiAnalyticsScreen(viewModel = viewModel)
                                5 -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        } else {
            // Phone Layout (Compact width < 600dp)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = greenColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = greenColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "CASH FLOW AI",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    color = if (isAnalyzing) Color(0xFFFFB300).copy(alpha = 0.2f) else greenColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnalyzing) Color(0xFFFFB300) else greenColor)
                                ) {
                                    Text(
                                        text = if (isAnalyzing) "ANALYZING..." else "LIVE QUANT",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (isAnalyzing) Color(0xFFFFB300) else greenColor
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = surfaceColor,
                            titleContentColor = textColor
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = surfaceColor,
                        contentColor = textColor,
                        modifier = Modifier.testTag("cashflow_bottom_nav")
                    ) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = greenColor,
                            selectedTextColor = greenColor,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = greenColor.copy(alpha = 0.15f)
                        )

                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            icon = { Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = "Screen HUD") },
                            label = { Text("Screen HUD", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_screen_hud")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") },
                            label = { Text("Camera", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_camera")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            icon = { Icon(Icons.Filled.Book, contentDescription = "Journal") },
                            label = { Text("Journal", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_journal")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.setSelectedTab(3) },
                            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Broker") },
                            label = { Text("Broker", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_broker")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { viewModel.setSelectedTab(4) },
                            icon = { Icon(Icons.Filled.Psychology, contentDescription = "AI Assistant") },
                            label = { Text("AI Assistant", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_analytics")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 5,
                            onClick = { viewModel.setSelectedTab(5) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontFamily = FontFamily.Monospace) },
                            colors = itemColors,
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(backgroundColor)
                ) {
                    when (selectedTab) {
                        0 -> ScreenAnalysisHudScreen(viewModel = viewModel)
                        1 -> CameraScannerScreen(viewModel = viewModel)
                        2 -> TradeJournalScreen(viewModel = viewModel)
                        3 -> BrokerConnectionScreen(viewModel = viewModel)
                        4 -> AiAnalyticsScreen(viewModel = viewModel)
                        5 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

