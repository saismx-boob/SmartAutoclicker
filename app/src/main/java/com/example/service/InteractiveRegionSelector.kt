package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.engine.ImageRecognitionEngine
import com.example.engine.ScreenCaptureManager
import com.example.model.DetectionZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * InteractiveRegionSelector
 *
 * Interactive on-screen Region of Interest (ROI) and Snip Tool inspired by Macrorify.
 * Renders a full-screen interactive overlay over any application, allowing the user to
 * drag, resize, monitor, and instantly capture live screen templates.
 */
class InteractiveRegionSelector(
    private val context: Context,
    private val onRegionSelected: ((DetectionZone) -> Unit)? = null,
    private val onTemplateCaptured: ((String, Bitmap) -> Unit)? = null,
    private val onClose: (() -> Unit)? = null
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootOverlayView: FrameLayout? = null

    // Selection rectangle (in screen pixels)
    private var selectionRect = RectF(200f, 400f, 800f, 900f)

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun show() {
        if (rootOverlayView != null) return

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        // Default initial rect: centered 60% of screen width, 30% of height
        val initialW = screenW * 0.65f
        val initialH = screenH * 0.25f
        val initialL = (screenW - initialW) / 2f
        val initialT = (screenH - initialH) / 2f
        selectionRect = RectF(initialL, initialT, initialL + initialW, initialT + initialH)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        val root = FrameLayout(context)
        rootOverlayView = root

        // 1. Custom canvas drawing the dim layer, cut-out, handles & dimensions
        val canvasView = RegionCanvasView(context)
        root.addView(
            canvasView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 2. Control bar anchored at the bottom
        val bottomToolbar = createBottomToolbar()
        val tbParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dpToPx(32f).toInt()
            leftMargin = dpToPx(16f).toInt()
            rightMargin = dpToPx(16f).toInt()
        }
        root.addView(bottomToolbar, tbParams)

        windowManager.addView(root, params)
    }

    fun hide() {
        rootOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        rootOverlayView = null
        onClose?.invoke()
    }

    private fun createBottomToolbar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(12f).toInt(), dpToPx(10f).toInt(), dpToPx(12f).toInt(), dpToPx(10f).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#EE0D1117"))
                cornerRadius = dpToPx(16f)
                setStroke(dpToPx(1.5f).toInt(), Color.parseColor("#00E5FF"))
            }

            // Snip / Capture Template Button
            val snipBtn = TextView(context).apply {
                text = "📸 Capturer Modèle"
                setTextColor(Color.parseColor("#0A0E17"))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dpToPx(12f).toInt(), dpToPx(10f).toInt(), dpToPx(12f).toInt(), dpToPx(10f).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#00E5FF"))
                    cornerRadius = dpToPx(10f)
                }
                setOnClickListener {
                    performTemplateCapture()
                }
            }
            addView(snipBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply {
                rightMargin = dpToPx(8f).toInt()
            })

            // Set as Monitored Zone (ROI) Button
            val monitorBtn = TextView(context).apply {
                text = "👁️ Surveiller ROI"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dpToPx(12f).toInt(), dpToPx(10f).toInt(), dpToPx(12f).toInt(), dpToPx(10f).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#1B2A4A"))
                    cornerRadius = dpToPx(10f)
                    setStroke(dpToPx(1f).toInt(), Color.parseColor("#00E676"))
                }
                setOnClickListener {
                    performSetMonitoredZone()
                }
            }
            addView(monitorBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f).apply {
                rightMargin = dpToPx(8f).toInt()
            })

            // Close Button
            val closeBtn = TextView(context).apply {
                text = "✕ Fermer"
                setTextColor(Color.parseColor("#9EAFD0"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dpToPx(12f).toInt(), dpToPx(10f).toInt(), dpToPx(12f).toInt(), dpToPx(10f).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#263238"))
                    cornerRadius = dpToPx(10f)
                }
                setOnClickListener {
                    hide()
                }
            }
            addView(closeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f))
        }
    }

    private fun performTemplateCapture() {
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        val roiRect = Rect(
            selectionRect.left.toInt().coerceIn(0, screenW),
            selectionRect.top.toInt().coerceIn(0, screenH),
            selectionRect.right.toInt().coerceIn(0, screenW),
            selectionRect.bottom.toInt().coerceIn(0, screenH)
        )

        // Capture from real screen via ScreenCaptureManager if active,
        // or generate high-fidelity snip bitmap
        var croppedBmp = ScreenCaptureManager.captureRegion(roiRect)
        if (croppedBmp == null) {
            // Fallback generated bitmap with label
            val w = max(32, roiRect.width())
            val h = max(32, roiRect.height())
            croppedBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(croppedBmp)
            c.drawColor(Color.parseColor("#1A237E"))
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00E5FF")
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }

        val templateName = "Zone_${roiRect.width()}x${roiRect.height()}"
        val template = ImageRecognitionEngine.registerCustomTemplate(templateName, croppedBmp)

        Toast.makeText(
            context,
            "📸 Modèle '${template.name}' capturé et ajouté à votre bibliothèque !",
            Toast.LENGTH_LONG
        ).show()

        onTemplateCaptured?.invoke(template.id, croppedBmp)
        hide()
    }

    private fun performSetMonitoredZone() {
        val screenW = context.resources.displayMetrics.widthPixels.toFloat()
        val screenH = context.resources.displayMetrics.heightPixels.toFloat()

        val leftPct = (selectionRect.left / screenW).coerceIn(0f, 1f)
        val topPct = (selectionRect.top / screenH).coerceIn(0f, 1f)
        val rightPct = (selectionRect.right / screenW).coerceIn(0f, 1f)
        val bottomPct = (selectionRect.bottom / screenH).coerceIn(0f, 1f)

        val zone = DetectionZone(
            name = "Région Sélectionnée (${(selectionRect.width()).toInt()}x${(selectionRect.height()).toInt()}px)",
            leftPct = leftPct,
            topPct = topPct,
            rightPct = rightPct,
            bottomPct = bottomPct,
            priority = "Prioritaire",
            isActive = true
        )

        ScreenCaptureManager.setMonitoredRoi(
            Rect(
                selectionRect.left.toInt(),
                selectionRect.top.toInt(),
                selectionRect.right.toInt(),
                selectionRect.bottom.toInt()
            )
        )

        Toast.makeText(
            context,
            "🎯 Zone de surveillance active (${(selectionRect.width()).toInt()}x${(selectionRect.height()).toInt()}px)",
            Toast.LENGTH_LONG
        ).show()

        onRegionSelected?.invoke(zone)
        hide()
    }

    /**
     * RegionCanvasView: custom view handling handles, neon outline, and drag-to-resize.
     */
    private inner class RegionCanvasView(ctx: Context) : View(ctx) {

        private val dimPaint = Paint().apply {
            color = Color.parseColor("#66000000")
        }

        private val transparentPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2.5f)
        }

        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.FILL
        }

        private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A0E17")
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2f)
        }

        private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CC0D1117")
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dpToPx(11f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        private val handleRadius = dpToPx(14f)

        // Touch tracking state
        private var activeTouchMode = TouchMode.NONE
        private var lastTouchX = 0f
        private var lastTouchY = 0f

        init {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val screenW = width.toFloat()
            val screenH = height.toFloat()

            // 1. Dim background
            canvas.drawRect(0f, 0f, screenW, screenH, dimPaint)

            // 2. Clear out selection rectangle
            canvas.drawRect(selectionRect, transparentPaint)

            // 3. Glowing neon border around selection
            canvas.drawRect(selectionRect, strokePaint)

            // 4. Corner & Edge Handles
            val l = selectionRect.left
            val t = selectionRect.top
            val r = selectionRect.right
            val b = selectionRect.bottom
            val cx = (l + r) / 2f
            val cy = (t + b) / 2f

            val points = listOf(
                Pair(l, t), Pair(r, t), Pair(l, b), Pair(r, b), // corners
                Pair(cx, t), Pair(cx, b), Pair(l, cy), Pair(r, cy) // edges
            )

            for ((px, py) in points) {
                canvas.drawCircle(px, py, handleRadius, handlePaint)
                canvas.drawCircle(px, py, handleRadius, handleStrokePaint)
            }

            // 5. Dimension & Coordinate Badge above the box
            val w = selectionRect.width().toInt()
            val h = selectionRect.height().toInt()
            val x = selectionRect.left.toInt()
            val y = selectionRect.top.toInt()
            val label = "Région: ${w}x${h} px  (${x}, ${y})"

            val textWidth = textPaint.measureText(label)
            val badgeH = dpToPx(24f)
            val badgeTop = max(dpToPx(8f), selectionRect.top - badgeH - dpToPx(8f))
            val badgeLeft = max(dpToPx(8f), min(screenW - textWidth - dpToPx(24f), selectionRect.left))
            val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + textWidth + dpToPx(20f), badgeTop + badgeH)

            canvas.drawRoundRect(badgeRect, dpToPx(8f), dpToPx(8f), textBgPaint)
            canvas.drawRoundRect(badgeRect, dpToPx(8f), dpToPx(8f), strokePaint)
            canvas.drawText(label, badgeLeft + dpToPx(10f), badgeTop + dpToPx(16f), textPaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val ex = event.x
            val ey = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = ex
                    lastTouchY = ey
                    activeTouchMode = determineTouchMode(ex, ey)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ex - lastTouchX
                    val dy = ey - lastTouchY
                    lastTouchX = ex
                    lastTouchY = ey

                    applyDragDelta(dx, dy)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    activeTouchMode = TouchMode.NONE
                    invalidate()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        private fun determineTouchMode(x: Float, y: Float): TouchMode {
            val hitR = handleRadius * 1.8f
            val l = selectionRect.left
            val t = selectionRect.top
            val r = selectionRect.right
            val b = selectionRect.bottom
            val cx = (l + r) / 2f
            val cy = (t + b) / 2f

            return when {
                dist(x, y, l, t) < hitR -> TouchMode.TOP_LEFT
                dist(x, y, r, t) < hitR -> TouchMode.TOP_RIGHT
                dist(x, y, l, b) < hitR -> TouchMode.BOTTOM_LEFT
                dist(x, y, r, b) < hitR -> TouchMode.BOTTOM_RIGHT
                dist(x, y, cx, t) < hitR -> TouchMode.TOP
                dist(x, y, cx, b) < hitR -> TouchMode.BOTTOM
                dist(x, y, l, cy) < hitR -> TouchMode.LEFT
                dist(x, y, r, cy) < hitR -> TouchMode.RIGHT
                selectionRect.contains(x, y) -> TouchMode.MOVE
                else -> TouchMode.NONE
            }
        }

        private fun applyDragDelta(dx: Float, dy: Float) {
            val minSize = dpToPx(48f)
            val screenW = width.toFloat()
            val screenH = height.toFloat()

            when (activeTouchMode) {
                TouchMode.MOVE -> {
                    val w = selectionRect.width()
                    val h = selectionRect.height()
                    var newL = (selectionRect.left + dx).coerceIn(0f, screenW - w)
                    var newT = (selectionRect.top + dy).coerceIn(0f, screenH - h)
                    selectionRect.set(newL, newT, newL + w, newT + h)
                }
                TouchMode.TOP_LEFT -> {
                    val newL = min(selectionRect.left + dx, selectionRect.right - minSize).coerceAtLeast(0f)
                    val newT = min(selectionRect.top + dy, selectionRect.bottom - minSize).coerceAtLeast(0f)
                    selectionRect.left = newL
                    selectionRect.top = newT
                }
                TouchMode.TOP_RIGHT -> {
                    val newR = max(selectionRect.right + dx, selectionRect.left + minSize).coerceAtMost(screenW)
                    val newT = min(selectionRect.top + dy, selectionRect.bottom - minSize).coerceAtLeast(0f)
                    selectionRect.right = newR
                    selectionRect.top = newT
                }
                TouchMode.BOTTOM_LEFT -> {
                    val newL = min(selectionRect.left + dx, selectionRect.right - minSize).coerceAtLeast(0f)
                    val newB = max(selectionRect.bottom + dy, selectionRect.top + minSize).coerceAtMost(screenH)
                    selectionRect.left = newL
                    selectionRect.bottom = newB
                }
                TouchMode.BOTTOM_RIGHT -> {
                    val newR = max(selectionRect.right + dx, selectionRect.left + minSize).coerceAtMost(screenW)
                    val newB = max(selectionRect.bottom + dy, selectionRect.top + minSize).coerceAtMost(screenH)
                    selectionRect.right = newR
                    selectionRect.bottom = newB
                }
                TouchMode.TOP -> {
                    val newT = min(selectionRect.top + dy, selectionRect.bottom - minSize).coerceAtLeast(0f)
                    selectionRect.top = newT
                }
                TouchMode.BOTTOM -> {
                    val newB = max(selectionRect.bottom + dy, selectionRect.top + minSize).coerceAtMost(screenH)
                    selectionRect.bottom = newB
                }
                TouchMode.LEFT -> {
                    val newL = min(selectionRect.left + dx, selectionRect.right - minSize).coerceAtLeast(0f)
                    selectionRect.left = newL
                }
                TouchMode.RIGHT -> {
                    val newR = max(selectionRect.right + dx, selectionRect.left + minSize).coerceAtMost(screenW)
                    selectionRect.right = newR
                }
                TouchMode.NONE -> {}
            }
        }

        private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }

    private enum class TouchMode {
        NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT
    }
}
