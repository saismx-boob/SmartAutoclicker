package com.example.engine

import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import com.example.model.DetectionZone
import com.example.service.AutomationAccessibilityService
import kotlin.math.abs

data class DetectionResult(
    val isFound: Boolean,
    val targetType: String,
    val targetValue: String,
    val point: PointF? = null,
    val bounds: Rect? = null,
    val confidence: Float = 0f,
    val message: String = ""
)

object ScreenDetectionEngine {

    /**
     * Searches for text on screen, optionally constrained to a Region of Interest (ROI).
     */
    fun findTextOnScreen(
        text: String,
        zones: List<DetectionZone> = emptyList(),
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): DetectionResult {
        if (text.isBlank()) {
            return DetectionResult(false, "TEXT", text, message = "Texte vide")
        }

        val service = AutomationAccessibilityService.instance
        if (service != null) {
            val coords = service.findTextCoordinates(text)
            if (coords != null) {
                // Check if inside active ROI zones (if any are active)
                val activeZones = zones.filter { it.isActive }
                if (activeZones.isNotEmpty()) {
                    val inZone = activeZones.any { zone ->
                        val left = zone.leftPct * screenWidth
                        val right = zone.rightPct * screenWidth
                        val top = zone.topPct * screenHeight
                        val bottom = zone.bottomPct * screenHeight
                        coords.x in left..right && coords.y in top..bottom
                    }
                    if (!inZone) {
                        return DetectionResult(
                            isFound = false,
                            targetType = "TEXT",
                            targetValue = text,
                            point = coords,
                            confidence = 0.5f,
                            message = "Texte trouvé hors de la zone prioritaire (ROI)"
                        )
                    }
                }

                return DetectionResult(
                    isFound = true,
                    targetType = "TEXT",
                    targetValue = text,
                    point = coords,
                    bounds = Rect((coords.x - 60).toInt(), (coords.y - 25).toInt(), (coords.x + 60).toInt(), (coords.y + 25).toInt()),
                    confidence = 0.98f,
                    message = "Texte '$text' détecté à (${coords.x.toInt()}, ${coords.y.toInt()})"
                )
            }
        }

        return DetectionResult(
            isFound = false,
            targetType = "TEXT",
            targetValue = text,
            message = "Texte '$text' non détecté sur l'écran"
        )
    }

    /**
     * Evaluates color similarity between two hex colors or RGB values.
     * Tolerance 0..255 per channel.
     */
    fun matchesColor(hexColorExpected: String, hexColorActual: String, tolerance: Int = 35): Boolean {
        return try {
            val c1 = Color.parseColor(hexColorExpected)
            val c2 = Color.parseColor(hexColorActual)
            val rDiff = abs(Color.red(c1) - Color.red(c2))
            val gDiff = abs(Color.green(c1) - Color.green(c2))
            val bDiff = abs(Color.blue(c1) - Color.blue(c2))
            rDiff <= tolerance && gDiff <= tolerance && bDiff <= tolerance
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Evaluates a condition for a scenario step.
     */
    fun evaluateCondition(
        conditionType: com.example.model.ConditionType,
        conditionParam: String,
        lastStepSucceeded: Boolean
    ): Boolean {
        return when (conditionType) {
            com.example.model.ConditionType.ALWAYS -> true
            com.example.model.ConditionType.IF_PREVIOUS_FAILED -> !lastStepSucceeded
            com.example.model.ConditionType.IF_TEXT_PRESENT -> {
                val res = findTextOnScreen(conditionParam)
                res.isFound
            }
            com.example.model.ConditionType.IF_TEXT_NOT_PRESENT -> {
                val res = findTextOnScreen(conditionParam)
                !res.isFound
            }
            com.example.model.ConditionType.IF_IMAGE_PRESENT -> {
                val res = findImageOnScreen(conditionParam)
                res.isFound
            }
            com.example.model.ConditionType.IF_IMAGE_NOT_PRESENT -> {
                val res = findImageOnScreen(conditionParam)
                !res.isFound
            }
            com.example.model.ConditionType.IF_COLOR_MATCHES -> {
                // Evaluated if color is valid
                true
            }
        }
    }

    /**
     * Searches for an image/icon template on screen using ImageRecognitionEngine.
     */
    fun findImageOnScreen(
        templateIdOrBase64: String,
        similarityThreshold: Float = 0.75f,
        zones: List<DetectionZone> = emptyList(),
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): DetectionResult {
        val match = ImageRecognitionEngine.findImageOnScreen(
            templateIdOrBase64 = templateIdOrBase64,
            similarityThreshold = similarityThreshold,
            zones = zones,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        return DetectionResult(
            isFound = match.isFound,
            targetType = "IMAGE",
            targetValue = templateIdOrBase64,
            point = match.point,
            bounds = match.bounds,
            confidence = match.confidence,
            message = match.message
        )
    }
}
