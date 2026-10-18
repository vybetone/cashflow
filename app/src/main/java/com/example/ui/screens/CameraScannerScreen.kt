package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.CashFlowViewModel
import com.example.ui.components.LiveSignalCard

@Composable
fun CameraScannerScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAnalyzing by viewModel.isAnalyzingChart.collectAsState()
    val latestSignal by viewModel.latestActiveSignal.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        hasCameraPermission = (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .testTag("camera_scanner_screen")
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070A0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusWeak,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CAMERA CHART SCANNER READY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Point phone camera at external trading monitor or tablet screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90A4AE)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("grant_camera_permission_btn")
                    ) {
                        Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(300.dp, 200.dp)
                .align(Alignment.Center)
                .border(2.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
                .background(Color(0x1A00E676))
        ) {
            Text(
                text = "ALIGN CHART IN FRAME",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color(0xFF00E676),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }

        Surface(
            color = Color(0xCC0D1117),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI CAMERA VISION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )

                Row {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = "Flash", tint = Color.White)
                    }
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            latestSignal?.let { signal ->
                if (signal.source == "CAMERA_ANALYSIS") {
                    LiveSignalCard(
                        signal = signal,
                        onConvertToJournal = { viewModel.convertSignalToJournalEntry(it) },
                        onDelete = { viewModel.deleteSignal(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Button(
                onClick = {
                    val cameraBitmap = createSimulatedCameraChartBitmap()
                    viewModel.processChartFrameBitmap(cameraBitmap, "CAMERA_ANALYSIS")
                },
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("snap_camera_chart_btn")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Snap Chart",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "TAP SHUTTER TO ANALYZE EXTERNAL CHART",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White
            )
        }
    }
}

private fun createSimulatedCameraChartBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    paint.color = android.graphics.Color.parseColor("#0B0E14")
    canvas.drawRect(0f, 0f, 800f, 600f, paint)

    val greenPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676") }
    val redPaint = Paint().apply { color = android.graphics.Color.parseColor("#FF5252") }

    for (i in 0 until 12) {
        val x = i * 60f + 40f
        val isGreen = i % 2 == 0
        val p = if (isGreen) greenPaint else redPaint
        canvas.drawRect(x, 200f, x + 40f, 380f, p)
    }

    return bitmap
}
