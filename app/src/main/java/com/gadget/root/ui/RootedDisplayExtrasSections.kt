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
import com.gadget.display.BrightnessOverrideConfig
import com.gadget.display.DensityOverrideConfig
import com.gadget.display.DisplayControllerResult
import com.gadget.display.RefreshRateOverrideConfig
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_BRIGHTNESS_PERCENT = 130
private const val DEMO_BRIGHTNESS_WINDOW_MS = 30_000L
private const val DEMO_REFRESH_MODE_ID = 0
private const val DEMO_DENSITY_DPI = 420

/**
 * Display root extras Card. Rendered inside `TorchScreen` after the
 * existing brightness slider. Auto-revert of every backlight / refresh-rate
 * / density mutation on screen dispose.
 */
@Composable
fun DisplayRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val display = entryPoint.displayController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { display.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.DisplayRootExtras.disclaimer(lang))
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
                    text = S.DisplayRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.DisplayRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                display.overrideBrightness(
                                    BrightnessOverrideConfig(
                                        percent = DEMO_BRIGHTNESS_PERCENT,
                                        activeWindowMillis = DEMO_BRIGHTNESS_WINDOW_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.DisplayRootExtras.overrideBrightness(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                display.overrideRefreshRate(
                                    RefreshRateOverrideConfig(
                                        targetModeId = DEMO_REFRESH_MODE_ID,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.DisplayRootExtras.overrideRefreshRate(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                display.overrideDensity(
                                    DensityOverrideConfig(dpi = DEMO_DENSITY_DPI),
                                ),
                            )
                        }
                    },
                ) { Text(S.DisplayRootExtras.overrideDensity(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(display.surfaceFlingerSnapshot())
                        }
                    },
                ) { Text(S.DisplayRootExtras.surfaceFlingerSnapshot(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(display.resetAllDisplayMutations())
                        }
                    },
                ) { Text(S.DisplayRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(result: DisplayControllerResult): String = when (result) {
    is DisplayControllerResult.Ok -> result.statusNote ?: "OK"
    DisplayControllerResult.Unsupported -> "Unsupported on this device"
    DisplayControllerResult.OptedOut -> "Disabled — enable in Settings"
    is DisplayControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is DisplayControllerResult.HardwareError -> "Error: ${result.message}"
    is DisplayControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is DisplayControllerResult.BrightnessSnapshot ->
        "Backlight ${result.originalRaw} → ${result.appliedRaw} (max=${result.maxBrightness})"
    is DisplayControllerResult.RefreshRateSnapshot ->
        "Mode ${result.originalModeId} → ${result.appliedModeId}"
    is DisplayControllerResult.DensitySnapshot ->
        "DPI ${result.originalDpi ?: "?"} → ${result.appliedDpi}"
    is DisplayControllerResult.SurfaceFlingerExcerpt ->
        "Read ${result.excerpt.length} bytes from SurfaceFlinger"
}
