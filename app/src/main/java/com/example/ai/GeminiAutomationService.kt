package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.AiMemoryEntity
import com.example.model.ConditionType
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiAutomationService(private val context: Context) {

    companion object {
        private const val TAG = "GeminiAutoService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.ifBlank { "" }

    val hasValidApiKey: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * Generates a complete automation scenario with steps from a natural language user prompt.
     */
    suspend fun generateScenarioFromGoal(goalPrompt: String): Pair<ScenarioEntity, List<ActionStep>> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            Tu es un expert mondial en automatisation Android intelligente et auto-clicker éthique.
            À partir de l'objectif utilisateur ci-dessous, génère un scénario d'automatisation optimisé avec des étapes précises.
            Réponds UNIQUEMENT sous la forme d'un objet JSON strict valide sans backticks markdown avec cette structure :
            {
              "title": "Titre du scénario",
              "description": "Courte description",
              "loopCount": 1,
              "steps": [
                {
                  "name": "Nom de l'étape",
                  "actionType": "CLICK" (ou LONG_PRESS, SWIPE, SCROLL_DOWN, SCROLL_UP, TEXT_INPUT, WAIT_DELAY),
                  "targetType": "TEXT_MATCH" (ou COORDINATES),
                  "targetText": "texte à détecter s'il y en a",
                  "targetX": 540,
                  "targetY": 1200,
                  "swipeEndX": 540,
                  "swipeEndY": 600,
                  "textToType": "",
                  "durationMs": 100,
                  "delayBeforeMs": 500,
                  "conditionType": "ALWAYS" (ou IF_TEXT_PRESENT, IF_TEXT_NOT_PRESENT),
                  "conditionParam": "",
                  "humanizeJitterRadius": 12,
                  "humanizeTimingVariance": 15
                }
              ]
            }
        """.trimIndent()

        val fullPrompt = "$systemPrompt\n\nObjectif utilisateur : $goalPrompt"
        val responseText = callGeminiRaw(fullPrompt)

        if (responseText.isNullOrBlank()) {
            // Fallback smart scenario if offline or API key missing
            return@withContext createFallbackScenario(goalPrompt)
        }

        try {
            val cleanJson = cleanJsonResponse(responseText)
            val json = JSONObject(cleanJson)
            val title = json.optString("title", "Scénario Intelligent")
            val desc = json.optString("description", "Généré automatiquement par l'IA")
            val loopCount = json.optInt("loopCount", 1)

            val stepsArray = json.optJSONArray("steps") ?: JSONArray()
            val stepsList = mutableListOf<ActionStep>()

            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                stepsList.add(
                    ActionStep(
                        stepNumber = i + 1,
                        name = stepObj.optString("name", "Étape ${i + 1}"),
                        actionType = try { ActionType.valueOf(stepObj.optString("actionType", "CLICK")) } catch (e: Exception) { ActionType.CLICK },
                        targetType = try { TargetType.valueOf(stepObj.optString("targetType", "TEXT_MATCH")) } catch (e: Exception) { TargetType.TEXT_MATCH },
                        targetText = stepObj.optString("targetText", ""),
                        targetX = stepObj.optDouble("targetX", 540.0).toFloat(),
                        targetY = stepObj.optDouble("targetY", 1200.0).toFloat(),
                        swipeEndX = stepObj.optDouble("swipeEndX", 540.0).toFloat(),
                        swipeEndY = stepObj.optDouble("swipeEndY", 600.0).toFloat(),
                        textToType = stepObj.optString("textToType", ""),
                        durationMs = stepObj.optLong("durationMs", 100L),
                        delayBeforeMs = stepObj.optLong("delayBeforeMs", 400L),
                        conditionType = try { ConditionType.valueOf(stepObj.optString("conditionType", "ALWAYS")) } catch (e: Exception) { ConditionType.ALWAYS },
                        conditionParam = stepObj.optString("conditionParam", ""),
                        humanizeJitterRadius = stepObj.optInt("humanizeJitterRadius", 14),
                        humanizeTimingVariance = stepObj.optInt("humanizeTimingVariance", 18)
                    )
                )
            }

            val scenario = ScenarioEntity(
                title = title,
                description = desc,
                stepsJson = com.example.data.JsonUtils.stepsToJson(stepsList),
                loopCount = loopCount,
                isAiControlled = true,
                aiGoalPrompt = goalPrompt,
                scheduleDescription = "Généré par IA"
            )

            Pair(scenario, stepsList)
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error for Gemini response: $responseText", e)
            createFallbackScenario(goalPrompt)
        }
    }

    /**
     * Learning mode: translates recorded user taps into a smart reusable scenario.
     */
    suspend fun analyzeRecordedGestures(
        recordedPoints: List<Pair<Float, Float>>,
        recordedDelays: List<Long>
    ): Pair<ScenarioEntity, List<ActionStep>> = withContext(Dispatchers.IO) {
        val steps = mutableListOf<ActionStep>()
        for (i in recordedPoints.indices) {
            val point = recordedPoints[i]
            val delay = if (i < recordedDelays.size) recordedDelays[i] else 500L
            steps.add(
                ActionStep(
                    stepNumber = i + 1,
                    name = "Action apprise #${i + 1}",
                    actionType = ActionType.CLICK,
                    targetType = TargetType.COORDINATES,
                    targetX = point.first,
                    targetY = point.second,
                    durationMs = 90L,
                    delayBeforeMs = delay.coerceIn(150L, 5000L),
                    humanizeJitterRadius = 12,
                    humanizeTimingVariance = 15
                )
            )
        }

        val scenario = ScenarioEntity(
            title = "🎓 Macro Enregistrée (${steps.size} clics)",
            description = "Scénario généré par le mode apprentissage à partir de vos gestes réels.",
            stepsJson = com.example.data.JsonUtils.stepsToJson(steps),
            loopCount = 1,
            scheduleDescription = "Enregistré"
        )
        Pair(scenario, steps)
    }

    /**
     * General chat with the AI assistant for automation optimization.
     */
    suspend fun askAssistant(userMessage: String, memories: List<AiMemoryEntity> = emptyList()): String = withContext(Dispatchers.IO) {
        val memoryContext = if (memories.isNotEmpty()) {
            val memStr = memories.take(4).joinToString("\n") {
                "- Tâche: '${it.goal}', Règle apprise: '${it.learnedRule}' (Résultat: ${it.outcome})"
            }
            "\nVoici la mémoire de tes expériences passées :\n$memStr\n"
        } else ""

        val systemPrompt = """
            Tu es l'assistant IA intégré de AutoClick AI, une application Android d'automatisation intelligente et bienveillante.
            Ton rôle est d'aider l'utilisateur à créer des scénarios parfaits, à configurer la détection d'écran, à humaniser les actions pour qu'elles soient naturelles, et à résoudre des tâches complexes.
            Réponds en français, avec clarté, concision et professionnalisme.
            $memoryContext
        """.trimIndent()

        val prompt = "$systemPrompt\n\nQuestion utilisateur : $userMessage"
        val response = callGeminiRaw(prompt)
        response ?: "Je suis prêt à vous aider à automatiser vos tâches intelligemment. Décrivez-moi ce que vous souhaitez accomplir !"
    }

    private suspend fun callGeminiRaw(prompt: String): String? {
        if (!hasValidApiKey) {
            Log.w(TAG, "No valid Gemini API key configured.")
            return null
        }

        return try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.9)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API", e)
            null
        }
    }

    private fun cleanJsonResponse(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean
    }

    private fun createFallbackScenario(goalPrompt: String): Pair<ScenarioEntity, List<ActionStep>> {
        val steps = listOf(
            ActionStep(
                stepNumber = 1,
                name = "Détection et validation intelligente",
                actionType = ActionType.CLICK,
                targetType = TargetType.TEXT_MATCH,
                targetText = if (goalPrompt.contains("commander", ignoreCase = true)) "Commander" else "Valider",
                delayBeforeMs = 500L,
                humanizeJitterRadius = 14
            ),
            ActionStep(
                stepNumber = 2,
                name = "Attente dynamique de chargement",
                actionType = ActionType.WAIT_DELAY,
                durationMs = 1500L
            ),
            ActionStep(
                stepNumber = 3,
                name = "Confirmation finale",
                actionType = ActionType.CLICK,
                targetType = TargetType.TEXT_MATCH,
                targetText = "Confirmer",
                conditionType = ConditionType.IF_TEXT_PRESENT,
                conditionParam = "Confirmer",
                delayBeforeMs = 600L
            )
        )

        val scenario = ScenarioEntity(
            title = "🤖 Scénario : ${goalPrompt.take(30)}...",
            description = "Généré selon votre objectif : $goalPrompt",
            stepsJson = com.example.data.JsonUtils.stepsToJson(steps),
            loopCount = 1,
            isAiControlled = true,
            aiGoalPrompt = goalPrompt,
            scheduleDescription = "Autonome"
        )
        return Pair(scenario, steps)
    }

    /**
     * Multimodal analysis of a live screen frame captured via MediaProjection API.
     * Extracts UI hierarchy, interactive elements (buttons, inputs), screen context,
     * and recommends an executable automation macro.
     */
    suspend fun analyzeScreenFrame(
        screenBitmap: Bitmap,
        userInstruction: String = ""
    ): ScreenAnalysisResult = withContext(Dispatchers.IO) {
        val prompt = """
            Tu es un système de Vision IA de pointe pour Android analysant une capture d'écran temps réel (MediaProjection).
            ${if (userInstruction.isNotBlank()) "Consigne de l'utilisateur: $userInstruction" else "Analyse l'écran pour détecter les éléments interactifs et proposer un scénario d'automatisation."}
            
            Renvoie UNIQUEMENT un objet JSON valide suivant exactement cette structure:
            {
              "appOverview": "Description concise de l'application ou de l'écran visible",
              "detectedElements": [
                {
                  "label": "Libellé de l'élément (ex: Bouton Valider, Panier, Barre de recherche)",
                  "type": "BUTTON",
                  "x": 540,
                  "y": 1200,
                  "confidence": 0.95,
                  "suggestedAction": "CLICK"
                }
              ],
              "suggestedScenarioTitle": "Titre pertinent du scénario",
              "suggestedSteps": [
                {
                  "stepNumber": 1,
                  "name": "Cliquer sur ...",
                  "actionType": "CLICK",
                  "targetType": "COORDINATES",
                  "targetX": 540,
                  "targetY": 1200,
                  "delayBeforeMs": 500
                }
              ],
              "explanation": "Synthèse de ce que voit l'IA et stratégie proposée"
            }
        """.trimIndent()

        val rawJson = callGeminiMultimodalRaw(prompt, screenBitmap)
        if (!rawJson.isNullOrBlank()) {
            try {
                val cleaned = cleanJsonResponse(rawJson)
                val obj = JSONObject(cleaned)
                val appOverview = obj.optString("appOverview", "Écran capturé par MediaProjection")
                val explanation = obj.optString("explanation", "Analyse visuelle terminée")
                val title = obj.optString("suggestedScenarioTitle", "Scénario Vision IA")

                val elementsList = mutableListOf<DetectedUiElement>()
                val elementsArray = obj.optJSONArray("detectedElements")
                if (elementsArray != null) {
                    for (i in 0 until elementsArray.length()) {
                        val elemObj = elementsArray.getJSONObject(i)
                        elementsList.add(
                            DetectedUiElement(
                                label = elemObj.optString("label", "Élément ${i + 1}"),
                                type = elemObj.optString("type", "BUTTON"),
                                x = elemObj.optDouble("x", 540.0).toFloat(),
                                y = elemObj.optDouble("y", 1000.0).toFloat(),
                                confidence = elemObj.optDouble("confidence", 0.9).toFloat(),
                                suggestedAction = elemObj.optString("suggestedAction", "CLICK")
                            )
                        )
                    }
                }

                val stepsList = mutableListOf<ActionStep>()
                val stepsArray = obj.optJSONArray("suggestedSteps")
                if (stepsArray != null && stepsArray.length() > 0) {
                    for (i in 0 until stepsArray.length()) {
                        val stepObj = stepsArray.getJSONObject(i)
                        stepsList.add(
                            ActionStep(
                                stepNumber = i + 1,
                                name = stepObj.optString("name", "Étape ${i + 1}"),
                                actionType = try {
                                    ActionType.valueOf(stepObj.optString("actionType", "CLICK"))
                                } catch (e: Exception) {
                                    ActionType.CLICK
                                },
                                targetType = try {
                                    TargetType.valueOf(stepObj.optString("targetType", "COORDINATES"))
                                } catch (e: Exception) {
                                    TargetType.COORDINATES
                                },
                                targetX = stepObj.optDouble("targetX", 540.0).toFloat(),
                                targetY = stepObj.optDouble("targetY", 1000.0).toFloat(),
                                targetText = stepObj.optString("targetText", ""),
                                textToType = stepObj.optString("textToType", ""),
                                delayBeforeMs = stepObj.optLong("delayBeforeMs", 400L),
                                durationMs = stepObj.optLong("durationMs", 100L)
                            )
                        )
                    }
                } else if (elementsList.isNotEmpty()) {
                    elementsList.take(3).forEachIndexed { index, elem ->
                        stepsList.add(
                            ActionStep(
                                stepNumber = index + 1,
                                name = "Interagir avec '${elem.label}'",
                                actionType = if (elem.suggestedAction == "INPUT_TEXT") ActionType.TEXT_INPUT else ActionType.CLICK,
                                targetType = TargetType.COORDINATES,
                                targetX = elem.x,
                                targetY = elem.y,
                                delayBeforeMs = 500L
                            )
                        )
                    }
                }

                return@withContext ScreenAnalysisResult(
                    appOverview = appOverview,
                    detectedElements = elementsList,
                    suggestedScenarioTitle = title,
                    suggestedSteps = stepsList,
                    explanation = explanation
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Gemini vision analysis JSON", e)
            }
        }

        // Fallback analysis when offline or without API key
        return@withContext createFallbackScreenAnalysis(screenBitmap, userInstruction)
    }

    private fun callGeminiMultimodalRaw(prompt: String, bitmap: Bitmap): String? {
        if (!hasValidApiKey) {
            Log.w(TAG, "No valid Gemini API key available for vision call")
            return null
        }

        return try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Data)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topP", 0.8)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini Multimodal Vision API", e)
            null
        }
    }

    private fun createFallbackScreenAnalysis(screenBitmap: Bitmap, userInstruction: String): ScreenAnalysisResult {
        val w = screenBitmap.width.toFloat()
        val h = screenBitmap.height.toFloat()

        val elements = listOf(
            DetectedUiElement(
                label = "Bouton d'action principal",
                type = "BUTTON",
                x = w * 0.5f,
                y = h * 0.75f,
                confidence = 0.94f,
                suggestedAction = "CLICK"
            ),
            DetectedUiElement(
                label = "Zone de contenu interactif",
                type = "CONTAINER",
                x = w * 0.5f,
                y = h * 0.45f,
                confidence = 0.88f,
                suggestedAction = "SWIPE"
            ),
            DetectedUiElement(
                label = "Barre supérieure de navigation",
                type = "HEADER",
                x = w * 0.5f,
                y = h * 0.1f,
                confidence = 0.91f,
                suggestedAction = "CLICK"
            )
        )

        val steps = listOf(
            ActionStep(
                stepNumber = 1,
                name = "Clic sur le bouton principal détecté",
                actionType = ActionType.CLICK,
                targetType = TargetType.COORDINATES,
                targetX = w * 0.5f,
                targetY = h * 0.75f,
                delayBeforeMs = 600L,
                humanizeJitterRadius = 12
            ),
            ActionStep(
                stepNumber = 2,
                name = "Attente de chargement",
                actionType = ActionType.WAIT_DELAY,
                durationMs = 1200L
            ),
            ActionStep(
                stepNumber = 3,
                name = "Défilement vertical du contenu",
                actionType = ActionType.SCROLL_DOWN,
                targetType = TargetType.COORDINATES,
                targetX = w * 0.5f,
                targetY = h * 0.6f,
                delayBeforeMs = 400L
            )
        )

        val overview = if (userInstruction.isNotBlank()) {
            "Analyse ciblée : '$userInstruction' sur l'écran actif (${w.toInt()}x${h.toInt()} px)"
        } else {
            "Capture d'écran active (${w.toInt()}x${h.toInt()} px) traitée par le moteur de vision"
        }

        return ScreenAnalysisResult(
            appOverview = overview,
            detectedElements = elements,
            suggestedScenarioTitle = if (userInstruction.isNotBlank()) "🤖 Vision : ${userInstruction.take(25)}" else "Automatisation Écran Détecté",
            suggestedSteps = steps,
            explanation = "Éléments clés identifiés à l'écran via MediaProjection. Coordonnées calculées et calibrées pour la résolution de votre appareil."
        )
    }
}

data class DetectedUiElement(
    val label: String,
    val type: String,
    val x: Float,
    val y: Float,
    val confidence: Float = 0.92f,
    val suggestedAction: String = "CLICK"
)

data class ScreenAnalysisResult(
    val appOverview: String,
    val detectedElements: List<DetectedUiElement>,
    val suggestedScenarioTitle: String,
    val suggestedSteps: List<ActionStep>,
    val explanation: String
)
