package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.JsonUtils
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ConditionType
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * FloatingActionPalette
 *
 * In-game real-time Action Palette & Coordinate Picker (inspired by Macrorify).
 * Allows placing numbered interactive click targets and swipe gesture trajectories
 * directly over third-party games and apps to obtain precise coordinates and test them live.
 */
class FloatingActionPalette(
    private val context: Context,
    private val scenarioId: Long? = null,
    private val onClose: (() -> Unit)? = null,
    private val onLaunchSnip: (() -> Unit)? = null
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)

    // Palette Bar View & Window
    private var paletteBarView: View? = null
    private var paletteParams: WindowManager.LayoutParams? = null

    // Fullscreen transparent canvas overlay for drawing swipe vectors and connector lines
    private var trajectoryOverlayView: TrajectoryCanvasView? = null
    private var trajectoryParams: WindowManager.LayoutParams? = null

    // Target pointer items placed on screen
    private val actionTargets = mutableListOf<InGameActionTarget>()
    private var areTargetsVisible = true
    private var isTestingRunning = false

    companion object {
        var activeScenarioIdForPalette: Long? = null
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun getOverlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    /**
     * Show the floating action palette and trajectory overlay.
     */
    fun show() {
        if (paletteBarView != null) return

        // 1. Attach Full-Screen Trajectory Canvas Overlay (Pass-through touches except when drawing)
        attachTrajectoryCanvas()

        // 2. Attach Floating Palette Bar
        attachPaletteBar()

        // 3. Add default first click target to immediately help the user
        addClickTarget(dpToPx(160f).toFloat(), dpToPx(300f).toFloat())

        Toast.makeText(
            context,
            "🎮 Palette active ! Glissez les cibles sur les boutons de votre jeu.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Attach full-screen transparent canvas for gesture lines & vectors.
     */
    private fun attachTrajectoryCanvas() {
        val canvas = TrajectoryCanvasView(context)
        trajectoryOverlayView = canvas

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        trajectoryParams = params
        windowManager.addView(canvas, params)
    }

    /**
     * Attach the floating action bar toolbar.
     */
    private fun attachPaletteBar() {
        val bar = createPaletteBarView()
        paletteBarView = bar

        val screenW = context.resources.displayMetrics.widthPixels
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - dpToPx(340f)) / 2
            y = dpToPx(48f)
        }
        paletteParams = params

        windowManager.addView(bar, params)
    }

    /**
     * Builds the sleek, glowing cyberpunk floating palette bar.
     */
    private fun createPaletteBarView(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(8f)
            setPadding(pad, pad, pad, pad)
            elevation = dpToPx(16f).toFloat()
            layoutParams = ViewGroup.LayoutParams(
                dpToPx(340f),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(14f).toFloat()
            setColor(Color.parseColor("#F50D111A"))
            setStroke(dpToPx(1.5f), Color.parseColor("#00E5FF"))
        }
        root.background = bgDrawable

        // Header: Drag Handle, Title & Action Counter, Minimize, Close
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Drag handle for moving the bar
        val dragHandle = TextView(context).apply {
            text = "⣿"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 18f
            setPadding(dpToPx(4f), 0, dpToPx(6f), 0)
        }
        header.addView(dragHandle)

        val title = TextView(context).apply {
            text = "🎮 Palette d'Actions Jeu"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val countBadge = TextView(context).apply {
            id = View.generateViewId()
            text = "1 pt"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val p = dpToPx(4f)
            setPadding(p * 2, p, p * 2, p)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(6f).toFloat()
                setColor(Color.parseColor("#1F2A38"))
            }
        }
        header.addView(countBadge)

        // Close button
        val closeBtn = TextView(context).apply {
            text = " ✕ "
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8f), 0, dpToPx(4f), 0)
            setOnClickListener {
                dismiss()
            }
        }
        header.addView(closeBtn)

        root.addView(header)

        // Touch listener on Header to drag the palette anywhere
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    paletteParams?.let {
                        startX = it.x
                        startY = it.y
                    }
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    paletteParams?.let {
                        it.x = (startX + (event.rawX - touchX)).toInt()
                        it.y = (startY + (event.rawY - touchY)).toInt()
                        windowManager.updateViewLayout(root, it)
                    }
                    true
                }
                else -> false
            }
        }

        // Action Buttons Row (Horizontally Scrollable)
        val scrollContainer = HorizontalScrollView(context).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(6f)
            }
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // 1. + Clic Target Button
        val addClickBtn = createToolbarButton("+ 🎯 Clic", "#00E5FF", "#102A38") {
            val screenW = context.resources.displayMetrics.widthPixels
            val screenH = context.resources.displayMetrics.heightPixels
            val nextX = (screenW * 0.3f + (actionTargets.size * 40f) % (screenW * 0.4f))
            val nextY = (screenH * 0.4f + (actionTargets.size * 50f) % (screenH * 0.3f))
            addClickTarget(nextX, nextY)
            updateCounterBadge(countBadge)
        }
        buttonsRow.addView(addClickBtn)

        // 2. + Glisser (Swipe) Button
        val addSwipeBtn = createToolbarButton("+ 👆 Swipe", "#00E676", "#103822") {
            val screenW = context.resources.displayMetrics.widthPixels
            val screenH = context.resources.displayMetrics.heightPixels
            val startX = screenW * 0.5f
            val startY = screenH * 0.65f
            val endX = screenW * 0.5f
            val endY = screenH * 0.35f
            addSwipeTarget(startX, startY, endX, endY)
            updateCounterBadge(countBadge)
        }
        buttonsRow.addView(addSwipeBtn)

        // 3. + Pause Button
        val addWaitBtn = createToolbarButton("+ ⏳ Pause", "#FFB300", "#382D10") {
            addWaitStep()
            updateCounterBadge(countBadge)
        }
        buttonsRow.addView(addWaitBtn)

        // 4. 📸 Snip Image / ROI
        val snipBtn = createToolbarButton("📸 Snip", "#E040FB", "#321038") {
            // Minimize palette and launch snip tool directly
            onLaunchSnip?.invoke()
        }
        buttonsRow.addView(snipBtn)

        // 5. ▶ Tester la séquence
        val testBtn = createToolbarButton("▶ Tester", "#00E676", "#004D40") {
            testPlacedGestures()
        }
        buttonsRow.addView(testBtn)

        // 6. 👁️ Masquer / Afficher cibles
        val eyeBtn = createToolbarButton("👁️ Cibles", "#9EAFD0", "#1C2433") {
            toggleTargetsVisibility()
        }
        buttonsRow.addView(eyeBtn)

        // 7. 💾 Sauvegarder dans scénario
        val saveBtn = createToolbarButton("💾 Sauver", "#00E5FF", "#00363A") {
            saveTargetsToScenario()
        }
        buttonsRow.addView(saveBtn)

        // 8. 🗑️ Effacer tout
        val clearBtn = createToolbarButton("🗑️ Vider", "#FF5252", "#381014") {
            clearAllTargets()
            updateCounterBadge(countBadge)
        }
        buttonsRow.addView(clearBtn)

        scrollContainer.addView(buttonsRow)
        root.addView(scrollContainer)

        return root
    }

    private fun createToolbarButton(
        label: String,
        textColorHex: String,
        bgColorHex: String,
        onClick: () -> Unit
    ): View {
        return TextView(context).apply {
            text = label
            setTextColor(Color.parseColor(textColorHex))
            textSize = 10.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val padH = dpToPx(8f)
            val padV = dpToPx(6f)
            setPadding(padH, padV, padH, padV)
            val h = dpToPx(30f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                h
            ).apply {
                rightMargin = dpToPx(5f)
            }
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(7f).toFloat()
                setColor(Color.parseColor(bgColorHex))
                setStroke(dpToPx(1f), Color.parseColor(textColorHex))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateCounterBadge(badge: TextView) {
        val count = actionTargets.size
        badge.text = "$count pt${if (count > 1) "s" else ""}"
    }

    // ==========================================
    // TARGET POINTER MANAGEMENT (CLICK & SWIPE)
    // ==========================================

    /**
     * Adds an interactive click target pointer at (x, y) with live coordinate readout.
     */
    private fun addClickTarget(initialX: Float, initialY: Float) {
        val targetIndex = actionTargets.size + 1
        val pointerView = ClickPointerView(context, targetIndex, initialX, initialY)

        val pointerSize = dpToPx(70f)
        val params = WindowManager.LayoutParams(
            pointerSize,
            pointerSize,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (initialX - pointerSize / 2).toInt()
            y = (initialY - pointerSize / 2).toInt()
        }

        pointerView.setupDraggable(params) { cx, cy ->
            trajectoryOverlayView?.invalidate()
        }

        pointerView.onSingleTap = {
            showPointDetailDialog(pointerView)
        }

        windowManager.addView(pointerView, params)

        val target = InGameActionTarget(
            id = UUID.randomUUID().toString(),
            index = targetIndex,
            type = ActionType.CLICK,
            view = pointerView,
            params = params,
            startX = initialX,
            startY = initialY
        )
        actionTargets.add(target)
        trajectoryOverlayView?.setTargets(actionTargets)
    }

    /**
     * Adds an interactive swipe trajectory (Start Pin A + End Pin B).
     */
    private fun addSwipeTarget(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val targetIndex = actionTargets.size + 1

        // Pin A (Start)
        val startPin = ClickPointerView(context, targetIndex, startX, startY, isSwipePin = true, pinLabel = "A")
        val pinSize = dpToPx(60f)
        val startParams = WindowManager.LayoutParams(
            pinSize,
            pinSize,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (startX - pinSize / 2).toInt()
            y = (startY - pinSize / 2).toInt()
        }
        startPin.setupDraggable(startParams) { cx, cy ->
            trajectoryOverlayView?.invalidate()
        }
        windowManager.addView(startPin, startParams)

        // Pin B (End)
        val endPin = ClickPointerView(context, targetIndex, endX, endY, isSwipePin = true, pinLabel = "B")
        val endParams = WindowManager.LayoutParams(
            pinSize,
            pinSize,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (endX - pinSize / 2).toInt()
            y = (endY - pinSize / 2).toInt()
        }
        endPin.setupDraggable(endParams) { cx, cy ->
            trajectoryOverlayView?.invalidate()
        }
        windowManager.addView(endPin, endParams)

        val target = InGameActionTarget(
            id = UUID.randomUUID().toString(),
            index = targetIndex,
            type = ActionType.SWIPE,
            view = startPin,
            params = startParams,
            startX = startX,
            startY = startY,
            endView = endPin,
            endParams = endParams,
            endX = endX,
            endY = endY,
            durationMs = 400L
        )
        actionTargets.add(target)
        trajectoryOverlayView?.setTargets(actionTargets)
    }

    /**
     * Adds a Pause / Wait delay step.
     */
    private fun addWaitStep() {
        val targetIndex = actionTargets.size + 1
        val target = InGameActionTarget(
            id = UUID.randomUUID().toString(),
            index = targetIndex,
            type = ActionType.WAIT_DELAY,
            view = null,
            params = null,
            startX = 0f,
            startY = 0f,
            durationMs = 500L
        )
        actionTargets.add(target)
        Toast.makeText(context, "⏳ Étape $targetIndex : Pause 500ms ajoutée", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mini detail & test card for a specific target point.
     */
    private fun showPointDetailDialog(pointerView: ClickPointerView) {
        val target = actionTargets.find { it.view == pointerView } ?: return
        val cx = pointerView.centerPoint.x.toInt()
        val cy = pointerView.centerPoint.y.toInt()

        Toast.makeText(
            context,
            "🎯 Cible #${target.index} à ($cx, $cy) - Test immédiat du clic...",
            Toast.LENGTH_SHORT
        ).show()

        // Trigger real click on the game via AccessibilityService
        AutomationAccessibilityService.instance?.performClick(
            pointerView.centerPoint.x,
            pointerView.centerPoint.y,
            target.durationMs
        ) { success ->
            handler.post {
                pointerView.flashSuccess(success)
            }
        }
    }

    /**
     * Executes all placed gestures in real-time in sequence on the screen.
     */
    private fun testPlacedGestures() {
        if (isTestingRunning) return
        if (actionTargets.isEmpty()) {
            Toast.makeText(context, "Aucune action à tester. Ajoutez un clic ou un geste !", Toast.LENGTH_SHORT).show()
            return
        }

        isTestingRunning = true
        Toast.makeText(context, "▶ Test de la séquence sur votre jeu...", Toast.LENGTH_SHORT).show()

        scope.launch(Dispatchers.Default) {
            for (target in actionTargets) {
                when (target.type) {
                    ActionType.CLICK -> {
                        val view = target.view as? ClickPointerView
                        val cx = view?.centerPoint?.x ?: target.startX
                        val cy = view?.centerPoint?.y ?: target.startY

                        withContext(Dispatchers.Main) {
                            view?.flashActive()
                        }

                        // Perform real click on target application
                        AutomationAccessibilityService.instance?.performClick(cx, cy, target.durationMs) { success ->
                            handler.post { view?.flashSuccess(success) }
                        }
                        kotlinx.coroutines.delay(target.durationMs + target.delayBeforeMs)
                    }
                    ActionType.SWIPE -> {
                        val startV = target.view as? ClickPointerView
                        val endV = target.endView as? ClickPointerView
                        val sx = startV?.centerPoint?.x ?: target.startX
                        val sy = startV?.centerPoint?.y ?: target.startY
                        val ex = endV?.centerPoint?.x ?: target.endX
                        val ey = endV?.centerPoint?.y ?: target.endY

                        withContext(Dispatchers.Main) {
                            startV?.flashActive()
                            endV?.flashActive()
                        }

                        AutomationAccessibilityService.instance?.performSwipe(sx, sy, ex, ey, target.durationMs) { success ->
                            handler.post {
                                startV?.flashSuccess(success)
                                endV?.flashSuccess(success)
                            }
                        }
                        kotlinx.coroutines.delay(target.durationMs + target.delayBeforeMs)
                    }
                    ActionType.WAIT_DELAY -> {
                        kotlinx.coroutines.delay(target.durationMs)
                    }
                    else -> Unit
                }
            }

            withContext(Dispatchers.Main) {
                isTestingRunning = false
                Toast.makeText(context, "✅ Test de séquence terminé avec succès !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Toggles target pointer visibility to see the game unobstructed.
     */
    private fun toggleTargetsVisibility() {
        areTargetsVisible = !areTargetsVisible
        val vis = if (areTargetsVisible) View.VISIBLE else View.GONE
        for (target in actionTargets) {
            target.view?.visibility = vis
            target.endView?.visibility = vis
        }
        trajectoryOverlayView?.visibility = vis
    }

    /**
     * Clears all placed target points from screen.
     */
    private fun clearAllTargets() {
        for (target in actionTargets) {
            target.view?.let { windowManager.removeView(it) }
            target.endView?.let { windowManager.removeView(it) }
        }
        actionTargets.clear()
        trajectoryOverlayView?.setTargets(actionTargets)
    }

    /**
     * Saves the placed actions directly into the Room Database Scenario.
     */
    private fun saveTargetsToScenario() {
        if (actionTargets.isEmpty()) {
            Toast.makeText(context, "Placez au moins un point avant d'enregistrer !", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert in-game targets to ActionSteps
        val steps = mutableListOf<ActionStep>()
        for ((idx, target) in actionTargets.withIndex()) {
            val stepNumber = idx + 1
            when (target.type) {
                ActionType.CLICK -> {
                    val view = target.view as? ClickPointerView
                    val cx = view?.centerPoint?.x ?: target.startX
                    val cy = view?.centerPoint?.y ?: target.startY
                    steps.add(
                        ActionStep(
                            id = target.id,
                            stepNumber = stepNumber,
                            name = "Clic #${stepNumber} (${cx.toInt()}, ${cy.toInt()})",
                            actionType = ActionType.CLICK,
                            targetType = TargetType.COORDINATES,
                            targetX = cx,
                            targetY = cy,
                            durationMs = target.durationMs,
                            delayBeforeMs = target.delayBeforeMs
                        )
                    )
                }
                ActionType.SWIPE -> {
                    val startV = target.view as? ClickPointerView
                    val endV = target.endView as? ClickPointerView
                    val sx = startV?.centerPoint?.x ?: target.startX
                    val sy = startV?.centerPoint?.y ?: target.startY
                    val ex = endV?.centerPoint?.x ?: target.endX
                    val ey = endV?.centerPoint?.y ?: target.endY
                    steps.add(
                        ActionStep(
                            id = target.id,
                            stepNumber = stepNumber,
                            name = "Glisser #${stepNumber}",
                            actionType = ActionType.SWIPE,
                            targetType = TargetType.COORDINATES,
                            targetX = sx,
                            targetY = sy,
                            swipeEndX = ex,
                            swipeEndY = ey,
                            durationMs = target.durationMs,
                            delayBeforeMs = target.delayBeforeMs
                        )
                    )
                }
                ActionType.WAIT_DELAY -> {
                    steps.add(
                        ActionStep(
                            id = target.id,
                            stepNumber = stepNumber,
                            name = "Pause ${target.durationMs}ms",
                            actionType = ActionType.WAIT_DELAY,
                            durationMs = target.durationMs,
                            delayBeforeMs = target.durationMs
                        )
                    )
                }
                else -> Unit
            }
        }

        scope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val targetScenarioId = scenarioId ?: activeScenarioIdForPalette

            if (targetScenarioId != null && targetScenarioId != 0L) {
                val existing = db.scenarioDao().getScenarioById(targetScenarioId)
                if (existing != null) {
                    val updated = existing.copy(
                        stepsJson = JsonUtils.stepsToJson(steps)
                    )
                    db.scenarioDao().updateScenario(updated)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "💾 Scénario '${existing.title}' mis à jour (${steps.size} actions exactes) !",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
            }

            // Otherwise create a new scenario
            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val newScenario = ScenarioEntity(
                title = "Jeu En Direct ($dateStr)",
                description = "Capturé avec la palette flottante en direct",
                stepsJson = JsonUtils.stepsToJson(steps),
                loopCount = 1
            )
            val newId = db.scenarioDao().insertScenario(newScenario)
            activeScenarioIdForPalette = newId

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "💾 Nouveau scénario créé (${steps.size} actions capturées) !",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Dismiss and remove all floating views safely.
     */
    fun dismiss() {
        try {
            clearAllTargets()
            paletteBarView?.let { windowManager.removeView(it) }
            paletteBarView = null
            trajectoryOverlayView?.let { windowManager.removeView(it) }
            trajectoryOverlayView = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onClose?.invoke()
    }

    /**
     * Represents an action target on screen.
     */
    data class InGameActionTarget(
        val id: String,
        val index: Int,
        val type: ActionType,
        val view: View?,
        val params: WindowManager.LayoutParams?,
        var startX: Float,
        var startY: Float,
        var endView: View? = null,
        var endParams: WindowManager.LayoutParams? = null,
        var endX: Float = 0f,
        var endY: Float = 0f,
        var durationMs: Long = 80L,
        var delayBeforeMs: Long = 300L
    )

    /**
     * Interactive numbered target pointer view with live coordinate display.
     */
    inner class ClickPointerView(
        ctx: Context,
        val index: Int,
        var currentX: Float,
        var currentY: Float,
        val isSwipePin: Boolean = false,
        val pinLabel: String = ""
    ) : FrameLayout(ctx) {

        val centerPoint = PointF(currentX, currentY)
        var onSingleTap: (() -> Unit)? = null

        private val coordBadge: TextView
        private val targetDisc: View

        init {
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }

            // Top Coordinate Badge (updates live as dragged)
            coordBadge = TextView(ctx).apply {
                text = "${currentX.toInt()}, ${currentY.toInt()}"
                setTextColor(Color.WHITE)
                textSize = 8.5f
                typeface = android.graphics.Typeface.MONOSPACE
                val p = dpToPx(2f)
                setPadding(p * 2, 0, p * 2, 0)
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(4f).toFloat()
                    setColor(Color.parseColor("#CC0D111A"))
                    setStroke(dpToPx(0.8f), if (isSwipePin) Color.parseColor("#00E676") else Color.parseColor("#00E5FF"))
                }
            }
            root.addView(coordBadge)

            // Circular Crosshair Target Disc
            val discSize = dpToPx(38f)
            targetDisc = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(discSize, discSize).apply {
                    topMargin = dpToPx(2f)
                }
                elevation = dpToPx(8f).toFloat()
            }

            val discBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#DD0A0E17"))
                setStroke(
                    dpToPx(2f),
                    if (isSwipePin) Color.parseColor("#00E676") else Color.parseColor("#00E5FF")
                )
            }
            targetDisc.background = discBg

            val label = TextView(ctx).apply {
                text = if (pinLabel.isNotEmpty()) pinLabel else "$index"
                setTextColor(if (isSwipePin) Color.parseColor("#00E676") else Color.parseColor("#00E5FF"))
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            (targetDisc as FrameLayout).addView(label)

            root.addView(targetDisc)
            addView(root)
        }

        fun updateCoords(x: Float, y: Float) {
            centerPoint.set(x, y)
            coordBadge.text = "${x.toInt()}, ${y.toInt()}"
        }

        fun flashActive() {
            targetDisc.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FFD600"))
                setStroke(dpToPx(2.5f), Color.WHITE)
            }
        }

        fun flashSuccess(success: Boolean) {
            targetDisc.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (success) Color.parseColor("#00E676") else Color.parseColor("#FF1744"))
                setStroke(dpToPx(2.5f), Color.WHITE)
            }
            handler.postDelayed({
                targetDisc.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#DD0A0E17"))
                    setStroke(
                        dpToPx(2f),
                        if (isSwipePin) Color.parseColor("#00E676") else Color.parseColor("#00E5FF")
                    )
                }
            }, 300L)
        }

        fun setupDraggable(params: WindowManager.LayoutParams, onMoved: (Float, Float) -> Unit) {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var isDrag = false

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isDrag = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchX
                        val dy = event.rawY - touchY
                        if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) {
                            isDrag = true
                        }
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()

                        val cx = params.x + width / 2f
                        val cy = params.y + height / 2f
                        updateCoords(cx, cy)
                        windowManager.updateViewLayout(this, params)
                        onMoved(cx, cy)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDrag) {
                            onSingleTap?.invoke()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    /**
     * Canvas view drawing visual connection lines and swipe trajectory vectors.
     */
    inner class TrajectoryCanvasView(ctx: Context) : View(ctx) {
        private var targets: List<InGameActionTarget> = emptyList()

        private val vectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            strokeWidth = dpToPx(3f).toFloat()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            style = Paint.Style.FILL
        }

        private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D00E5FF")
            strokeWidth = dpToPx(1.5f).toFloat()
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }

        fun setTargets(list: List<InGameActionTarget>) {
            targets = list
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            var prevCenter: PointF? = null

            for (target in targets) {
                if (target.type == ActionType.CLICK) {
                    val view = target.view as? ClickPointerView ?: continue
                    val currentCenter = view.centerPoint

                    // Draw dashed connector line between sequential clicks
                    prevCenter?.let { prev ->
                        canvas.drawLine(prev.x, prev.y, currentCenter.x, currentCenter.y, dashedLinePaint)
                    }
                    prevCenter = currentCenter
                } else if (target.type == ActionType.SWIPE) {
                    val startV = target.view as? ClickPointerView ?: continue
                    val endV = target.endView as? ClickPointerView ?: continue
                    val p1 = startV.centerPoint
                    val p2 = endV.centerPoint

                    // Draw solid vector line
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, vectorPaint)

                    // Draw direction arrow at end
                    drawArrow(canvas, p1.x, p1.y, p2.x, p2.y)
                    prevCenter = p2
                }
            }
        }

        private fun drawArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
            val deltaX = x2 - x1
            val deltaY = y2 - y1
            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
            if (distance < 10f) return

            val angle = Math.atan2(deltaY.toDouble(), deltaX.toDouble())
            val arrowLength = dpToPx(16f).toFloat()
            val arrowAngle = Math.PI / 6 // 30 degrees

            val x3 = (x2 - arrowLength * Math.cos(angle - arrowAngle)).toFloat()
            val y3 = (y2 - arrowLength * Math.sin(angle - arrowAngle)).toFloat()
            val x4 = (x2 - arrowLength * Math.cos(angle + arrowAngle)).toFloat()
            val y4 = (y2 - arrowLength * Math.sin(angle + arrowAngle)).toFloat()

            val path = android.graphics.Path().apply {
                moveTo(x2, y2)
                lineTo(x3, y3)
                lineTo(x4, y4)
                close()
            }
            canvas.drawPath(path, arrowPaint)
        }
    }
}
