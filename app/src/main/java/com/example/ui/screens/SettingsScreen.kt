package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.SignalNotificationManager
import com.example.ui.CashFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val preferredModel by viewModel.preferredModel.collectAsState()
    val autoInterval by viewModel.autoCaptureIntervalSeconds.collectAsState()
    val currentThemeMode by viewModel.appThemeMode.collectAsState()
    val activeBroker by viewModel.activeBrokerAccount.collectAsState()
    val brokerConnectionStatus by viewModel.brokerConnectionStatus.collectAsState()

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(SignalNotificationManager.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val greenColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val hasCustomApiKey = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (e: Throwable) {
        false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        // Global Theme Switcher Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_switcher_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = greenColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GLOBAL THEME MODE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = textColor
                        )
                    }

                    Surface(
                        color = greenColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (currentThemeMode) {
                                com.example.ui.theme.AppThemeMode.DARK -> "DARK CANVAS"
                                com.example.ui.theme.AppThemeMode.LIGHT -> "LIGHT CANVAS"
                                com.example.ui.theme.AppThemeMode.SYSTEM -> "SYSTEM DEFAULT"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = greenColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Seamlessly switch between dark trading mode and high-contrast light mode. All charts, signals, and analytics adapt dynamically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentThemeMode == com.example.ui.theme.AppThemeMode.DARK,
                        onClick = { viewModel.setAppThemeMode(com.example.ui.theme.AppThemeMode.DARK) },
                        label = { Text("🌙 Dark Mode") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = greenColor.copy(alpha = 0.2f),
                            selectedLabelColor = greenColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_dark_chip")
                    )

                    FilterChip(
                        selected = currentThemeMode == com.example.ui.theme.AppThemeMode.LIGHT,
                        onClick = { viewModel.setAppThemeMode(com.example.ui.theme.AppThemeMode.LIGHT) },
                        label = { Text("☀️ Light Mode") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = greenColor.copy(alpha = 0.2f),
                            selectedLabelColor = greenColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_chip")
                    )

                    FilterChip(
                        selected = currentThemeMode == com.example.ui.theme.AppThemeMode.SYSTEM,
                        onClick = { viewModel.setAppThemeMode(com.example.ui.theme.AppThemeMode.SYSTEM) },
                        label = { Text("📱 System") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = greenColor.copy(alpha = 0.2f),
                            selectedLabelColor = greenColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_system_chip")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = greenColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI API CREDENTIALS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = textColor
                        )
                    }

                    Surface(
                        color = if (hasCustomApiKey) greenColor.copy(alpha = 0.2f) else cardBorder,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasCustomApiKey) "ACTIVE" else "SECRETS PANEL / LOCAL FALLBACK",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (hasCustomApiKey) greenColor else Color(0xFFFFB300)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Google AI Studio automatically injects your GEMINI_API_KEY from Secrets at build time. If offline or no key is set, Cash Flow AI uses local high-speed chart vision intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI Model Engine Choice",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = preferredModel == "gemini-3.5-flash",
                onClick = { viewModel.setPreferredModel("gemini-3.5-flash") },
                label = { Text("gemini-3.5-flash (Fastest)") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor),
                modifier = Modifier.testTag("model_select_flash_chip")
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = preferredModel == "gemini-3.1-pro-preview",
                onClick = { viewModel.setPreferredModel("gemini-3.1-pro-preview") },
                label = { Text("gemini-3.1-pro-preview (Deep Reasoning)") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF82B1FF).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF82B1FF)),
                modifier = Modifier.testTag("model_select_pro_chip")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Live Screen Auto-Scan Interval",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(3, 5, 10, 30).forEach { sec ->
                FilterChip(
                    selected = autoInterval == sec,
                    onClick = { viewModel.setAutoCaptureInterval(sec) },
                    label = { Text("${sec}s") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth().testTag("notification_settings_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = greenColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Signal Notification System",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                    }

                    Surface(
                        color = if (hasNotificationPermission) greenColor.copy(alpha = 0.2f) else cardBorder,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasNotificationPermission) "ENABLED" else "PERMISSION REQUIRED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (hasNotificationPermission) greenColor else Color(0xFFFFB300)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Automatically sends local alerts when the AI Signal Engine detects a high-confidence pattern or live breakout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.weight(1f).testTag("enable_notifications_btn")
                        ) {
                            Text("Enable Alerts", color = greenColor)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.sendTestSignalNotification()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = greenColor,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).testTag("test_notification_btn")
                    ) {
                        Text("Send Test Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Broker Connection Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("broker_settings_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (brokerConnectionStatus.startsWith("CONNECTED")) greenColor.copy(alpha = 0.5f) else cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = greenColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broker Connection Bridge",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                    }

                    Surface(
                        color = if (brokerConnectionStatus.startsWith("CONNECTED")) greenColor.copy(alpha = 0.2f) else cardBorder,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (brokerConnectionStatus.startsWith("CONNECTED")) "ACTIVE" else "DISCONNECTED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (brokerConnectionStatus.startsWith("CONNECTED")) greenColor else Color(0xFFFFB300)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected: ${activeBroker?.accountName ?: "CashFlow Paper Engine"} (${activeBroker?.brokerType ?: "PAPER_TRADING"})\n" +
                            "Supports Deriv WebSocket API, MetaTrader 5 Bridge, Interactive Brokers (IBKR), OANDA v20 REST & Virtual Sandbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.setSelectedTab(3) },
                    colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_broker_tab_btn")
                ) {
                    Text("Open Broker Control Panel →", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = greenColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Risk Management Rules",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Max Capital Risk Per Trade: 1.0%", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                Text("Default Stop Loss Distance: 1.5 ATR", style = MaterialTheme.typography.bodySmall, color = subTextColor)
                Text("Minimum Risk/Reward Threshold: 1 : 2.0", style = MaterialTheme.typography.bodySmall, color = subTextColor)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PC & Phone Cross-Platform Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("pc_phone_compatibility_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, greenColor.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = greenColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PC & Phone Compatibility",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = "PC",
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Windows PC & Desktop Workstation",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                        Text(
                            text = "Run seamlessly via Windows Subsystem for Android (WSA), Google Play Games on PC, or Emulators (BlueStacks, LDPlayer). Layout automatically adapts to wide screens with a side navigation rail, window resizing, and mouse/keyboard navigation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Phone",
                        tint = greenColor,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Android Mobile Phones & Tablets",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                        Text(
                            text = "Runs natively as an installable APK on Android 8.0+. Supports real-time camera chart scanning, floating HUD over mobile trading apps, and background push notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subTextColor
                        )
                    }
                }
            }
        }
    }
}
