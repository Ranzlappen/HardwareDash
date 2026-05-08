package com.gadget.root.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S

/**
 * Permanent rooted-mode legal notice rendered at the top of the rooted area
 * in SettingsScreen. Visible only when root is granted, regardless of
 * acknowledgement state — the standalone first-launch ack lives in
 * [RootedFirstAckDialog].
 */
@Composable
fun RootedLegalNoticeCard(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = S.RootedLegalNotice.cardTitle(lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = S.RootedLegalNotice.body(lang),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
