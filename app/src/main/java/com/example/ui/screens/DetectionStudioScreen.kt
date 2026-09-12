package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ScreenAnalysisResult
import com.example.engine.DetectionResult
import com.example.engine.ImageRecognitionEngine
import com.example.model.ActionStep
import com.example.model.DetectionZone
import com.example.model.ScenarioEntity
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DetectionStudioScreen(
    zones: List<DetectionZone>,
    detectionResult: DetectionResult?,
    isScreenCaptureRunning: Boolean = false,
    screenCaptureFps: Int = 0,
    latestCapturedFrame: Bitmap? = null,
    isAiAnalyzingScreen: Boolean = false,
    latestScreenAnalysis: ScreenAnalysisResult? = null,
    onAnalyzeLiveScreen: (String) -> Unit = {},
    onApplyScenario: ((ScenarioEntity, List<ActionStep>) -> Unit)? = null,
    onRequestMediaProjection: () -> Unit = {},
    onStopMediaProjection: () -> Unit = {},
    onLaunchInteractiveSnip: () -> Unit = {},
    onCaptureRegion: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onAddCustomZone: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onTestDetection: (String, String) -> Unit = { _, _ -> },
    onTestImageDetection: (String, Float) -> Unit = { _, _ -> },
    onToggleZone: (String) -> Unit = {}
) {
    var studioTab by remember { mutableIntStateOf(0) } // 0: Vision & Écran en direct, 1: Zone de surveillance (ROI), 2: Modèles & OCR
    var selectedTemplateId by remember { mutableStateOf("ic_check") }
    var similarityThreshold by remember { mutableFloatStateOf(0.75f) }
    var testInputText by remember { mutableStateOf("Valider") }
    var testColorHex by remember { mutableStateOf("#00E5FF") }

    // Region of Interest slider values (0f..1f)
    var roiLeftPct by remember { mutableFloatStateOf(0.1f) }
    var roiTopPct by remember { mutableFloatStateOf(0.3f) }
    var roiRightPct by remember { mutableFloatStateOf(0.9f) }
    var roiBottomPct by remember { mutableFloatStateOf(0.7f) }
    var newZoneName by remember { mutableStateOf("Zone_Surveillée_1") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Column {
                Text(
                    text = "Studio Vision IA & Écran en Direct",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "MediaProjection temps réel, sélection de région (ROI) et reconnaissance de modèles (Macrorify)",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // 2. MediaProjection Real-time Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isScreenCaptureRunning) CyberEmerald.copy(alpha = 0.6f) else CyberBorder,
                        RoundedCornerShape(16.dp)
                    )
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
                                    .background(if (isScreenCaptureRunning) CyberEmerald else EmergencyCrimson)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScreenCaptureRunning) "MediaProjection ACTIF (Flux Réel)" else "MediaProjection INACTIF",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = if (isScreenCaptureRunning) CyberEmerald else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isScreenCaptureRunning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$screenCaptureFps FPS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isScreenCaptureRunning) {
                            "Flux vidéo écran capturé à faible latence sans lag. Prêt pour l'analyse IA et les déclencheurs de scénario."
                        } else {
                            "Autorisez MediaProjection pour que l'IA puisse voir l'écran de n'importe quel jeu ou application tierce en direct."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isScreenCaptureRunning) {
                            Button(
                                onClick = onRequestMediaProjection,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("btn_request_mediaprojection")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Activer MediaProjection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onStopMediaProjection,
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyCrimson, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_stop_mediaprojection")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Arrêter Flux", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Macrorify-style on-screen Snip & Region Selector tool button
                        OutlinedButton(
                            onClick = onLaunchInteractiveSnip,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("btn_interactive_screen_snip")
                        ) {
                            Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🎯 Outil Snip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Tab Navigation within Studio
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf("Écran en Direct", "Zone ROI", "Catalogue Modèles")
                tabs.forEachIndexed { index, title ->
                    val isSelected = studioTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan else Color.Transparent)
                            .clickable { studioTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) SlateSurface else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        // 4. TAB 0: LIVE SCREEN VIEWFINDER
        if (studioTab == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Radar, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (latestCapturedFrame != null) "Flux Réel de l'Appareil" else "Écran de Simulation / Test",
                                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = if (latestCapturedFrame != null) "${latestCapturedFrame.width}x${latestCapturedFrame.height} px" else "1080x2400 px",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Viewfinder Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBg)
                                .border(1.5.dp, CyberBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (latestCapturedFrame != null) {
                                val imageBmp = remember(latestCapturedFrame) { latestCapturedFrame.asImageBitmap() }
                                Image(
                                    bitmap = imageBmp,
                                    contentDescription = "Capture écran en temps réel",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // High-tech simulation screen preview
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Jeu / App Cible", color = TextMuted, fontSize = 11.sp)
                                        Text("Simulation Active", color = CyberAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Simulated button target
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CyberCyan.copy(alpha = 0.2f))
                                            .border(1.5.dp, CyberCyan, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(testInputText, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        "Lancez MediaProjection pour remplacer la simulation par l'écran réel de l'appareil",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }

                            // Dynamic Region of Interest (ROI) box outline
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(
                                        start = (roiLeftPct * 260).dp,
                                        top = (roiTopPct * 260).dp,
                                        end = ((1f - roiRightPct) * 260).dp,
                                        bottom = ((1f - roiBottomPct) * 260).dp
                                    )
                                    .border(1.5.dp, CyberCyan.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            )

                            // Detected target reticle highlight
                            if (detectionResult?.isFound == true && detectionResult.point != null) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald.copy(alpha = 0.3f))
                                        .border(2.dp, CyberEmerald, CircleShape)
                                )
                            }
                        }

                        // AI Multimodal Analysis Action
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (isScreenCaptureRunning) {
                                    onAnalyzeLiveScreen("Analyse l'écran et repère les actions clés")
                                } else {
                                    onRequestMediaProjection()
                                }
                            },
                            enabled = !isAiAnalyzingScreen,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScreenCaptureRunning) ElectricViolet else CyberCyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_vision_ai_screen_analysis")
                        ) {
                            if (isAiAnalyzingScreen) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyse multimodale Gemini en cours...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isScreenCaptureRunning) "Analyser ce flux avec Vision IA" else "Activer MediaProjection & Analyser",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // AI Analysis Result Card if present
                        if (latestScreenAnalysis != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Vision IA : ${latestScreenAnalysis.appOverview}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = ElectricViolet,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = latestScreenAnalysis.explanation,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )

                                    if (latestScreenAnalysis.detectedElements.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Éléments repérés :", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(latestScreenAnalysis.detectedElements) { elem ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(SlateSurface)
                                                        .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        "${elem.label} (${elem.x.toInt()}, ${elem.y.toInt()})",
                                                        fontSize = 10.sp,
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (onApplyScenario != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val scn = ScenarioEntity(
                                                    title = latestScreenAnalysis.suggestedScenarioTitle,
                                                    description = latestScreenAnalysis.explanation,
                                                    stepsJson = com.example.data.JsonUtils.stepsToJson(latestScreenAnalysis.suggestedSteps),
                                                    isAiControlled = true
                                                )
                                                onApplyScenario(scn, latestScreenAnalysis.suggestedSteps)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberEmerald,
                                                contentColor = SlateSurface
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(34.dp).testTag("btn_load_ai_scenario_from_studio")
                                        ) {
                                            Text(
                                                "⚡ Créer Scénario (${latestScreenAnalysis.suggestedSteps.size} étapes déduites)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (detectionResult != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (detectionResult.isFound) CyberEmerald.copy(alpha = 0.15f) else EmergencyCrimson.copy(alpha = 0.15f))
                                    .border(1.dp, if (detectionResult.isFound) CyberEmerald else EmergencyCrimson, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (detectionResult.isFound) Icons.Default.CheckCircle else Icons.Default.Radar,
                                            contentDescription = null,
                                            tint = if (detectionResult.isFound) CyberEmerald else EmergencyCrimson,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (detectionResult.isFound) "CIBLE TROUVÉE" else "AUCUN RÉSULTAT",
                                            color = if (detectionResult.isFound) CyberEmerald else EmergencyCrimson,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = detectionResult.message,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. TAB 1: REAL-TIME REGION OF INTEREST (ROI) & SNIP
        if (studioTab == 1) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CropFree, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Définir la Zone d'Écran à Surveiller (ROI)",
                                style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "Surveiller une région précise accélère la détection par 10x et évite les fausses détections (modèle Macrorify).",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // ROI Sliders
                        Text("Bord Gauche (${(roiLeftPct * 100).toInt()}%)", color = TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = roiLeftPct,
                            onValueChange = { if (it < roiRightPct - 0.05f) roiLeftPct = it },
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )

                        Text("Bord Droit (${(roiRightPct * 100).toInt()}%)", color = TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = roiRightPct,
                            onValueChange = { if (it > roiLeftPct + 0.05f) roiRightPct = it },
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )

                        Text("Bord Supérieur (${(roiTopPct * 100).toInt()}%)", color = TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = roiTopPct,
                            onValueChange = { if (it < roiBottomPct - 0.05f) roiTopPct = it },
                            colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                        )

                        Text("Bord Inférieur (${(roiBottomPct * 100).toInt()}%)", color = TextSecondary, fontSize = 11.sp)
                        Slider(
                            value = roiBottomPct,
                            onValueChange = { if (it > roiTopPct + 0.05f) roiBottomPct = it },
                            colors = SliderDefaults.colors(thumbColor = CyberEmerald, activeTrackColor = CyberEmerald)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newZoneName,
                            onValueChange = { newZoneName = it },
                            label = { Text("Nom de la Zone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCaptureRegion(newZoneName, roiLeftPct, roiTopPct, roiRightPct, roiBottomPct)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📸 Capturer Zone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onAddCustomZone(newZoneName, roiLeftPct, roiTopPct, roiRightPct, roiBottomPct)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald, contentColor = SlateSurface),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("👁️ Activer Veille", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Active Zones List
                Spacer(modifier = Modifier.height(8.dp))
                Text("Zones Définies", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
            }

            items(zones) { zone ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GunmetalCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (zone.isActive) CyberCyan.copy(alpha = 0.5f) else CyberBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(zone.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "X: ${(zone.leftPct * 100).toInt()}%..${(zone.rightPct * 100).toInt()}% | Y: ${(zone.topPct * 100).toInt()}%..${(zone.bottomPct * 100).toInt()}%",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = zone.isActive,
                            onCheckedChange = { onToggleZone(zone.id) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // 6. TAB 2: TEMPLATE CATALOG & TEST
        if (studioTab == 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modèles d'Images & Icônes (Vision IA)",
                                style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Template selector row
                        val allTemplates = ImageRecognitionEngine.getAllTemplates()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allTemplates) { t ->
                                val isSelected = selectedTemplateId == t.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else GunmetalCard)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) CyberCyan else CyberBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedTemplateId = t.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(t.name, color = if (isSelected) CyberCyan else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(t.category, color = TextMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Threshold slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Seuil de Similarité", color = TextSecondary, fontSize = 11.sp)
                            Text("${(similarityThreshold * 100).toInt()}%", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = similarityThreshold,
                            onValueChange = { similarityThreshold = it },
                            valueRange = 0.50f..0.95f,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onTestImageDetection(selectedTemplateId, similarityThreshold)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SlateSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_test_image_matching")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rechercher le Modèle sur l'Écran (Temps Réel)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
