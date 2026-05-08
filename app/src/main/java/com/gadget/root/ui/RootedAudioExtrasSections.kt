package com.gadget.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.gadget.audio.AudioRoutingControllerResult
import com.gadget.audio.AudioRoutingTarget
import com.gadget.audio.AudioStreamType
import com.gadget.audio.ForceRoutingConfig
import com.gadget.audio.MuteAllStreamsConfig
import com.gadget.audio.StreamVolumeBypassConfig
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_VOLUME_PERCENT = 130
private const val DEMO_VOLUME_WINDOW_MS = 30_000L
private const val DEMO_MUTE_DURATION_MS = 30_000L

/**
 * Audio routing root extras Card. Rendered inside `MicScreen` next to
 * the existing microphone-extras Card. Auto-revert of every cmd-audio
 * + audio-policy mutation on screen dispose.
 */
@Composable
fun AudioRoutingRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val audio = entryPoint.audioRoutingController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { audio.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.AudioRoutingRootExtras.disclaimer(lang))
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = S.AudioRoutingRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.AudioRoutingRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = S.AudioRoutingRootExtras.hearingSafetyWarning(lang),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                audio.bypassStreamVolumeCap(
                                    StreamVolumeBypassConfig(
                                        stream = AudioStreamType.MUSIC,
                                        percent = DEMO_VOLUME_PERCENT,
                                        activeWindowMillis = DEMO_VOLUME_WINDOW_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.AudioRoutingRootExtras.bypassVolume(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                audio.forceRouting(
                                    ForceRoutingConfig(target = AudioRoutingTarget.SPEAKER),
                                ),
                            )
                        }
                    },
                ) { Text(S.AudioRoutingRootExtras.forceRouting(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                audio.muteAllStreams(
                                    MuteAllStreamsConfig(durationMillis = DEMO_MUTE_DURATION_MS),
                                ),
                            )
                        }
                    },
                ) { Text(S.AudioRoutingRootExtras.muteAll(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(audio.dumpAudioPolicy())
                        }
                    },
                ) { Text(S.AudioRoutingRootExtras.dumpAudioPolicy(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(audio.resetAllAudioRoutingMutations())
                        }
                    },
                ) { Text(S.AudioRoutingRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(result: AudioRoutingControllerResult): String = when (result) {
    is AudioRoutingControllerResult.Ok -> result.statusNote ?: "OK"
    AudioRoutingControllerResult.Unsupported -> "Unsupported on this device"
    AudioRoutingControllerResult.OptedOut -> "Disabled — enable in Settings"
    is AudioRoutingControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is AudioRoutingControllerResult.HardwareError -> "Error: ${result.message}"
    is AudioRoutingControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is AudioRoutingControllerResult.VolumeSnapshot ->
        "Stream ${result.stream.name}: ${result.originalIndex} → ${result.appliedIndex} " +
            "(max=${result.maxIndex})"
    is AudioRoutingControllerResult.RoutingSnapshot ->
        "Routing ${result.priorTarget.name} → ${result.appliedTarget.name}"
    is AudioRoutingControllerResult.AudioDumpExcerpt ->
        "Read ${result.excerpt.length} bytes from cmd audio dump"
}
