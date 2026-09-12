package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActiveExecutionState
import com.example.engine.EngineStatus
import com.example.data.JsonUtils
import com.example.model.ActionStep
import com.example.model.ScenarioEntity
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    scenarios: List<ScenarioEntity>,
    executionState: ActiveExecutionState,
    isAccessibilityActive: Boolean,
    isOverlayActive: Boolean,
    isDryRun: Boolean,
    onRunScenario: (ScenarioEntity, Boolean) -> Unit,
    onEmergencyStop: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onToggleOverlay: () -> Unit = onOpenOverlaySettings,
    onNavigateToBuilder: () -> Unit,
    onNavigateToAi: () -> Unit,
    onStartRecording: () -> Unit,
    onSelectScenarioForEdit: (ScenarioEntity) -> Unit = {}
) {
    val totalExecutions = scenarios.sumOf { it.totalExecutions }
    val totalSuccess = scenarios.sumOf { it.successExecutions }
    val successRate = if (totalExecutions > 0) (totalSuccess * 100) / totalExecutions else 98
    var isNoviceGuideExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Guide Démarrage Facile pour Novices (3 étapes simples)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141926)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isNoviceGuideExpanded = !isNoviceGuideExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Guide Débutant : 3 Étapes Simples",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Comment automatiser vos actions en 1 minute",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { isNoviceGuideExpanded = !isNoviceGuideExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isNoviceGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        }
                    }

                    if (isNoviceGuideExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Étape 1 : Accessibilité
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAccessibilityActive) Color(0xFF0F261C) else Color(0xFF261214))
                                .border(
                                    1.dp,
                                    if (isAccessibilityActive) CyberEmerald.copy(alpha = 0.4f) else EmergencyCrimson.copy(alpha = 0.4f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccessibilityActive) CyberEmerald else EmergencyCrimson),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAccessibilityActive) "Service d'accessibilité prêt" else "1. Activer le service Android",
                                    color = if (isAccessibilityActive) CyberEmerald else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAccessibilityActive) "Autorisation accordée pour cliquer sans root" else "Indispensable pour effectuer les clics à l'écran",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            if (!isAccessibilityActive) {
                                Button(
                                    onClick = onOpenAccessibility,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Activer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Étape 2 : Choisir ou Créer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurface)
                                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "2. Choisir ou créer un scénario",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sélectionnez un modèle prêt ci-dessous ou cliquez sur Créer",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            OutlinedButton(
                                onClick = onNavigateToBuilder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("+ Créer", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Étape 3 : Bouton Flottant / Lancer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOverlayActive) Color(0xFF0C2433) else SlateSurface)
                                .border(
                                    1.dp,
                                    if (isOverlayActive) CyberCyan.copy(alpha = 0.5f) else CyberBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(CyberAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("3", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "3. Bouton Flottant (Par-dessus vos apps)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOverlayActive) "Bouton actif ! Ouvrez n'importe quel jeu ou app" else "Lancez le bouton pour démarrer vos macros par-dessus vos apps",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = onToggleOverlay,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverlayActive) EmergencyCrimson else CyberCyan,
                                    contentColor = if (isOverlayActive) Color.White else SlateSurface
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (isOverlayActive) "Masquer" else "Lancer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // 1. Accessibility Service Warning Banner (if inactive)
        if (!isAccessibilityActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmergencyCrimson.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(1.dp, EmergencyCrimson.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = EmergencyCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Service d'accessibilité inactif",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = EmergencyCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Requis pour effectuer les clics et gestes réels sans root.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        Button(
                            onClick = onOpenAccessibility,
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("activate_service_button")
                        ) {
                            Text("Activer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Floating Action Button Overlay (HUD par-dessus les autres apps)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isOverlayActive) CyberCyan else CyberBorder,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverlayActive) CyberCyan.copy(alpha = 0.2f)
                                else GunmetalCard
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (isOverlayActive) CyberCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Bouton Flottant (Overlay)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (isOverlayActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIF", color = CyberEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = if (isOverlayActive) "Actif par-dessus les autres applications. Glissez et touchez le bouton pour démarrer/arrêter."
                            else "Affiche un bouton persistant au-dessus de vos jeux & apps pour contrôler vos macros.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                        )
                    }
                    Button(
                        onClick = onToggleOverlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayActive) EmergencyCrimson else CyberCyan,
                            contentColor = if (isOverlayActive) Color.White else SlateSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("toggle_floating_overlay_btn")
                    ) {
                        Text(
                            text = if (isOverlayActive) "Désactiver" else "Lancer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Active Execution Card (if running)
        if (executionState.status == EngineStatus.RUNNING || executionState.status == EngineStatus.PAUSED) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyberCyan, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EN COURS D'EXÉCUTION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberEmerald,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            if (executionState.isDryRun) {
                                Text(
                                    text = "TEST À BLANC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyberAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = executionState.scenarioTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Étape ${executionState.currentStepNumber}/${executionState.totalSteps} : ${executionState.currentStepName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (executionState.totalSteps > 0)
                                    executionState.currentStepNumber.toFloat() / executionState.totalSteps.toFloat()
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = GunmetalCard
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cycle ${executionState.currentLoop}/${executionState.totalLoops}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                            Button(
                                onClick = onEmergencyStop,
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("dash_emergency_stop")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Arrêt immédiat", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Actions faites",
                    value = "$totalExecutions",
                    icon = Icons.Default.TouchApp,
                    tint = CyberCyan
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Taux de succès",
                    value = "$successRate%",
                    icon = Icons.Default.Speed,
                    tint = CyberEmerald
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Temps sauvé",
                    value = "${(totalExecutions * 4) / 60}h",
                    icon = Icons.Default.FlashOn,
                    tint = ElectricViolet
                )
            }
        }

        // 4. Quick Actions Row (Record Gestures, AI Goal, Floating Overlay)
        item {
            Text(
                text = "Actions Rapides & Intelligence",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Macro recorder card
                Card(
                    onClick = onStartRecording,
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                        .testTag("record_macro_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmergencyCrimson.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = EmergencyCrimson, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Enregistrer Gestes", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        Text("Mode apprentissage", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                    }
                }

                // AI Copilot generator card
                Card(
                    onClick = onNavigateToAi,
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .testTag("ai_assistant_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Objectif par IA", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                        Text("Langage naturel", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp))
                    }
                }
            }
        }

        // 5. Scenarios Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scénarios Prêts à l'Emploi (${scenarios.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                TextButton(onClick = onNavigateToBuilder) {
                    Text("+ Nouveau", color = CyberCyan, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 6. List of scenarios
        items(scenarios) { scenario ->
            ScenarioCard(
                scenario = scenario,
                isDryRun = isDryRun,
                onRun = { onRunScenario(scenario, false) },
                onDryRun = { onRunScenario(scenario, true) },
                onEdit = { onSelectScenarioForEdit(scenario) }
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier.border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun ScenarioCard(
    scenario: ScenarioEntity,
    isDryRun: Boolean,
    onRun: () -> Unit,
    onDryRun: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val steps: List<ActionStep> = remember(scenario.stepsJson) {
        JsonUtils.jsonToSteps(scenario.stepsJson)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scenario.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 2
                    )
                }

                if (scenario.isAiControlled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricViolet.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "IA",
                            color = ElectricViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Steps overview badge for novices
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B2230))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${steps.size} étape${if (steps.size > 1) "s" else ""}",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // If scenario has flow control/branching, show a badge
                val hasBranch = steps.any { it.actionType == com.example.model.ActionType.BRANCH_IF_ELSE }
                if (hasBranch) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberAmber.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🔀 Décision Si/Alors",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = scenario.scheduleDescription,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Edit button
                    OutlinedButton(
                        onClick = onEdit,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("edit_scenario_btn_${scenario.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Modifier", fontSize = 11.sp)
                    }

                    // Test à blanc button
                    OutlinedButton(
                        onClick = onDryRun,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("dry_run_btn_${scenario.id}")
                    ) {
                        Text("Test", fontSize = 11.sp)
                    }

                    // Real run button
                    Button(
                        onClick = onRun,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp).testTag("run_scenario_btn_${scenario.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lancer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lancer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
