package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.engine.EngineStatus
import com.example.engine.ScenarioExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Persistent Floating Action Button (FAB) and Floating Controller Overlay Service.
 * Stays active over third-party applications to provide instant Start, Pause, Stop,
 * and Emergency Stop actions without switching back to the app.
 */
class FloatingOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_EMERGENCY_STOP = "com.example.service.ACTION_EMERGENCY_STOP"
        const val ACTION_SHOW_OVERLAY = "com.example.service.ACTION_SHOW_OVERLAY"
        const val ACTION_LAUNCH_SNIP = "com.example.service.ACTION_LAUNCH_SNIP"
        const val ACTION_LAUNCH_PALETTE = "com.example.service.ACTION_LAUNCH_PALETTE"

        private val _isOverlayVisible = MutableStateFlow(false)
        val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun launchActionPalette(context: Context, scenarioId: Long? = null) {
            FloatingActionPalette.activeScenarioIdForPalette = scenarioId
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_LAUNCH_PALETTE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun launchRegionSelector(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_LAUNCH_SNIP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var rootOverlayView: FrameLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null

    // Sub-views
    private var fabButtonView: View? = null
    private var expandedHudView: View? = null

    // UI elements to update dynamically
    private var statusBadgeText: TextView? = null
    private var scenarioTitleText: TextView? = null
    private var stepInfoText: TextView? = null
    private var playPauseButton: TextView? = null
    private var fabIconText: TextView? = null

    private var isExpanded = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification("Contrôleur actif - Touchez le bouton flottant")
        startForeground(NOTIFICATION_ID, notification)
        _isOverlayVisible.value = true

        observeExecutionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> triggerStartOrResume()
            ACTION_PAUSE -> triggerPause()
            ACTION_STOP -> triggerStop()
            ACTION_EMERGENCY_STOP -> triggerEmergencyStop()
            ACTION_LAUNCH_SNIP -> {
                if (canDrawOverlays(this)) {
                    buildAndAttachOverlay()
                    openInteractiveRegionSelector()
                }
            }
            ACTION_LAUNCH_PALETTE -> {
                if (canDrawOverlays(this)) {
                    buildAndAttachOverlay()
                    openActionPalette()
                }
            }
            else -> {
                if (canDrawOverlays(this)) {
                    buildAndAttachOverlay()
                }
            }
        }
        return START_STICKY
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun buildAndAttachOverlay() {
        if (rootOverlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 350
        }
        windowParams = params

        rootOverlayView = FrameLayout(this)

        // 1. Create Compact Circular FAB
        fabButtonView = createFabButton()
        // 2. Create Expanded HUD Controller
        expandedHudView = createExpandedHud()

        rootOverlayView?.addView(fabButtonView)
        rootOverlayView?.addView(expandedHudView)

        // Initial state: Show FAB, hide expanded HUD
        expandedHudView?.visibility = View.GONE
        fabButtonView?.visibility = View.VISIBLE

        setupTouchDrag(rootOverlayView!!, params)

        try {
            windowManager?.addView(rootOverlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Compact Floating Action Button (FAB)
     * Pulsing circular button that hovers over any app.
     */
    private fun createFabButton(): View {
        val sizePx = dpToPx(56f)
        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            elevation = dpToPx(12f).toFloat()
        }

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E60A0E17"))
            setStroke(dpToPx(2.5f), Color.parseColor("#00E5FF"))
        }
        container.background = bgDrawable

        val icon = TextView(this).apply {
            text = "⚡"
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        fabIconText = icon
        container.addView(icon)

        return container
    }

    /**
     * Expanded Floating Controller HUD Bar
     */
    private fun createExpandedHud(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(14f)
            setPadding(pad, pad, pad, pad)
            elevation = dpToPx(16f).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(310f),
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16f).toFloat()
            setColor(Color.parseColor("#F50A0E17"))
            setStroke(dpToPx(1.5f), Color.parseColor("#00E5FF"))
        }
        container.background = bgDrawable

        // Header: Drag handle, Title, Minimize & Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val title = TextView(this).apply {
            text = "⚡ AutoClick AI"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val minimizeBtn = TextView(this).apply {
            text = " — "
            setTextColor(Color.parseColor("#9EAFD0"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8f), 0, dpToPx(8f), 0)
            setOnClickListener {
                toggleExpandMode(false)
            }
        }
        header.addView(minimizeBtn)

        val closeBtn = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 15f
            setPadding(dpToPx(6f), 0, dpToPx(4f), 0)
            setOnClickListener {
                stopSelf()
            }
        }
        header.addView(closeBtn)
        container.addView(header)

        // Status & Scenario Info Block
        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(8f)
            setPadding(pad, pad, pad, pad)
            val margin = dpToPx(8f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = margin
                bottomMargin = margin
            }
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#1A1F2C"))
            }
        }

        val statusBadge = TextView(this).apply {
            text = "⚪ EN ATTENTE"
            setTextColor(Color.parseColor("#9EAFD0"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        statusBadgeText = statusBadge
        infoCard.addView(statusBadge)

        val scTitle = TextView(this).apply {
            text = "Scénario : Aucun sélectionné"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, dpToPx(2f), 0, 0)
        }
        scenarioTitleText = scTitle
        infoCard.addView(scTitle)

        val stepInfo = TextView(this).apply {
            text = "Prêt à déclencher les gestes sans root"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            setPadding(0, dpToPx(2f), 0, 0)
        }
        stepInfoText = stepInfo
        infoCard.addView(stepInfo)

        container.addView(infoCard)

        // Action Buttons Row: [START / PAUSE] [STOP] [EMERGENCY STOP]
        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Play/Pause Button
        val playBtn = TextView(this).apply {
            text = "▶ Démarrer"
            setTextColor(Color.parseColor("#0A0E17"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val h = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(0, h, 1.2f).apply {
                rightMargin = dpToPx(6f)
            }
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            setOnClickListener {
                triggerStartOrResume()
            }
        }
        playPauseButton = playBtn
        buttonsRow.addView(playBtn)

        // Normal Stop Button
        val stopBtn = TextView(this).apply {
            text = "⏹ Stop"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            val h = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(0, h, 0.9f).apply {
                rightMargin = dpToPx(6f)
            }
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#263238"))
                setStroke(dpToPx(1f), Color.parseColor("#455A64"))
            }
            setOnClickListener {
                triggerStop()
            }
        }
        buttonsRow.addView(stopBtn)

        // BIG RED EMERGENCY STOP BUTTON
        val emergencyBtn = TextView(this).apply {
            text = "🛑 URGENCE"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val h = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(0, h, 1.3f)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#FF1744"))
            }
            setOnClickListener {
                triggerEmergencyStop()
            }
        }
        buttonsRow.addView(emergencyBtn)
        container.addView(buttonsRow)

        // In-Game Palette & Screen Snip Tools Row
        val toolsRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8f)
            }
        }

        // 1. In-game Action Palette Button (Target coordinates directly over game)
        val inGamePaletteBtn = TextView(this).apply {
            text = "🎮 Palette d'Actions en Jeu (En direct)"
            setTextColor(Color.parseColor("#0A0E17"))
            textSize = 11.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val h = dpToPx(36f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                h
            ).apply {
                bottomMargin = dpToPx(6f)
            }
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#00E5FF"))
            }
            setOnClickListener {
                openActionPalette()
            }
        }
        toolsRow.addView(inGamePaletteBtn)

        // 2. Region / Screen Snip Tool Button
        val regionSnipBtn = TextView(this).apply {
            text = "🎯 Sélecteur de Région / Snip (Écran)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val h = dpToPx(34f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                h
            )
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#152033"))
                setStroke(dpToPx(1f), Color.parseColor("#00E5FF"))
            }
            setOnClickListener {
                openInteractiveRegionSelector()
            }
        }
        toolsRow.addView(regionSnipBtn)
        container.addView(toolsRow)

        return container
    }

    private var activeRegionSelector: InteractiveRegionSelector? = null
    private var activeActionPalette: FloatingActionPalette? = null

    private fun openActionPalette() {
        toggleExpandMode(false)
        rootOverlayView?.visibility = View.GONE

        activeActionPalette = FloatingActionPalette(
            context = this,
            scenarioId = FloatingActionPalette.activeScenarioIdForPalette,
            onClose = {
                rootOverlayView?.visibility = View.VISIBLE
                activeActionPalette = null
            },
            onLaunchSnip = {
                openInteractiveRegionSelector()
            }
        ).apply {
            show()
        }
    }

    private fun openInteractiveRegionSelector() {
        toggleExpandMode(false)
        rootOverlayView?.visibility = View.GONE

        activeRegionSelector = InteractiveRegionSelector(
            context = this,
            onRegionSelected = { _ ->
                rootOverlayView?.visibility = View.VISIBLE
                activeRegionSelector = null
            },
            onTemplateCaptured = { _, _ ->
                rootOverlayView?.visibility = View.VISIBLE
                activeRegionSelector = null
            },
            onClose = {
                rootOverlayView?.visibility = View.VISIBLE
                activeRegionSelector = null
            }
        ).apply {
            show()
        }
    }

    /**
     * Draggable touch listener with smooth tracking & tap detection.
     */
    private fun setupTouchDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (!isDragging && (abs(dx) > 12 || abs(dy) > 12)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager?.updateViewLayout(rootOverlayView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Single Tap Detected on FAB!
                            if (!isExpanded) {
                                toggleExpandMode(true)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun toggleExpandMode(expand: Boolean) {
        isExpanded = expand
        if (expand) {
            fabButtonView?.visibility = View.GONE
            expandedHudView?.visibility = View.VISIBLE
        } else {
            expandedHudView?.visibility = View.GONE
            fabButtonView?.visibility = View.VISIBLE
        }
    }

    /**
     * Listens to the ScenarioExecutor state stream and updates the floating UI dynamically.
     */
    private fun observeExecutionState() {
        stateObserverJob = scope.launch {
            val executor = ScenarioExecutor.activeInstance
            executor?.state?.collect { state ->
                updateUiForState(state.status, state.scenarioTitle, state.currentStepNumber, state.totalSteps, state.currentStepName)
            }
        }
    }

    private fun updateUiForState(
        status: EngineStatus,
        scenarioTitle: String,
        currentStepNumber: Int,
        totalSteps: Int,
        currentStepName: String
    ) {
        when (status) {
            EngineStatus.RUNNING -> {
                statusBadgeText?.text = "🟢 EN COURS D'EXÉCUTION"
                statusBadgeText?.setTextColor(Color.parseColor("#00E676"))
                playPauseButton?.text = "⏸ Pause"
                playPauseButton?.background = GradientDrawable().apply {
                    cornerRadius = dpToPx(8f).toFloat()
                    setColor(Color.parseColor("#FFD600"))
                }
                fabIconText?.text = "▶"
                (fabButtonView?.background as? GradientDrawable)?.setStroke(dpToPx(3f), Color.parseColor("#00E676"))
            }
            EngineStatus.PAUSED -> {
                statusBadgeText?.text = "⏸ EN PAUSE"
                statusBadgeText?.setTextColor(Color.parseColor("#FFD600"))
                playPauseButton?.text = "▶ Reprendre"
                playPauseButton?.background = GradientDrawable().apply {
                    cornerRadius = dpToPx(8f).toFloat()
                    setColor(Color.parseColor("#00E676"))
                }
                fabIconText?.text = "⏸"
                (fabButtonView?.background as? GradientDrawable)?.setStroke(dpToPx(3f), Color.parseColor("#FFD600"))
            }
            EngineStatus.STOPPED -> {
                statusBadgeText?.text = "🛑 ARRÊT D'URGENCE"
                statusBadgeText?.setTextColor(Color.parseColor("#FF1744"))
                playPauseButton?.text = "▶ Démarrer"
                playPauseButton?.background = GradientDrawable().apply {
                    cornerRadius = dpToPx(8f).toFloat()
                    setColor(Color.parseColor("#00E676"))
                }
                fabIconText?.text = "🛑"
                (fabButtonView?.background as? GradientDrawable)?.setStroke(dpToPx(3f), Color.parseColor("#FF1744"))
            }
            EngineStatus.IDLE -> {
                statusBadgeText?.text = "⚪ EN ATTENTE"
                statusBadgeText?.setTextColor(Color.parseColor("#9EAFD0"))
                playPauseButton?.text = "▶ Démarrer"
                playPauseButton?.background = GradientDrawable().apply {
                    cornerRadius = dpToPx(8f).toFloat()
                    setColor(Color.parseColor("#00E676"))
                }
                fabIconText?.text = "⚡"
                (fabButtonView?.background as? GradientDrawable)?.setStroke(dpToPx(2.5f), Color.parseColor("#00E5FF"))
            }
        }

        if (scenarioTitle.isNotBlank()) {
            scenarioTitleText?.text = "Scénario : $scenarioTitle"
        }
        if (totalSteps > 0 && currentStepNumber > 0) {
            stepInfoText?.text = "Étape $currentStepNumber / $totalSteps : $currentStepName"
        }
    }

    // --- Action triggers ---

    private fun triggerStartOrResume() {
        val executor = ScenarioExecutor.activeInstance
        if (executor != null) {
            val currentState = executor.state.value.status
            if (currentState == EngineStatus.PAUSED) {
                executor.resume()
            } else if (currentState != EngineStatus.RUNNING) {
                executor.startDefaultOrResume(isDryRun = false)
            }
        }
    }

    private fun triggerPause() {
        ScenarioExecutor.activeInstance?.pause()
    }

    private fun triggerStop() {
        ScenarioExecutor.activeInstance?.stopImmediately()
    }

    private fun triggerEmergencyStop() {
        ScenarioExecutor.activeInstance?.stopImmediately()
        // Flash HUD red briefly
        statusBadgeText?.text = "🛑 ARRÊT D'URGENCE ACTIVÉ"
        statusBadgeText?.setTextColor(Color.parseColor("#FF1744"))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contrôleur Flottant AutoClick AI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bouton flottant persistant de contrôle par-dessus les autres applications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val emergencyIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingOverlayService::class.java).apply {
                action = ACTION_EMERGENCY_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingOverlayService::class.java).apply {
                action = ACTION_START
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ AutoClick AI - Contrôleur Flottant")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_play, "Démarrer", playIntent)
            .addAction(android.R.drawable.ic_delete, "Arrêt d'Urgence", emergencyIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        activeActionPalette?.dismiss()
        activeActionPalette = null
        activeRegionSelector?.hide()
        activeRegionSelector = null
        if (rootOverlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(rootOverlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rootOverlayView = null
        }
        _isOverlayVisible.value = false
    }
}
