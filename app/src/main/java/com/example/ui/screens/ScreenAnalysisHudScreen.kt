package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CashFlowViewModel
import com.example.ui.components.AiSignal90Card
import com.example.ui.components.LiveChartOverlayCanvas
import com.example.ui.components.LiveSignalCard
import com.example.ui.components.MarketUniverseSelectorBar
import com.example.data.MarketAsset
import com.example.data.MarketCategory
import com.example.data.MarketUniverse

@Composable
fun ScreenAnalysisHudScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingChart.collectAsState()
    val isScreenCaptureActive by viewModel.isScreenCaptureActive.collectAsState()
    val latestSignal by viewModel.latestActiveSignal.collectAsState()
    val allSignals by viewModel.filteredSignals.collectAsState()
    val highConfidenceSignal by viewModel.highConfidenceAiSignal.collectAsState()
    val activeBroker by viewModel.activeBrokerAccount.collectAsState()
    val brokerConnectionStatus by viewModel.brokerConnectionStatus.collectAsState()
    val selectedCategory by viewModel.selectedMarketCategory.collectAsState()
    val selectedAsset by viewModel.selectedMarketAsset.collectAsState()

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!com.example.data.SignalNotificationManager.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onMediaProjectionGranted(context, result.resultCode, result.data)
        } else {
            viewModel.onMediaProjectionDenied()
        }
    }

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("screen_analysis_hud_screen")
    ) {
        val isWideScreen = maxWidth >= 600.dp
        val isDesktopExpanded = maxWidth >= 960.dp

        if (isWideScreen) {
            // MULTI-PANE GRID / SPLIT DESKTOP LAYOUT (PC Screen / Tablet)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // LEFT PANE: Sticky Live Chart Canvas Window & Controls
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.48f else 0.50f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Control Bar
                    ScreenControlBar(
                        isScreenCaptureActive = isScreenCaptureActive,
                        greenColor = greenColor,
                        redColor = redColor,
                        onToggleScreenCapture = {
                            if (isScreenCaptureActive) {
                                viewModel.stopScreenCaptureService(context)
                            } else {
                                if (mediaProjectionManager != null) {
                                    try {
                                        val intent = mediaProjectionManager.createScreenCaptureIntent()
                                        mediaProjectionLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        viewModel.startInAppSimulationCapture()
                                    }
                                } else {
                                    viewModel.startInAppSimulationCapture()
                                }
                            }
                        }
                    )

                    // Confirmed Markets Universe Selector (1 FOREX, 2 GOLD, 3 CRYPTO, 4 STOCKS, 5 ALL OF THEM)
                    MarketUniverseSelectorBar(
                        selectedCategory = selectedCategory,
                        selectedAsset = selectedAsset,
                        isScanning = isAnalyzing,
                        onCategorySelected = { viewModel.setMarketCategory(it) },
                        onAssetSelected = { viewModel.setSelectedMarketAsset(it) },
                        onScanAllMarkets = { viewModel.scanAllConfirmedMarkets() }
                    )

                    // Live Chart Canvas Window
                    LiveChartWindow(
                        latestSignal = latestSignal,
                        selectedAsset = selectedAsset,
                        isAnalyzing = isAnalyzing,
                        greenColor = greenColor,
                        redColor = redColor,
                        onScanChart = {
                            val dummyBitmap = createSimulatedChartBitmap()
                            viewModel.processChartFrameBitmap(dummyBitmap, "SCREEN_ANALYSIS", selectedAsset)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Desktop Mode Status Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00E676).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = greenColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MULTI-PANE HUD ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = greenColor
                                )
                                Text(
                                    text = "Split layout enabled for PC screen: Live chart frame on the left, quantitative signal feed on the right.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF90A4AE)
                                )
                            }
                        }
                    }
                }

                // RIGHT PANE: Real-Time Signal Matrix Grid
                Column(
                    modifier = Modifier
                        .weight(if (isDesktopExpanded) 0.52f else 0.50f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Real-Time AI Trading Signals",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${allSignals.size} Signals",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF82B1FF)
                            )
                        }
                    }

                    // Quick Broker Execution Banner
                    Surface(
                        color = Color(0xFF131822),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { viewModel.setSelectedTab(3) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (brokerConnectionStatus.startsWith("CONNECTED")) Color(0xFF00E676) else Color(0xFF90A4AE),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeBroker?.accountName ?: "CashFlow Paper Engine",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (activeBroker?.autoTradeEnabled == true) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF263238),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (activeBroker?.autoTradeEnabled == true) "AUTO-EXECUTE ON" else "1-TAP BROKER READY",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (activeBroker?.autoTradeEnabled == true) Color(0xFF00E676) else Color(0xFF90A4AE)
                                    )
                                }
                            }
                            Text(
                                text = "BROKER SETTINGS →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("signals_list"),
                        contentPadding = PaddingValues(bottom = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        highConfidenceSignal?.let { sig ->
                            item {
                                AiSignal90Card(
                                    signal = sig,
                                    onRefreshSignal = { viewModel.recalculateHighConfidenceSignal() },
                                    onLogToJournal = { viewModel.convertAiSignalToJournalEntry(it) }
                                )
                            }
                        }

                        items(allSignals) { signal ->
                            LiveSignalCard(
                                signal = signal,
                                onConvertToJournal = { viewModel.convertSignalToJournalEntry(it) },
                                onDelete = { viewModel.deleteSignal(it) },
                                onExecuteOnBroker = { viewModel.executeSignalOnBroker(it) }
                            )
                        }
                    }
                }
            }
        } else {
            // VERTICAL MOBILE LIST VIEW (Default Compact Screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Control Bar
                ScreenControlBar(
                    isScreenCaptureActive = isScreenCaptureActive,
                    greenColor = greenColor,
                    redColor = redColor,
                    onToggleScreenCapture = {
                        if (isScreenCaptureActive) {
                            viewModel.stopScreenCaptureService(context)
                        } else {
                            if (mediaProjectionManager != null) {
                                try {
                                    val intent = mediaProjectionManager.createScreenCaptureIntent()
                                    mediaProjectionLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    viewModel.startInAppSimulationCapture()
                                }
                            } else {
                                viewModel.startInAppSimulationCapture()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirmed Markets Universe Selector (1 FOREX, 2 GOLD, 3 CRYPTO, 4 STOCKS, 5 ALL OF THEM)
                MarketUniverseSelectorBar(
                    selectedCategory = selectedCategory,
                    selectedAsset = selectedAsset,
                    isScanning = isAnalyzing,
                    onCategorySelected = { viewModel.setMarketCategory(it) },
                    onAssetSelected = { viewModel.setSelectedMarketAsset(it) },
                    onScanAllMarkets = { viewModel.scanAllConfirmedMarkets() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Chart Canvas Window
                LiveChartWindow(
                    latestSignal = latestSignal,
                    selectedAsset = selectedAsset,
                    isAnalyzing = isAnalyzing,
                    greenColor = greenColor,
                    redColor = redColor,
                    onScanChart = {
                        val dummyBitmap = createSimulatedChartBitmap()
                        viewModel.processChartFrameBitmap(dummyBitmap, "SCREEN_ANALYSIS", selectedAsset)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time AI Trading Signals",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${allSignals.size} Signals",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF82B1FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mobile Broker Execution Status Banner
                Surface(
                    color = Color(0xFF131822),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSelectedTab(3) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (brokerConnectionStatus.startsWith("CONNECTED")) Color(0xFF00E676) else Color(0xFF90A4AE),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeBroker?.accountName ?: "CashFlow Paper Engine",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (activeBroker?.autoTradeEnabled == true) "⚡ AUTOTRADE ON" else "⚡ 1-TAP EXECUTE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (activeBroker?.autoTradeEnabled == true) Color(0xFF00E676) else Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("signals_list")
                ) {
                    highConfidenceSignal?.let { sig ->
                        item {
                            AiSignal90Card(
                                signal = sig,
                                onRefreshSignal = { viewModel.recalculateHighConfidenceSignal() },
                                onLogToJournal = { viewModel.convertAiSignalToJournalEntry(it) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(allSignals) { signal ->
                        LiveSignalCard(
                            signal = signal,
                            onConvertToJournal = { viewModel.convertSignalToJournalEntry(it) },
                            onDelete = { viewModel.deleteSignal(it) },
                            onExecuteOnBroker = { viewModel.executeSignalOnBroker(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenControlBar(
    isScreenCaptureActive: Boolean,
    greenColor: Color,
    redColor: Color,
    onToggleScreenCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isScreenCaptureActive) greenColor.copy(alpha = 0.5f) else Color(0xFF263238)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isScreenCaptureActive) greenColor else Color(0xFFFFB300))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isScreenCaptureActive) "SCREEN ANALYSIS ACTIVE" else "SCREEN MONITOR STANDBY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Text(
                        text = if (isScreenCaptureActive) "Capturing live trading chart frames..." else "Tap start to begin real-time analysis",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF90A4AE)
                    )
                }
            }

            Button(
                onClick = onToggleScreenCapture,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScreenCaptureActive) redColor else greenColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("toggle_screen_capture_btn")
            ) {
                Icon(
                    imageVector = if (isScreenCaptureActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isScreenCaptureActive) "STOP" else "START",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun LiveChartWindow(
    latestSignal: Any?,
    selectedAsset: MarketAsset,
    isAnalyzing: Boolean,
    greenColor: Color,
    redColor: Color,
    onScanChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val candleWidth = (w / 18f).coerceAtLeast(10f)
                var lastPrice = h * 0.5f

                for (i in 0 until 18) {
                    val cx = i * candleWidth + (candleWidth / 2f)
                    val delta = (Math.sin(i * 0.6) * 28 + Math.cos(i * 0.4) * 18).toFloat()
                    val open = lastPrice
                    val close = open + delta
                    lastPrice = close

                    val isUp = close < open
                    val cColor = if (isUp) greenColor else redColor

                    val high = Math.min(open, close) - 12f
                    val low = Math.max(open, close) + 12f

                    drawLine(
                        color = cColor.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(cx, high),
                        end = androidx.compose.ui.geometry.Offset(cx, low),
                        strokeWidth = 2f
                    )

                    drawRect(
                        color = cColor,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - candleWidth * 0.35f, Math.min(open, close)),
                        size = androidx.compose.ui.geometry.Size(candleWidth * 0.70f, Math.abs(close - open).coerceAtLeast(4f))
                    )
                }
            }

            LiveChartOverlayCanvas(
                signal = latestSignal as? com.example.data.TradingSignalEntity,
                isAnalyzing = isAnalyzing
            )

            Surface(
                color = Color(0xCC0D1117),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${selectedAsset.symbol} • ${selectedAsset.category.shortName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedAsset.decimalDigits == 4) {
                            String.format(java.util.Locale.US, "%.4f", selectedAsset.basePrice)
                        } else {
                            String.format(java.util.Locale.US, "$%.2f", selectedAsset.basePrice)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            Button(
                onClick = onScanChart,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
                    .testTag("capture_and_analyze_screen_btn")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ANALYZING...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCAN CHART NOW", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun createSimulatedChartBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    paint.color = android.graphics.Color.parseColor("#070A0F")
    canvas.drawRect(0f, 0f, 800f, 500f, paint)

    val greenPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676") }
    val redPaint = Paint().apply { color = android.graphics.Color.parseColor("#FF5252") }

    var lastY = 250f
    for (i in 0 until 16) {
        val x = i * 48f + 20f
        val delta = (Math.sin(i.toDouble()) * 40 + Math.cos(i * 0.7) * 20).toFloat()
        val nextY = lastY + delta
        val isGreen = nextY < lastY

        val p = if (isGreen) greenPaint else redPaint
        canvas.drawRect(x, Math.min(lastY, nextY), x + 30f, Math.max(lastY, nextY) + 10f, p)
        lastY = nextY
    }

    return bitmap
}

