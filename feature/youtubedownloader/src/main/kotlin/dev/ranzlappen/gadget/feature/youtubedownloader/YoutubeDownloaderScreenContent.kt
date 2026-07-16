package dev.ranzlappen.gadget.feature.youtubedownloader

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind

/**
 * Stateless content for the YouTube downloader. Hilt-free so it stays
 * preview- and instrumentation-test-friendly; the stateful
 * [YoutubeDownloaderScreen] feeds it state and forwards [YtdlEvent]s.
 */
@Composable
fun YoutubeDownloaderScreenContent(
    state: YoutubeDownloaderUiState,
    onEvent: (YtdlEvent) -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    monitor: @Composable () -> Unit = {},
    liveMonitor: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.ytdl_screen_title),
        modifier = modifier,
        functional = {
            SourceCard(state, onEvent)
            FormatCard(state.config, onEvent)
            ScopeCard(state.config, onEvent)
            ExtrasCard(state.config, onEvent)
            PrivacyCard(state, onEvent, onSignIn)
            monitor()
            liveMonitor()
            QueueCard(state.tasks, onEvent)
        },
        moduleInfo = downloaderModuleInfo(),
    )
}

@Composable
private fun SourceCard(state: YoutubeDownloaderUiState, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_source_title), icon = Icons.Outlined.Download) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            GadgetTextField(
                value = state.config.url,
                onValueChange = { onEvent(YtdlEvent.UrlChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.ytdl_url_label),
                placeholder = stringResource(R.string.ytdl_url_placeholder),
                singleLine = true,
            )
            GadgetPrimaryButton(
                onClick = { onEvent(YtdlEvent.StartDownload) },
                text = stringResource(R.string.ytdl_download_button),
                modifier = Modifier.fillMaxWidth(),
                enabled = state.config.url.isNotBlank(),
                leadingIcon = Icons.Outlined.Download,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatCard(config: DownloadConfig, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_format_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = config.kind == MediaKind.VIDEO,
                    onClick = { onEvent(YtdlEvent.KindChanged(MediaKind.VIDEO)) },
                    label = stringResource(R.string.ytdl_kind_video),
                )
                GadgetChip(
                    selected = config.kind == MediaKind.AUDIO,
                    onClick = { onEvent(YtdlEvent.KindChanged(MediaKind.AUDIO)) },
                    label = stringResource(R.string.ytdl_kind_audio),
                )
            }
            when (config.kind) {
                MediaKind.VIDEO -> {
                    Text(
                        text = stringResource(R.string.ytdl_video_quality_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        VideoQuality.entries.forEach { quality ->
                            GadgetChip(
                                selected = config.videoQuality == quality,
                                onClick = { onEvent(YtdlEvent.VideoQualityChanged(quality)) },
                                label = videoQualityLabel(quality),
                            )
                        }
                    }
                }
                MediaKind.AUDIO -> {
                    Text(
                        text = stringResource(R.string.ytdl_audio_format_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        AudioFormat.entries.forEach { format ->
                            GadgetChip(
                                selected = config.audioFormat == format,
                                onClick = { onEvent(YtdlEvent.AudioFormatChanged(format)) },
                                label = format.ytdlp.uppercase(),
                            )
                        }
                    }
                    if (config.audioFormat == AudioFormat.MP3) {
                        Text(
                            text = stringResource(R.string.ytdl_audio_quality_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                            AudioQuality.entries.forEach { quality ->
                                GadgetChip(
                                    selected = config.audioQuality == quality,
                                    onClick = { onEvent(YtdlEvent.AudioQualityChanged(quality)) },
                                    label = quality.value,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScopeCard(config: DownloadConfig, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_scope_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                GadgetChip(
                    selected = config.scope == PlaylistScope.SINGLE,
                    onClick = { onEvent(YtdlEvent.ScopeChanged(PlaylistScope.SINGLE)) },
                    label = stringResource(R.string.ytdl_scope_single),
                )
                GadgetChip(
                    selected = config.scope == PlaylistScope.PLAYLIST,
                    onClick = { onEvent(YtdlEvent.ScopeChanged(PlaylistScope.PLAYLIST)) },
                    label = stringResource(R.string.ytdl_scope_playlist),
                )
            }
            if (config.scope == PlaylistScope.PLAYLIST) {
                GadgetTextField(
                    value = config.playlistItems,
                    onValueChange = { onEvent(YtdlEvent.PlaylistItemsChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.ytdl_playlist_items_label),
                    placeholder = stringResource(R.string.ytdl_playlist_items_placeholder),
                    supportingText = stringResource(R.string.ytdl_playlist_items_hint),
                    singleLine = true,
                )
            }
        }
    }
}

@Composable
private fun ExtrasCard(config: DownloadConfig, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_extras_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            ToggleRow(stringResource(R.string.ytdl_embed_thumbnail), config.embedThumbnail) {
                onEvent(YtdlEvent.ToggleThumbnail)
            }
            ToggleRow(stringResource(R.string.ytdl_embed_metadata), config.embedMetadata) {
                onEvent(YtdlEvent.ToggleMetadata)
            }
            ToggleRow(stringResource(R.string.ytdl_embed_chapters), config.embedChapters) {
                onEvent(YtdlEvent.ToggleChapters)
            }
            ToggleRow(stringResource(R.string.ytdl_sponsorblock), config.sponsorblockRemove) {
                onEvent(YtdlEvent.ToggleSponsorblock)
            }
            ToggleRow(stringResource(R.string.ytdl_restrict_filenames), config.restrictFilenames) {
                onEvent(YtdlEvent.ToggleRestrict)
            }
        }
    }
}

@Composable
private fun PrivacyCard(
    state: YoutubeDownloaderUiState,
    onEvent: (YtdlEvent) -> Unit,
    onSignIn: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_privacy_title), icon = Icons.Outlined.Lock) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.ytdl_privacy_blurb),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.hasCookies) {
                Text(
                    text = stringResource(R.string.ytdl_signed_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                ToggleRow(stringResource(R.string.ytdl_use_cookies), state.config.useCookies) {
                    onEvent(YtdlEvent.ToggleUseCookies)
                }
                GadgetTertiaryButton(
                    onClick = { onEvent(YtdlEvent.SignOut) },
                    text = stringResource(R.string.ytdl_sign_out),
                )
            } else {
                GadgetSecondaryButton(
                    onClick = onSignIn,
                    text = stringResource(R.string.ytdl_sign_in),
                    leadingIcon = Icons.Outlined.Lock,
                )
            }
        }
    }
}

@Composable
private fun QueueCard(tasks: List<DownloadTask>, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(title = stringResource(R.string.ytdl_queue_title)) {
        if (tasks.isEmpty()) {
            GadgetEmptyState(
                title = stringResource(R.string.ytdl_queue_empty),
                icon = Icons.Outlined.Downloading,
                modifier = Modifier.fillMaxWidth(),
            )
            return@DashCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            tasks.forEach { task -> TaskRow(task, onEvent) }
            if (tasks.any { it.status != DownloadStatus.Running && it.status != DownloadStatus.Queued }) {
                GadgetTertiaryButton(
                    onClick = { onEvent(YtdlEvent.ClearFinished) },
                    text = stringResource(R.string.ytdl_clear_finished),
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: DownloadTask, onEvent: (YtdlEvent) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = taskStatusLabel(task),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (task.status == DownloadStatus.Running || task.status == DownloadStatus.Queued) {
            LinearProgressIndicator(
                progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetTertiaryButton(
                onClick = { onEvent(YtdlEvent.Cancel(task.id)) },
                text = stringResource(R.string.ytdl_cancel),
            )
        }
        task.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun videoQualityLabel(quality: VideoQuality): String = stringResource(
    when (quality) {
        VideoQuality.BEST -> R.string.ytdl_video_best
        VideoQuality.P1080 -> R.string.ytdl_video_1080
        VideoQuality.P720 -> R.string.ytdl_video_720
        VideoQuality.P480 -> R.string.ytdl_video_480
    },
)

@Composable
private fun taskStatusLabel(task: DownloadTask): String = when (task.status) {
    DownloadStatus.Queued -> stringResource(R.string.ytdl_status_queued)
    DownloadStatus.Running -> stringResource(R.string.ytdl_status_running, task.progress.toInt())
    DownloadStatus.Completed -> stringResource(R.string.ytdl_status_completed)
    DownloadStatus.Failed -> stringResource(R.string.ytdl_status_failed)
    DownloadStatus.Cancelled -> stringResource(R.string.ytdl_status_cancelled)
}

@Composable
private fun downloaderModuleInfo(): ModuleInfo = ModuleInfo(
    permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                ModulePermission(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    label = stringResource(R.string.ytdl_perm_notifications_label),
                    rationale = stringResource(R.string.ytdl_perm_notifications_rationale),
                    optional = true,
                ),
            )
        }
    },
    compatibility = OsCompatibility(minSdk = Build.VERSION_CODES.Q),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.ytdl_cap_engine_name),
            detail = stringResource(R.string.ytdl_cap_engine_detail),
            status = {
                CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.ytdl_cap_engine_ok))
            },
        ),
    ),
)
