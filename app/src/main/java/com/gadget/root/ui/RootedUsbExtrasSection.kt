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
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.usbdebug.UsbDebuggingControllerResult
import com.gadget.usbdebug.UsbFunctionType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * USB Debugging root extras Card. Rendered inside `BugReportScreen` since
 * the four methods are diagnostic-flavored (`dumpsys usb`,
 * `dumpsys SerialService`, `/sys/kernel/debug/usb/devices`) plus a
 * single function-switch toggle. Auto-revert of every `cmd-usb://`
 * mutation on screen dispose.
 */
@Composable
fun UsbDebuggingRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val usb = entryPoint.usbDebuggingController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { usb.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.UsbDebuggingRootExtras.disclaimer(lang))
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
                    text = S.UsbDebuggingRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.UsbDebuggingRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeUsbResult(
                                usb.switchUsbFunction(UsbFunctionType.MTP),
                            )
                        }
                    },
                ) { Text(S.UsbDebuggingRootExtras.switchFunction(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { status = describeUsbResult(usb.dumpUsb()) } },
                ) { Text(S.UsbDebuggingRootExtras.dumpUsb(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeUsbResult(usb.dumpSerialService()) }
                    },
                ) { Text(S.UsbDebuggingRootExtras.dumpSerialService(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeUsbResult(usb.dumpUsbDevicesDebug()) }
                    },
                ) { Text(S.UsbDebuggingRootExtras.dumpDevicesDebug(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeUsbResult(usb.resetAllUsbMutations()) }
                    },
                ) { Text(S.UsbDebuggingRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describeUsbResult(result: UsbDebuggingControllerResult): String = when (result) {
    is UsbDebuggingControllerResult.Ok -> result.statusNote ?: "OK"
    UsbDebuggingControllerResult.Unsupported -> "Unsupported on this device"
    UsbDebuggingControllerResult.OptedOut -> "Disabled — enable in Settings"
    is UsbDebuggingControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is UsbDebuggingControllerResult.HardwareError -> "Error: ${result.message}"
    is UsbDebuggingControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is UsbDebuggingControllerResult.UsbFunctionSnapshot ->
        "USB function: ${result.priorFunction ?: "?"} -> ${result.appliedFunction.wireName}"
    is UsbDebuggingControllerResult.UsbDumpExcerpt ->
        "Read ${result.excerpt.length} bytes from ${result.source}"
}
