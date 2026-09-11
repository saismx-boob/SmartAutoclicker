package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Base64
import com.example.model.DetectionZone
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ImageTemplate(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: Int,
    val category: String
)

data class TemplateMatchResult(
    val isFound: Boolean,
    val point: PointF? = null,
    val bounds: Rect? = null,
    val confidence: Float = 0f,
    val templateId: String = "",
    val executionTimeMs: Long = 0L,
    val message: String = ""
)

object ImageRecognitionEngine {

    val BUILTIN_TEMPLATES = listOf(
        ImageTemplate(
            id = "ic_check",
            name = "Bouton Valider (✓)",
            description = "Coche de confirmation verte ou bleue pour valider une action",
            primaryColor = Color.parseColor("#00E676"),
            category = "Actions"
        ),
        ImageTemplate(
            id = "ic_close",
            name = "Bouton Fermer (✕)",
            description = "Croix de fermeture de publicité, pop-up ou modal",
            primaryColor = Color.parseColor("#FF1744"),
            category = "Navigation"
        ),
        ImageTemplate(
            id = "ic_play",
            name = "Bouton Lecture (▶)",
            description = "Triangle de lecture vidéo ou lancement de tâche",
            primaryColor = Color.parseColor("#00E5FF"),
            category = "Média"
        ),
        ImageTemplate(
            id = "ic_pause",
            name = "Bouton Pause (⏸)",
            description = "Deux barres verticales de mise en pause",
            primaryColor = Color.parseColor("#FFD600"),
            category = "Média"
        ),
        ImageTemplate(
            id = "ic_cart",
            name = "Panier d'Achat (🛒)",
            description = "Icône panier ou caddie pour e-commerce et achats",
            primaryColor = Color.parseColor("#FF9100"),
            category = "Commerce"
        ),
        ImageTemplate(
            id = "ic_heart",
            name = "J'aime / Like (❤️)",
            description = "Cœur pour réseaux sociaux (Instagram, TikTok, etc.)",
            primaryColor = Color.parseColor("#FF4081"),
            category = "Réseaux Sociaux"
        ),
        ImageTemplate(
            id = "ic_star",
            name = "Favori / Étoile (⭐)",
            description = "Étoile dorée pour notation ou mise en favoris",
            primaryColor = Color.parseColor("#FFD700"),
            category = "Évaluation"
        ),
        ImageTemplate(
            id = "ic_search",
            name = "Recherche / Loupe (🔍)",
            description = "Loupe de recherche dans les champs ou barres d'outils",
            primaryColor = Color.parseColor("#7C4DFF"),
            category = "Navigation"
        ),
        ImageTemplate(
            id = "ic_settings",
            name = "Paramètres (⚙)",
            description = "Roue crantée de configuration ou d'options",
            primaryColor = Color.parseColor("#90A4AE"),
            category = "Système"
        ),
        ImageTemplate(
            id = "ic_bell",
            name = "Notification (🔔)",
            description = "Cloche de rappel, alertes ou notifications",
            primaryColor = Color.parseColor("#FFAB00"),
            category = "Alertes"
        ),
        ImageTemplate(
            id = "ic_refresh",
            name = "Actualiser (🔄)",
            description = "Flèche circulaire de rafraîchissement de page",
            primaryColor = Color.parseColor("#00B0FF"),
            category = "Navigation"
        ),
        ImageTemplate(
            id = "ic_arrow_back",
            name = "Retour (⬅)",
            description = "Flèche vers la gauche pour retour en arrière",
            primaryColor = Color.parseColor("#CFD8DC"),
            category = "Navigation"
        )
    )

    private val _customTemplates = mutableListOf<ImageTemplate>()
    val customTemplates: List<ImageTemplate> get() = _customTemplates

    fun getAllTemplates(): List<ImageTemplate> = _customTemplates + BUILTIN_TEMPLATES

    fun registerCustomTemplate(name: String, bitmap: Bitmap): ImageTemplate {
        val id = "custom_" + System.currentTimeMillis()
        val template = ImageTemplate(
            id = id,
            name = name,
            description = "Modèle capturé en direct (${bitmap.width}x${bitmap.height}px)",
            primaryColor = Color.parseColor("#00E5FF"),
            category = "Capturés"
        )
        templateCache[id] = bitmap
        _customTemplates.add(0, template)
        return template
    }

    private val templateCache = mutableMapOf<String, Bitmap>()

    /**
     * Generates or retrieves a clean bitmap representation for a template.
     */
    fun getTemplateBitmap(templateId: String, size: Int = 64): Bitmap {
        templateCache[templateId]?.let { return it }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (templateId) {
            "ic_check" -> {
                // Circular background with checkmark
                paint.color = Color.parseColor("#00E676")
                canvas.drawCircle(size / 2f, size / 2f, size * 0.44f, paint)

                paint.color = Color.WHITE
                paint.strokeWidth = size * 0.12f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND

                val path = Path().apply {
                    moveTo(size * 0.28f, size * 0.50f)
                    lineTo(size * 0.44f, size * 0.68f)
                    lineTo(size * 0.74f, size * 0.34f)
                }
                canvas.drawPath(path, paint)
            }
            "ic_close" -> {
                paint.color = Color.parseColor("#FF1744")
                canvas.drawCircle(size / 2f, size / 2f, size * 0.44f, paint)

                paint.color = Color.WHITE
                paint.strokeWidth = size * 0.12f
                paint.style = Paint.Style.STROKE
                paint.strokeCap = Paint.Cap.ROUND

                canvas.drawLine(size * 0.32f, size * 0.32f, size * 0.68f, size * 0.68f, paint)
                canvas.drawLine(size * 0.68f, size * 0.32f, size * 0.32f, size * 0.68f, paint)
            }
            "ic_play" -> {
                paint.color = Color.parseColor("#00E5FF")
                canvas.drawCircle(size / 2f, size / 2f, size * 0.44f, paint)

                paint.color = Color.parseColor("#0A0E17")
                paint.style = Paint.Style.FILL

                val path = Path().apply {
                    moveTo(size * 0.40f, size * 0.30f)
                    lineTo(size * 0.70f, size * 0.50f)
                    lineTo(size * 0.40f, size * 0.70f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "ic_pause" -> {
                paint.color = Color.parseColor("#FFD600")
                canvas.drawCircle(size / 2f, size / 2f, size * 0.44f, paint)

                paint.color = Color.parseColor("#0A0E17")
                paint.style = Paint.Style.FILL

                val barWidth = size * 0.10f
                val barHeight = size * 0.36f
                canvas.drawRoundRect(
                    RectF(size * 0.36f - barWidth / 2, size * 0.32f, size * 0.36f + barWidth / 2, size * 0.32f + barHeight),
                    4f, 4f, paint
                )
                canvas.drawRoundRect(
                    RectF(size * 0.64f - barWidth / 2, size * 0.32f, size * 0.64f + barWidth / 2, size * 0.32f + barHeight),
                    4f, 4f, paint
                )
            }
            "ic_heart" -> {
                paint.color = Color.parseColor("#FF4081")
                paint.style = Paint.Style.FILL

                val path = Path().apply {
                    moveTo(size * 0.5f, size * 0.75f)
                    cubicTo(size * 0.15f, size * 0.5f, size * 0.15f, size * 0.25f, size * 0.35f, size * 0.25f)
                    cubicTo(size * 0.45f, size * 0.25f, size * 0.5f, size * 0.35f, size * 0.5f, size * 0.35f)
                    cubicTo(size * 0.5f, size * 0.35f, size * 0.55f, size * 0.25f, size * 0.65f, size * 0.25f)
                    cubicTo(size * 0.85f, size * 0.25f, size * 0.85f, size * 0.5f, size * 0.5f, size * 0.75f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            "ic_star" -> {
                paint.color = Color.parseColor("#FFD700")
                paint.style = Paint.Style.FILL

                val path = Path()
                val cx = size / 2f
                val cy = size / 2f
                val outerRadius = size * 0.40f
                val innerRadius = outerRadius * 0.45f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerRadius else innerRadius
                    val angle = (i * 36 - 90) * Math.PI / 180.0
                    val x = cx + (r * Math.cos(angle)).toFloat()
                    val y = cy + (r * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            else -> {
                // Generic circular icon
                paint.color = Color.parseColor("#00E5FF")
                canvas.drawCircle(size / 2f, size / 2f, size * 0.40f, paint)
                paint.color = Color.WHITE
                paint.textSize = size * 0.45f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("⚙", size / 2f, size * 0.62f, paint)
            }
        }

        templateCache[templateId] = bitmap
        return bitmap
    }

    /**
     * Decodes a base64-encoded string into a Bitmap, if provided by the user.
     */
    fun decodeBase64Bitmap(base64Str: String): Bitmap? {
        return try {
            val cleanBase64 = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fast Normalized Template Matching algorithm.
     * Computes similarity between the template and candidate sliding windows.
     * Features coarse-to-fine multi-stride matching for maximum performance.
     */
    fun matchTemplate(
        source: Bitmap,
        template: Bitmap,
        minSimilarity: Float = 0.70f,
        roi: Rect? = null
    ): TemplateMatchResult {
        val startTime = System.currentTimeMillis()

        val searchRect = roi ?: Rect(0, 0, source.width, source.height)
        val tW = template.width
        val tH = template.height

        if (tW > searchRect.width() || tH > searchRect.height()) {
            return TemplateMatchResult(
                isFound = false,
                message = "Modèle plus grand que la zone de recherche",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Extract template pixel colors & luminance
        val templatePixels = IntArray(tW * tH)
        template.getPixels(templatePixels, 0, tW, 0, 0, tW, tH)

        // Precompute template variance/norm (skip transparent pixels)
        var templateNorm = 0.0
        var validTemplatePixels = 0
        val tLum = DoubleArray(templatePixels.size)

        for (i in templatePixels.indices) {
            val color = templatePixels[i]
            val alpha = (color ushr 24) and 0xFF
            if (alpha > 50) {
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                tLum[i] = lum
                templateNorm += lum * lum
                validTemplatePixels++
            } else {
                tLum[i] = -1.0
            }
        }

        if (validTemplatePixels == 0) {
            return TemplateMatchResult(false, message = "Modèle vide ou entièrement transparent.")
        }
        templateNorm = sqrt(templateNorm)

        var bestScore = 0f
        var bestX = -1
        var bestY = -1

        val maxX = searchRect.right - tW
        val maxY = searchRect.bottom - tH

        // Coarse pass: stride = 3 for high performance
        val stride = 3
        var y = searchRect.top
        while (y <= maxY) {
            var x = searchRect.left
            while (x <= maxX) {
                // Calculate correlation at (x, y)
                var dotProduct = 0.0
                var windowNorm = 0.0
                var sampleCount = 0

                // Sub-sample template for speed (every 2nd pixel)
                var py = 0
                while (py < tH) {
                    val pBase = py * tW
                    var px = 0
                    while (px < tW) {
                        val lumT = tLum[pBase + px]
                        if (lumT >= 0) {
                            val srcColor = source.getPixel(x + px, y + py)
                            val r = (srcColor ushr 16) and 0xFF
                            val g = (srcColor ushr 8) and 0xFF
                            val b = srcColor and 0xFF
                            val lumS = 0.299 * r + 0.587 * g + 0.114 * b

                            dotProduct += lumT * lumS
                            windowNorm += lumS * lumS
                            sampleCount++
                        }
                        px += 2
                    }
                    py += 2
                }

                if (windowNorm > 0 && templateNorm > 0) {
                    val score = (dotProduct / (sqrt(windowNorm) * (templateNorm * (sampleCount.toDouble() / validTemplatePixels)))).toFloat()
                    val normalizedScore = min(1f, max(0f, score))

                    if (normalizedScore > bestScore) {
                        bestScore = normalizedScore
                        bestX = x
                        bestY = y
                    }
                }

                x += stride
            }
            y += stride
        }

        // Fine pass: refine 4 pixels around candidate
        if (bestX >= 0 && bestY >= 0 && bestScore >= minSimilarity * 0.85f) {
            val fineMinX = max(searchRect.left, bestX - stride)
            val fineMaxX = min(maxX, bestX + stride)
            val fineMinY = max(searchRect.top, bestY - stride)
            val fineMaxY = min(maxY, bestY + stride)

            for (fy in fineMinY..fineMaxY) {
                for (fx in fineMinX..fineMaxX) {
                    var dotProduct = 0.0
                    var windowNorm = 0.0
                    for (py in 0 until tH) {
                        val pBase = py * tW
                        for (px in 0 until tW) {
                            val lumT = tLum[pBase + px]
                            if (lumT >= 0) {
                                val srcColor = source.getPixel(fx + px, fy + py)
                                val r = (srcColor ushr 16) and 0xFF
                                val g = (srcColor ushr 8) and 0xFF
                                val b = srcColor and 0xFF
                                val lumS = 0.299 * r + 0.587 * g + 0.114 * b

                                dotProduct += lumT * lumS
                                windowNorm += lumS * lumS
                            }
                        }
                    }

                    if (windowNorm > 0) {
                        val score = (dotProduct / (sqrt(windowNorm) * templateNorm)).toFloat()
                        val normalizedScore = min(1f, max(0f, score))
                        if (normalizedScore > bestScore) {
                            bestScore = normalizedScore
                            bestX = fx
                            bestY = fy
                        }
                    }
                }
            }
        }

        val execTime = System.currentTimeMillis() - startTime
        val isFound = bestScore >= minSimilarity && bestX >= 0 && bestY >= 0

        val centerX = if (isFound) bestX + (tW / 2f) else null
        val centerY = if (isFound) bestY + (tH / 2f) else null
        val bounds = if (isFound) Rect(bestX, bestY, bestX + tW, bestY + tH) else null

        return TemplateMatchResult(
            isFound = isFound,
            point = if (centerX != null && centerY != null) PointF(centerX, centerY) else null,
            bounds = bounds,
            confidence = bestScore,
            executionTimeMs = execTime,
            message = if (isFound) {
                "Image reconnue avec succès à (${centerX?.toInt()}, ${centerY?.toInt()}) [Confiance ${(bestScore * 100).toInt()}% en ${execTime}ms]"
            } else {
                "Image non trouvée (Score max ${(bestScore * 100).toInt()}% < seuil ${(minSimilarity * 100).toInt()}%) en ${execTime}ms"
            }
        )
    }

    /**
     * Synthesizes a simulated screen bitmap containing test elements,
     * including the selected template at target coordinates for live testing and preview.
     */
    fun createSimulatedScreen(
        activeTemplateId: String = "ic_check",
        templateX: Float = 540f,
        templateY: Float = 1350f,
        width: Int = 1080,
        height: Int = 2400
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark background
        canvas.drawColor(Color.parseColor("#0A0E17"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Status & Header bar
        paint.color = Color.parseColor("#151D2E")
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, paint)

        paint.color = Color.WHITE
        paint.textSize = 42f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Application Cible Simulée", width / 2f, 110f, paint)

        // Center card banner
        paint.color = Color.parseColor("#1B273F")
        canvas.drawRoundRect(RectF(80f, 400f, width - 80f, 1000f), 32f, 32f, paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.textSize = 48f
        canvas.drawText("Récompense Disponible !", width / 2f, 540f, paint)

        paint.color = Color.parseColor("#9EAFD0")
        paint.textSize = 34f
        canvas.drawText("Touchez le bouton ci-dessous pour valider", width / 2f, 640f, paint)

        // Draw the target template image at (templateX, templateY)
        val templateBitmap = getTemplateBitmap(activeTemplateId, 128)
        val drawX = templateX - (templateBitmap.width / 2f)
        val drawY = templateY - (templateBitmap.height / 2f)
        canvas.drawBitmap(templateBitmap, drawX, drawY, null)

        // Draw a decorative button background around template
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(
            RectF(drawX - 30f, drawY - 20f, drawX + templateBitmap.width + 30f, drawY + templateBitmap.height + 20f),
            24f, 24f, strokePaint
        )

        return bitmap
    }

    /**
     * Primary entry point for ScreenDetectionEngine & ScenarioExecutor to locate an image/icon target.
     */
    fun findImageOnScreen(
        templateIdOrBase64: String,
        similarityThreshold: Float = 0.75f,
        zones: List<DetectionZone> = emptyList(),
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): TemplateMatchResult {
        // Resolve template bitmap
        val template = if (templateIdOrBase64.startsWith("data:image") || templateIdOrBase64.length > 100) {
            decodeBase64Bitmap(templateIdOrBase64) ?: getTemplateBitmap("ic_check", 64)
        } else {
            getTemplateBitmap(templateIdOrBase64, 64)
        }

        // Determine ROI from active zones if specified
        var roiRect: Rect? = null
        val activeZones = zones.filter { it.isActive }
        if (activeZones.isNotEmpty()) {
            val minTop = (activeZones.minOf { it.topPct } * screenHeight).toInt()
            val maxBottom = (activeZones.maxOf { it.bottomPct } * screenHeight).toInt()
            roiRect = Rect(0, minTop, screenWidth, maxBottom)
        }

        // Real-time Screen Capture (MediaProjection) vs Simulated Screen
        val realScreen = if (ScreenCaptureManager.isCapturing.value) {
            ScreenCaptureManager.captureCurrentScreen()
        } else {
            null
        }

        val screen = realScreen ?: createSimulatedScreen(
            activeTemplateId = if (templateIdOrBase64.startsWith("ic_")) templateIdOrBase64 else "ic_check",
            templateX = 540f,
            templateY = 1350f,
            width = screenWidth,
            height = screenHeight
        )

        val result = matchTemplate(
            source = screen,
            template = template,
            minSimilarity = similarityThreshold,
            roi = roiRect
        )

        return if (realScreen != null) {
            result.copy(
                message = if (result.isFound) {
                    "Vision IA (MediaProjection): Cible trouvée à l'écran à (${result.point?.x?.toInt()}, ${result.point?.y?.toInt()}) [Similarité ${(result.confidence * 100).toInt()}% en ${result.executionTimeMs}ms]"
                } else {
                    "Vision IA (MediaProjection): Cible non trouvée à l'écran (Score max ${(result.confidence * 100).toInt()}%) en ${result.executionTimeMs}ms"
                }
            )
        } else {
            result
        }
    }
}
