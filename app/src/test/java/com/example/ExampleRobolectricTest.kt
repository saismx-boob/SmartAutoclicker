package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.JsonUtils
import com.example.engine.Humanizer
import com.example.engine.ScreenDetectionEngine
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ConditionType
import com.example.service.FloatingActionPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun read_appName_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AutoClick AI", appName)
  }

  @Test
  fun test_jsonUtils_serialization() {
    val step = ActionStep(
      stepNumber = 1,
      name = "Test Clic",
      actionType = ActionType.CLICK,
      targetX = 350f,
      targetY = 700f
    )
    val json = JsonUtils.stepsToJson(listOf(step))
    val deserialized = JsonUtils.jsonToSteps(json)
    assertEquals(1, deserialized.size)
    assertEquals("Test Clic", deserialized[0].name)
    assertEquals(350f, deserialized[0].targetX)
  }

  @Test
  fun test_humanizer_randomizePoint() {
    val x = 500f
    val y = 1000f
    val radius = 15
    val point = Humanizer.randomizePoint(x, y, radius)
    assertTrue("Point X should be within radius", kotlin.math.abs(point.x - x) <= radius)
    assertTrue("Point Y should be within radius", kotlin.math.abs(point.y - y) <= radius)
  }

  @Test
  fun test_condition_evaluation() {
    val alwaysTrue = ScreenDetectionEngine.evaluateCondition(ConditionType.ALWAYS, "", true)
    assertTrue(alwaysTrue)
  }

  @Test
  fun test_in_game_action_target_to_action_step() {
    val clickTarget = FloatingActionPalette.InGameActionTarget(
      id = "test_click_1",
      index = 1,
      type = ActionType.CLICK,
      view = null,
      params = null,
      startX = 450f,
      startY = 890f,
      delayBeforeMs = 250L
    )

    val step = ActionStep(
      stepNumber = clickTarget.index,
      name = "Clic en Jeu #1",
      actionType = ActionType.CLICK,
      targetX = clickTarget.startX,
      targetY = clickTarget.startY,
      delayBeforeMs = clickTarget.delayBeforeMs
    )

    assertEquals(1, step.stepNumber)
    assertEquals(450f, step.targetX)
    assertEquals(890f, step.targetY)
    assertEquals(250L, step.delayBeforeMs)

    val json = JsonUtils.stepsToJson(listOf(step))
    val decoded = JsonUtils.jsonToSteps(json)
    assertEquals(1, decoded.size)
    assertEquals(450f, decoded[0].targetX)
    assertEquals(890f, decoded[0].targetY)
  }

  @Test
  fun test_branch_if_else_serialization() {
    val branchStep = ActionStep(
      stepNumber = 1,
      name = "Test Si/Alors/Sinon",
      actionType = ActionType.BRANCH_IF_ELSE,
      conditionType = ConditionType.IF_IMAGE_PRESENT,
      conditionParam = "ic_check",
      thenActionType = ActionType.CLICK,
      thenStepJump = 3,
      elseActionType = ActionType.SWIPE,
      elseStepJump = 5,
      elseDurationMs = 450L
    )

    val json = JsonUtils.stepsToJson(listOf(branchStep))
    val decoded = JsonUtils.jsonToSteps(json)
    assertEquals(1, decoded.size)
    assertEquals(ActionType.BRANCH_IF_ELSE, decoded[0].actionType)
    assertEquals(ConditionType.IF_IMAGE_PRESENT, decoded[0].conditionType)
    assertEquals(ActionType.CLICK, decoded[0].thenActionType)
    assertEquals(3, decoded[0].thenStepJump)
    assertEquals(ActionType.SWIPE, decoded[0].elseActionType)
    assertEquals(5, decoded[0].elseStepJump)
    assertEquals(450L, decoded[0].elseDurationMs)
  }
}
