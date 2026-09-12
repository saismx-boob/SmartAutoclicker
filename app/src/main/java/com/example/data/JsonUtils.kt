package com.example.data

import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ConditionType
import com.example.model.TargetType
import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {

    fun stepsToJson(steps: List<ActionStep>): String {
        val array = JSONArray()
        for (step in steps) {
            val obj = JSONObject()
            obj.put("id", step.id)
            obj.put("stepNumber", step.stepNumber)
            obj.put("name", step.name)
            obj.put("actionType", step.actionType.name)
            obj.put("targetType", step.targetType.name)
            obj.put("targetX", step.targetX.toDouble())
            obj.put("targetY", step.targetY.toDouble())
            obj.put("targetText", step.targetText)
            obj.put("targetColorHex", step.targetColorHex)
            obj.put("textToType", step.textToType)
            obj.put("swipeEndX", step.swipeEndX.toDouble())
            obj.put("swipeEndY", step.swipeEndY.toDouble())
            obj.put("durationMs", step.durationMs)
            obj.put("delayBeforeMs", step.delayBeforeMs)
            obj.put("conditionType", step.conditionType.name)
            obj.put("conditionParam", step.conditionParam)
            obj.put("thenActionType", step.thenActionType.name)
            obj.put("thenStepJump", step.thenStepJump)
            obj.put("thenDurationMs", step.thenDurationMs)
            obj.put("thenTextToType", step.thenTextToType)
            obj.put("elseActionType", step.elseActionType.name)
            obj.put("elseTargetX", step.elseTargetX.toDouble())
            obj.put("elseTargetY", step.elseTargetY.toDouble())
            obj.put("elseSwipeEndX", step.elseSwipeEndX.toDouble())
            obj.put("elseSwipeEndY", step.elseSwipeEndY.toDouble())
            obj.put("elseDurationMs", step.elseDurationMs)
            obj.put("elseDelayBeforeMs", step.elseDelayBeforeMs)
            obj.put("elseTextToType", step.elseTextToType)
            obj.put("elseStepJump", step.elseStepJump)
            obj.put("humanizeJitterRadius", step.humanizeJitterRadius)
            obj.put("humanizeTimingVariance", step.humanizeTimingVariance)
            obj.put("isEnabled", step.isEnabled)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToSteps(json: String?): List<ActionStep> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ActionStep>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val step = ActionStep(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    stepNumber = obj.optInt("stepNumber", i + 1),
                    name = obj.optString("name", "Étape ${i + 1}"),
                    actionType = try { ActionType.valueOf(obj.optString("actionType", "CLICK")) } catch (e: Exception) { ActionType.CLICK },
                    targetType = try { TargetType.valueOf(obj.optString("targetType", "COORDINATES")) } catch (e: Exception) { TargetType.COORDINATES },
                    targetX = obj.optDouble("targetX", 540.0).toFloat(),
                    targetY = obj.optDouble("targetY", 1200.0).toFloat(),
                    targetText = obj.optString("targetText", ""),
                    targetColorHex = obj.optString("targetColorHex", "#00E5FF"),
                    textToType = obj.optString("textToType", ""),
                    swipeEndX = obj.optDouble("swipeEndX", 540.0).toFloat(),
                    swipeEndY = obj.optDouble("swipeEndY", 600.0).toFloat(),
                    durationMs = obj.optLong("durationMs", 100L),
                    delayBeforeMs = obj.optLong("delayBeforeMs", 300L),
                    conditionType = try { ConditionType.valueOf(obj.optString("conditionType", "ALWAYS")) } catch (e: Exception) { ConditionType.ALWAYS },
                    conditionParam = obj.optString("conditionParam", ""),
                    thenActionType = try { ActionType.valueOf(obj.optString("thenActionType", "CLICK")) } catch (e: Exception) { ActionType.CLICK },
                    thenStepJump = obj.optInt("thenStepJump", 0),
                    thenDurationMs = obj.optLong("thenDurationMs", 200L),
                    thenTextToType = obj.optString("thenTextToType", ""),
                    elseActionType = try { ActionType.valueOf(obj.optString("elseActionType", "WAIT_DELAY")) } catch (e: Exception) { ActionType.WAIT_DELAY },
                    elseTargetX = obj.optDouble("elseTargetX", 540.0).toFloat(),
                    elseTargetY = obj.optDouble("elseTargetY", 1400.0).toFloat(),
                    elseSwipeEndX = obj.optDouble("elseSwipeEndX", 540.0).toFloat(),
                    elseSwipeEndY = obj.optDouble("elseSwipeEndY", 600.0).toFloat(),
                    elseDurationMs = obj.optLong("elseDurationMs", 200L),
                    elseDelayBeforeMs = obj.optLong("elseDelayBeforeMs", 200L),
                    elseTextToType = obj.optString("elseTextToType", ""),
                    elseStepJump = obj.optInt("elseStepJump", 0),
                    humanizeJitterRadius = obj.optInt("humanizeJitterRadius", 14),
                    humanizeTimingVariance = obj.optInt("humanizeTimingVariance", 18),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                list.add(step)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
