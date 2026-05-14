package com.gadget.notifications

import com.gadget.localization.S
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat

@Composable
fun NotificationPreviewCard(spec: NotifSpec, modifier: Modifier = Modifier) {
    val accent = remember(spec.accentColor) {
        spec.accentColor?.let(::Color) ?: Color(0xFF2196F3)
    }
    val isSecret = spec.visibility == NotificationCompat.VISIBILITY_SECRET

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isSecret) 0.3f else 1f),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status bar: app icon (accent-tinted) + "Gadget · now" + visibility chip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Gadget · now" + if (spec.subtext.isNotBlank()) " · ${spec.subtext}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    VisibilityChip(spec.visibility)
                }

                // Accent stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accent),
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Title
                    Text(
                        text = spec.title.ifBlank { "(no title)" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )

                    // Body — varies by style
                    when (spec.style) {
                        NotifStyle.NORMAL -> Text(
                            text = spec.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 2,
                        )
                        NotifStyle.BIG_TEXT -> Text(
                            text = spec.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        )
                        NotifStyle.INBOX -> Column {
                            spec.body.lines().take(5).forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // Progress
                    when (spec.progressMode) {
                        ProgressMode.OFF -> Unit
                        ProgressMode.INDETERMINATE -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = accent,
                        )
                        ProgressMode.DETERMINATE -> LinearProgressIndicator(
                            progress = { spec.progressValue.coerceIn(0, 100) / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = accent,
                        )
                    }

                    // Action buttons
                    if (spec.actions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            spec.actions.take(3).forEach { entry ->
                                OutlinedButton(
                                    onClick = {},
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(entry.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        // Quick-reply hint under action 1
                        if (spec.quickReplyHint.isNotBlank()) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                placeholder = { Text(spec.quickReplyHint, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                enabled = false,
                                singleLine = true,
                            )
                        }
                    }

                    // Footer pills
                    val showFooter = spec.ongoing || spec.badge > 0 || !spec.sound || !spec.vibrate || spec.timeoutSec > 0
                    if (showFooter) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (spec.ongoing) FooterPill(Icons.Default.PushPin, "Ongoing")
                            if (spec.badge > 0) FooterPill(Icons.Default.Info, "•${spec.badge}")
                            if (!spec.sound) FooterPill(Icons.Default.NotificationsOff, "Silent")
                            if (!spec.vibrate) FooterPill(Icons.Default.Vibration, "No vibrate")
                            if (spec.timeoutSec > 0) FooterPill(Icons.Default.Timer, "${spec.timeoutSec}s")
                        }
                    }
                }
            }
        }

        if (isSecret) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null)
                    Spacer(Modifier.width(6.dp))
                    Text(S.lock.hiddenOnLockScreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun VisibilityChip(visibility: Int) {
    val (icon, label) = when (visibility) {
        NotificationCompat.VISIBILITY_PUBLIC -> Icons.Default.Visibility to "Public"
        NotificationCompat.VISIBILITY_PRIVATE -> Icons.Default.VisibilityOff to "Private"
        NotificationCompat.VISIBILITY_SECRET -> Icons.Default.Lock to "Secret"
        else -> Icons.Default.Visibility to "Public"
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) },
        colors = AssistChipDefaults.assistChipColors(),
    )
}

@Composable
private fun FooterPill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 10.sp)
    }
}
