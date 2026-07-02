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
import dev.ranzlappen.gadget.feature.notification.control.LockScreenOverlayConfig
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import dev.ranzlappen.gadget.feature.notification.control.StickyOverrideConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_CHANNEL_ID = "link_service"
private const val DEMO_OVERLAY_DURATION_MS = 10_000L

/**
 * Notification root extras Card. Rendered inside `LockScreenScreen`
 * inside the existing collapsible "Lock Screen Controls" block.
 * Auto-revert of listener access + lock-screen overlay on screen
 * dispose.
 */
@Composable
fun NotificationRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val notification = entryPoint.notificationController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { notification.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.NotificationRootExtras.disclaimer(lang))
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = S.NotificationRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.NotificationRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                notification.overrideStickyChannel(
                                    StickyOverrideConfig(channelId = DEMO_CHANNEL_ID),
                                ),
                            )
                        }
                    },
                ) { Text(S.NotificationRootExtras.stickyOverride(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(notification.grantListenerAccess())
                        }
                    },
                ) { Text(S.NotificationRootExtras.listenerAccess(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                notification.showLockScreenOverlay(
                                    LockScreenOverlayConfig(
                                        message = "Rooted demo overlay",
                                        durationMillis = DEMO_OVERLAY_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.NotificationRootExtras.overlay(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(notification.resetAllNotificationMutations())
                        }
                    },
                ) { Text(S.NotificationRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(result: NotificationControllerResult): String = when (result) {
    is NotificationControllerResult.Ok -> result.statusNote ?: "OK"
    NotificationControllerResult.Unsupported -> "Unsupported on this device"
    NotificationControllerResult.OptedOut -> "Disabled — enable in Settings"
    is NotificationControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is NotificationControllerResult.HardwareError -> "Error: ${result.message}"
    is NotificationControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is NotificationControllerResult.ChannelImportanceSnapshot ->
        "Importance: ${result.previousImportance} → ${result.newImportance}"
}
