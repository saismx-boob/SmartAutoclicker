package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionStep
import com.example.model.AiMemoryEntity
import com.example.model.ScenarioEntity
import com.example.ui.ChatMessage
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
fun AiCopilotScreen(
    chatMessages: List<ChatMessage>,
    memories: List<AiMemoryEntity>,
    isAiGenerating: Boolean,
    isAiAnalyzingScreen: Boolean = false,
    isScreenCaptureRunning: Boolean = false,
    onRequestMediaProjection: () -> Unit = {},
    onAnalyzeLiveScreen: (String) -> Unit = {},
    onSendGoal: (String) -> Unit,
    onApplyScenario: (ScenarioEntity, List<ActionStep>) -> Unit,
    onClearMemories: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var userPromptText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // AI Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricViolet.copy(alpha = 0.2f))
                        .border(1.dp, ElectricViolet, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Studio IA Autonome",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Vision intelligente, résolution d'objectifs & mémoire adaptative",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateSurface,
                contentColor = CyberCyan,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Assistant & Objectifs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Mémoire d'Expérience (${memories.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        if (selectedTab == 0) {
            // Chat & Goal Planner
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // MediaProjection Vision Status & Quick Capture Header Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isScreenCaptureRunning) CyberEmerald.copy(alpha = 0.5f) else CyberBorder,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isScreenCaptureRunning) CyberEmerald else EmergencyCrimson)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isScreenCaptureRunning) "Vision MediaProjection ACTIVE" else "Vision MediaProjection INACTIVE",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (isScreenCaptureRunning) CyberEmerald else TextSecondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    if (!isScreenCaptureRunning) {
                                        TextButton(
                                            onClick = onRequestMediaProjection,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Activer flux", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isScreenCaptureRunning) {
                                        "L'IA peut capturer et analyser l'écran de n'importe quelle app pour générer automatiquement vos macros."
                                    } else {
                                        "Activez MediaProjection pour que l'IA puisse voir l'écran réel de votre appareil et repérer les boutons."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (!isScreenCaptureRunning) {
                                            onRequestMediaProjection()
                                        } else {
                                            onAnalyzeLiveScreen(userPromptText)
                                            userPromptText = ""
                                        }
                                    },
                                    enabled = !isAiAnalyzingScreen && !isAiGenerating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isScreenCaptureRunning) ElectricViolet else CyberCyan,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("btn_analyze_live_screen")
                                ) {
                                    if (isAiAnalyzingScreen) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Analyse de l'écran en cours...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = if (isScreenCaptureRunning) Icons.Default.CameraAlt else Icons.Default.Videocam,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isScreenCaptureRunning) "📸 Analyser l'écran actuel avec l'IA" else "Activer MediaProjection & Analyser",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Predefined Goal Prompts suggestion chips
                    item {
                        Text(
                            text = "Suggestions d'objectifs en langage naturel :",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GoalSuggestionChip(
                                text = "🛒 Réclamer & confirmer",
                                onClick = { userPromptText = "Surveille l'écran et clique sur 'Réclamer', attends 1s puis clique sur 'Confirmer'" }
                            )
                            GoalSuggestionChip(
                                text = "📸 Scanner écran actif",
                                onClick = {
                                    if (isScreenCaptureRunning) {
                                        onAnalyzeLiveScreen("Identifie les boutons interactifs et prépare un parcours")
                                    } else {
                                        onRequestMediaProjection()
                                    }
                                }
                            )
                        }
                    }

                    items(chatMessages) { message ->
                        ChatMessageItem(
                            message = message,
                            onApplyScenario = onApplyScenario
                        )
                    }

                    if (isAiGenerating) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.border(1.dp, ElectricViolet.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricViolet, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("L'IA conçoit le scénario optimal...", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                }
                            }
                        }
                    }
                }

                // Input bar fixed at bottom
                Surface(
                    color = SlateSurface,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userPromptText,
                            onValueChange = { userPromptText = it },
                            placeholder = { Text("Donnez un objectif à l'IA...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).testTag("ai_goal_input")
                        )

                        Button(
                            onClick = {
                                val text = userPromptText
                                userPromptText = ""
                                onSendGoal(text)
                            },
                            enabled = userPromptText.isNotBlank() && !isAiGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(50.dp).testTag("send_ai_goal_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Envoyer", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            // AI Memory View
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Règles & Adaptations Apprises",
                            style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onClearMemories) {
                            Text("Effacer la mémoire", color = EmergencyCrimson, fontSize = 12.sp)
                        }
                    }
                }

                items(memories) { memory ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = memory.goal,
                                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (memory.outcome == "SUCCESS") CyberEmerald.copy(alpha = 0.2f) else EmergencyCrimson.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = memory.outcome,
                                        color = if (memory.outcome == "SUCCESS") CyberEmerald else EmergencyCrimson,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 Règle apprise : ${memory.learnedRule}",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberCyan, fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Action menée : ${memory.actionDecision}",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalSuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = CyberCyan, fontSize = 11.sp)
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onApplyScenario: (ScenarioEntity, List<ActionStep>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) GunmetalCard else SlateSurface
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (message.isUser) 14.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 14.dp
            ),
            modifier = Modifier
                .fillMaxWidth(if (message.isUser) 0.85f else 0.95f)
                .border(
                    1.dp,
                    if (message.isUser) CyberCyan.copy(alpha = 0.4f) else CyberBorder,
                    RoundedCornerShape(14.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AutoClick AI", style = MaterialTheme.typography.labelSmall.copy(color = ElectricViolet, fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )

                // Captured Frame Image Preview if present
                if (message.capturedFrameBitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Image écran capturée (MediaProjection)", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${message.capturedFrameBitmap.width}x${message.capturedFrameBitmap.height} px",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val imageBmp = remember(message.capturedFrameBitmap) { message.capturedFrameBitmap.asImageBitmap() }
                            Image(
                                bitmap = imageBmp,
                                contentDescription = "Capture écran analysée par l'IA",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Detected Elements breakdown
                if (message.analysisResult != null && message.analysisResult.detectedElements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cibles détectées sur l'écran :", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.analysisResult.detectedElements.forEach { elem ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ObsidianBg)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${elem.label} (${elem.x.toInt()}, ${elem.y.toInt()})",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (message.generatedScenario != null && message.generatedSteps != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, CyberEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📋 ${message.generatedScenario.title}",
                                style = MaterialTheme.typography.titleSmall.copy(color = CyberEmerald, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${message.generatedSteps.size} étapes configurées avec détection et délais humains",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onApplyScenario(message.generatedScenario, message.generatedSteps) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald, contentColor = SlateSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("apply_ai_scenario_btn")
                            ) {
                                Text("Ouvrir dans l'Éditeur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
