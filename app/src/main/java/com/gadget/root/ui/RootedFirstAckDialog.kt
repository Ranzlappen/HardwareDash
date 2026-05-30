package com.gadget.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import kotlinx.coroutines.launch

/**
 * Non-cancellable first-launch acknowledgement modal. Renders only when
 * root is granted AND the user has not yet tapped "I understand". Place
 * this Composable at the top of [com.gadget.ui.screens.SettingsScreen] so
 * the dialog overlays the entire Settings surface. After the user
 * confirms once, [dev.ranzlappen.gadget.core.root.RootPrefKeys.RootedAcknowledged] is
 * persisted and the dialog never re-shows.
 *
 * The standard flavor's [com.gadget.root.NoOpRootFeatureToggles] reports
 * `isRootedAcknowledged()` as `flowOf(true)`, so this Composable is a
 * no-op there — standard APK rendering is unchanged.
 */
@Composable
fun RootedFirstAckDialog() {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val toggles = entryPoint.featureToggles()
    val acknowledged by toggles.isRootedAcknowledged().collectAsState(initial = true)
    if (acknowledged) return

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)

    AlertDialog(
        onDismissRequest = { /* non-cancellable */ },
        title = {
            Text(
                text = S.RootedFirstAck.dialogTitle(lang),
                color = MaterialTheme.colorScheme.error,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = S.RootedLegalNotice.cardTitle(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = S.RootedFirstAck.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch { toggles.setRootedAcknowledged(true) }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(S.RootedFirstAck.acknowledge(lang)) }
        },
    )
}
