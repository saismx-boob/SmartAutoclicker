package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class ActionType(val label: String, val iconName: String) {
    CLICK("Clic (Tap)", "touch_app"),
    LONG_PRESS("Appui Long", "pan_tool"),
    SWIPE("Glisser (Swipe)", "swipe"),
    SCROLL_UP("Défiler Haut", "arrow_upward"),
    SCROLL_DOWN("Défiler Bas", "arrow_downward"),
    TEXT_INPUT("Écrire Texte", "keyboard"),
    PINCH_ZOOM("Zoom Pincement", "pinch"),
    WAIT_DELAY("Pause / Attente", "hourglass_empty"),
    BRANCH_IF_ELSE("Si / Alors / Sinon (Branche)", "alt_route"),
    JUMP_TO_STEP("Sauter à l'Étape", "redo"),
    STOP_SCENARIO("Arrêter le Scénario", "stop_circle")
}

enum class TargetType(val label: String) {
    COORDINATES("Coordonnées (X, Y)"),
    TEXT_MATCH("Texte à l'écran (OCR)"),
    IMAGE_MATCH("Reconnaissance d'Image / Icône"),
    COLOR_MATCH("Couleur de pixel"),
    SCREEN_CHANGE("Changement visuel")
}

enum class ConditionType(val label: String) {
    ALWAYS("Toujours exécuter"),
    IF_TEXT_PRESENT("Si le texte apparaît"),
    IF_TEXT_NOT_PRESENT("Si le texte disparaît"),
    IF_IMAGE_PRESENT("Si l'image / icône apparaît"),
    IF_IMAGE_NOT_PRESENT("Si l'image / icône disparaît"),
    IF_COLOR_MATCHES("Si la couleur correspond"),
    IF_PREVIOUS_FAILED("Si l'étape précédente a échoué")
}

@JsonClass(generateAdapter = true)
data class ActionStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val stepNumber: Int = 1,
    val name: String = "Étape",
    val actionType: ActionType = ActionType.CLICK,
    val targetType: TargetType = TargetType.COORDINATES,
    val targetX: Float = 540f,
    val targetY: Float = 1200f,
    val targetText: String = "",
    val targetColorHex: String = "#00E5FF",
    val targetImageTemplate: String = "ic_check",
    val targetImageName: String = "Bouton Valider (✓)",
    val imageSimilarityThreshold: Float = 0.75f,
    val textToType: String = "",
    val swipeEndX: Float = 540f,
    val swipeEndY: Float = 600f,
    val durationMs: Long = 100L,
    val delayBeforeMs: Long = 300L,
    val conditionType: ConditionType = ConditionType.ALWAYS,
    val conditionParam: String = "",
    val thenActionType: ActionType = ActionType.CLICK,
    val thenStepJump: Int = 0,
    val thenDurationMs: Long = 200L,
    val thenTextToType: String = "",
    val elseActionType: ActionType = ActionType.WAIT_DELAY,
    val elseTargetX: Float = 540f,
    val elseTargetY: Float = 1400f,
    val elseSwipeEndX: Float = 540f,
    val elseSwipeEndY: Float = 600f,
    val elseDurationMs: Long = 200L,
    val elseDelayBeforeMs: Long = 200L,
    val elseTextToType: String = "",
    val elseStepJump: Int = 0,
    val humanizeJitterRadius: Int = 14,
    val humanizeTimingVariance: Int = 18,
    val isEnabled: Boolean = true
)

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val stepsJson: String,
    val loopCount: Int = 1, // 0 = infini
    val intervalBetweenLoopsMs: Long = 1000L,
    val isEnabled: Boolean = true,
    val isAiControlled: Boolean = false,
    val aiGoalPrompt: String = "",
    val scheduleDescription: String = "Manuel",
    val totalExecutions: Int = 0,
    val successExecutions: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0L
)

enum class LogLevel(val tag: String) {
    INFO("INFO"),
    ACTION("ACTION"),
    DETECTION("DÉTECTION"),
    AI_THOUGHT("IA"),
    SUCCESS("SUCCÈS"),
    WARNING("ALERTE"),
    ERROR("ERREUR")
}

@Entity(tableName = "execution_logs")
data class ExecutionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scenarioId: Long,
    val scenarioTitle: String,
    val level: String,
    val message: String,
    val details: String = ""
)

@Entity(tableName = "ai_memories")
data class AiMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioTitle: String,
    val goal: String,
    val observedState: String,
    val actionDecision: String,
    val outcome: String, // "SUCCESS" or "FAILURE"
    val learnedRule: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DetectionZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val leftPct: Float = 0.1f,
    val topPct: Float = 0.2f,
    val rightPct: Float = 0.9f,
    val bottomPct: Float = 0.8f,
    val priority: String = "Haute",
    val isActive: Boolean = true
)
