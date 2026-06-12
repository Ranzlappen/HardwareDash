package dev.ranzlappen.gadget.feature.vibration

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.feature.vibration.components.PatternBuilderCard
import dev.ranzlappen.gadget.feature.vibration.components.SavedPatternsCard
import dev.ranzlappen.gadget.feature.vibration.components.VibrationControlsCard
import dev.ranzlappen.gadget.feature.vibration.components.VibrationRootToolsCard
import dev.ranzlappen.gadget.feature.vibration.components.WidgetsCard
import dev.ranzlappen.gadget.feature.vibration.components.vibrationModuleInfo

/**
 * Stateless VibrationScreen content — a single [VibrationScreenState] snapshot
 * plus a flat [VibrationUiEvent] dispatcher. The monitor / live-monitor tiles
 * are injected as slots (supplied by the Hilt route) so this stays Hilt-free
 * for previews/tests. Mirror of `TorchScreenContent`. Cards render top-to-
 * bottom; each is collapsible with persisted state.
 */
@Composable
fun VibrationScreenContent(
    state: VibrationScreenState,
    onEvent: (VibrationUiEvent) -> Unit,
    onResolveIcon: (String) -> WidgetIconSource,
    modifier: Modifier = Modifier,
    monitor: @Composable () -> Unit = {},
    liveMonitor: @Composable () -> Unit = {},
) {
    fun expanded(id: String): Boolean = state.expandedSections[id] ?: true
    ModuleScreenScaffold(
        title = stringResource(R.string.vibration_screen_title),
        modifier = modifier,
        functional = {
            VibrationControlsCard(
                state = state.vibration,
                amplitudePercent = state.oneShotAmplitudePercent,
                durationMs = state.oneShotDurationMs,
                onPredefined = { onEvent(VibrationUiEvent.PredefinedClick(it)) },
                onAmplitudeChange = { onEvent(VibrationUiEvent.OneShotAmplitudeChange(it)) },
                onDurationChange = { onEvent(VibrationUiEvent.OneShotDurationChange(it)) },
                onCommit = { onEvent(VibrationUiEvent.OneShotCommit) },
                onOneShot = { onEvent(VibrationUiEvent.OneShot) },
                onStop = { onEvent(VibrationUiEvent.Stop) },
                expanded = expanded(VibrationSectionId.Controls),
                onExpandedChange = { onEvent(VibrationUiEvent.SectionToggle(VibrationSectionId.Controls)) },
            )
            PatternBuilderCard(
                samples = state.patternDraft,
                loop = state.patternLoop,
                enabled = state.vibration.isAvailable,
                onSamplesChange = { onEvent(VibrationUiEvent.PatternDraftChange(it)) },
                onLoopChange = { onEvent(VibrationUiEvent.PatternLoopChange(it)) },
                onPlay = { onEvent(VibrationUiEvent.PatternPlay) },
                onStop = { onEvent(VibrationUiEvent.Stop) },
                onClear = { onEvent(VibrationUiEvent.PatternClear) },
                onSave = { onEvent(VibrationUiEvent.PatternSaveRequest(it)) },
                expanded = expanded(VibrationSectionId.PatternBuilder),
                onExpandedChange = { onEvent(VibrationUiEvent.SectionToggle(VibrationSectionId.PatternBuilder)) },
            )
            SavedPatternsCard(
                patterns = state.savedPatterns,
                onPlay = { onEvent(VibrationUiEvent.PatternPlaySaved(it)) },
                onDelete = { onEvent(VibrationUiEvent.PatternDelete(it)) },
                expanded = expanded(VibrationSectionId.Patterns),
                onExpandedChange = { onEvent(VibrationUiEvent.SectionToggle(VibrationSectionId.Patterns)) },
            )
            // Two independent monitoring tiles supplied as slots.
            monitor()
            liveMonitor()
            WidgetsCard(
                widgets = state.widgets,
                onResolveIcon = onResolveIcon,
                onAddWidget = { onEvent(VibrationUiEvent.AddWidget) },
                onEditWidget = { onEvent(VibrationUiEvent.EditWidget(it)) },
                onDeleteWidget = { onEvent(VibrationUiEvent.DeleteWidget(it)) },
                expanded = expanded(VibrationSectionId.Widgets),
                onExpandedChange = { onEvent(VibrationUiEvent.SectionToggle(VibrationSectionId.Widgets)) },
            )
            if (state.rootAvailability.rootReady) {
                VibrationRootToolsCard(
                    config = state.rootTools,
                    availability = state.rootAvailability,
                    maxAmplitudePercent = state.maxAmplitudePercent,
                    onConfigChange = { onEvent(VibrationUiEvent.RootToolsChange(it)) },
                    onConfigCommit = { onEvent(VibrationUiEvent.RootToolsCommit) },
                    onExtremeAmplitude = { onEvent(VibrationUiEvent.RootExtremeAmplitude) },
                    onDirectPwm = { onEvent(VibrationUiEvent.RootDirectPwm) },
                    onDualActuator = { onEvent(VibrationUiEvent.RootDualActuator) },
                    onSustainedRumble = { onEvent(VibrationUiEvent.RootSustainedRumble) },
                    expanded = expanded(VibrationSectionId.RootTools),
                    onExpandedChange = { onEvent(VibrationUiEvent.SectionToggle(VibrationSectionId.RootTools)) },
                )
            }
        },
        moduleInfo = vibrationModuleInfo(state.vibration, state.rootAvailability),
    )
}
