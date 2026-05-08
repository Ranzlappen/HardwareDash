package com.gadget.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.root.RootFeaturesEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Settings card listing every rooted feature with a per-feature opt-in
 * switch. Renders nothing on the standard flavor or on a rooted device
 * without root granted — the parent layout sees an empty Composable and
 * the rest of the Settings screen stacks normally.
 */
@Composable
fun RootedFeatureTogglesCard(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val toggles = entryPoint.featureToggles()
    val descriptors = remember(entryPoint) {
        entryPoint.featureRegistry().allDescriptors().toList()
    }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Rooted feature toggles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Each feature ships OFF. Enable individually after reading the safety notes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            descriptors.forEach { descriptor ->
                val enabled by toggles.isEnabled(descriptor.key)
                    .collectAsState(initial = false)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = descriptor.key.id.replace('_', ' '),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (descriptor.requiresExplicitConfirm) {
                            Text(
                                text = "⚠ Hardware risk — read before enabling",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newValue ->
                            scope.launch { toggles.setEnabled(descriptor.key, newValue) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberRootFeatures(): RootFeaturesEntryPoint {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RootFeaturesEntryPoint::class.java,
        )
    }
}

