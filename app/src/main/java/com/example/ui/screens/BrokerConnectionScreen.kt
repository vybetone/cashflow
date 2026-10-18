package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BrokerAccountEntity
import com.example.data.BrokerExecutionLog
import com.example.data.BrokerPosition
import com.example.data.MarketCategory
import com.example.data.MarketUniverse
import com.example.ui.CashFlowViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrokerConnectionScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val allAccounts by viewModel.allBrokerAccounts.collectAsState()
    val activeAccount by viewModel.activeBrokerAccount.collectAsState()
    val connectionStatus by viewModel.brokerConnectionStatus.collectAsState()
    val pingMs by viewModel.brokerPingMs.collectAsState()
    val openPositions by viewModel.openBrokerPositions.collectAsState()
    val executionLogs by viewModel.brokerExecutionLogs.collectAsState()
    val feedbackMessage by viewModel.brokerFeedbackMessage.collectAsState()
    val autoTradeUniverse by viewModel.autoTradeMarketUniverse.collectAsState()

    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0: BROKERS & ACCOUNTS, 1: OPEN POSITIONS, 2: AUTO-TRADE ENGINE, 3: AUDIT LOGS
    var showAccountEditDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<BrokerAccountEntity?>(null) }
    var showNewOrderDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBrokerFeedbackMessage()
        }
    }

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)
    val cyanColor = Color(0xFF00E5FF)
    val cardBg = Color(0xFF131822)
    val surfaceBorder = Color(0xFF263238)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("broker_connection_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Live Broker Telemetry Header Card
            BrokerHeaderCard(
                activeAccount = activeAccount,
                connectionStatus = connectionStatus,
                pingMs = pingMs,
                openPositionsCount = openPositions.size,
                floatingPnL = openPositions.sumOf { it.unrealizedPnL },
                onDisconnect = { viewModel.disconnectBroker() },
                onQuickPing = { activeAccount?.let { viewModel.testBrokerPing(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedSectionTab,
                containerColor = Color(0xFF101520),
                contentColor = greenColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSectionTab]),
                        color = greenColor,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSectionTab == 0,
                    onClick = { selectedSectionTab = 0 },
                    text = {
                        Text(
                            "BROKERS & CONNECT",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedSectionTab == 1,
                    onClick = { selectedSectionTab = 1 },
                    text = {
                        Text(
                            "POSITIONS (${openPositions.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedSectionTab == 2,
                    onClick = { selectedSectionTab = 2 },
                    text = {
                        Text(
                            "AUTOTRADE AI",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedSectionTab == 3,
                    onClick = { selectedSectionTab = 3 },
                    text = {
                        Text(
                            "AUDIT LOGS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Tab Content
            when (selectedSectionTab) {
                0 -> {
                    // Broker Profiles Section
                    BrokersManagementContent(
                        allAccounts = allAccounts,
                        activeAccount = activeAccount,
                        connectionStatus = connectionStatus,
                        onConnect = { viewModel.connectBroker(it) },
                        onTestPing = { viewModel.testBrokerPing(it) },
                        onEdit = {
                            accountToEdit = it
                            showAccountEditDialog = true
                        },
                        onDelete = { viewModel.deleteBrokerAccount(it) },
                        onAddNew = {
                            accountToEdit = null
                            showAccountEditDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                1 -> {
                    // Open Positions Section
                    OpenPositionsContent(
                        positions = openPositions,
                        onClosePosition = { viewModel.closeBrokerPosition(it) },
                        onCloseAll = { viewModel.closeAllBrokerPositions() },
                        onOpenNewOrder = { showNewOrderDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                2 -> {
                    // Autonomous AI Execution Rules Section
                    AutoTradeConfigContent(
                        activeAccount = activeAccount,
                        marketUniverse = autoTradeUniverse,
                        onSelectMarketUniverse = { viewModel.setAutoTradeMarketUniverse(it) },
                        onToggleAutoTrade = { viewModel.toggleBrokerAutoTrade(it) },
                        onUpdateRisk = { threshold, lotSize, maxRisk, slTp ->
                            viewModel.updateBrokerRiskSettings(threshold, lotSize, maxRisk, slTp)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                3 -> {
                    // Execution Audit Trail Logs
                    AuditLogsContent(
                        logs = executionLogs,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Dialog for Adding or Editing Broker Connection
        if (showAccountEditDialog) {
            BrokerAccountEditDialog(
                account = accountToEdit,
                onDismiss = { showAccountEditDialog = false },
                onSave = { updated ->
                    viewModel.saveBrokerAccount(updated)
                    showAccountEditDialog = false
                }
            )
        }

        // Dialog for 1-Tap Manual Market Order
        if (showNewOrderDialog) {
            ManualOrderDialog(
                activeAccount = activeAccount,
                onDismiss = { showNewOrderDialog = false },
                onSubmitOrder = { symbol, side, price, sl, tp, volume ->
                    viewModel.executeManualTrade(symbol, side, price, sl, tp, volume)
                    showNewOrderDialog = false
                }
            )
        }
    }
}

@Composable
private fun BrokerHeaderCard(
    activeAccount: BrokerAccountEntity?,
    connectionStatus: String,
    pingMs: Long,
    openPositionsCount: Int,
    floatingPnL: Double,
    onDisconnect: () -> Unit,
    onQuickPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionStatus.startsWith("CONNECTED")
    val statusColor = when (connectionStatus) {
        "CONNECTED_LIVE", "CONNECTED_PAPER" -> Color(0xFF00E5FF)
        "CONNECTED_DEMO" -> Color(0xFF00E5FF)
        "CONNECTING" -> Color(0xFFFFB300)
        "ERROR" -> Color(0xFFFF5252)
        else -> Color(0xFF90A4AE)
    }

    Card(
        modifier = modifier.testTag("broker_header_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isConnected) statusColor.copy(alpha = 0.5f) else Color(0xFF263238))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Broker Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = activeAccount?.accountName ?: "NO BROKER CONNECTED",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (activeAccount != null) "${activeAccount.brokerType} • Account: ${activeAccount.accountId}" else "Select a broker below to link trading account",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (connectionStatus) {
                                    "CONNECTED_LIVE", "CONNECTED_PAPER" -> "PAPER ACTIVE (SIMULATED)"
                                    "CONNECTED_DEMO" -> "DEMO ACTIVE (SIMULATED)"
                                    "CONNECTING" -> "CONNECTING..."
                                    "ERROR" -> "AUTH ERROR"
                                    else -> "STANDBY"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = statusColor
                            )
                        }
                    }

                    if (isConnected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDisconnect,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Account Financials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = "BALANCE",
                    value = "$${String.format(Locale.US, "%,.2f", activeAccount?.balance ?: 0.0)}",
                    accentColor = Color.White
                )
                MetricColumn(
                    title = "EQUITY",
                    value = "$${String.format(Locale.US, "%,.2f", activeAccount?.equity ?: 0.0)}",
                    accentColor = Color(0xFF00E5FF)
                )
                MetricColumn(
                    title = "FREE MARGIN",
                    value = "$${String.format(Locale.US, "%,.2f", activeAccount?.freeMargin ?: 0.0)}",
                    accentColor = Color.White
                )
                MetricColumn(
                    title = "OPEN P&L",
                    value = if (floatingPnL >= 0) "+$${String.format(Locale.US, "%.2f", floatingPnL)}" else "-$${String.format(Locale.US, "%.2f", Math.abs(floatingPnL))}",
                    accentColor = if (floatingPnL >= 0) Color(0xFF00E676) else Color(0xFFFF5252)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Server Endpoint & Latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lan,
                        contentDescription = null,
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeAccount?.serverOrEndpoint ?: "Endpoint: Not connected",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF78909C)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onQuickPing() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF00E676) else Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) "${pingMs}ms" else "-- ms",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isConnected) Color(0xFF00E676) else Color(0xFF78909C)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    title: String,
    value: String,
    accentColor: Color
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color(0xFF78909C)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            ),
            color = accentColor
        )
    }
}

@Composable
private fun BrokersManagementContent(
    allAccounts: List<BrokerAccountEntity>,
    activeAccount: BrokerAccountEntity?,
    connectionStatus: String,
    onConnect: (BrokerAccountEntity) -> Unit,
    onTestPing: (BrokerAccountEntity) -> Unit,
    onEdit: (BrokerAccountEntity) -> Unit,
    onDelete: (BrokerAccountEntity) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Configured Broker Bridges",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Official connections to Deriv, MT5, IBKR, OANDA & Sandbox",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF90A4AE)
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_broker_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Account", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allAccounts) { account ->
                val isActive = activeAccount?.id == account.id && connectionStatus.startsWith("CONNECTED")
                BrokerAccountCard(
                    account = account,
                    isActive = isActive,
                    onConnect = { onConnect(account) },
                    onTestPing = { onTestPing(account) },
                    onEdit = { onEdit(account) },
                    onDelete = { onDelete(account) }
                )
            }
        }
    }
}

@Composable
private fun BrokerAccountCard(
    account: BrokerAccountEntity,
    isActive: Boolean,
    onConnect: () -> Unit,
    onTestPing: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val greenColor = Color(0xFF00E676)
    val brokerBadgeColor = when (account.brokerType) {
        "DERIV" -> Color(0xFFFF444F) // Official Deriv Red
        "METATRADER_5" -> Color(0xFF2962FF) // MT5 Blue
        "INTERACTIVE_BROKERS" -> Color(0xFFD32F2F) // IBKR Crimson
        "OANDA" -> Color(0xFFFF9100) // OANDA Amber
        else -> Color(0xFF00E676) // CashFlow Paper
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("broker_item_${account.brokerType}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isActive) greenColor.copy(alpha = 0.8f) else Color(0xFF263238)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = brokerBadgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, brokerBadgeColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = account.brokerType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = brokerBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (account.environment == "LIVE") "PAPER (SIMULATED)" else "DEMO (SIMULATED)",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFB0BEC5), modifier = Modifier.size(16.dp))
                    }
                    if (account.brokerType != "PAPER_TRADING") {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = account.accountName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White
            )

            Text(
                text = "ID: ${account.accountId} • Server: ${account.serverOrEndpoint}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF90A4AE)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Balance: $${String.format(Locale.US, "%,.2f", account.balance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00E676)
                    )
                    Text(
                        text = "Auto-Trade: ${if (account.autoTradeEnabled) "ACTIVE (>=${account.minConfidenceThreshold}%)" else "OFF"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (account.autoTradeEnabled) Color(0xFF00E676) else Color(0xFF78909C)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTestPing,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFF37474F))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF90CAF9), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping", color = Color(0xFF90CAF9), fontSize = 12.sp)
                    }

                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFF1E293B) else greenColor,
                            contentColor = if (isActive) Color(0xFF00E676) else Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isActive) "CONNECTED" else "CONNECT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenPositionsContent(
    positions: List<BrokerPosition>,
    onClosePosition: (String) -> Unit,
    onCloseAll: () -> Unit,
    onOpenNewOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalFloatingPnL = positions.sumOf { it.unrealizedPnL }
    val pnlColor = if (totalFloatingPnL >= 0) Color(0xFF00E676) else Color(0xFFFF5252)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Broker Positions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Unrealized Floating P&L: ${if (totalFloatingPnL >= 0) "+$" else "-$"}${String.format(Locale.US, "%.2f", Math.abs(totalFloatingPnL))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = pnlColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (positions.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onCloseAll,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Close All", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onOpenNewOrder,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1-Tap Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (positions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF131822), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF263238), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFF455A64), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No open positions on broker.", color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                    Text("Signals executed via HUD or Auto-Trade will appear live here.", color = Color(0xFF607D8B), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(positions) { pos ->
                    PositionCard(
                        position = pos,
                        onClose = { onClosePosition(pos.ticketId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionCard(
    position: BrokerPosition,
    onClose: () -> Unit
) {
    val isBuy = position.side.equals("BUY", ignoreCase = true)
    val sideColor = if (isBuy) Color(0xFF00E676) else Color(0xFFFF5252)
    val pnlColor = if (position.unrealizedPnL >= 0) Color(0xFF00E676) else Color(0xFFFF5252)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF263238))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = sideColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, sideColor)
                    ) {
                        Text(
                            text = position.side,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = sideColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = position.symbol,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${position.volume} Lots",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF90A4AE)
                    )
                }

                Surface(
                    color = pnlColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, pnlColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (position.unrealizedPnL >= 0) "+$${String.format(Locale.US, "%.2f", position.unrealizedPnL)}" else "-$${String.format(Locale.US, "%.2f", Math.abs(position.unrealizedPnL))}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                        color = pnlColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Open: ${position.openPrice}  →  Current: ${String.format(Locale.US, "%.4f", position.currentPrice)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White
                    )
                    Text(
                        text = "SL: ${position.stopLoss} | TP: ${position.takeProfit} | Ticket: #${position.ticketId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF78909C)
                    )
                }

                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Close", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AutoTradeConfigContent(
    activeAccount: BrokerAccountEntity?,
    marketUniverse: MarketCategory,
    onSelectMarketUniverse: (MarketCategory) -> Unit,
    onToggleAutoTrade: (Boolean) -> Unit,
    onUpdateRisk: (Int, Double, Double, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var threshold by remember(activeAccount) { mutableIntStateOf(activeAccount?.minConfidenceThreshold ?: 80) }
    var lotSize by remember(activeAccount) { mutableStateOf((activeAccount?.defaultLotSize ?: 0.10).toString()) }
    var maxRisk by remember(activeAccount) { mutableStateOf((activeAccount?.maxRiskPercentage ?: 1.0).toString()) }
    var autoSlTp by remember(activeAccount) { mutableStateOf(activeAccount?.autoStopLossTakeProfit ?: true) }

    val isArmed = activeAccount?.autoTradeEnabled == true
    val greenColor = Color(0xFF00E676)
    val cyanColor = Color(0xFF00E5FF)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CARD 1: CONFIRMED CASH FLOW TRADE UNIVERSE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = greenColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.4f)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = greenColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CONFIRMED TRADE UNIVERSE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "Cash Flow AI Active Trading Scope",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = greenColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, greenColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "VERIFIED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = greenColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Select active auto-trade market bucket:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0BEC5)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5 CATEGORIES ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MarketCategory.values().forEach { category ->
                            val isSelected = marketUniverse == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectMarketUniverse(category) },
                                label = {
                                    Text(
                                        text = when (category) {
                                            MarketCategory.FOREX -> "1. FOREX"
                                            MarketCategory.GOLD -> "2. GOLD"
                                            MarketCategory.CRYPTO -> "3. CRYPTO"
                                            MarketCategory.STOCKS -> "4. STOCKS"
                                            MarketCategory.ALL_MARKETS -> "5. ALL"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = greenColor.copy(alpha = 0.2f),
                                    selectedLabelColor = greenColor,
                                    containerColor = Color(0xFF1A2230),
                                    labelColor = Color(0xFF90A4AE)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) greenColor else Color(0xFF263238),
                                    selectedBorderColor = greenColor,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DISPLAY INSTRUMENTS COVERED UNDER SELECTED CATEGORY
                    val activeAssets = MarketUniverse.getAssetsForCategory(marketUniverse)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0B0E14),
                        border = BorderStroke(1.dp, Color(0xFF263238))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = marketUniverse.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = cyanColor
                                )
                                Text(
                                    text = "${activeAssets.size} Assets Armed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF78909C)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeAssets.forEach { asset ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B),
                                        border = BorderStroke(0.5.dp, Color(0xFF37474F))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(greenColor, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = asset.symbol,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 2: AUTONOMOUS AI EXECUTION RULES
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isArmed) greenColor else Color(0xFF263238))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (isArmed) greenColor else Color(0xFF78909C),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Autonomous AI Execution",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = if (isArmed) "Orders execute automatically when AI detects high confidence" else "Manual approval required for each signal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }

                        Switch(
                            checked = isArmed,
                            onCheckedChange = { onToggleAutoTrade(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = greenColor,
                                uncheckedThumbColor = Color(0xFF78909C),
                                uncheckedTrackColor = Color(0xFF263238)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Threshold Slider
                    Text(
                        text = "Minimum AI Signal Confidence: $threshold%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { threshold = it.toInt() },
                        valueRange = 70f..95f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = greenColor,
                            activeTrackColor = greenColor,
                            inactiveTrackColor = Color(0xFF263238)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lot Size & Risk
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = lotSize,
                            onValueChange = { lotSize = it },
                            label = { Text("Default Lots") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = greenColor,
                                unfocusedBorderColor = Color(0xFF37474F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = maxRisk,
                            onValueChange = { maxRisk = it },
                            label = { Text("Max Risk %") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = greenColor,
                                unfocusedBorderColor = Color(0xFF37474F),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-Attach Stop Loss & Take Profit from AI Levels",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Switch(
                            checked = autoSlTp,
                            onCheckedChange = { autoSlTp = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = greenColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val parsedLots = lotSize.toDoubleOrNull() ?: 0.10
                            val parsedRisk = maxRisk.toDoubleOrNull() ?: 1.0
                            onUpdateRisk(threshold, parsedLots, parsedRisk, autoSlTp)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Execution Parameters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsContent(
    logs: List<BrokerExecutionLog>,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(modifier = modifier) {
        Text(
            text = "Real-Time Broker Execution Audit Trail",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Every trade request, ping, and fill is timestamped here for institutional transparency",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF90A4AE)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(logs) { log ->
                val logColor = if (log.success) Color(0xFF00E676) else Color(0xFFFF5252)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101520)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "[${dateFormat.format(Date(log.timestamp))}]",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFF78909C)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.brokerType,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = logColor
                                )
                            }
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokerAccountEditDialog(
    account: BrokerAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (BrokerAccountEntity) -> Unit
) {
    var brokerType by remember { mutableStateOf(account?.brokerType ?: "DERIV") }
    var accountName by remember { mutableStateOf(account?.accountName ?: "") }
    var accountId by remember { mutableStateOf(account?.accountId ?: "") }
    var tokenOrPass by remember { mutableStateOf(account?.apiTokenOrPassword ?: "") }
    var serverOrUrl by remember { mutableStateOf(account?.serverOrEndpoint ?: "") }
    var env by remember { mutableStateOf(account?.environment ?: "DEMO") }
    var balance by remember { mutableStateOf((account?.balance ?: 10000.0).toString()) }

    val greenColor = Color(0xFF00E676)

    // Automatically fill recommended defaults when brokerType changes if new
    LaunchedEffect(brokerType) {
        if (account == null) {
            when (brokerType) {
                "DERIV" -> {
                    accountName = "Deriv Official Account"
                    serverOrUrl = "wss://ws.derivws.com/websockets/v3"
                    if (accountId.isBlank()) accountId = "CR-194821"
                }
                "METATRADER_5" -> {
                    accountName = "MetaTrader 5 Bridge"
                    serverOrUrl = "MetaQuotes-Demo"
                    if (accountId.isBlank()) accountId = "2094812"
                }
                "INTERACTIVE_BROKERS" -> {
                    accountName = "Interactive Brokers Web Gateway"
                    serverOrUrl = "https://127.0.0.1:5000/v1/api"
                    if (accountId.isBlank()) accountId = "U1849204"
                }
                "OANDA" -> {
                    accountName = "OANDA v20 Practice"
                    serverOrUrl = "https://api-fxpractice.oanda.com/v3"
                    if (accountId.isBlank()) accountId = "001-004-9843-001"
                }
                "PAPER_TRADING" -> {
                    accountName = "CashFlow Virtual Paper Sandbox"
                    serverOrUrl = "Local High-Speed Virtual Engine"
                    if (accountId.isBlank()) accountId = "VIRTUAL-001"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (account != null) "Edit Broker Connection" else "Link New Broker",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Broker Platform:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF90A4AE))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DERIV", "METATRADER_5", "INTERACTIVE_BROKERS", "OANDA").forEach { type ->
                        FilterChip(
                            selected = brokerType == type,
                            onClick = { brokerType = type },
                            label = { Text(when (type) {
                                "DERIV" -> "Deriv"
                                "METATRADER_5" -> "MT5"
                                "INTERACTIVE_BROKERS" -> "IBKR"
                                "OANDA" -> "OANDA"
                                else -> type
                            }, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = greenColor.copy(alpha = 0.2f),
                                selectedLabelColor = greenColor
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Account Label") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountId,
                    onValueChange = { accountId = it },
                    label = { Text("Account / Login ID (e.g. CR..., MT5 #, IBKR #)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tokenOrPass,
                    onValueChange = { tokenOrPass = it },
                    label = { Text("API Token / Password / Secret") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serverOrUrl,
                    onValueChange = { serverOrUrl = it },
                    label = { Text("Server / WebSocket Endpoint") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = env == "DEMO",
                        onClick = { env = "DEMO" },
                        label = { Text("DEMO / PRACTICE") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = env == "LIVE",
                        onClick = { env = "LIVE" },
                        label = { Text("SIMULATED PAPER SANDBOX") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = BrokerAccountEntity(
                        id = account?.id ?: 0,
                        brokerType = brokerType,
                        accountName = accountName.ifBlank { "$brokerType Account" },
                        accountId = accountId.ifBlank { "DEMO-001" },
                        apiTokenOrPassword = tokenOrPass.ifBlank { "TOKEN_SANDBOX" },
                        serverOrEndpoint = serverOrUrl.ifBlank { "default-server" },
                        environment = env,
                        balance = balance.toDoubleOrNull() ?: 10000.0,
                        equity = balance.toDoubleOrNull() ?: 10000.0,
                        freeMargin = balance.toDoubleOrNull() ?: 10000.0,
                        isConnected = account?.isConnected ?: false
                    )
                    onSave(entity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.Black)
            ) {
                Text("Save Broker", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB0BEC5))
            }
        },
        containerColor = Color(0xFF131822)
    )
}

@Composable
private fun ManualOrderDialog(
    activeAccount: BrokerAccountEntity?,
    onDismiss: () -> Unit,
    onSubmitOrder: (String, String, Double, Double, Double, Double) -> Unit
) {
    var symbol by remember { mutableStateOf("EUR/USD") }
    var side by remember { mutableStateOf("BUY") }
    var price by remember { mutableStateOf("1.0850") }
    var stopLoss by remember { mutableStateOf("1.0810") }
    var takeProfit by remember { mutableStateOf("1.0920") }
    var volume by remember { mutableStateOf("0.10") }

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚡ Instant Broker Execution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Routing order to: ${activeAccount?.accountName ?: "CashFlow Paper Broker"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00E5FF)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { side = "BUY" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == "BUY") greenColor else Color(0xFF1E293B),
                            contentColor = if (side == "BUY") Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BUY / LONG", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { side = "SELL" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == "SELL") redColor else Color(0xFF1E293B),
                            contentColor = if (side == "SELL") Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SELL / SHORT", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Asset / Symbol (EUR/USD, BTC/USD, Vol 75)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = volume,
                        onValueChange = { volume = it },
                        label = { Text("Lots") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Entry Price") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stopLoss,
                        onValueChange = { stopLoss = it },
                        label = { Text("Stop Loss") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = redColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = takeProfit,
                        onValueChange = { takeProfit = it },
                        label = { Text("Take Profit") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = greenColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 1.0
                    val sl = stopLoss.toDoubleOrNull() ?: (p * 0.98)
                    val tp = takeProfit.toDoubleOrNull() ?: (p * 1.02)
                    val vol = volume.toDoubleOrNull() ?: 0.10
                    onSubmitOrder(symbol, side, p, sl, tp, vol)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (side == "BUY") greenColor else redColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Transmit Order", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFB0BEC5))
            }
        },
        containerColor = Color(0xFF131822)
    )
}
