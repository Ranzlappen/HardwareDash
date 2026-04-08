package com.hardwaredash.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardwaredash.localization.Language
import com.hardwaredash.localization.LocalizationManager
import com.hardwaredash.localization.S
import kotlin.math.roundToInt

private const val WIDGET_PREFS = "widget_settings"
private const val KEY_RING_DURATION = "phone_ring_duration_seconds"
private const val KEY_NOTIFY_DELAY = "notify_delay_seconds"

const val DEFAULT_RING_DURATION = 30
const val DEFAULT_NOTIFY_DELAY = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val strings = S.settings

    // Language state
    val currentLang by LocalizationManager.currentLanguage

    // Widget settings from SharedPreferences
    val widgetPrefs = remember { context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE) }
    var ringDuration by remember {
        mutableIntStateOf(widgetPrefs.getInt(KEY_RING_DURATION, DEFAULT_RING_DURATION))
    }
    var notifyDelay by remember {
        mutableIntStateOf(widgetPrefs.getInt(KEY_NOTIFY_DELAY, DEFAULT_NOTIFY_DELAY))
    }

    // Language dropdown expanded state
    var langExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Settings, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                strings.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // SECTION 1 — Language
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.language,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            strings.languageDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = { langExpanded = it },
        ) {
            OutlinedTextField(
                value = currentLang.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false },
            ) {
                Language.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(lang.displayName)
                                if (lang == currentLang) {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        onClick = {
                            LocalizationManager.setLanguage(context, lang)
                            langExpanded = false
                        },
                    )
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 2 — Widget Customizer
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.widgetCustomizer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // ── Phone Ring Duration ──────────────────────────────────────────
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhoneInTalk, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.phoneRingDuration,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.phoneRingDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                Text(
                    "$ringDuration ${strings.seconds}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = ringDuration.toFloat(),
                    onValueChange = { ringDuration = it.roundToInt() },
                    onValueChangeFinished = {
                        widgetPrefs.edit().putInt(KEY_RING_DURATION, ringDuration).apply()
                    },
                    valueRange = 5f..120f,
                    steps = 22, // 5-second increments: (120-5)/5 - 1 = 22
                )
            }
        }

        // ── Notify Delay ─────────────────────────────────────────────────
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Notifications, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.notifyDelay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.notifyDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                Text(
                    "$notifyDelay ${strings.seconds}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = notifyDelay.toFloat(),
                    onValueChange = { notifyDelay = it.roundToInt() },
                    onValueChangeFinished = {
                        widgetPrefs.edit().putInt(KEY_NOTIFY_DELAY, notifyDelay).apply()
                    },
                    valueRange = 5f..300f,
                    steps = 58, // 5-second increments: (300-5)/5 - 1 = 58
                )
            }
        }
    }
}
