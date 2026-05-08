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
import com.gadget.automation.AutomationControllerResult
import com.gadget.automation.PrivilegedIntentConfig
import com.gadget.automation.PrivilegedIntentVerb
import com.gadget.automation.SystemSettingsOverrideConfig
import com.gadget.automation.SystemSettingsScope
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_INTENT_ACTION = "android.intent.action.MAIN"
private const val DEMO_SETTING_KEY = "screen_brightness_mode"
private const val DEMO_SETTING_VALUE = "1"

/**
 * Automation root extras Card. Rendered inside `LinkScreen` after
 * the monitoring toggle. Auto-revert of any `settings://` mutations
 * on screen dispose.
 */
@Composable
fun AutomationRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val automation = entryPoint.automationController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { automation.revertOnScreenExit() }
        }
    }

    Column(modifier = modifier) {
        RootExtrasDisclaimerCard(text = S.AutomationRootExtras.disclaimer(lang))
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = S.AutomationRootExtras.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.AutomationRootExtras.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                automation.firePrivilegedIntent(
                                    PrivilegedIntentConfig(
                                        verb = PrivilegedIntentVerb.BROADCAST,
                                        action = DEMO_INTENT_ACTION,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.AutomationRootExtras.firePrivilegedIntent(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(
                                automation.overrideSystemSetting(
                                    SystemSettingsOverrideConfig(
                                        scope = SystemSettingsScope.SYSTEM,
                                        key = DEMO_SETTING_KEY,
                                        value = DEMO_SETTING_VALUE,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text(S.AutomationRootExtras.overrideSetting(lang)) }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(automation.dumpsysSnapshot())
                        }
                    },
                ) { Text(S.AutomationRootExtras.dumpsysSnapshot(lang)) }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describe(automation.resetAllAutomationMutations())
                        }
                    },
                ) { Text(S.AutomationRootExtras.resetAll(lang)) }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describe(result: AutomationControllerResult): String = when (result) {
    is AutomationControllerResult.Ok -> result.statusNote ?: "OK"
    AutomationControllerResult.Unsupported -> "Unsupported on this device"
    AutomationControllerResult.OptedOut -> "Disabled — enable in Settings"
    is AutomationControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is AutomationControllerResult.HardwareError -> "Error: ${result.message}"
    is AutomationControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is AutomationControllerResult.IntentResult ->
        "Intent fired (exit=${result.exitCode})"
    is AutomationControllerResult.DumpsysExcerpt ->
        "Read ${result.sections.size} dumpsys sections"
}
