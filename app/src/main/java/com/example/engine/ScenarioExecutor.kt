package com.example.engine

import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.AutomationRepository
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import com.example.service.AutomationAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

enum class EngineStatus(val label: String) {
    IDLE("En attente"),
    RUNNING("En cours d'exécution"),
    PAUSED("En pause"),
    STOPPED("Arrêté d'urgence")
}

data class ActiveExecutionState(
    val status: EngineStatus = EngineStatus.IDLE,
    val scenarioTitle: String = "",
    val scenarioId: Long = 0L,
    val currentStepNumber: Int = 0,
    val totalSteps: Int = 0,
    val currentStepName: String = "",
    val currentLoop: Int = 0,
    val totalLoops: Int = 1,
    val isDryRun: Boolean = false,
    val activeTargetPoint: PointF? = null,
    val lastActionDescription: String = "",
    val errorMessage: String? = null
)

class ScenarioExecutor(
    private val context: Context,
    private val repository: AutomationRepository
) {
    companion object {
        private var instanceRef: java.lang.ref.WeakReference<ScenarioExecutor>? = null
        var activeInstance: ScenarioExecutor?
            get() = instanceRef?.get()
            set(value) {
                instanceRef = if (value != null) java.lang.ref.WeakReference(value) else null
            }
    }

    init {
        activeInstance = this
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var executionJob: Job? = null

    private val _state = MutableStateFlow(ActiveExecutionState())
    val state: StateFlow<ActiveExecutionState> = _state.asStateFlow()

    private var isPaused = false

    /**
     * Starts execution of a scenario given an entity.
     */
    fun startScenario(scenario: ScenarioEntity, isDryRun: Boolean = false) {
        val steps = com.example.data.JsonUtils.jsonToSteps(scenario.stepsJson)
        startScenario(scenario, steps, isDryRun)
    }

    /**
     * Starts the first available scenario or resumes the current execution.
     */
    fun startDefaultOrResume(isDryRun: Boolean = false) {
        if (isPaused) {
            resume()
            return
        }
        if (_state.value.status == EngineStatus.RUNNING) {
            return
        }

        scope.launch {
            val scenarios = repository.allScenarios.firstOrNull() ?: emptyList()
            val targetScenario = scenarios.firstOrNull { it.isEnabled } ?: scenarios.firstOrNull()
            if (targetScenario != null) {
                val steps = com.example.data.JsonUtils.jsonToSteps(targetScenario.stepsJson)
                startScenario(targetScenario, steps, isDryRun)
            } else {
                _state.value = ActiveExecutionState(
                    status = EngineStatus.IDLE,
                    errorMessage = "Aucun scénario disponible. Créez un scénario pour démarrer."
                )
            }
        }
    }

    /**
     * Starts execution of a scenario.
     * @param isDryRun If true, runs a "test à blanc" without dispatching real gestures.
     */
    fun startScenario(
        scenario: ScenarioEntity,
        steps: List<ActionStep>,
        isDryRun: Boolean = false
    ) {
        stopImmediately() // Cancel any ongoing execution
        isPaused = false

        if (steps.isEmpty()) {
            _state.value = ActiveExecutionState(
                status = EngineStatus.IDLE,
                errorMessage = "Le scénario ne contient aucune étape active."
            )
            return
        }

        executionJob = scope.launch {
            val totalLoops = if (scenario.loopCount <= 0) 999 else scenario.loopCount
            _state.value = ActiveExecutionState(
                status = EngineStatus.RUNNING,
                scenarioTitle = scenario.title,
                scenarioId = scenario.id,
                currentStepNumber = 1,
                totalSteps = steps.size,
                currentStepName = steps.first().name,
                currentLoop = 1,
                totalLoops = totalLoops,
                isDryRun = isDryRun
            )

            repository.log(
                scenarioId = scenario.id,
                scenarioTitle = scenario.title,
                level = "INFO",
                message = "Démarrage du scénario : ${scenario.title} ${if (isDryRun) "[TEST À BLANC]" else ""}"
            )

            var overallSuccess = true
            var loopIndex = 1

            try {
                while (loopIndex <= totalLoops) {
                    if (isPaused) {
                        _state.value = _state.value.copy(status = EngineStatus.PAUSED)
                        while (isPaused) {
                            delay(150)
                        }
                        _state.value = _state.value.copy(status = EngineStatus.RUNNING)
                    }

                    _state.value = _state.value.copy(currentLoop = loopIndex)
                    var lastStepSucceeded = true

                    var stepIndex = 0
                    var loopTerminatedEarly = false

                    while (stepIndex < steps.size && !loopTerminatedEarly) {
                        val step = steps[stepIndex]
                        if (!step.isEnabled) {
                            stepIndex++
                            continue
                        }

                        _state.value = _state.value.copy(
                            currentStepNumber = stepIndex + 1,
                            currentStepName = step.name
                        )

                        // If the step is a dedicated Flow Control block (Si / Alors / Sinon)
                        if (step.actionType == ActionType.BRANCH_IF_ELSE) {
                            val conditionPassed = ScreenDetectionEngine.evaluateCondition(
                                step.conditionType,
                                step.conditionParam,
                                lastStepSucceeded
                            )

                            if (conditionPassed) {
                                repository.log(
                                    scenarioId = scenario.id,
                                    scenarioTitle = scenario.title,
                                    level = "DÉTECTION",
                                    message = "✅ Condition [${step.conditionType.label}] REMPLIE -> ALORS : ${step.thenActionType.label}"
                                )
                                if (step.thenActionType == ActionType.STOP_SCENARIO) {
                                    loopTerminatedEarly = true
                                    break
                                } else if (step.thenActionType == ActionType.JUMP_TO_STEP && step.thenStepJump in 1..steps.size) {
                                    stepIndex = step.thenStepJump - 1
                                    continue
                                } else {
                                    val thenStep = step.copy(
                                        actionType = step.thenActionType,
                                        durationMs = step.thenDurationMs,
                                        textToType = step.thenTextToType
                                    )
                                    lastStepSucceeded = executeSingleStep(scenario, thenStep, isDryRun)
                                    if (step.thenStepJump in 1..steps.size) {
                                        stepIndex = step.thenStepJump - 1
                                        continue
                                    }
                                }
                            } else {
                                repository.log(
                                    scenarioId = scenario.id,
                                    scenarioTitle = scenario.title,
                                    level = "DÉTECTION",
                                    message = "❌ Condition [${step.conditionType.label}] NON REMPLIE -> SINON : ${step.elseActionType.label}"
                                )
                                if (step.elseActionType == ActionType.STOP_SCENARIO) {
                                    loopTerminatedEarly = true
                                    break
                                } else if (step.elseActionType == ActionType.JUMP_TO_STEP && step.elseStepJump in 1..steps.size) {
                                    stepIndex = step.elseStepJump - 1
                                    continue
                                } else {
                                    val elseStep = step.copy(
                                        actionType = step.elseActionType,
                                        targetX = step.elseTargetX,
                                        targetY = step.elseTargetY,
                                        swipeEndX = step.elseSwipeEndX,
                                        swipeEndY = step.elseSwipeEndY,
                                        durationMs = step.elseDurationMs,
                                        delayBeforeMs = step.elseDelayBeforeMs,
                                        textToType = step.elseTextToType
                                    )
                                    lastStepSucceeded = executeSingleStep(scenario, elseStep, isDryRun)
                                    if (step.elseStepJump in 1..steps.size) {
                                        stepIndex = step.elseStepJump - 1
                                        continue
                                    }
                                }
                            }

                            stepIndex++
                            delay(Humanizer.randomizeDelay(120L, 20))
                            continue
                        }

                        // Handling for regular actions with condition
                        val conditionPassed = ScreenDetectionEngine.evaluateCondition(
                            step.conditionType,
                            step.conditionParam,
                            lastStepSucceeded
                        )

                        if (!conditionPassed) {
                            if (step.elseActionType != ActionType.WAIT_DELAY || step.elseStepJump > 0) {
                                repository.log(
                                    scenarioId = scenario.id,
                                    scenarioTitle = scenario.title,
                                    level = "DÉTECTION",
                                    message = "❌ Condition [${step.conditionType.label}] non remplie -> Branche alternative SINON (${step.elseActionType.label})"
                                )
                                if (step.elseActionType == ActionType.STOP_SCENARIO) {
                                    loopTerminatedEarly = true
                                    break
                                }
                                val elseStep = step.copy(
                                    actionType = step.elseActionType,
                                    targetX = step.elseTargetX,
                                    targetY = step.elseTargetY,
                                    durationMs = step.elseDurationMs,
                                    textToType = step.elseTextToType
                                )
                                lastStepSucceeded = executeSingleStep(scenario, elseStep, isDryRun)
                                if (step.elseStepJump in 1..steps.size) {
                                    stepIndex = step.elseStepJump - 1
                                    continue
                                }
                            } else {
                                repository.log(
                                    scenarioId = scenario.id,
                                    scenarioTitle = scenario.title,
                                    level = "INFO",
                                    message = "Étape ${step.stepNumber} ignorée (condition non remplie : ${step.conditionType.label})"
                                )
                            }
                            stepIndex++
                            continue
                        }

                        // 2. Delay before action with human variation
                        val delayMs = Humanizer.randomizeDelay(step.delayBeforeMs, step.humanizeTimingVariance)
                        if (delayMs > 0) {
                            delay(delayMs)
                        }

                        // 3. Check for direct stop or jump
                        if (step.actionType == ActionType.STOP_SCENARIO) {
                            repository.log(scenario.id, scenario.title, "ACTION", "Arrêt programmé du scénario (Étape ${step.stepNumber})")
                            loopTerminatedEarly = true
                            break
                        } else if (step.actionType == ActionType.JUMP_TO_STEP && step.thenStepJump in 1..steps.size) {
                            repository.log(scenario.id, scenario.title, "ACTION", "Saut vers l'étape ${step.thenStepJump}")
                            stepIndex = step.thenStepJump - 1
                            continue
                        }

                        // 4. Execute step
                        val stepResult = executeSingleStep(scenario, step, isDryRun)
                        lastStepSucceeded = stepResult
                        if (!stepResult) {
                            overallSuccess = false
                        }

                        // Short resting delay between steps
                        delay(Humanizer.randomizeDelay(120L, 20))
                        stepIndex++
                    }

                    // Interval between loops
                    if (loopIndex < totalLoops && scenario.intervalBetweenLoopsMs > 0) {
                        delay(Humanizer.randomizeDelay(scenario.intervalBetweenLoopsMs, 15))
                    }

                    loopIndex++
                }

                repository.recordScenarioExecution(scenario.id, overallSuccess)
                repository.log(
                    scenarioId = scenario.id,
                    scenarioTitle = scenario.title,
                    level = "SUCCÈS",
                    message = "Scénario terminé avec succès (${loopIndex - 1} cycles complétés)"
                )

                _state.value = ActiveExecutionState(
                    status = EngineStatus.IDLE,
                    scenarioTitle = scenario.title,
                    scenarioId = scenario.id,
                    lastActionDescription = "Scénario complété avec succès."
                )

            } catch (e: Exception) {
                _state.value = ActiveExecutionState(
                    status = EngineStatus.STOPPED,
                    scenarioTitle = scenario.title,
                    errorMessage = e.message ?: "Interruption du scénario"
                )
            }
        }
    }

    private suspend fun executeSingleStep(
        scenario: ScenarioEntity,
        step: ActionStep,
        isDryRun: Boolean
    ): Boolean {
        // Resolve target coordinates
        var targetPoint = PointF(step.targetX, step.targetY)

        if (step.targetType == TargetType.TEXT_MATCH) {
            val detection = ScreenDetectionEngine.findTextOnScreen(step.targetText)
            if (detection.isFound && detection.point != null) {
                targetPoint = detection.point
                repository.log(
                    scenarioId = scenario.id,
                    scenarioTitle = scenario.title,
                    level = "DÉTECTION",
                    message = "Élément texte '${step.targetText}' trouvé à (${targetPoint.x.toInt()}, ${targetPoint.y.toInt()})"
                )
            } else if (!isDryRun) {
                repository.log(
                    scenarioId = scenario.id,
                    scenarioTitle = scenario.title,
                    level = "ALERTE",
                    message = "Texte cible '${step.targetText}' non trouvé, repli sur coordonnées de secours."
                )
            }
        } else if (step.targetType == TargetType.IMAGE_MATCH) {
            val detection = ScreenDetectionEngine.findImageOnScreen(
                templateIdOrBase64 = step.targetImageTemplate,
                similarityThreshold = step.imageSimilarityThreshold
            )
            if (detection.isFound && detection.point != null) {
                targetPoint = detection.point
                repository.log(
                    scenarioId = scenario.id,
                    scenarioTitle = scenario.title,
                    level = "DÉTECTION",
                    message = "Image '${step.targetImageName}' reconnue avec succès à (${targetPoint.x.toInt()}, ${targetPoint.y.toInt()}) [Similarité ${(detection.confidence * 100).toInt()}%]"
                )
            } else if (!isDryRun) {
                repository.log(
                    scenarioId = scenario.id,
                    scenarioTitle = scenario.title,
                    level = "ALERTE",
                    message = "Image '${step.targetImageName}' non détectée avec un seuil suffisant, repli sur coordonnées (${step.targetX.toInt()}, ${step.targetY.toInt()})"
                )
            }
        }

        // Apply humanization jitter to target point
        val humanizedPoint = Humanizer.randomizePoint(
            targetPoint.x,
            targetPoint.y,
            step.humanizeJitterRadius
        )

        _state.value = _state.value.copy(
            activeTargetPoint = humanizedPoint,
            lastActionDescription = "${step.actionType.label} à (${humanizedPoint.x.toInt()}, ${humanizedPoint.y.toInt()})"
        )

        if (isDryRun) {
            // Visual simulation: pause to show the reticle
            repository.log(
                scenarioId = scenario.id,
                scenarioTitle = scenario.title,
                level = "ACTION",
                message = "[TEST À BLANC] ${step.actionType.label} simulé à (${humanizedPoint.x.toInt()}, ${humanizedPoint.y.toInt()})"
            )
            delay(step.durationMs + 200L)
            _state.value = _state.value.copy(activeTargetPoint = null)
            return true
        }

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            repository.log(
                scenarioId = scenario.id,
                scenarioTitle = scenario.title,
                level = "ERREUR",
                message = "Service d'accessibilité inactif. Activez-le dans les paramètres pour les clics réels."
            )
            delay(step.durationMs)
            return false
        }

        val success = when (step.actionType) {
            ActionType.CLICK -> {
                val tapDuration = Humanizer.randomizeTapDuration(step.durationMs, step.humanizeTimingVariance)
                suspendCancellableCoroutine { continuation ->
                    service.performClick(humanizedPoint.x, humanizedPoint.y, tapDuration) { ok ->
                        if (continuation.isActive) continuation.resume(ok)
                    }
                }
            }
            ActionType.LONG_PRESS -> {
                val longPressDuration = 800L
                suspendCancellableCoroutine { continuation ->
                    service.performClick(humanizedPoint.x, humanizedPoint.y, longPressDuration) { ok ->
                        if (continuation.isActive) continuation.resume(ok)
                    }
                }
            }
            ActionType.SWIPE, ActionType.SCROLL_UP, ActionType.SCROLL_DOWN -> {
                val endX = if (step.actionType == ActionType.SCROLL_UP) humanizedPoint.x else if (step.actionType == ActionType.SCROLL_DOWN) humanizedPoint.x else step.swipeEndX
                val endY = if (step.actionType == ActionType.SCROLL_UP) humanizedPoint.y - 700f else if (step.actionType == ActionType.SCROLL_DOWN) humanizedPoint.y + 700f else step.swipeEndY
                suspendCancellableCoroutine { continuation ->
                    service.performSwipe(humanizedPoint.x, humanizedPoint.y, endX, endY, step.durationMs) { ok ->
                        if (continuation.isActive) continuation.resume(ok)
                    }
                }
            }
            ActionType.TEXT_INPUT -> {
                val typed = service.inputText(step.textToType)
                typed
            }
            ActionType.PINCH_ZOOM -> {
                suspendCancellableCoroutine { continuation ->
                    service.performPinch(humanizedPoint.x, humanizedPoint.y, zoomIn = true, durationMs = step.durationMs) { ok ->
                        if (continuation.isActive) continuation.resume(ok)
                    }
                }
            }
            ActionType.WAIT_DELAY -> {
                delay(step.durationMs)
                true
            }
            ActionType.BRANCH_IF_ELSE -> {
                // Handled in main loop, fallback just in case
                true
            }
            ActionType.JUMP_TO_STEP -> {
                true
            }
            ActionType.STOP_SCENARIO -> {
                true
            }
        }

        repository.log(
            scenarioId = scenario.id,
            scenarioTitle = scenario.title,
            level = if (success) "ACTION" else "ALERTE",
            message = "${step.actionType.label} ${if (success) "effectué" else "annulé/échec"} à (${humanizedPoint.x.toInt()}, ${humanizedPoint.y.toInt()})"
        )

        _state.value = _state.value.copy(activeTargetPoint = null)
        return success
    }

    fun pause() {
        isPaused = true
        _state.value = _state.value.copy(status = EngineStatus.PAUSED)
    }

    fun resume() {
        isPaused = false
        _state.value = _state.value.copy(status = EngineStatus.RUNNING)
    }

    /**
     * Instant emergency stop: stops execution, cancels jobs, triggers haptic feedback.
     */
    fun stopImmediately() {
        executionJob?.cancel()
        executionJob = null
        isPaused = false

        _state.value = _state.value.copy(
            status = EngineStatus.STOPPED,
            activeTargetPoint = null,
            lastActionDescription = "Arrêt d'urgence déclenché immédiatement."
        )

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(200L)
            }
        } catch (e: Exception) {
            // Non-fatal
        }
    }
}
