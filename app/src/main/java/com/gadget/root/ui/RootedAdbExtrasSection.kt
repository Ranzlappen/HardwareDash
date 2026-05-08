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
import com.gadget.adbdebug.AdbDebuggingControllerResult
import com.gadget.adbdebug.AdbNetworkConfig
import com.gadget.adbdebug.SetPropConfig
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_ADB_NETWORK_PORT = 5555
private const val DEMO_SETPROP_KEY = "log.tag.GadgetDebug"
private const val DEMO_SETPROP_VALUE = "VERBOSE"

/**
 * ADB Debugging root extras Card. Rendered inside `SettingsScreen` after
 * the existing `RootedFeatureTogglesCard()`. Auto-revert of every
 * `adb-toggle://` + `setprop://` mutation on screen dispose.
 */
@Composable
fun AdbDebuggingRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val adb = entryPoint.adbDebuggingController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }
    var adbEnabledLocal by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { adb.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.AdbDebuggingRootExtras.disclaimer(lang))
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
                    text = S.AdbDebuggingRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.AdbDebuggingRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val nextValue = adbEnabledLocal.not()
                            val result = adb.toggleAdbEnabled(nextValue)
                            if (result is AdbDebuggingControllerResult.AdbToggleSnapshot) {
                                adbEnabledLocal = result.appliedEnabled
                            }
                            status = describeAdbResult(result)
                        }
                    },
                ) { Text(S.AdbDebuggingRootExtras.toggleAdb(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeAdbResult(
                                adb.toggleAdbOverNetwork(
                                    AdbNetworkConfig(
                                        enabled = true,
                                        port = DEMO_ADB_NETWORK_PORT,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.AdbDebuggingRootExtras.toggleNetwork(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeAdbResult(adb.dumpProperties(persist = true))
                        }
                    },
                ) { Text(S.AdbDebuggingRootExtras.dumpProperties(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeAdbResult(
                                adb.overrideSystemProperty(
                                    SetPropConfig(
                                        key = DEMO_SETPROP_KEY,
                                        value = DEMO_SETPROP_VALUE,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.AdbDebuggingRootExtras.setpropOverride(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeAdbResult(adb.resetAllAdbMutations()) }
                    },
                ) { Text(S.AdbDebuggingRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describeAdbResult(result: AdbDebuggingControllerResult): String = when (result) {
    is AdbDebuggingControllerResult.Ok -> result.statusNote ?: "OK"
    AdbDebuggingControllerResult.Unsupported -> "Unsupported on this device"
    AdbDebuggingControllerResult.OptedOut -> "Disabled — enable in Settings"
    is AdbDebuggingControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is AdbDebuggingControllerResult.HardwareError -> "Error: ${result.message}"
    is AdbDebuggingControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is AdbDebuggingControllerResult.AdbToggleSnapshot ->
        "ADB enabled: ${result.priorEnabled ?: "?"} -> ${result.appliedEnabled}"
    is AdbDebuggingControllerResult.AdbNetworkSnapshot ->
        "ADB network port: ${result.priorPort ?: "off"} -> ${result.appliedPort ?: "off"}"
    is AdbDebuggingControllerResult.PropertyDump ->
        "getprop: ${result.excerpt.length} bytes; persisted=${result.persistedFile ?: "no"}"
    is AdbDebuggingControllerResult.SetpropSnapshot ->
        "${result.key}: ${result.priorValue ?: "?"} -> ${result.appliedValue}"
}
