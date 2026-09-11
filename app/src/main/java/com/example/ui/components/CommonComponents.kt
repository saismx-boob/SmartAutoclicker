package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActiveExecutionState
import com.example.engine.EngineStatus
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TopBar(
    isAccessibilityActive: Boolean,
    isDryRun: Boolean,
    executionState: ActiveExecutionState,
    onEmergencyStop: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onToggleDryRun: () -> Unit,
    onOpenEthics: () -> Unit
) {
    Surface(
        color = SlateSurface,
        modifier = Modifier.fillMaxWidth().shadow(6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AutoClick AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isDryRun) "Mode Test à Blanc" else "Mode Réel Humain",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDryRun) CyberCyan else CyberEmerald,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ethics / Info button
                    IconButton(
                        onClick = onOpenEthics,
                        modifier = Modifier.size(40.dp).testTag("ethics_info_button")
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Charte d'usage responsable",
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Emergency stop button
                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("emergency_stop_button")
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "Arrêt d'Urgence",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "STOP",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Sub-status row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Accessibility Service Status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isAccessibilityActive) CyberEmerald.copy(alpha = 0.15f)
                            else EmergencyCrimson.copy(alpha = 0.15f)
                        )
                        .clickable(onClick = onOpenAccessibility)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAccessibilityActive) CyberEmerald else EmergencyCrimson)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAccessibilityActive) "Service Accessibilité Actif" else "Activer Service (Sans Root)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isAccessibilityActive) CyberEmerald else EmergencyCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Dry run toggle button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberBorder.copy(alpha = 0.4f))
                        .clickable(onClick = onToggleDryRun)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isDryRun) "🧪 Test à blanc: ON" else "⚡ Action réelle: ON",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDryRun) CyberCyan else TextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingReticleHud(
    executionState: ActiveExecutionState,
    isOverlayActive: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    // Draggable position state
    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(160f) }

    AnimatedVisibility(
        visible = executionState.status == EngineStatus.RUNNING || executionState.status == EngineStatus.PAUSED
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianBg.copy(alpha = 0.92f))
                .border(1.5.dp, CyberCyan, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing execution dot
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (executionState.status == EngineStatus.RUNNING) scale else 1f)
                            .clip(CircleShape)
                            .background(if (executionState.status == EngineStatus.RUNNING) CyberEmerald else Color.Yellow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = executionState.scenarioTitle.take(22),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Étape ${executionState.currentStepNumber}/${executionState.totalSteps} : ${executionState.currentStepName}",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 11.sp)
                )

                if (executionState.isDryRun) {
                    Text(
                        text = "🔬 SIMULATION (Test à blanc)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Yellow, fontSize = 10.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (executionState.status == EngineStatus.RUNNING) {
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(containerColor = GunmetalCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("resume_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reprendre", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reprendre", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onEmergencyStop,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("stop_hud_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Arrêter", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EthicsDisclaimerDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sécurité & Usage Responsable", color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AutoClick AI est un outil d'automatisation personnelle moderne conçu pour éliminer les gestes répétitifs du quotidien.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
                Text(
                    text = "🔒 100% Sans Root ni Manipulation Intrusive :\nL'application s'appuie exclusivement sur l'API Accessibility officielle d'Android pour agir comme le ferait un humain bienveillant.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberEmerald)
                )
                Text(
                    text = "⚖️ Charte Éthique :\n- À utiliser dans le respect des conditions d'utilisation des services tiers.\n- Ne pas utiliser pour du spamming abusif ou des comportements malveillants.\n- Le bouton d'arrêt d'urgence reste accessible en permanence pour neutraliser toute action instantanément.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface)
            ) {
                Text("J'ai compris & J'accepte", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SlateSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
