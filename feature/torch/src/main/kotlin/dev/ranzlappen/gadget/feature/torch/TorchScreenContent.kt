package dev.ranzlappen.gadget.feature.torch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.feature.torch.components.RootToolsCard
import dev.ranzlappen.gadget.feature.torch.components.StrobeDefaultsCard
import dev.ranzlappen.gadget.feature.torch.components.TorchToggleCard
import dev.ranzlappen.gadget.feature.torch.components.WidgetsCard
import dev.ranzlappen.gadget.feature.torch.components.torchModuleInfo
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconSource

/**
 * Stateless TorchScreen content — receives a single [TorchScreenState]
 * snapshot plus a flat [TorchUiEvent] dispatcher for every user-initiated
 * event. (Read-only lookups like [onResolveIcon] stay separate — they
 * aren't events.)
 *
 * The cards render top-to-bottom inside the screen scaffold; each is a
 * collapsible card whose expanded state persists — the torch-owned cards
 * via [TorchScreenState.expandedSections] + [TorchUiEvent.SectionToggle]
 * (see [TorchSectionId]), the two monitor tiles self-manage via their own
 * `collapseId`:
 *
 *   1. **Torch toggle** — hero controls plus in-app strobe / Morse toggles
 *      for immediate testing without pinning a widget.
 *   2. **Strobe defaults** — slider feeding
 *      [dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository.setDefaultStrobeRateHz];
 *      the value is the default captured into every new strobe widget at
 *      pin time.
 *   3. **Monitoring** — two independent tiles supplied as slots: the
 *      persisted history chart ([monitor]) and the in-memory live-stream
 *      chart ([liveMonitor]).
 *   4. **Your widgets** — the saved-widget list with edit / delete + add.
 *   5. **Root tools** — privileged controls, shown only when rooted.
 */
@Composable
fun TorchScreenContent(
    state: TorchScreenState,
    onEvent: (TorchUiEvent) -> Unit,
    onResolveIcon: (String) -> WidgetIconSource,
    modifier: Modifier = Modifier,
    monitor: @Composable () -> Unit = {},
    liveMonitor: @Composable () -> Unit = {},
) {
    // Every torch card is collapsible; the expanded state is persisted and
    // hoisted in via [TorchScreenState.expandedSections] (default expanded).
    // The monitor / live-monitor tiles manage their own collapse inside the
    // reusable containers, so only the torch-owned cards read this map.
    fun expanded(id: String): Boolean = state.expandedSections[id] ?: true
    ModuleScreenScaffold(
        title = stringResource(R.string.torch_screen_title),
        modifier = modifier,
        functional = {
            TorchToggleCard(
                torch = state.torch,
                strobeRunning = state.strobeRunning,
                morseText = state.morseText,
                expanded = expanded(TorchSectionId.Controls),
                onExpandedChange = { onEvent(TorchUiEvent.SectionToggle(TorchSectionId.Controls)) },
                onToggleClick = { onEvent(TorchUiEvent.ToggleClick) },
                onMomentaryHold = { onEvent(TorchUiEvent.MomentaryHold(it)) },
                onStrobeToggle = { onEvent(TorchUiEvent.StrobeToggle) },
                onStrobeHold = { onEvent(TorchUiEvent.StrobeHold(it)) },
                onMorseToggle = { onEvent(TorchUiEvent.MorseToggle) },
                onMorseHold = { onEvent(TorchUiEvent.MorseHold(it)) },
                onMorseTextChange = { onEvent(TorchUiEvent.MorseTextChange(it)) },
            )
            StrobeDefaultsCard(
                rateHz = state.defaultStrobeRateHz,
                onRateChange = { onEvent(TorchUiEvent.RateChange(it)) },
                onRateCommit = { onEvent(TorchUiEvent.RateCommit) },
                expanded = expanded(TorchSectionId.StrobeDefaults),
                onExpandedChange = { onEvent(TorchUiEvent.SectionToggle(TorchSectionId.StrobeDefaults)) },
            )
            // Two independent monitoring tiles (torch's instantiations of the
            // reusable containers). Injected as slots so the stateless content
            // stays Hilt-free for previews/tests; TorchScreen supplies them.
            // The persisted history chart…
            monitor()
            // …and the in-memory live-stream chart, operating separately.
            liveMonitor()
            WidgetsCard(
                widgets = state.widgets,
                onResolveIcon = onResolveIcon,
                onAddFlashlight = { onEvent(TorchUiEvent.AddFlashlight) },
                onAddStrobe = { onEvent(TorchUiEvent.AddStrobe) },
                onQuickPinFlashlight = { onEvent(TorchUiEvent.QuickPinFlashlight) },
                onQuickPinStrobe = { onEvent(TorchUiEvent.QuickPinStrobe) },
                onEditWidget = { onEvent(TorchUiEvent.EditWidget(it)) },
                onDeleteWidget = { onEvent(TorchUiEvent.DeleteWidget(it)) },
                expanded = expanded(TorchSectionId.Widgets),
                onExpandedChange = { onEvent(TorchUiEvent.SectionToggle(TorchSectionId.Widgets)) },
            )
            // Rooted-only privileged controls — shown only when the
            // rooted app version reports a usable root shell.
            if (state.rootAvailability.rootReady) {
                RootToolsCard(
                    onBoostBrightness = { onEvent(TorchUiEvent.RootBoostBrightness) },
                    onDutyStrobe = { onEvent(TorchUiEvent.RootDutyStrobe) },
                    onMultiLed = { onEvent(TorchUiEvent.RootMultiLed) },
                    onThermal = { onEvent(TorchUiEvent.RootThermal) },
                    expanded = expanded(TorchSectionId.RootTools),
                    onExpandedChange = { onEvent(TorchUiEvent.SectionToggle(TorchSectionId.RootTools)) },
                )
            }
        },
        moduleInfo = torchModuleInfo(state.torch, state.rootAvailability),
    )
}
