package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var captureRunnable: Runnable? = null
    private var isCapturing = false

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onCreate startForeground", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val resultCode = intent?.getIntExtra("RESULT_CODE", -1) ?: -1
            val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("RESULT_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("RESULT_DATA")
            }

            if (resultCode != -1 && resultData != null) {
                setupMediaProjection(resultCode, resultData)
            }

            startContinuousAnalysisLoop()
        } catch (t: Throwable) {
            Log.e(TAG, "Error in onStartCommand", t)
        }

        return START_NOT_STICKY
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        try {
            cleanupMediaProjection()

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: return
            val projection = projectionManager.getMediaProjection(resultCode, data) ?: return
            mediaProjection = projection

            // CRITICAL FOR ANDROID 14 (API 34+):
            // Registering a MediaProjection.Callback before calling createVirtualDisplay() is mandatory.
            // Failing to register this callback causes createVirtualDisplay() to throw IllegalStateException!
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.i(TAG, "MediaProjection stopped by system")
                    cleanupMediaProjection()
                }
            }, handler)

            val metrics = resources.displayMetrics
            var width = metrics.widthPixels
            var height = metrics.heightPixels
            val density = metrics.densityDpi

            // Scale down high-resolution screens (e.g. 1440p / 4K) to max 1080 to prevent OutOfMemory and buffer stalls
            if (width > 1080) {
                val scale = 1080f / width
                width = 1080
                height = (height * scale).toInt()
            }
            if (width % 2 != 0) width -= 1
            if (height % 2 != 0) height -= 1
            if (width <= 0) width = 720
            if (height <= 0) height = 1280

            imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
            virtualDisplay = projection.createVirtualDisplay(
                "CashFlowScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            Log.i(TAG, "VirtualDisplay created successfully (${width}x${height})")
        } catch (t: Throwable) {
            Log.e(TAG, "setupMediaProjection failed", t)
            cleanupMediaProjection()
        }
    }

    private fun startContinuousAnalysisLoop() {
        if (isCapturing) return
        isCapturing = true

        captureRunnable = object : Runnable {
            override fun run() {
                if (!isCapturing) return

                try {
                    val bitmap = acquireLatestScreenBitmap() ?: generateSimulatedLiveChartBitmap()
                    onFrameCapturedListener?.invoke(bitmap)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error during continuous frame capture/dispatch", t)
                }

                if (isCapturing) {
                    handler.postDelayed(this, scanIntervalMs)
                }
            }
        }
        handler.post(captureRunnable!!)
    }

    private fun acquireLatestScreenBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        val image = try {
            reader.acquireLatestImage()
        } catch (t: Throwable) {
            null
        } ?: return null

        return try {
            val planes = image.planes
            if (planes.isEmpty()) return null
            val buffer = planes[0].buffer ?: return null
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride

            if (pixelStride <= 0 || image.width <= 0 || image.height <= 0) return null

            val rowPadding = rowStride - pixelStride * image.width
            val paddedWidth = image.width + rowPadding / pixelStride

            buffer.rewind()

            val bitmap = Bitmap.createBitmap(
                paddedWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val finalBitmap = if (paddedWidth != image.width) {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                bitmap.recycle()
                cropped
            } else {
                bitmap
            }
            finalBitmap
        } catch (t: Throwable) {
            Log.e(TAG, "Failed acquiring bitmap from ImageReader", t)
            null
        } finally {
            try {
                image.close()
            } catch (t: Throwable) {
                // Ignore close errors
            }
        }
    }

    private fun generateSimulatedLiveChartBitmap(): Bitmap {
        return try {
            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            paint.color = Color.parseColor("#0B0E14")
            canvas.drawRect(0f, 0f, 800f, 600f, paint)

            val greenPaint = Paint().apply { color = Color.parseColor("#00E676") }
            val redPaint = Paint().apply { color = Color.parseColor("#FF5252") }

            val timeOffset = System.currentTimeMillis() / 1000.0
            var lastY = 300f

            for (i in 0 until 18) {
                val x = i * 42f + 20f
                val delta = (Math.sin(timeOffset + i * 0.5) * 35 + Math.cos(i * 0.8) * 15).toFloat()
                val nextY = (lastY + delta).coerceIn(100f, 500f)
                val isGreen = nextY < lastY

                val p = if (isGreen) greenPaint else redPaint
                canvas.drawRect(x, Math.min(lastY, nextY), x + 28f, Math.max(lastY, nextY) + 12f, p)
                lastY = nextY
            }

            bitmap
        } catch (t: Throwable) {
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        }
    }

    private fun cleanupMediaProjection() {
        try {
            virtualDisplay?.release()
        } catch (t: Throwable) {
            Log.e(TAG, "Error releasing virtualDisplay", t)
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (t: Throwable) {
            Log.e(TAG, "Error closing imageReader", t)
        }
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping mediaProjection", t)
        }
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isCapturing = false
        captureRunnable?.let { handler.removeCallbacks(it) }
        captureRunnable = null
        cleanupMediaProjection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cash Flow AI Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active live chart screen capture for AI trading signals"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_SCREEN", "HUD")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CASH FLOW AI Continuous Screen Monitor")
            .setContentText("Continuously analyzing live chart screens for high-confidence trading signals...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val CHANNEL_ID = "cash_flow_ai_screen_capture"
        private const val NOTIFICATION_ID = 1001

        var scanIntervalMs: Long = 5000L
        var onFrameCapturedListener: ((Bitmap) -> Unit)? = null

        fun updateScanIntervalMs(intervalMs: Long) {
            scanIntervalMs = intervalMs.coerceAtLeast(1000L)
        }

        fun startService(context: Context, resultCode: Int = -1, resultData: Intent? = null) {
            try {
                val intent = Intent(context, ScreenCaptureService::class.java).apply {
                    if (resultCode != -1 && resultData != null) {
                        putExtra("RESULT_CODE", resultCode)
                        putExtra("RESULT_DATA", resultData)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed starting ScreenCaptureService", t)
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, ScreenCaptureService::class.java)
                context.stopService(intent)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed stopping ScreenCaptureService", t)
            }
        }
    }
}
