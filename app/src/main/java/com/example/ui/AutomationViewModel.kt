package com.example.ui

import android.app.Application
import android.content.Intent
import android.graphics.PointF
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAutomationService
import com.example.data.AppDatabase
import com.example.data.AutomationRepository
import com.example.data.JsonUtils
import com.example.engine.ActiveExecutionState
import com.example.engine.DetectionResult
import com.example.engine.EngineStatus
import com.example.engine.ScenarioExecutor
import com.example.engine.ScreenDetectionEngine
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.AiMemoryEntity
import com.example.model.DetectionZone
import com.example.model.ExecutionLogEntity
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import com.example.service.AutomationAccessibilityService
import com.example.service.FloatingOverlayService
import com.example.service.ScreenCaptureService
import com.example.engine.ScreenCaptureManager
import com.example.engine.ImageRecognitionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val generatedScenario: ScenarioEntity? = null,
    val generatedSteps: List<ActionStep>? = null
)

class AutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = AutomationRepository(database)
    val executor = ScenarioExecutor(application, repository)
    val geminiService = GeminiAutomationService(application)

    // Data streams
    val scenarios: StateFlow<List<ScenarioEntity>> = repository.allScenarios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<ExecutionLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<AiMemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val executionState: StateFlow<ActiveExecutionState> = executor.state
    val isAccessibilityActive: StateFlow<Boolean> = AutomationAccessibilityService.isServiceActive
    val isOverlayActive: StateFlow<Boolean> = FloatingOverlayService.isOverlayVisible

    // Currently edited scenario
    private val _editingScenario = MutableStateFlow<ScenarioEntity?>(null)
    val editingScenario: StateFlow<ScenarioEntity?> = _editingScenario.asStateFlow()

    private val _editingSteps = MutableStateFlow<List<ActionStep>>(emptyList())
    val editingSteps: StateFlow<List<ActionStep>> = _editingSteps.asStateFlow()

    // Gesture Recording (Macro Recorder)
    private val _isRecordingGestures = MutableStateFlow(false)
    val isRecordingGestures: StateFlow<Boolean> = _isRecordingGestures.asStateFlow()

    private val recordedPoints = mutableListOf<Pair<Float, Float>>()
    private val recordedDelays = mutableListOf<Long>()
    private var lastRecordedTime = 0L

    private val _recordedCount = MutableStateFlow(0)
    val recordedCount: StateFlow<Int> = _recordedCount.asStateFlow()

    // AI Copilot state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Bonjour ! Je suis l'intelligence artificielle de AutoClick AI. Je peux concevoir des scénarios d'automatisation sur mesure, analyser vos tâches, ou convertir vos gestes en macros intelligentes. Que souhaitez-vous automatiser aujourd'hui ?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    // Live Detection Testing state
    private val _detectionZones = MutableStateFlow(
        listOf(
            DetectionZone(name = "Zone Haute (Bannières & En-têtes)", leftPct = 0.05f, topPct = 0.05f, rightPct = 0.95f, bottomPct = 0.35f, priority = "Moyenne"),
            DetectionZone(name = "Zone Centrale (Contenu & Items)", leftPct = 0.05f, topPct = 0.35f, rightPct = 0.95f, bottomPct = 0.75f, priority = "Haute"),
            DetectionZone(name = "Zone Basse (Boutons d'action & Validation)", leftPct = 0.05f, topPct = 0.75f, rightPct = 0.95f, bottomPct = 0.95f, priority = "Critique")
        )
    )
    val detectionZones: StateFlow<List<DetectionZone>> = _detectionZones.asStateFlow()

    private val _liveDetectionResult = MutableStateFlow<DetectionResult?>(null)
    val liveDetectionResult: StateFlow<DetectionResult?> = _liveDetectionResult.asStateFlow()

    // Real-time Screen Capture (MediaProjection) flows
    val isScreenCaptureRunning: StateFlow<Boolean> = ScreenCaptureManager.isCapturing
    val screenCaptureFps: StateFlow<Int> = ScreenCaptureManager.fps
    val latestCapturedFrame: StateFlow<android.graphics.Bitmap?> = ScreenCaptureManager.latestFrame
    val activeMonitoredRoi: StateFlow<android.graphics.Rect?> = ScreenCaptureManager.activeMonitoredRoi

    // Selected Tab
    private val _currentNavIndex = MutableStateFlow(0)
    val currentNavIndex: StateFlow<Int> = _currentNavIndex.asStateFlow()

    // Dry Run Mode Global Toggle
    private val _isGlobalDryRun = MutableStateFlow(false)
    val isGlobalDryRun: StateFlow<Boolean> = _isGlobalDryRun.asStateFlow()

    // Floating Test HUD Visible in App
    private val _showInAppFloatingHud = MutableStateFlow(true)
    val showInAppFloatingHud: StateFlow<Boolean> = _showInAppFloatingHud.asStateFlow()

    // Ethics & Terms Dialog
    private val _showEthicsModal = MutableStateFlow(false)
    val showEthicsModal: StateFlow<Boolean> = _showEthicsModal.asStateFlow()

    fun setNavIndex(index: Int) {
        _currentNavIndex.value = index
    }

    fun toggleGlobalDryRun() {
        _isGlobalDryRun.value = !_isGlobalDryRun.value
    }

    fun toggleInAppFloatingHud() {
        _showInAppFloatingHud.value = !_showInAppFloatingHud.value
    }

    fun setEthicsModalVisible(visible: Boolean) {
        _showEthicsModal.value = visible
    }

    // --- Execution Controls ---

    fun runScenario(scenario: ScenarioEntity, isDryRun: Boolean = _isGlobalDryRun.value) {
        val steps = JsonUtils.jsonToSteps(scenario.stepsJson)
        executor.startScenario(scenario, steps, isDryRun)
    }

    fun pauseExecution() {
        executor.pause()
    }

    fun resumeExecution() {
        executor.resume()
    }

    fun triggerEmergencyStop() {
        executor.stopImmediately()
        viewModelScope.launch {
            repository.log(
                scenarioId = 0,
                scenarioTitle = "ARRÊT D'URGENCE",
                level = "ERREUR",
                message = "Arrêt d'urgence immédiat activé par l'utilisateur. Toutes les actions et gestes sont neutralisés."
            )
        }
    }

    // --- Scenario Editing ---

    fun createNewScenario() {
        val defaultStep = ActionStep(
            stepNumber = 1,
            name = "Clic sur élément",
            actionType = ActionType.CLICK,
            targetType = TargetType.COORDINATES,
            targetX = 540f,
            targetY = 1200f
        )
        val newScenario = ScenarioEntity(
            title = "Nouveau Scénario",
            description = "Description de mon automatisation personnalisée",
            stepsJson = JsonUtils.stepsToJson(listOf(defaultStep)),
            loopCount = 1
        )
        _editingScenario.value = newScenario
        _editingSteps.value = listOf(defaultStep)
    }

    fun selectScenarioForEdit(scenario: ScenarioEntity) {
        _editingScenario.value = scenario
        _editingSteps.value = JsonUtils.jsonToSteps(scenario.stepsJson)
    }

    fun addStepToEditing(actionType: ActionType = ActionType.CLICK) {
        val current = _editingSteps.value.toMutableList()
        val nextNumber = current.size + 1
        current.add(
            ActionStep(
                stepNumber = nextNumber,
                name = "${actionType.label} #$nextNumber",
                actionType = actionType,
                targetType = if (actionType == ActionType.TEXT_INPUT) TargetType.COORDINATES else TargetType.TEXT_MATCH,
                targetX = 540f,
                targetY = 1000f + (nextNumber * 40f)
            )
        )
        _editingSteps.value = current
    }

    fun removeStepFromEditing(index: Int) {
        val current = _editingSteps.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            // Re-index step numbers
            val reindexed = current.mapIndexed { i, step -> step.copy(stepNumber = i + 1) }
            _editingSteps.value = reindexed
        }
    }

    fun updateStep(index: Int, updated: ActionStep) {
        val current = _editingSteps.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _editingSteps.value = current
        }
    }

    fun reorderSteps(fromIndex: Int, toIndex: Int) {
        val current = _editingSteps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val moved = current.removeAt(fromIndex)
            current.add(toIndex, moved)
            _editingSteps.value = current.mapIndexed { idx, s -> s.copy(stepNumber = idx + 1) }
        }
    }

    fun moveStepUp(index: Int) {
        if (index > 0) {
            reorderSteps(index, index - 1)
        }
    }

    fun moveStepDown(index: Int) {
        if (index < _editingSteps.value.size - 1) {
            reorderSteps(index, index + 1)
        }
    }

    fun duplicateStep(index: Int) {
        val current = _editingSteps.value.toMutableList()
        if (index in current.indices) {
            val original = current[index]
            val cloned = original.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${original.name} (Copie)"
            )
            current.add(index + 1, cloned)
            _editingSteps.value = current.mapIndexed { idx, s -> s.copy(stepNumber = idx + 1) }
        }
    }

    fun testSingleStep(step: ActionStep) {
        val scenario = _editingScenario.value ?: ScenarioEntity(
            title = "Test Étape Unique",
            description = "Test direct",
            stepsJson = "[]"
        )
        executor.startScenario(scenario, listOf(step), isDryRun = _isGlobalDryRun.value)
    }

    fun saveEditingScenario(title: String, description: String, loopCount: Int) {
        val current = _editingScenario.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title.ifBlank { "Scénario sans titre" },
                description = description,
                loopCount = loopCount,
                stepsJson = JsonUtils.stepsToJson(_editingSteps.value)
            )
            repository.saveScenario(updated)
            _editingScenario.value = null
            _editingSteps.value = emptyList()
        }
    }

    fun cancelEditing() {
        _editingScenario.value = null
        _editingSteps.value = emptyList()
    }

    fun deleteScenario(id: Long) {
        viewModelScope.launch {
            repository.deleteScenario(id)
        }
    }

    // --- Gesture Recording (Macro Recorder) ---

    fun startGestureRecording() {
        recordedPoints.clear()
        recordedDelays.clear()
        lastRecordedTime = System.currentTimeMillis()
        _recordedCount.value = 0
        _isRecordingGestures.value = true
    }

    fun recordTouchPoint(x: Float, y: Float) {
        if (!_isRecordingGestures.value) return
        val now = System.currentTimeMillis()
        val delaySinceLast = if (recordedPoints.isEmpty()) 300L else (now - lastRecordedTime)
        recordedPoints.add(Pair(x, y))
        recordedDelays.add(delaySinceLast)
        lastRecordedTime = now
        _recordedCount.value = recordedPoints.size
    }

    fun finishRecordingAndGenerate() {
        _isRecordingGestures.value = false
        if (recordedPoints.isEmpty()) return

        viewModelScope.launch {
            val (scenario, steps) = geminiService.analyzeRecordedGestures(
                recordedPoints,
                recordedDelays
            )
            val id = repository.saveScenario(scenario)
            repository.log(
                scenarioId = id,
                scenarioTitle = scenario.title,
                level = "INFO",
                message = "Macro enregistrée avec ${steps.size} clics générée avec succès."
            )
            _editingScenario.value = scenario.copy(id = id)
            _editingSteps.value = steps
            _currentNavIndex.value = 1 // Switch to Scenario Builder
        }
    }

    fun cancelRecording() {
        _isRecordingGestures.value = false
        recordedPoints.clear()
        recordedDelays.clear()
        _recordedCount.value = 0
    }

    // --- AI Copilot Interactions ---

    fun sendUserGoalToAi(goalText: String) {
        if (goalText.isBlank()) return
        val userMsg = ChatMessage(isUser = true, text = goalText)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiGenerating.value = true

        viewModelScope.launch {
            try {
                val (scenario, steps) = geminiService.generateScenarioFromGoal(goalText)
                val replyText = "J'ai conçu un scénario adapté pour votre objectif : \"${scenario.title}\". Il contient ${steps.size} étapes optimisées avec humanisation des mouvements et vérifications conditionnelles."

                val aiMsg = ChatMessage(
                    isUser = false,
                    text = replyText,
                    generatedScenario = scenario,
                    generatedSteps = steps
                )
                _chatMessages.value = _chatMessages.value + aiMsg

                // Record memory
                repository.addMemory(
                    AiMemoryEntity(
                        scenarioTitle = scenario.title,
                        goal = goalText,
                        observedState = "Création de nouveau scénario",
                        actionDecision = "${steps.size} étapes générées",
                        outcome = "SUCCESS",
                        learnedRule = "Structure optimisée avec pauses adaptatives pour '${goalText.take(25)}'"
                    )
                )
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    isUser = false,
                    text = "Une erreur est survenue lors de la génération. Voici un scénario de base prêt à être adapté."
                )
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun applyAiGeneratedScenario(scenario: ScenarioEntity, steps: List<ActionStep>) {
        viewModelScope.launch {
            val id = repository.saveScenario(scenario)
            _editingScenario.value = scenario.copy(id = id)
            _editingSteps.value = steps
            _currentNavIndex.value = 1 // Switch to builder
        }
    }

    // --- Detection Testing ---

    fun testDetection(targetText: String, targetColorHex: String) {
        viewModelScope.launch {
            val result = ScreenDetectionEngine.findTextOnScreen(
                text = targetText,
                zones = _detectionZones.value
            )
            _liveDetectionResult.value = result

            repository.log(
                scenarioId = 0,
                scenarioTitle = "TESTEUR DE DÉTECTION (OCR)",
                level = if (result.isFound) "DÉTECTION" else "ALERTE",
                message = if (result.isFound) "Cible texte '$targetText' trouvée avec score ${(result.confidence * 100).toInt()}%" else "Cible texte '$targetText' non détectée à l'écran."
            )
        }
    }

    fun testImageDetection(templateId: String, similarityThreshold: Float = 0.75f) {
        viewModelScope.launch {
            val result = ScreenDetectionEngine.findImageOnScreen(
                templateIdOrBase64 = templateId,
                similarityThreshold = similarityThreshold,
                zones = _detectionZones.value
            )
            _liveDetectionResult.value = result

            val templateName = com.example.engine.ImageRecognitionEngine.BUILTIN_TEMPLATES.find { it.id == templateId }?.name ?: templateId
            repository.log(
                scenarioId = 0,
                scenarioTitle = "RECONNAISSANCE D'IMAGE",
                level = if (result.isFound) "DÉTECTION" else "ALERTE",
                message = if (result.isFound) {
                    "Image '$templateName' reconnue à (${result.point?.x?.toInt()}, ${result.point?.y?.toInt()}) [Similarité ${(result.confidence * 100).toInt()}%]"
                } else {
                    "Image '$templateName' non détectée avec un seuil de ${(similarityThreshold * 100).toInt()}%"
                }
            )
        }
    }

    fun toggleZoneActive(zoneId: String) {
        _detectionZones.value = _detectionZones.value.map {
            if (it.id == zoneId) it.copy(isActive = !it.isActive) else it
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    // Toggle persistent floating action button service over other apps
    fun toggleFloatingOverlay() {
        val context = getApplication<Application>()
        if (FloatingOverlayService.isOverlayVisible.value) {
            FloatingOverlayService.stopService(context)
        } else {
            if (!FloatingOverlayService.canDrawOverlays(context)) {
                openOverlaySettings()
            } else {
                FloatingOverlayService.startService(context)
            }
        }
    }

    // Open accessibility settings on device
    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Open overlay settings
    fun openOverlaySettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${getApplication<Application>().packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // MediaProjection API controls
    fun startMediaProjection(resultCode: Int, data: Intent) {
        val context = getApplication<Application>()
        ScreenCaptureService.start(context, resultCode, data)
        viewModelScope.launch {
            repository.log(
                scenarioId = 0,
                scenarioTitle = "MEDIA PROJECTION",
                level = "INFO",
                message = "Capture d'écran temps réel initialisée avec succès (MediaProjection API)"
            )
        }
    }

    fun stopMediaProjection() {
        val context = getApplication<Application>()
        ScreenCaptureService.stop(context)
        viewModelScope.launch {
            repository.log(
                scenarioId = 0,
                scenarioTitle = "MEDIA PROJECTION",
                level = "INFO",
                message = "Capture d'écran arrêtée"
            )
        }
    }

    // Launch Macrorify-style Interactive Region / Snip Tool directly over other apps
    fun launchInteractiveRegionSelector() {
        val context = getApplication<Application>()
        if (!FloatingOverlayService.canDrawOverlays(context)) {
            openOverlaySettings()
        } else {
            FloatingOverlayService.launchRegionSelector(context)
        }
    }

    // Capture specific region (0.0..1.0) and save as reusable template
    fun captureAndSaveRegion(name: String, leftPct: Float, topPct: Float, rightPct: Float, bottomPct: Float) {
        viewModelScope.launch {
            val bmp = ScreenCaptureManager.captureRegionPercent(leftPct, topPct, rightPct, bottomPct)
            if (bmp != null) {
                val template = ImageRecognitionEngine.registerCustomTemplate(name, bmp)
                repository.log(
                    scenarioId = 0,
                    scenarioTitle = "CAPTURE RÉGION (ROI)",
                    level = "SUCCÈS",
                    message = "Nouvelle région '$name' capturée (${bmp.width}x${bmp.height}px) et ajoutée aux modèles"
                )
            } else {
                repository.log(
                    scenarioId = 0,
                    scenarioTitle = "CAPTURE RÉGION (ROI)",
                    level = "ALERTE",
                    message = "Veuillez activer MediaProjection pour capturer l'écran réel"
                )
            }
        }
    }

    fun addCustomZone(name: String, leftPct: Float, topPct: Float, rightPct: Float, bottomPct: Float) {
        val zone = DetectionZone(
            name = name,
            leftPct = leftPct,
            topPct = topPct,
            rightPct = rightPct,
            bottomPct = bottomPct,
            priority = "Haute",
            isActive = true
        )
        _detectionZones.value = listOf(zone) + _detectionZones.value
    }
}
