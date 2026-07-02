package com.gadget.root.ui

import dev.ranzlappen.gadget.core.root.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.feature.camera.control.CameraControllerResult
import dev.ranzlappen.gadget.feature.camera.control.HighFpsConfig
import dev.ranzlappen.gadget.feature.camera.control.ManualExposureConfig
import dev.ranzlappen.gadget.feature.camera.control.MultiCameraConfig
import dev.ranzlappen.gadget.feature.camera.control.RawCaptureConfig
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.microphone.DirectPcmConfig
import com.gadget.microphone.GainBoostConfig
import com.gadget.microphone.MicrophoneControllerResult
import com.gadget.microphone.MultiMicConfig
import kotlinx.coroutines.launch

private const val DEMO_HIGH_FPS = 120
private const val DEMO_HIGH_FPS_DURATION_MS = 5_000L
private const val DEMO_MANUAL_ISO = 800
private const val DEMO_MANUAL_EXPOSURE_NANOS = 1_000_000L
private const val DEMO_RAW_FRAME_COUNT = 3
private const val DEMO_MULTI_CAM_DURATION_MS = 8_000L
private const val DEMO_MIC_GAIN_DB = 18
private const val DEMO_MIC_GAIN_DURATION_MS = 10_000L
private const val DEMO_MIC_PCM_SAMPLE_RATE = 48_000
private const val DEMO_MIC_PCM_CHANNELS = 1
private const val DEMO_MIC_PCM_BITS = 16
private const val DEMO_MIC_PCM_DURATION_MS = 5_000L
private const val DEMO_MULTI_MIC_DURATION_MS = 10_000L
private const val DEMO_SYSTEM_AUDIO_DURATION_MS = 30_000L

/**
 * Floating button + dialog for the Camera "Root extras" surface. Hidden
 * when root is not granted. Used inside the existing fullscreen camera
 * Box overlay so the camera preview's layout stays untouched.
 */
@Composable
fun CameraRootExtrasButton(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    var open by remember { mutableStateOf(false) }
    SmallFloatingActionButton(
        onClick = { open = true },
        modifier = modifier,
    ) {
        Text("R", fontWeight = FontWeight.Bold)
    }
    if (open) {
        CameraExtrasDialog(onDismiss = { open = false })
    }
}

@Composable
private fun CameraExtrasDialog(onDismiss: () -> Unit) {
    val entryPoint = rememberRootFeatures()
    val camera = entryPoint.cameraController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera root extras") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Direct Camera2 / HAL access. Each call is rate-limited and " +
                        "requires the matching feature to be enabled in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = S.RootExtrasGenericDisclaimer.text(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(
                                camera.highFpsCapture(
                                    HighFpsConfig(
                                        cameraId = "0",
                                        fps = DEMO_HIGH_FPS,
                                        durationMillis = DEMO_HIGH_FPS_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("High-FPS capture (${DEMO_HIGH_FPS}fps)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(
                                camera.manualOverride(
                                    ManualExposureConfig(
                                        cameraId = "0",
                                        isoSensitivity = DEMO_MANUAL_ISO,
                                        exposureTimeNanos = DEMO_MANUAL_EXPOSURE_NANOS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("Manual ISO=$DEMO_MANUAL_ISO + 1ms shutter") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(
                                camera.rawCapture(
                                    RawCaptureConfig(cameraId = "0", frameCount = DEMO_RAW_FRAME_COUNT),
                                ),
                            )
                        }
                    },
                ) { Text("RAW DNG capture (${DEMO_RAW_FRAME_COUNT} frames)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(
                                camera.multiCameraCapture(
                                    MultiCameraConfig(
                                        cameraIds = listOf("0", "1"),
                                        durationMillis = DEMO_MULTI_CAM_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("Multi-camera concurrent capture") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(camera.halBypassFrame())
                        }
                    },
                ) { Text("HAL bypass probe (best-effort)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeCameraResult(camera.setShutterSoundEnabled(false))
                        }
                    },
                ) { Text("Silence shutter sound") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/**
 * Card for the Microphone "Root extras" surface. Fits inside MicScreen's
 * vertically scrolling Column, mirroring the sibling `Rooted*ExtrasSection`
 * cards.
 */
@Composable
fun MicrophoneRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val mic = entryPoint.microphoneController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    RootExtrasDisclaimerCard(text = S.RootExtrasGenericDisclaimer.text(lang))
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Root extras (Microphone)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Direct ALSA mixer + tinycap PCM. Hard caps on duration; " +
                    "system-audio capture requires explicit legal-warning confirm.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeMicResult(
                            mic.gainBoost(
                                GainBoostConfig(
                                    boostDb = DEMO_MIC_GAIN_DB,
                                    durationMillis = DEMO_MIC_GAIN_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Gain boost +${DEMO_MIC_GAIN_DB}dB for ${DEMO_MIC_GAIN_DURATION_MS / 1000}s") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeMicResult(
                            mic.directPcm(
                                DirectPcmConfig(
                                    sampleRate = DEMO_MIC_PCM_SAMPLE_RATE,
                                    channelCount = DEMO_MIC_PCM_CHANNELS,
                                    bitsPerSample = DEMO_MIC_PCM_BITS,
                                    durationMillis = DEMO_MIC_PCM_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Direct PCM read (5s)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeMicResult(
                            mic.multiMicRaw(MultiMicConfig(durationMillis = DEMO_MULTI_MIC_DURATION_MS)),
                        )
                    }
                },
            ) { Text("Multi-mic raw capture") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeMicResult(mic.disableEffects())
                    }
                },
            ) { Text("Disable NS/AGC/AEC (snapshot+restore)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeMicResult(mic.systemAudioCapture(DEMO_SYSTEM_AUDIO_DURATION_MS))
                    }
                },
            ) { Text("System audio capture (legal-warning gated)") }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun describeCameraResult(result: CameraControllerResult): String = when (result) {
    CameraControllerResult.Ok -> "OK"
    CameraControllerResult.Unsupported -> "Unsupported on this device"
    CameraControllerResult.OptedOut -> "Disabled — enable in Settings"
    is CameraControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is CameraControllerResult.HardwareError -> "Hardware error: ${result.message}"
}

private fun describeMicResult(result: MicrophoneControllerResult): String = when (result) {
    MicrophoneControllerResult.Ok -> "OK"
    MicrophoneControllerResult.Unsupported -> "Unsupported on this device"
    MicrophoneControllerResult.OptedOut -> "Disabled — enable in Settings"
    is MicrophoneControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is MicrophoneControllerResult.HardwareError -> "Hardware error: ${result.message}"
}
