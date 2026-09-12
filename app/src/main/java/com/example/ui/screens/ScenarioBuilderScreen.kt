package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.ActionStep
import com.example.model.ActionType
import com.example.model.ConditionType
import com.example.model.ScenarioEntity
import com.example.model.TargetType
import com.example.ui.components.VisualStepCard
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

@Composable
fun ScenarioBuilderScreen(
    scenarios: List<ScenarioEntity>,
    editingScenario: ScenarioEntity?,
    editingSteps: List<ActionStep>,
    isRecording: Boolean,
    recordedCount: Int,
    onSelectScenario: (ScenarioEntity) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteScenario: (Long) -> Unit,
    onSaveScenario: (String, String, Int) -> Unit,
    onCancelEdit: () -> Unit,
    onAddStep: (ActionType) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onUpdateStep: (Int, ActionStep) -> Unit,
    onReorderStep: (Int, Int) -> Unit = { _, _ -> },
    onMoveStepUp: (Int) -> Unit = {},
    onMoveStepDown: (Int) -> Unit = {},
    onDuplicateStep: (Int) -> Unit = {},
    onTestStep: (ActionStep) -> Unit = {},
    onLaunchInGamePalette: (Long?) -> Unit = {},
    onStartRecording: () -> Unit,
    onRecordTouch: (Float, Float) -> Unit,
    onFinishRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    // If macro recording is active, show interactive touch canvas
    if (isRecording) {
        GestureRecorderView(
            recordedCount = recordedCount,
            onTouch = onRecordTouch,
            onFinish = onFinishRecording,
            onCancel = onCancelRecording
        )
        return
    }

    // If an individual scenario is open for editing, show visual step builder
    if (editingScenario != null) {
        ScenarioEditorView(
            scenario = editingScenario,
            steps = editingSteps,
            onSave = onSaveScenario,
            onCancel = onCancelEdit,
            onAddStep = onAddStep,
            onRemoveStep = onRemoveStep,
            onUpdateStep = onUpdateStep,
            onReorderStep = onReorderStep,
            onMoveStepUp = onMoveStepUp,
            onMoveStepDown = onMoveStepDown,
            onDuplicateStep = onDuplicateStep,
            onTestStep = onTestStep,
            onLaunchInGamePalette = { onLaunchInGamePalette(it) }
        )
        return
    }

    // Otherwise show list of scenarios
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Constructeur de Scénarios",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Éditeur visuel avec glisser-déposer et conditions d'image",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("create_new_scenario_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer", fontWeight = FontWeight.Bold)
                }
            }
        }

        // In-Game Real-Time Action Palette Card (Direct Game Overlay)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .testTag("in_game_palette_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Palette d'Actions en Jeu (Direct)",
                                style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EN DIRECT", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = "Positionnez des cibles et des trajectoires de glissement directement par-dessus votre jeu pour obtenir les coordonnées exactes.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchInGamePalette(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("launch_palette_btn")
                    ) {
                        Text("Lancer", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Gesture recorder helper card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmergencyCrimson.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = EmergencyCrimson, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enregistreur de Gestes (Macro)",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Touchez directement l'écran pour enregistrer vos clics et délais sans saisie manuelle.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                    Button(
                        onClick = onStartRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("start_gesture_rec_btn")
                    ) {
                        Text("REC", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Scenario items list
        itemsIndexed(scenarios) { _, scenario ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    .testTag("scenario_card_${scenario.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scenario.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = scenario.description.ifBlank { "Aucune description" },
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp),
                                maxLines = 2
                            )
                        }

                        IconButton(onClick = { onDeleteScenario(scenario.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = EmergencyCrimson.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Metadata chips: loops, steps, executions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GunmetalCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (scenario.loopCount <= 0) "Boucle: ∞" else "Boucle: ${scenario.loopCount}x",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GunmetalCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Exécutions: ${scenario.totalExecutions}",
                                color = CyberEmerald,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { onSelectScenario(scenario) },
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("edit_scenario_${scenario.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modifier", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GestureRecorderView(
    recordedCount: Int,
    onTouch: (Float, Float) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onTouch(offset.x, offset.y)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurface)
                    .border(1.5.dp, EmergencyCrimson, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmergencyCrimson))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENREGISTREMENT EN COURS : $recordedCount clics capturés",
                        style = MaterialTheme.typography.labelMedium.copy(color = EmergencyCrimson, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Touchez n'importe où sur l'écran pour enregistrer vos points d'impact et vos délais.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Annuler")
            }

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald, contentColor = SlateSurface),
                modifier = Modifier.weight(1f).height(48.dp).testTag("finish_recording_btn")
            ) {
                Text("Générer Scénario ($recordedCount)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Visual Drag-and-Drop Scenario Editor
 */
@Composable
fun ScenarioEditorView(
    scenario: ScenarioEntity,
    steps: List<ActionStep>,
    onSave: (String, String, Int) -> Unit,
    onCancel: () -> Unit,
    onAddStep: (ActionType) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onUpdateStep: (Int, ActionStep) -> Unit,
    onReorderStep: (Int, Int) -> Unit,
    onMoveStepUp: (Int) -> Unit,
    onMoveStepDown: (Int) -> Unit,
    onDuplicateStep: (Int) -> Unit,
    onTestStep: (ActionStep) -> Unit,
    onLaunchInGamePalette: (Long) -> Unit = {}
) {
    var title by remember { mutableStateOf(scenario.title) }
    var description by remember { mutableStateOf(scenario.description) }
    var loopCount by remember { mutableIntStateOf(scenario.loopCount) }

    // Drag tracking states
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header with Save and Cancel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Éditeur de Scénario",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { onSave(title, description, loopCount) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_scenario_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sauvegarder", fontWeight = FontWeight.Bold)
                }
            }
        }

        // In-game live targeting banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2231)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎮 Poser les cibles en direct sur le jeu",
                            style = MaterialTheme.typography.titleSmall.copy(color = CyberCyan, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Affiche la palette flottante par-dessus votre jeu pour ajuster et enregistrer les coordonnées exactes.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchInGamePalette(scenario.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("editor_launch_palette_btn")
                    ) {
                        Text("Ouvrir", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Scenario metadata card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre du scénario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (loopCount <= 0) "Boucle infinie (0)" else "Nombre de répétitions : $loopCount",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { loopCount = 1 }, shape = RoundedCornerShape(6.dp)) { Text("1x", fontSize = 11.sp) }
                            OutlinedButton(onClick = { loopCount = 5 }, shape = RoundedCornerShape(6.dp)) { Text("5x", fontSize = 11.sp) }
                            OutlinedButton(onClick = { loopCount = 0 }, shape = RoundedCornerShape(6.dp)) { Text("∞", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }

        // Visual Sequencing Header & Quick Help
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Séquence d'Actions (${steps.size} étapes)",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Glissez les poignées ≡ ou utilisez ▲ ▼ pour ordonnancer les actions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }
        }

        // Drag-and-Drop Action Steps List
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            val isDragging = draggedIndex == index

            val dragModifier = Modifier.pointerInput(steps.size, index) {
                detectDragGestures(
                    onDragStart = {
                        draggedIndex = index
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y

                        val thresholdPx = 140f
                        if (dragOffsetY > thresholdPx && index < steps.size - 1) {
                            onReorderStep(index, index + 1)
                            draggedIndex = index + 1
                            dragOffsetY = 0f
                        } else if (dragOffsetY < -thresholdPx && index > 0) {
                            onReorderStep(index, index - 1)
                            draggedIndex = index - 1
                            dragOffsetY = 0f
                        }
                    },
                    onDragEnd = {
                        draggedIndex = null
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        draggedIndex = null
                        dragOffsetY = 0f
                    }
                )
            }

            Column {
                VisualStepCard(
                    step = step,
                    index = index,
                    totalSteps = steps.size,
                    isDragging = isDragging,
                    dragModifier = dragModifier,
                    onUpdate = { updated -> onUpdateStep(index, updated) },
                    onDelete = { onRemoveStep(index) },
                    onMoveUp = { onMoveStepUp(index) },
                    onMoveDown = { onMoveStepDown(index) },
                    onDuplicate = { onDuplicateStep(index) },
                    onTest = { onTestStep(step) },
                    modifier = Modifier
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                        }
                )

                // Visual flow pipe connector between consecutive steps
                if (index < steps.size - 1) {
                    FlowConnector(
                        delayMs = steps[index + 1].delayBeforeMs,
                        hasCondition = steps[index + 1].conditionType != ConditionType.ALWAYS
                    )
                }
            }
        }

        // Quick-Action Palette to Sequence New Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajouter à la séquence :",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )

                    // Row 1: Click, Swipe, Wait
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddStep(ActionType.CLICK) },
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("add_click_step")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Clic", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddStep(ActionType.SWIPE) },
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("add_swipe_step")
                        ) {
                            Icon(Icons.Default.Swipe, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Glisser", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddStep(ActionType.WAIT_DELAY) },
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("add_wait_step")
                        ) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Pause", fontSize = 11.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row 2: Condition Image (If image detected, then perform action) & Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Flow Control Block: Si [Condition Visuelle] Alors [Action] Sinon [Autre Action]
                        Button(
                            onClick = {
                                onAddStep(ActionType.BRANCH_IF_ELSE)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF261E0E)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).testTag("add_branch_if_else_step")
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Bloc Si/Alors/Sinon", fontSize = 11.sp, color = CyberAmber, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddStep(ActionType.TEXT_INPUT) },
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f).testTag("add_text_step")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFFE040FB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Texte", fontSize = 11.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual execution connector between steps (diagram flow)
 */
@Composable
fun FlowConnector(
    delayMs: Long,
    hasCondition: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .background(if (hasCondition) CyberAmber.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GunmetalCard)
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (hasCondition) CyberAmber else CyberCyan,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${delayMs}ms",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .background(if (hasCondition) CyberAmber.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f))
        )
    }
}
