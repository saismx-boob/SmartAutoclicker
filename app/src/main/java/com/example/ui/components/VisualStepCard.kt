package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ImageRecognitionEngine
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ConditionType
import com.example.model.TargetType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Visual drag-and-drop step card for the scenario builder.
 * Supports action configuration (Click, Swipe, Wait, Text) and
 * visual logic conditions ('If image detected, then perform action').
 */
@Composable
fun VisualStepCard(
    step: ActionStep,
    index: Int,
    totalSteps: Int,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
    onUpdate: (ActionStep) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val hasCondition = step.conditionType != ConditionType.ALWAYS

    val elevationDp by animateFloatAsState(
        targetValue = if (isDragging) 16f else 2f,
        label = "elevation"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) GunmetalElevated else SlateSurface
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevationDp.dp, RoundedCornerShape(14.dp))
            .border(
                width = if (isDragging) 2.dp else 1.dp,
                color = if (isDragging) CyberCyan else if (hasCondition) CyberAmber.copy(alpha = 0.6f) else CyberBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("step_card_$index")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. TOP HEADER: Drag Handle, Step Index, Action Type Icon, Name, Reorder Buttons & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Tactile Drag Handle icon for drag-and-drop sequencing
                    Box(
                        modifier = dragModifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GunmetalCard)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("drag_handle_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Glisser pour réordonner",
                            tint = if (isDragging) CyberCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Step sequence badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (hasCondition) CyberAmber.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (hasCondition) CyberAmber else CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Action Type Badge Icon
                    val actionIcon = when (step.actionType) {
                        ActionType.CLICK -> Icons.Default.TouchApp
                        ActionType.SWIPE -> Icons.Default.Swipe
                        ActionType.WAIT_DELAY -> Icons.Default.HourglassEmpty
                        ActionType.TEXT_INPUT -> Icons.Default.Keyboard
                        ActionType.BRANCH_IF_ELSE -> Icons.Default.AltRoute
                        ActionType.JUMP_TO_STEP -> Icons.Default.Redo
                        ActionType.STOP_SCENARIO -> Icons.Default.StopCircle
                        else -> Icons.Default.TouchApp
                    }
                    val actionColor = when (step.actionType) {
                        ActionType.CLICK -> CyberCyan
                        ActionType.SWIPE -> CyberEmerald
                        ActionType.WAIT_DELAY -> CyberAmber
                        ActionType.TEXT_INPUT -> Color(0xFFE040FB)
                        ActionType.BRANCH_IF_ELSE -> Color(0xFFFFB300)
                        ActionType.JUMP_TO_STEP -> CyberCyan
                        ActionType.STOP_SCENARIO -> EmergencyCrimson
                        else -> TextPrimary
                    }

                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = step.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (hasCondition) {
                            Text(
                                text = "🔀 Condition: ${step.conditionType.label}",
                                color = CyberAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Controls row: Move Up, Move Down, Duplicate, Test, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Move Up button
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Monter l'étape",
                            tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Move Down button
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalSteps - 1,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Descendre l'étape",
                            tint = if (index < totalSteps - 1) TextSecondary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Test single step button
                    IconButton(
                        onClick = onTest,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Tester cette étape",
                            tint = CyberEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Duplicate button
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Dupliquer",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = EmergencyCrimson.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. ACTION TYPE SWITCHER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GunmetalCard)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    ActionType.CLICK to "🎯 Clic",
                    ActionType.SWIPE to "👆 Glisser",
                    ActionType.WAIT_DELAY to "⏳ Pause",
                    ActionType.TEXT_INPUT to "⌨️ Texte",
                    ActionType.BRANCH_IF_ELSE to "🔀 Si/Alors/Sinon"
                ).forEach { (aType, label) ->
                    val isSelected = step.actionType == aType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) {
                                    if (aType == ActionType.BRANCH_IF_ELSE) CyberAmber.copy(alpha = 0.25f) else CyberCyan.copy(alpha = 0.25f)
                                } else Color.Transparent
                            )
                            .clickable {
                                onUpdate(
                                    step.copy(
                                        actionType = aType,
                                        name = when (aType) {
                                            ActionType.CLICK -> "Clic Étape ${index + 1}"
                                            ActionType.SWIPE -> "Glisser Étape ${index + 1}"
                                            ActionType.WAIT_DELAY -> "Pause ${step.durationMs}ms"
                                            ActionType.TEXT_INPUT -> "Saisie Texte"
                                            ActionType.BRANCH_IF_ELSE -> "Si Cible Vue ➔ Clic, Sinon ➔ Pause"
                                            else -> step.name
                                        },
                                        conditionType = if (aType == ActionType.BRANCH_IF_ELSE && step.conditionType == ConditionType.ALWAYS) {
                                            ConditionType.IF_IMAGE_PRESENT
                                        } else step.conditionType
                                    )
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) {
                                if (aType == ActionType.BRANCH_IF_ELSE) CyberAmber else CyberCyan
                            } else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (step.actionType == ActionType.BRANCH_IF_ELSE) {
                // Dedicated full visual flow control block
                BranchIfElseActionParameters(
                    step = step,
                    onUpdate = onUpdate,
                    totalSteps = totalSteps
                )
            } else {
                // 3. LOGIC CONDITION BLOCK ('If image detected, then perform action')
                LogicConditionSection(
                    step = step,
                    onUpdate = onUpdate,
                    totalSteps = totalSteps
                )

                // 4. ACTION PARAMETERS SECTION BASED ON ACTION TYPE
                when (step.actionType) {
                    ActionType.CLICK -> {
                        ClickActionParameters(
                            step = step,
                            onUpdate = onUpdate
                        )
                    }
                    ActionType.SWIPE -> {
                        SwipeActionParameters(
                            step = step,
                            onUpdate = onUpdate
                        )
                    }
                    ActionType.WAIT_DELAY -> {
                        WaitActionParameters(
                            step = step,
                            onUpdate = onUpdate
                        )
                    }
                    ActionType.TEXT_INPUT -> {
                        TextInputActionParameters(
                            step = step,
                            onUpdate = onUpdate
                        )
                    }
                    else -> Unit
                }
            }

            // 5. TIMING & HUMANIZATION FOOTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(GunmetalCard.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Délai avant: ${step.delayBeforeMs}ms",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "Anti-détection: ±${step.humanizeJitterRadius}px / ±${step.humanizeTimingVariance}%",
                    color = CyberCyan.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Dedicated visual logic condition block:
 * 'Si image détectée, alors exécuter l'action'
 */
@Composable
fun LogicConditionSection(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit,
    totalSteps: Int = 1
) {
    val isConditionEnabled = step.conditionType != ConditionType.ALWAYS
    val templates = ImageRecognitionEngine.getAllTemplates()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConditionEnabled) Color(0xFF1E1C14) else GunmetalCard
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isConditionEnabled) CyberAmber.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = if (isConditionEnabled) CyberAmber else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Condition Logique (SI ... ALORS)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isConditionEnabled) CyberAmber else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Switch(
                    checked = isConditionEnabled,
                    onCheckedChange = { enabled ->
                        val newCondition = if (enabled) ConditionType.IF_IMAGE_PRESENT else ConditionType.ALWAYS
                        onUpdate(
                            step.copy(
                                conditionType = newCondition,
                                conditionParam = if (enabled && step.conditionParam.isBlank()) step.targetImageTemplate else step.conditionParam
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberAmber,
                        checkedTrackColor = CyberAmber.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }

            // Expanded condition editor when enabled
            AnimatedVisibility(visible = isConditionEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Condition Type Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateSurface)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            ConditionType.IF_IMAGE_PRESENT to "🖼️ Si Image Présente",
                            ConditionType.IF_IMAGE_NOT_PRESENT to "🚫 Si Image Absente",
                            ConditionType.IF_TEXT_PRESENT to "🔤 Si Texte Présent"
                        ).forEach { (cType, label) ->
                            val isSelected = step.conditionType == cType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberAmber.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable {
                                        onUpdate(
                                            step.copy(
                                                conditionType = cType,
                                                conditionParam = if (cType == ConditionType.IF_TEXT_PRESENT) step.targetText else step.targetImageTemplate
                                            )
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberAmber else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // If Image Condition: Template selector + Similarity threshold
                    if (step.conditionType == ConditionType.IF_IMAGE_PRESENT || step.conditionType == ConditionType.IF_IMAGE_NOT_PRESENT) {
                        Text(
                            text = "Modèle d'image / icône à surveiller :",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(templates) { template ->
                                val isSelected = (step.conditionParam == template.id) || (step.targetImageTemplate == template.id)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyberAmber.copy(alpha = 0.25f) else SlateSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberAmber else CyberBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            onUpdate(
                                                step.copy(
                                                    conditionParam = template.id,
                                                    targetImageTemplate = template.id,
                                                    targetImageName = template.name
                                                )
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = template.name,
                                            color = if (isSelected) CyberAmber else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = template.category,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Auto-target option: Click directly where image is found!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎯 Cliquer au centre de l'image détectée",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Positionne automatiquement le clic sur la cible trouvée",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Switch(
                                checked = step.targetType == TargetType.IMAGE_MATCH,
                                onCheckedChange = { checked ->
                                    onUpdate(
                                        step.copy(
                                            targetType = if (checked) TargetType.IMAGE_MATCH else TargetType.COORDINATES
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberCyan,
                                    checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(width = 40.dp, height = 22.dp)
                            )
                        }

                        // Similarity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Seuil de similarité requis", color = TextMuted, fontSize = 11.sp)
                            Text("${(step.imageSimilarityThreshold * 100).toInt()}%", color = CyberAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.imageSimilarityThreshold,
                            onValueChange = { onUpdate(step.copy(imageSimilarityThreshold = it)) },
                            valueRange = 0.50f..0.95f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberAmber,
                                activeTrackColor = CyberAmber
                            )
                        )
                    } else if (step.conditionType == ConditionType.IF_TEXT_PRESENT) {
                        OutlinedTextField(
                            value = step.conditionParam,
                            onValueChange = { onUpdate(step.copy(conditionParam = it, targetText = it)) },
                            label = { Text("Texte qui doit apparaître à l'écran") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Logic Execution Flow: ALORS (GREEN)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F261C))
                            .border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🟢 ALORS : Exécuter l'action principale [${step.actionType.label}]",
                                color = CyberEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Logic Execution Flow: SINON (RED/PINK) - Interactive
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF261016))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🔴 SINON [Si condition non remplie] :",
                            color = Color(0xFFFF80AB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(SlateSurface)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                ActionType.WAIT_DELAY to "⏳ Pause",
                                ActionType.CLICK to "🎯 Clic repli",
                                ActionType.SWIPE to "👆 Glisser",
                                ActionType.STOP_SCENARIO to "⏹️ Arrêter"
                            ).forEach { (eType, label) ->
                                val isSel = step.elseActionType == eType
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) Color(0xFFFF5252).copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable { onUpdate(step.copy(elseActionType = eType)) }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color(0xFFFF80AB) else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        if (step.elseActionType == ActionType.WAIT_DELAY) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Durée de pause si non trouvé :", color = TextSecondary, fontSize = 10.sp)
                                Text("${step.elseDurationMs}ms", color = CyberAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = step.elseDurationMs.toFloat(),
                                onValueChange = { onUpdate(step.copy(elseDurationMs = it.toLong())) },
                                valueRange = 100f..3000f,
                                colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                            )
                        } else if (step.elseActionType == ActionType.CLICK) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("X repli: ${step.elseTargetX.toInt()}px", color = TextMuted, fontSize = 10.sp)
                                    Slider(
                                        value = step.elseTargetX,
                                        onValueChange = { onUpdate(step.copy(elseTargetX = it)) },
                                        valueRange = 0f..1080f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF80AB), activeTrackColor = Color(0xFFFF80AB))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Y repli: ${step.elseTargetY.toInt()}px", color = TextMuted, fontSize = 10.sp)
                                    Slider(
                                        value = step.elseTargetY,
                                        onValueChange = { onUpdate(step.copy(elseTargetY = it)) },
                                        valueRange = 0f..2400f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF80AB), activeTrackColor = Color(0xFFFF80AB))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Click Action Controls
 */
@Composable
fun ClickActionParameters(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (step.targetType == TargetType.COORDINATES) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = step.targetX.toInt().toString(),
                    onValueChange = { it.toFloatOrNull()?.let { x -> onUpdate(step.copy(targetX = x)) } },
                    label = { Text("X (px)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = step.targetY.toInt().toString(),
                    onValueChange = { it.toFloatOrNull()?.let { y -> onUpdate(step.copy(targetY = y)) } },
                    label = { Text("Y (px)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Quick Coordinate Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Centre" to (540f to 1200f),
                    "Bouton Bas" to (540f to 2100f),
                    "Haut Droit (✕)" to (980f to 150f),
                    "Haut Gauche" to (100f to 150f)
                ).forEach { (label, coords) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GunmetalCard)
                            .clickable { onUpdate(step.copy(targetX = coords.first, targetY = coords.second)) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = TextMuted, fontSize = 9.sp)
                    }
                }
            }
        } else if (step.targetType == TargetType.IMAGE_MATCH) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.1f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Le clic ciblera dynamiquement l'image '${step.targetImageName}' dès qu'elle sera détectée.",
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Swipe Action Controls
 */
@Composable
fun SwipeActionParameters(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = "(${step.targetX.toInt()}, ${step.targetY.toInt()})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Départ (X, Y)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = "(${step.swipeEndX.toInt()}, ${step.swipeEndY.toInt()})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Arrivée (X, Y)") },
                modifier = Modifier.weight(1f)
            )
        }

        // Direction Presets
        Text("Directions rapides :", color = TextMuted, fontSize = 10.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "⬆️ Vers le Haut" to (540f to 1600f to (540f to 600f)),
                "⬇️ Vers le Bas" to (540f to 600f to (540f to 1600f)),
                "⬅️ Vers la Gauche" to (900f to 1200f to (180f to 1200f)),
                "➡️ Vers la Droite" to (180f to 1200f to (900f to 1200f))
            ).forEach { (label, data) ->
                val start = data.first
                val end = data.second
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GunmetalCard)
                        .clickable {
                            onUpdate(
                                step.copy(
                                    targetX = start.first,
                                    targetY = start.second,
                                    swipeEndX = end.first,
                                    swipeEndY = end.second
                                )
                            )
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Swipe Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Durée du glissement", color = TextMuted, fontSize = 11.sp)
            Text("${step.durationMs} ms", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = step.durationMs.toFloat(),
            onValueChange = { onUpdate(step.copy(durationMs = it.toLong())) },
            valueRange = 100f..1500f,
            colors = SliderDefaults.colors(
                thumbColor = CyberEmerald,
                activeTrackColor = CyberEmerald
            )
        )
    }
}

/**
 * Wait Action Controls
 */
@Composable
fun WaitActionParameters(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Durée de la pause", color = TextSecondary, fontSize = 11.sp)
            Text(
                text = "${step.durationMs} ms (${"%.1f".format(step.durationMs / 1000f)}s)",
                color = CyberAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = step.durationMs.toFloat(),
            onValueChange = { onUpdate(step.copy(durationMs = it.toLong(), delayBeforeMs = it.toLong())) },
            valueRange = 100f..5000f,
            colors = SliderDefaults.colors(
                thumbColor = CyberAmber,
                activeTrackColor = CyberAmber
            )
        )

        // Quick Wait Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(250L, 500L, 1000L, 2000L, 3000L).forEach { ms ->
                val isSelected = step.durationMs == ms
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CyberAmber.copy(alpha = 0.25f) else GunmetalCard)
                        .border(1.dp, if (isSelected) CyberAmber else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { onUpdate(step.copy(durationMs = ms, delayBeforeMs = ms)) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (ms >= 1000L) "${ms / 1000}s" else "${ms}ms",
                        color = if (isSelected) CyberAmber else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Text Input Controls
 */
@Composable
fun TextInputActionParameters(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit
) {
    OutlinedTextField(
        value = step.textToType,
        onValueChange = { onUpdate(step.copy(textToType = it)) },
        label = { Text("Texte à taper automatiquement") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/**
 * Dedicated visual control block for 'Si [condition], alors [action], sinon [autre action]'
 */
@Composable
fun BranchIfElseActionParameters(
    step: ActionStep,
    onUpdate: (ActionStep) -> Unit,
    totalSteps: Int
) {
    val templates = ImageRecognitionEngine.getAllTemplates()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Explanatory Banner for novices
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1824)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, CyberAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AltRoute,
                    contentDescription = null,
                    tint = CyberAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Bloc Décisionnel : Si ➔ Alors ➔ Sinon",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = CyberAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "Vérifie l'écran : Si la condition visuelle est validée, exécute l'action ALORS. Sinon, exécute l'action alternative SINON.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // ==========================================
        // 1. SECTION CONDITION (SI)
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, CyberAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(CyberAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SI [Condition Visuelle à l'Écran]",
                        color = CyberAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Type of visual condition
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GunmetalCard)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        ConditionType.IF_IMAGE_PRESENT to "🖼️ Image Visible",
                        ConditionType.IF_IMAGE_NOT_PRESENT to "🚫 Image Absente",
                        ConditionType.IF_TEXT_PRESENT to "🔤 Texte Détecté"
                    ).forEach { (cType, label) ->
                        val isSelected = step.conditionType == cType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CyberAmber.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    onUpdate(
                                        step.copy(
                                            conditionType = cType,
                                            conditionParam = if (cType == ConditionType.IF_TEXT_PRESENT) step.targetText else step.targetImageTemplate
                                        )
                                    )
                                }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CyberAmber else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (step.conditionType == ConditionType.IF_TEXT_PRESENT) {
                    OutlinedTextField(
                        value = step.conditionParam,
                        onValueChange = { onUpdate(step.copy(conditionParam = it, targetText = it)) },
                        label = { Text("Texte à rechercher à l'écran") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // Image template selector
                    Text("Cible visuelle à surveiller :", color = TextSecondary, fontSize = 10.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { template ->
                            val isSelected = (step.conditionParam == template.id) || (step.targetImageTemplate == template.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CyberAmber.copy(alpha = 0.25f) else GunmetalCard)
                                    .border(1.dp, if (isSelected) CyberAmber else CyberBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdate(
                                            step.copy(
                                                conditionParam = template.id,
                                                targetImageTemplate = template.id,
                                                targetImageName = template.name
                                            )
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = template.name,
                                    color = if (isSelected) CyberAmber else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Auto click on image position
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(GunmetalCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Clic automatique centré sur la cible",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                        Switch(
                            checked = step.targetType == TargetType.IMAGE_MATCH,
                            onCheckedChange = { checked ->
                                onUpdate(step.copy(targetType = if (checked) TargetType.IMAGE_MATCH else TargetType.COORDINATES))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = CyberCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Similarity threshold slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seuil de ressemblance", color = TextMuted, fontSize = 10.sp)
                        Text("${(step.imageSimilarityThreshold * 100).toInt()}%", color = CyberAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = step.imageSimilarityThreshold,
                        onValueChange = { onUpdate(step.copy(imageSimilarityThreshold = it)) },
                        valueRange = 0.50f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = CyberAmber, activeTrackColor = CyberAmber)
                    )
                }
            }
        }

        // ==========================================
        // 2. SECTION ALORS (SI CONDITION VRAIE)
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALORS [Action si VRAI]",
                        color = CyberEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GunmetalCard)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        ActionType.CLICK to "🎯 Clic",
                        ActionType.WAIT_DELAY to "⏳ Pause",
                        ActionType.TEXT_INPUT to "⌨️ Texte",
                        ActionType.JUMP_TO_STEP to "🔁 Saut"
                    ).forEach { (aType, label) ->
                        val isSelected = step.thenActionType == aType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CyberEmerald.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { onUpdate(step.copy(thenActionType = aType)) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) CyberEmerald else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                when (step.thenActionType) {
                    ActionType.CLICK -> {
                        if (step.targetType != TargetType.IMAGE_MATCH) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Position X: ${step.targetX.toInt()} px", color = TextMuted, fontSize = 10.sp)
                                    Slider(
                                        value = step.targetX,
                                        onValueChange = { onUpdate(step.copy(targetX = it)) },
                                        valueRange = 0f..1080f,
                                        colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Position Y: ${step.targetY.toInt()} px", color = TextMuted, fontSize = 10.sp)
                                    Slider(
                                        value = step.targetY,
                                        onValueChange = { onUpdate(step.copy(targetY = it)) },
                                        valueRange = 0f..2400f,
                                        colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                                    )
                                }
                            }
                        } else {
                            Text("Le clic sera automatiquement centré sur la cible détectée.", color = CyberEmerald, fontSize = 10.sp)
                        }
                    }
                    ActionType.WAIT_DELAY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Durée de pause :", color = TextMuted, fontSize = 10.sp)
                            Text("${step.thenDurationMs}ms", color = CyberEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.thenDurationMs.toFloat(),
                            onValueChange = { onUpdate(step.copy(thenDurationMs = it.toLong())) },
                            valueRange = 100f..5000f,
                            colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                        )
                    }
                    ActionType.TEXT_INPUT -> {
                        OutlinedTextField(
                            value = step.thenTextToType,
                            onValueChange = { onUpdate(step.copy(thenTextToType = it)) },
                            label = { Text("Texte à taper") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    ActionType.JUMP_TO_STEP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sauter directement à l'étape :", color = TextPrimary, fontSize = 11.sp)
                            Text("#${step.thenStepJump.coerceAtLeast(1)}", color = CyberEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.thenStepJump.coerceIn(1, totalSteps.coerceAtLeast(1)).toFloat(),
                            onValueChange = { onUpdate(step.copy(thenStepJump = it.toInt())) },
                            valueRange = 1f..totalSteps.coerceAtLeast(1).toFloat(),
                            steps = (totalSteps - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                        )
                    }
                    else -> Unit
                }
            }
        }

        // ==========================================
        // 3. SECTION SINON (SI CONDITION FAUSSE)
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SINON [Autre action si FAUX]",
                        color = Color(0xFFFF80AB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(GunmetalCard)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        ActionType.WAIT_DELAY to "⏳ Pause",
                        ActionType.CLICK to "🎯 Clic repli",
                        ActionType.SWIPE to "👆 Glisser",
                        ActionType.JUMP_TO_STEP to "🔁 Saut",
                        ActionType.STOP_SCENARIO to "⏹️ Arrêt"
                    ).forEach { (aType, label) ->
                        val isSelected = step.elseActionType == aType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFFFF5252).copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { onUpdate(step.copy(elseActionType = aType)) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFFF80AB) else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                when (step.elseActionType) {
                    ActionType.CLICK -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("X repli: ${step.elseTargetX.toInt()} px", color = TextMuted, fontSize = 10.sp)
                                Slider(
                                    value = step.elseTargetX,
                                    onValueChange = { onUpdate(step.copy(elseTargetX = it)) },
                                    valueRange = 0f..1080f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Y repli: ${step.elseTargetY.toInt()} px", color = TextMuted, fontSize = 10.sp)
                                Slider(
                                    value = step.elseTargetY,
                                    onValueChange = { onUpdate(step.copy(elseTargetY = it)) },
                                    valueRange = 0f..2400f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                                )
                            }
                        }
                    }
                    ActionType.WAIT_DELAY -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Durée d'attente / pause :", color = TextMuted, fontSize = 10.sp)
                            Text("${step.elseDurationMs}ms", color = Color(0xFFFF80AB), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.elseDurationMs.toFloat(),
                            onValueChange = { onUpdate(step.copy(elseDurationMs = it.toLong())) },
                            valueRange = 100f..5000f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                        )
                    }
                    ActionType.SWIPE -> {
                        Text(
                            text = "Glisser l'écran pour chercher la cible plus bas (défilement automatique).",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    ActionType.JUMP_TO_STEP -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sauter directement à l'étape :", color = TextPrimary, fontSize = 11.sp)
                            Text("#${step.elseStepJump.coerceAtLeast(1)}", color = Color(0xFFFF80AB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = step.elseStepJump.coerceIn(1, totalSteps.coerceAtLeast(1)).toFloat(),
                            onValueChange = { onUpdate(step.copy(elseStepJump = it.toInt())) },
                            valueRange = 1f..totalSteps.coerceAtLeast(1).toFloat(),
                            steps = (totalSteps - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                        )
                    }
                    ActionType.STOP_SCENARIO -> {
                        Text(
                            text = "⏹️ Arrêt automatique immédiat du scénario si la condition n'est pas remplie.",
                            color = EmergencyCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
