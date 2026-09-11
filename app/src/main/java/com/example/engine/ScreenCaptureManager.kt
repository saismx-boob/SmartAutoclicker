package com.example.engine

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * ScreenCaptureManager
 *
 * High-performance, low-latency screen capture engine powered by MediaProjection API.
 * Designed with modern zero-unnecessary-allocation patterns, hardware buffer pooling,
 * and real-time Region of Interest (ROI) slicing inspired by Macrorify.
 */
object ScreenCaptureManager {

    private const val TAG = "ScreenCaptureManager"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Screen dimensions & metrics
    var screenWidth: Int = 1080
        private set
    var screenHeight: Int = 2400
        private set
    var screenDensity: Int = 420
        private set

    // Observable states
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _activeMonitoredRoi = MutableStateFlow<Rect?>(null)
    val activeMonitoredRoi: StateFlow<Rect?> = _activeMonitoredRoi.asStateFlow()

    // FPS calculation tracking
    private var frameCounter = 0
    private var lastFpsTimestamp = 0L

    // Reusable cached frame bitmap to avoid garbage collector pressure
    private var cachedFullBitmap: Bitmap? = null
    private var cachedCleanBitmap: Bitmap? = null
    private val frameLock = Any()

    /**
     * Updates screen metrics dynamically from Android display metrics.
     */
    fun updateMetrics(width: Int, height: Int, density: Int) {
        screenWidth = max(320, width)
        screenHeight = max(480, height)
        screenDensity = max(120, density)
        Log.i(TAG, "Screen metrics set: ${screenWidth}x${screenHeight} @ ${screenDensity}dpi")
    }

    /**
     * Initializes MediaProjection and starts the hardware VirtualDisplay.
     */
    fun startCapture(projection: MediaProjection, width: Int, height: Int, density: Int) {
        stopCapture()

        updateMetrics(width, height, density)
        mediaProjection = projection

        // Background handler for image listener to avoid blocking main thread
        val thread = HandlerThread("ScreenCaptureThread").apply { start() }
        backgroundThread = thread
        val handler = Handler(thread.looper)
        backgroundHandler = handler

        try {
            // Allocate ImageReader with 2 buffers (double buffering)
            val reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ ir ->
                handleImageAvailable(ir)
            }, handler)

            // Register callback to cleanly handle stop
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w(TAG, "MediaProjection stopped by system")
                    stopCapture()
                }
            }, handler)

            // Create VirtualDisplay
            virtualDisplay = projection.createVirtualDisplay(
                "AIScreenVisionDisplay",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            _isCapturing.value = true
            Log.i(TAG, "VirtualDisplay created and screen capture running at ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VirtualDisplay", e)
            stopCapture()
        }
    }

    /**
     * Reads image from ImageReader and updates the latest frame.
     */
    private fun handleImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) return

            val planes = image.planes
            if (planes.isEmpty()) return

            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            synchronized(frameLock) {
                val fullWidth = screenWidth + rowPadding / pixelStride
                var fullBmp = cachedFullBitmap
                if (fullBmp == null || fullBmp.width != fullWidth || fullBmp.height != screenHeight || fullBmp.isRecycled) {
                    fullBmp?.recycle()
                    fullBmp = Bitmap.createBitmap(fullWidth, screenHeight, Bitmap.Config.ARGB_8888)
                    cachedFullBitmap = fullBmp
                }

                buffer.rewind()
                fullBmp.copyPixelsFromBuffer(buffer)

                // If there is row padding, crop to real screen width
                val finalBitmap = if (rowPadding == 0) {
                    fullBmp
                } else {
                    var cleanBmp = cachedCleanBitmap
                    if (cleanBmp == null || cleanBmp.width != screenWidth || cleanBmp.height != screenHeight || cleanBmp.isRecycled) {
                        cleanBmp?.recycle()
                        cleanBmp = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                        cachedCleanBitmap = cleanBmp
                    }
                    val canvas = android.graphics.Canvas(cleanBmp)
                    val srcRect = Rect(0, 0, screenWidth, screenHeight)
                    val dstRect = Rect(0, 0, screenWidth, screenHeight)
                    canvas.drawBitmap(fullBmp, srcRect, dstRect, null)
                    cleanBmp
                }

                _latestFrame.value = finalBitmap
            }

            // Calculate FPS
            frameCounter++
            val now = System.currentTimeMillis()
            if (now - lastFpsTimestamp >= 1000L) {
                _fps.value = frameCounter
                frameCounter = 0
                lastFpsTimestamp = now
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring screen image", e)
        } finally {
            image?.close()
        }
    }

    /**
     * Captures a safe, independent copy of the current live screen.
     * If MediaProjection is not active, returns null.
     */
    fun captureCurrentScreen(): Bitmap? {
        synchronized(frameLock) {
            val live = _latestFrame.value ?: cachedCleanBitmap ?: cachedFullBitmap ?: return null
            if (live.isRecycled) return null
            return try {
                if (live.width == screenWidth && live.height == screenHeight) {
                    live.copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    Bitmap.createBitmap(live, 0, 0, screenWidth, screenHeight)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying current screen bitmap", e)
                null
            }
        }
    }

    /**
     * Real-time Region of Interest (ROI) extraction.
     * Efficiently extracts only the pixels of the specified rectangular region,
     * exactly like Macrorify's sub-region monitoring.
     */
    fun captureRegion(roi: Rect): Bitmap? {
        val safeLeft = max(0, min(roi.left, screenWidth - 1))
        val safeTop = max(0, min(roi.top, screenHeight - 1))
        val safeRight = max(safeLeft + 1, min(roi.right, screenWidth))
        val safeBottom = max(safeTop + 1, min(roi.bottom, screenHeight))

        val width = safeRight - safeLeft
        val height = safeBottom - safeTop

        synchronized(frameLock) {
            val live = _latestFrame.value ?: cachedCleanBitmap ?: cachedFullBitmap ?: return null
            if (live.isRecycled) return null
            return try {
                Bitmap.createBitmap(live, safeLeft, safeTop, width, height)
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing region $roi", e)
                null
            }
        }
    }

    /**
     * Captures a Region of Interest defined by percentage coordinates (0.0f..1.0f).
     */
    fun captureRegionPercent(leftPct: Float, topPct: Float, rightPct: Float, bottomPct: Float): Bitmap? {
        val left = (leftPct * screenWidth).toInt()
        val top = (topPct * screenHeight).toInt()
        val right = (rightPct * screenWidth).toInt()
        val bottom = (bottomPct * screenHeight).toInt()
        return captureRegion(Rect(left, top, right, bottom))
    }

    /**
     * Sets the active monitored region of interest.
     */
    fun setMonitoredRoi(roi: Rect?) {
        _activeMonitoredRoi.value = roi
    }

    /**
     * Converts a bitmap to a base64 encoded PNG string.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Stops the MediaProjection and cleans up hardware resources.
     */
    fun stopCapture() {
        _isCapturing.value = false
        _fps.value = 0

        try {
            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.close()
            imageReader = null

            mediaProjection?.stop()
            mediaProjection = null

            backgroundThread?.quitSafely()
            backgroundThread = null
            backgroundHandler = null

            synchronized(frameLock) {
                cachedFullBitmap?.recycle()
                cachedFullBitmap = null
                cachedCleanBitmap?.recycle()
                cachedCleanBitmap = null
                _latestFrame.value = null
            }
            Log.i(TAG, "Screen capture cleanly stopped and resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screen capture", e)
        }
    }
}
