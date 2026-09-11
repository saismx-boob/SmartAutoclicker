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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
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
                        else -> Icons.Default.TouchApp
                    }
                    val actionColor = when (step.actionType) {
                        ActionType.CLICK -> CyberCyan
                        ActionType.SWIPE -> CyberEmerald
                        ActionType.WAIT_DELAY -> CyberAmber
                        ActionType.TEXT_INPUT -> Color(0xFFE040FB)
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
                    ActionType.TEXT_INPUT to "⌨️ Texte"
                ).forEach { (aType, label) ->
                    val isSelected = step.actionType == aType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable {
                                onUpdate(
                                    step.copy(
                                        actionType = aType,
                                        name = when (aType) {
                                            ActionType.CLICK -> "Clic Étape ${index + 1}"
                                            ActionType.SWIPE -> "Glisser Étape ${index + 1}"
                                            ActionType.WAIT_DELAY -> "Pause ${step.durationMs}ms"
                                            ActionType.TEXT_INPUT -> "Saisie Texte"
                                            else -> step.name
                                        }
                                    )
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) CyberCyan else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // 3. LOGIC CONDITION BLOCK ('If image detected, then perform action')
            LogicConditionSection(
                step = step,
                onUpdate = onUpdate
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
    onUpdate: (ActionStep) -> Unit
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

                    // Logic Execution Flow Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF151821))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🟢 ALORS : Exécuter l'action [${step.actionType.label}]",
                                color = CyberEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "🔴 SINON : Ignorer l'étape et passer à la suivante",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
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
