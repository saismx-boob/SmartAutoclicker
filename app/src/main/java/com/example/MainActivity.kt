package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.engine.EngineStatus
import com.example.service.FloatingOverlayService
import com.example.ui.AutomationViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.EthicsDisclaimerDialog
import com.example.ui.components.FloatingReticleHud
import com.example.ui.components.TopBar
import com.example.ui.screens.AiCopilotScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetectionStudioScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.ScenarioBuilderScreen
import com.example.ui.theme.AutoClickAiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AutomationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleEmergencyIntent(intent)

        setContent {
            AutoClickAiTheme {
                val scenarios by viewModel.scenarios.collectAsState()
                val logs by viewModel.logs.collectAsState()
                val memories by viewModel.memories.collectAsState()
                val executionState by viewModel.executionState.collectAsState()
                val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
                val isOverlayActive by viewModel.isOverlayActive.collectAsState()
                val currentNavIndex by viewModel.currentNavIndex.collectAsState()
                val isDryRun by viewModel.isGlobalDryRun.collectAsState()
                val showEthicsModal by viewModel.showEthicsModal.collectAsState()

                val editingScenario by viewModel.editingScenario.collectAsState()
                val editingSteps by viewModel.editingSteps.collectAsState()
                val isRecording by viewModel.isRecordingGestures.collectAsState()
                val recordedCount by viewModel.recordedCount.collectAsState()

                val chatMessages by viewModel.chatMessages.collectAsState()
                val isAiGenerating by viewModel.isAiGenerating.collectAsState()
                val isAiAnalyzingScreen by viewModel.isAiAnalyzingScreen.collectAsState()
                val latestScreenAnalysis by viewModel.latestScreenAnalysis.collectAsState()

                val detectionZones by viewModel.detectionZones.collectAsState()
                val liveDetectionResult by viewModel.liveDetectionResult.collectAsState()

                val isScreenCaptureRunning by viewModel.isScreenCaptureRunning.collectAsState()
                val screenCaptureFps by viewModel.screenCaptureFps.collectAsState()
                val latestCapturedFrame by viewModel.latestCapturedFrame.collectAsState()

                val mediaProjectionManager = remember {
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                }

                val mediaProjectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        viewModel.startMediaProjection(result.resultCode, result.data!!)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopBar(
                            isAccessibilityActive = isAccessibilityActive,
                            isDryRun = isDryRun,
                            executionState = executionState,
                            onEmergencyStop = { viewModel.triggerEmergencyStop() },
                            onOpenAccessibility = { viewModel.openAccessibilitySettings() },
                            onToggleDryRun = { viewModel.toggleGlobalDryRun() },
                            onOpenEthics = { viewModel.setEthicsModalVisible(true) }
                        )
                    },
                    bottomBar = {
                        BottomNavBar(
                            currentIndex = currentNavIndex,
                            onSelectTab = { viewModel.setNavIndex(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentNavIndex) {
                            0 -> DashboardScreen(
                                scenarios = scenarios,
                                executionState = executionState,
                                isAccessibilityActive = isAccessibilityActive,
                                isOverlayActive = isOverlayActive,
                                isDryRun = isDryRun,
                                onRunScenario = { sc, dry -> viewModel.runScenario(sc, dry) },
                                onEmergencyStop = { viewModel.triggerEmergencyStop() },
                                onOpenAccessibility = { viewModel.openAccessibilitySettings() },
                                onOpenOverlaySettings = { viewModel.openOverlaySettings() },
                                onToggleOverlay = { viewModel.toggleFloatingOverlay() },
                                onNavigateToBuilder = {
                                    viewModel.createNewScenario()
                                    viewModel.setNavIndex(1)
                                },
                                onNavigateToAi = { viewModel.setNavIndex(3) },
                                onStartRecording = {
                                    viewModel.startGestureRecording()
                                    viewModel.setNavIndex(1)
                                },
                                onSelectScenarioForEdit = { sc ->
                                    viewModel.selectScenarioForEdit(sc)
                                    viewModel.setNavIndex(1)
                                }
                            )

                            1 -> ScenarioBuilderScreen(
                                scenarios = scenarios,
                                editingScenario = editingScenario,
                                editingSteps = editingSteps,
                                isRecording = isRecording,
                                recordedCount = recordedCount,
                                onSelectScenario = { viewModel.selectScenarioForEdit(it) },
                                onCreateNew = { viewModel.createNewScenario() },
                                onDeleteScenario = { viewModel.deleteScenario(it) },
                                onSaveScenario = { title, desc, loops -> viewModel.saveEditingScenario(title, desc, loops) },
                                onCancelEdit = { viewModel.cancelEditing() },
                                onAddStep = { viewModel.addStepToEditing(it) },
                                onRemoveStep = { viewModel.removeStepFromEditing(it) },
                                onUpdateStep = { idx, st -> viewModel.updateStep(idx, st) },
                                onReorderStep = { from, to -> viewModel.reorderSteps(from, to) },
                                onMoveStepUp = { viewModel.moveStepUp(it) },
                                onMoveStepDown = { viewModel.moveStepDown(it) },
                                onDuplicateStep = { viewModel.duplicateStep(it) },
                                onTestStep = { viewModel.testSingleStep(it) },
                                onLaunchInGamePalette = { scenarioId ->
                                    if (FloatingOverlayService.canDrawOverlays(this@MainActivity)) {
                                        FloatingOverlayService.launchActionPalette(this@MainActivity, scenarioId)
                                        moveTaskToBack(true)
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:$packageName")
                                                )
                                            )
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Activez l'autorisation de superposition pour afficher la palette en jeu",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onStartRecording = { viewModel.startGestureRecording() },
                                onRecordTouch = { x, y -> viewModel.recordTouchPoint(x, y) },
                                onFinishRecording = { viewModel.finishRecordingAndGenerate() },
                                onCancelRecording = { viewModel.cancelRecording() }
                            )

                            2 -> DetectionStudioScreen(
                                zones = detectionZones,
                                detectionResult = liveDetectionResult,
                                isScreenCaptureRunning = isScreenCaptureRunning,
                                screenCaptureFps = screenCaptureFps,
                                latestCapturedFrame = latestCapturedFrame,
                                isAiAnalyzingScreen = isAiAnalyzingScreen,
                                latestScreenAnalysis = latestScreenAnalysis,
                                onAnalyzeLiveScreen = { prompt ->
                                    viewModel.analyzeLiveScreenWithAi(
                                        userInstruction = prompt,
                                        onNeedMediaProjection = {
                                            try {
                                                mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
                                                    mediaProjectionLauncher.launch(intent)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    )
                                },
                                onApplyScenario = { sc, steps -> viewModel.applyAiGeneratedScenario(sc, steps) },
                                onRequestMediaProjection = {
                                    try {
                                        mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
                                            mediaProjectionLauncher.launch(intent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onStopMediaProjection = { viewModel.stopMediaProjection() },
                                onLaunchInteractiveSnip = { viewModel.launchInteractiveRegionSelector() },
                                onCaptureRegion = { name, l, t, r, b -> viewModel.captureAndSaveRegion(name, l, t, r, b) },
                                onAddCustomZone = { name, l, t, r, b -> viewModel.addCustomZone(name, l, t, r, b) },
                                onTestDetection = { text, color -> viewModel.testDetection(text, color) },
                                onTestImageDetection = { tId, thresh -> viewModel.testImageDetection(tId, thresh) },
                                onToggleZone = { viewModel.toggleZoneActive(it) }
                            )

                            3 -> AiCopilotScreen(
                                chatMessages = chatMessages,
                                memories = memories,
                                isAiGenerating = isAiGenerating,
                                isAiAnalyzingScreen = isAiAnalyzingScreen,
                                isScreenCaptureRunning = isScreenCaptureRunning,
                                onRequestMediaProjection = {
                                    try {
                                        mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
                                            mediaProjectionLauncher.launch(intent)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onAnalyzeLiveScreen = { prompt ->
                                    viewModel.analyzeLiveScreenWithAi(
                                        userInstruction = prompt,
                                        onNeedMediaProjection = {
                                            try {
                                                mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
                                                    mediaProjectionLauncher.launch(intent)
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    )
                                },
                                onSendGoal = { viewModel.sendUserGoalToAi(it) },
                                onApplyScenario = { sc, steps -> viewModel.applyAiGeneratedScenario(sc, steps) },
                                onClearMemories = { viewModel.clearMemories() }
                            )

                            4 -> LogsScreen(
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                        }

                        // Floating In-App Reticle & HUD controller
                        FloatingReticleHud(
                            executionState = executionState,
                            isOverlayActive = isOverlayActive,
                            onPause = { viewModel.pauseExecution() },
                            onResume = { viewModel.resumeExecution() },
                            onEmergencyStop = { viewModel.triggerEmergencyStop() }
                        )

                        // Responsible Usage & Security Dialog
                        if (showEthicsModal) {
                            EthicsDisclaimerDialog(
                                onDismiss = { viewModel.setEthicsModalVisible(false) }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleEmergencyIntent(intent)
    }

    private fun handleEmergencyIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("TRIGGER_EMERGENCY_STOP", false) == true) {
            viewModel.triggerEmergencyStop()
        }
    }

    /**
     * Hardware emergency stop: pressing Volume Down or Volume Up during active execution
     * triggers instant stop!
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (viewModel.executionState.value.status == EngineStatus.RUNNING) {
                viewModel.triggerEmergencyStop()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
