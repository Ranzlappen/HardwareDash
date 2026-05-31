package com.gadget.root.ui

import dev.ranzlappen.gadget.core.root.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsControllerResult
import com.gadget.vibration.PwmPulse
import com.gadget.vibration.VibrationControllerResult
import kotlinx.coroutines.launch

private const val DEMO_BOOST_PERCENT = 150
private const val DEMO_STROBE_FREQ_HZ = 30
private const val DEMO_STROBE_DUTY_PERCENT = 10
private const val DEMO_STROBE_DURATION_MS = 5_000L
private const val DEMO_MULTI_LED_DURATION_MS = 10_000L
private const val DEMO_THERMAL_DURATION_MS = 30_000L
private const val DEMO_AMPLITUDE_PERCENT = 100
private const val DEMO_AMPLITUDE_DURATION_MS = 2_500L
private const val DEMO_RUMBLE_DURATION_MS = 60_000L
private const val DEMO_RUMBLE_AMPLITUDE = 35
private const val DEMO_PWM_PULSE_ON_US = 8_000L
private const val DEMO_PWM_PULSE_OFF_US = 12_000L
private const val DEMO_PWM_PULSE_COUNT = 20
private const val DEMO_DUAL_PHASE_OFFSET_US = 5_000L
private const val DEMO_DUAL_PULSE_ON_US = 15_000L
private const val DEMO_DUAL_PULSE_OFF_US = 30_000L
private const val DEMO_DUAL_PULSE_COUNT = 12

/**
 * Composable section appended to TorchScreen. Hidden when root is not
 * granted, the standard flavor's controller will return Unsupported for
 * every call so the buttons would be no-ops anyway — but hiding them
 * also prevents user confusion.
 */
@Composable
fun TorchRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val torch = entryPoint.torchSysfsController()
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
                text = "Root extras (Torch)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Direct sysfs control. Each call is rate-limited and " +
                    "requires the matching feature to be enabled in Settings.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeTorchResult(torch.boostBrightness(DEMO_BOOST_PERCENT))
                    }
                },
            ) { Text("Boost LED to $DEMO_BOOST_PERCENT%") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeTorchResult(
                            torch.dutyCycleStrobe(
                                frequencyHz = DEMO_STROBE_FREQ_HZ,
                                dutyPercent = DEMO_STROBE_DUTY_PERCENT,
                                durationMillis = DEMO_STROBE_DURATION_MS,
                            ),
                        )
                    }
                },
            ) { Text("Strobe ${DEMO_STROBE_FREQ_HZ}Hz @ ${DEMO_STROBE_DUTY_PERCENT}%") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeTorchResult(
                            torch.multiLedActivate(
                                durationMillis = DEMO_MULTI_LED_DURATION_MS,
                                includeScreen = true,
                            ),
                        )
                    }
                },
            ) { Text("Multi-LED panic mode") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeTorchResult(
                            torch.withThermalOverride(DEMO_THERMAL_DURATION_MS) {
                                torch.boostBrightness(DEMO_BOOST_PERCENT)
                            },
                        )
                    }
                },
            ) { Text("Thermal override + boost ${DEMO_THERMAL_DURATION_MS / 1000}s") }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun VibrationRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val vibration = entryPoint.vibrationController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    val pwmPattern = remember {
        List(DEMO_PWM_PULSE_COUNT) { PwmPulse(DEMO_PWM_PULSE_ON_US, DEMO_PWM_PULSE_OFF_US) }
    }
    val lraPattern = remember {
        List(DEMO_DUAL_PULSE_COUNT) { PwmPulse(DEMO_DUAL_PULSE_ON_US, DEMO_DUAL_PULSE_OFF_US) }
    }
    val ermPattern = remember {
        List(DEMO_DUAL_PULSE_COUNT) { PwmPulse(DEMO_DUAL_PULSE_ON_US * 2, DEMO_DUAL_PULSE_OFF_US * 2) }
    }

    RootExtrasDisclaimerCard(text = S.RootExtrasGenericDisclaimer.text(lang))
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Root extras (Vibration)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Direct PWM, dual-actuator, and sustained-rumble drive. " +
                    "Sustained rumble auto-aborts on excessive battery drain or motor heat.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeVibrationResult(
                            vibration.extremeAmplitude(
                                DEMO_AMPLITUDE_PERCENT,
                                DEMO_AMPLITUDE_DURATION_MS,
                            ),
                        )
                    }
                },
            ) { Text("Extreme amplitude burst") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeVibrationResult(vibration.directPwm(pwmPattern))
                    }
                },
            ) { Text("Raw PWM micro-pattern") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeVibrationResult(
                            vibration.dualActuator(
                                lraPattern = lraPattern,
                                ermPattern = ermPattern,
                                phaseOffsetMicros = DEMO_DUAL_PHASE_OFFSET_US,
                            ),
                        )
                    }
                },
            ) { Text("Dual-actuator phase test") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeVibrationResult(
                            vibration.sustainedRumble(
                                DEMO_RUMBLE_DURATION_MS,
                                DEMO_RUMBLE_AMPLITUDE,
                            ),
                        )
                    }
                },
            ) { Text("Sustained rumble (${DEMO_RUMBLE_DURATION_MS / 1000}s)") }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun describeTorchResult(result: TorchSysfsControllerResult): String = when (result) {
    TorchSysfsControllerResult.Ok -> "OK"
    TorchSysfsControllerResult.Unsupported -> "Unsupported on this device"
    TorchSysfsControllerResult.OptedOut -> "Disabled — enable in Settings"
    is TorchSysfsControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is TorchSysfsControllerResult.HardwareError -> "Hardware error: ${result.message}"
}

private fun describeVibrationResult(result: VibrationControllerResult): String = when (result) {
    VibrationControllerResult.Ok -> "OK"
    VibrationControllerResult.Unsupported -> "Unsupported on this device"
    VibrationControllerResult.OptedOut -> "Disabled — enable in Settings"
    is VibrationControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is VibrationControllerResult.HardwareError -> "Hardware error: ${result.message}"
}
