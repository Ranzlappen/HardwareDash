package dev.ranzlappen.gadget.feature.torch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.monitoring.CollapseStateRepository
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinResult
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetCreator
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetFunctionCatalog
import dev.ranzlappen.gadget.feature.torch.widget.broadcastTorchWidgetUpdate
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationResult
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconCatalog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregating ViewModel for [TorchScreen].
 *
 * Combines four reactive sources into a single [TorchScreenState]:
 * - [TorchController.state] — live hardware snapshot.
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }] —
 *   the persisted slider value that becomes the default rate at
 *   widget-pin time.
 * - [WidgetConfigStore.all] — every persisted widget config.
 * - [StrobeRuntime.running] — the live strobe-running signal published
 *   by the [StrobeService] lifecycle (no polling); drives the in-app
 *   strobe toggle button label / pressed state.
 *
 * Event handlers cover the screen's full surface area:
 * - [onToggleClick] — passthrough to the controller.
 * - [onRateChange] — local optimistic update (the slider's local
 *   drag value handles UI feedback; this fires only if the caller
 *   wants to react live to drag values).
 * - [onRateCommit] — fired by [GadgetSlider.onValueChangeFinished].
 *   Writes the slider's last value to
 *   [UserPreferencesRepository.setDefaultStrobeRateHz]. No debounce
 *   needed because the slider commits exactly once per release.
 * - [onStrobeToggle] — start / stop [StrobeService] using the
 *   current slider value. Mirrors the strobe widget tap path.
 * - [onAddWidget] — open the customization sheet for a new widget; confirm
 *   kicks off the [TorchWidgetCreator] pin flow.
 * - [onEditWidget] / [onSheetConfirmed] / [onSheetDismissed] —
 *   widget configuration sheet round-trip.
 * - [onDeleteWidget] — purge a saved config.
 */
@HiltViewModel
class TorchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TorchController,
    private val userPreferences: UserPreferencesRepository,
    private val widgetRepository: WidgetConfigStore<TorchWidgetConfig>,
    private val widgetCreator: TorchWidgetCreator,
    private val widgetFunctionCatalog: TorchWidgetFunctionCatalog,
    private val iconCatalog: WidgetIconCatalog,
    private val rootCapabilities: TorchRootCapabilities,
    private val collapseRepo: CollapseStateRepository,
    private val strobeRuntime: StrobeRuntime,
    private val rootToolsRepo: RootToolsConfigRepository,
) : ViewModel() {

    /** Live availability of the rooted Torch capabilities (probed once on
     *  init). Standard flavor stays [TorchRootAvailability.Unavailable]. */
    private val rootAvailability = MutableStateFlow(TorchRootAvailability.Unavailable)

    init {
        viewModelScope.launch { rootAvailability.value = rootCapabilities.probe() }
    }

    /** One-shot results from rooted-tool invocations, surfaced as a
     *  snackbar by the screen. */
    private val _rootToolEvents = MutableSharedFlow<TorchRootResult>(extraBufferCapacity = 1)
    val rootToolEvents: SharedFlow<TorchRootResult> = _rootToolEvents.asSharedFlow()

    /** Public read-only torch hardware snapshot. */
    val torchState: StateFlow<TorchState> = controller.state

    /** Latest slider value the user landed on; written through to
     *  DataStore by [onRateCommit]. */
    private val pendingRateHz = MutableStateFlow<Float?>(null)

    /** In-flight brightness drag value; overlays the DataStore default so the
     *  slider doesn't snap back mid-drag. Cleared on [onBrightnessCommit]. */
    private val pendingBrightness = MutableStateFlow<Float?>(null)

    /** Optimistic in-flight edit of the rooted-tool parameters. Overlays the
     *  persisted [RootToolsConfigRepository] value so sliders move live; cleared
     *  on [onRootToolsCommit] once the value reaches DataStore. */
    private val pendingRootTools = MutableStateFlow<TorchRootToolsConfig?>(null)

    /** Derived rooted-tool view-state: the effective config (pending overlay on
     *  persisted, clamped to the live ceiling) paired with that ceiling. Folded
     *  as a single source into [state] so the outer combine stays within the
     *  typed-arity cap. */
    private val rootToolsState: kotlinx.coroutines.flow.Flow<RootToolsState> = combine(
        rootToolsRepo.config,
        pendingRootTools,
        rootCapabilities.maxBrightnessPercentFlow,
    ) { persisted, pending, maxBrightness ->
        RootToolsState(
            config = (pending ?: persisted).coercedTo(maxBrightness),
            maxBrightnessPercent = maxBrightness,
        )
    }

    private data class RootToolsState(
        val config: TorchRootToolsConfig,
        val maxBrightnessPercent: Int,
    )

    /** Live strobe-running signal, owned by [StrobeRuntime] and updated by
     *  the [StrobeService] lifecycle. Folded into the screen state below —
     *  no polling, recomposes only on an actual transition. */
    private val strobeRunning: StateFlow<Boolean> = strobeRuntime.running

    /** Five-source aggregate so [state]'s outer `combine` can fit
     *  rooted-availability + collapse state alongside it under the
     *  5-source typed `combine` cap (R4 #20 — replaces an inner
     *  combine that produced a half-baked TorchScreenState). */
    private data class TorchInputs(
        val torch: TorchState,
        val defaultRateHz: Float,
        val widgets: Map<Int, TorchWidgetConfig>,
        val strobeRunning: Boolean,
        val morseText: String,
    )

    private val inputs: kotlinx.coroutines.flow.Flow<TorchInputs> = combine(
        controller.state,
        userPreferences.flow.map { it.defaultStrobeRateHz },
        widgetRepository.all,
        strobeRunning,
        userPreferences.flow.map { it.morseText },
        ::TorchInputs,
    )

    private val defaultBrightnessFlow = userPreferences.flow.map { it.defaultTorchBrightness }

    val state: StateFlow<TorchScreenState> = combine(
        inputs,
        rootAvailability,
        collapseRepo.expandedStates(TorchSectionId.hoisted),
        rootToolsState,
        defaultBrightnessFlow,
    ) { i, root, expanded, rootTools, persistedBrightness ->
        TorchScreenState(
            torch = i.torch,
            defaultStrobeRateHz = pendingRateHz.value ?: i.defaultRateHz,
            defaultBrightness = pendingBrightness.value ?: persistedBrightness,
            widgets = i.widgets
                .toSortedMap()
                // Drop widgets the user deleted in-app (kept on disk as
                // `removed` only so the provider stops self-healing them).
                .filterValues { !it.removed }
                .map { (id, config) -> SavedTorchWidget(id, config) },
            strobeRunning = i.strobeRunning,
            morseText = i.morseText,
            rootAvailability = root,
            rootTools = rootTools.config,
            maxBrightnessPercent = rootTools.maxBrightnessPercent,
            expandedSections = expanded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = TorchScreenState.Initial,
    )

    /**
     * One-shot signal raised when the user requests a widget pin but
     * the active launcher doesn't support
     * [android.appwidget.AppWidgetManager.requestPinAppWidget].
     */
    private val _pinUnsupportedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pinUnsupportedEvents: SharedFlow<Unit> = _pinUnsupportedEvents.asSharedFlow()

    /**
     * One-shot signal raised when the user requests a widget pin but the
     * per-kind cap ([dev.ranzlappen.gadget.core.widgetkit.WidgetPinPolicy])
     * is already reached, so the screen can explain why nothing happened.
     */
    private val _pinCapReachedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pinCapReachedEvents: SharedFlow<Unit> = _pinCapReachedEvents.asSharedFlow()

    /**
     * One-shot signal raised after a widget is deleted from the in-app
     * list, so the screen can tell the user the placed home-screen
     * instance must still be removed manually (the app can't pull it off
     * a third-party launcher).
     */
    private val _widgetRemovedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val widgetRemovedEvents: SharedFlow<Unit> = _widgetRemovedEvents.asSharedFlow()

    /** Transient "open the configuration sheet" signal. */
    private val _sheetTarget = MutableStateFlow<SheetTarget?>(null)
    val sheetTarget: StateFlow<SheetTarget?> = _sheetTarget.asStateFlow()

    /**
     * Single dispatch entry point consumed by [TorchScreenContent].
     *
     * Each [TorchUiEvent] variant maps to one of the typed handlers
     * below — those stay as the implementation so per-action unit
     * testing remains easy and the snackbar / sheet / pin paths keep
     * a stable invocation site.
     */
    fun onEvent(event: TorchUiEvent) {
        when (event) {
            TorchUiEvent.ToggleClick -> onToggleClick()
            is TorchUiEvent.MomentaryHold -> onMomentaryHold(event.active)
            TorchUiEvent.StrobeToggle -> onStrobeToggle()
            is TorchUiEvent.StrobeHold -> onStrobeHold(event.active)
            TorchUiEvent.MorseToggle -> onMorseToggle()
            is TorchUiEvent.MorseHold -> onMorseHold(event.active)
            is TorchUiEvent.MorseTextChange -> onMorseTextChange(event.text)
            is TorchUiEvent.RateChange -> onRateChange(event.rateHz)
            TorchUiEvent.RateCommit -> onRateCommit()
            is TorchUiEvent.BrightnessChange -> onBrightnessChange(event.level)
            TorchUiEvent.BrightnessCommit -> onBrightnessCommit()
            TorchUiEvent.AddWidget -> onAddWidget()
            is TorchUiEvent.EditWidget -> onEditWidget(event.widget)
            is TorchUiEvent.DeleteWidget -> onDeleteWidget(event.widget)
            TorchUiEvent.RootBoostBrightness -> onRootBoostBrightness()
            TorchUiEvent.RootDutyStrobe -> onRootDutyCycleStrobe()
            TorchUiEvent.RootMultiLed -> onRootMultiLed()
            TorchUiEvent.RootThermal -> onRootThermalOverride()
            is TorchUiEvent.RootToolsChange -> onRootToolsChange(event.config)
            TorchUiEvent.RootToolsCommit -> onRootToolsCommit()
            is TorchUiEvent.SectionToggle -> onSectionToggle(event.id)
        }
    }

    fun onToggleClick() {
        controller.toggle()
    }

    /** Momentary "hold for light": torch on while the button is held
     *  ([active] = true on press), off the moment it's released
     *  ([active] = false). */
    fun onMomentaryHold(active: Boolean) {
        controller.setOn(active)
    }

    /** Slider drag handler — keeps the optimistic pending value in
     *  sync so the rest of the combined flow doesn't snap back to
     *  the stored DataStore value mid-drag. Persists nothing. */
    fun onRateChange(newRateHz: Float) {
        pendingRateHz.value = newRateHz.coerceIn(
            TorchWidgetConfig.MIN_RATE_HZ,
            TorchWidgetConfig.MAX_RATE_HZ,
        )
    }

    /** Slider release handler — persists the last optimistic value
     *  to DataStore. Clears the pending shadow on commit. */
    fun onRateCommit() {
        val rate = pendingRateHz.value ?: return
        viewModelScope.launch {
            userPreferences.setDefaultStrobeRateHz(rate)
            pendingRateHz.value = null
        }
    }

    /** Brightness slider drag — updates pending state and applies the level to
     *  the hardware immediately so the torch responds while the user drags.
     *  Persists nothing; [onBrightnessCommit] handles the DataStore write. */
    fun onBrightnessChange(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        pendingBrightness.value = clamped
        controller.setBrightness(clamped)
    }

    /** Brightness slider release — persists the final value and clears the
     *  pending shadow so the DataStore emission takes over. */
    fun onBrightnessCommit() {
        val level = pendingBrightness.value ?: return
        viewModelScope.launch {
            userPreferences.setDefaultTorchBrightness(level)
            pendingBrightness.value = null
        }
    }

    /** Tap-to-toggle constant strobe (mirrors the strobe widget). */
    fun onStrobeToggle() {
        if (strobeRuntime.running.value) stopStrobeService() else startStrobeService(morseText = null)
    }

    /** Momentary constant strobe — runs only while the button is held. */
    fun onStrobeHold(active: Boolean) {
        if (active) startStrobeService(morseText = null) else stopStrobeService()
    }

    /** Tap-to-toggle Morse playback of the persistent [TorchScreenState.morseText]. */
    fun onMorseToggle() {
        if (strobeRuntime.running.value) stopStrobeService() else startStrobeService(morseText = state.value.morseText)
    }

    /** Momentary Morse playback — loops the message only while held. */
    fun onMorseHold(active: Boolean) {
        if (active) startStrobeService(morseText = state.value.morseText) else stopStrobeService()
    }

    /** Persist the in-app Morse message. */
    fun onMorseTextChange(text: String) {
        viewModelScope.launch { userPreferences.setMorseText(text) }
    }

    /** Toggle (and persist) a torch card's collapsed/expanded state. */
    fun onSectionToggle(id: String) {
        viewModelScope.launch { collapseRepo.toggle(id) }
    }

    // ─── Rooted tools ────────────────────────────────────────────────
    // Each run uses the user's persisted parameters (TorchRootToolsConfig,
    // edited via the screen's sliders) and routes through the rooted
    // implementation's RootSafetyGate (capability + opt-out + rate-limit);
    // the result is surfaced via [rootToolEvents]. No-ops on the standard
    // flavor (the seam reports Unsupported and the controls aren't shown).

    fun onRootBoostBrightness() = runRootTool {
        rootCapabilities.boostBrightness(currentRootTools().boostBrightnessPercent)
    }

    fun onRootDutyCycleStrobe() = runRootTool {
        val c = currentRootTools()
        rootCapabilities.dutyCycleStrobe(c.dutyFrequencyHz, c.dutyPercent, c.dutyDurationMs)
    }

    fun onRootMultiLed() = runRootTool {
        val c = currentRootTools()
        rootCapabilities.multiLedActivate(c.multiLedDurationMs, c.multiLedIncludeScreen)
    }

    fun onRootThermalOverride() = runRootTool {
        val c = currentRootTools()
        rootCapabilities.thermalOverrideStrobe(c.thermalFrequencyHz, c.thermalDutyPercent, c.thermalDurationMs)
    }

    /** The parameters a run should use: the optimistic in-flight edit if the
     *  user is mid-drag, else the current persisted view-state value. Clamped
     *  to the live ceiling so a run can never exceed the hardware limit. */
    private fun currentRootTools(): TorchRootToolsConfig =
        (pendingRootTools.value ?: state.value.rootTools)
            .coercedTo(state.value.maxBrightnessPercent)

    /** Slider-drag / toggle handler — keeps the optimistic pending value in
     *  sync so the card reflects the edit live. Persists nothing. */
    fun onRootToolsChange(updated: TorchRootToolsConfig) {
        pendingRootTools.value = updated.coercedTo(state.value.maxBrightnessPercent)
    }

    /** Slider-release / toggle-flip handler — persists the last optimistic
     *  value to DataStore and clears the pending shadow. */
    fun onRootToolsCommit() {
        val config = pendingRootTools.value ?: return
        viewModelScope.launch {
            rootToolsRepo.save(config)
            pendingRootTools.value = null
        }
    }

    private fun runRootTool(action: suspend () -> TorchRootResult) {
        viewModelScope.launch { _rootToolEvents.tryEmit(action()) }
    }

    private fun startStrobeService(morseText: String?) {
        val startIntent = Intent(context, StrobeService::class.java).apply {
            putExtra(StrobeService.EXTRA_RATE_HZ, state.value.defaultStrobeRateHz)
            if (!morseText.isNullOrBlank()) putExtra(StrobeService.EXTRA_MORSE_TEXT, morseText)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (API 31+) is an
            // IllegalStateException subtype the OS throws when the app isn't
            // in an allowed FGS-start window. Degrade gracefully instead of
            // crashing the screen.
            android.util.Log.w("TorchViewModel", "Strobe FGS start refused", e)
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.strobe_widget_start_failed),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun stopStrobeService() {
        context.startService(
            Intent(context, StrobeService::class.java).setAction(StrobeService.ACTION_STOP),
        )
    }

    /** Open the customization sheet for a brand-new widget, seeded with the
     *  flashlight function + the default name. The user picks the function
     *  (flashlight / strobe / morse), tunes its params, and styles it inside
     *  the sheet; confirm requests the pin. */
    fun onAddWidget() {
        _sheetTarget.value = SheetTarget.New(defaultNewConfig())
    }

    /** The seed config a new-widget sheet opens with: the flashlight power
     *  toggle, default look, default size. */
    private fun defaultNewConfig(): TorchWidgetConfig = TorchWidgetConfig(
        displayName = context.getString(R.string.torch_widget_default_name_flashlight),
        actionKey = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
    )

    /** Drive the same pin path the sheet's confirm uses, mapping the
     *  result to the matching one-shot snackbar event. */
    private fun requestPin(config: TorchWidgetConfig) {
        viewModelScope.launch {
            when (widgetCreator.requestPin(config)) {
                WidgetPinResult.Requested -> Unit
                WidgetPinResult.LauncherUnsupported -> _pinUnsupportedEvents.tryEmit(Unit)
                WidgetPinResult.CapReached -> _pinCapReachedEvents.tryEmit(Unit)
            }
        }
    }

    fun onEditWidget(widget: SavedTorchWidget) {
        _sheetTarget.value = SheetTarget.Existing(widget.appWidgetId, widget.config)
    }

    /** Resolve a widget icon key to a render source (bundled drawable or
     *  a user-imported custom file) for the live appearance preview and
     *  the in-app widget list. */
    fun resolveWidgetIcon(key: String): WidgetIconSource = iconCatalog.resolveSource(key)

    /** Copy + downscale a picked image into app-internal storage and
     *  return its stable custom icon key, or `null` on failure. */
    suspend fun importCustomIcon(uri: Uri): String? = iconCatalog.importCustomIcon(uri)

    /** Icons the configuration sheet offers in its icon picker. Mapped
     *  into the kit's generic [WidgetIconChoice] shape so the sheet's
     *  kit-side appearance section can render them without knowing
     *  about torch's catalog. */
    val iconChoices: List<WidgetIconChoice> = iconCatalog.entries.map { entry ->
        WidgetIconChoice(
            key = entry.key,
            drawable = entry.drawable,
            displayName = entry.displayName,
        )
    }

    /**
     * The widget functions the customization sheet offers, flavor-filtered:
     * any `requiresRoot` function is dropped when root isn't available so the
     * picker only lists runnable functions. Torch has no rooted widget
     * functions today (so this is a no-op filter), but the filter is wired for
     * parity with the contract — `rootAvailability` flips on the rooted flavor
     * once it reports a usable shell.
     */
    val functions: List<WidgetFunction>
        get() = widgetFunctionCatalog.functions.filter {
            !it.requiresRoot || rootAvailability.value.rootReady
        }

    fun onSheetDismissed() {
        _sheetTarget.value = null
    }

    /**
     * Commit the customization sheet's [result] into a v2 [TorchWidgetConfig].
     * A new widget pins (the function the user picked rides in `actionKey` +
     * `params`); an edit saves and repaints the placed instance.
     */
    fun onSheetConfirmed(result: WidgetCustomizationResult) {
        when (val target = _sheetTarget.value) {
            is SheetTarget.New -> requestPin(result.toConfig(target.config))
            is SheetTarget.Existing -> {
                val updated = result.toConfig(target.config)
                viewModelScope.launch {
                    widgetRepository.save(target.appWidgetId, updated)
                    // Repaint the placed widget now — saving alone leaves
                    // the launcher's cached RemoteViews stale until the
                    // next tap.
                    broadcastTorchWidgetUpdate(context, target.appWidgetId)
                }
            }
            null -> Unit
        }
        _sheetTarget.value = null
    }

    /** Map the generic sheet result onto a v2 config, preserving the
     *  non-customizable fields ([TorchWidgetConfig.removed] / schema) from the
     *  config the sheet opened with. */
    private fun WidgetCustomizationResult.toConfig(base: TorchWidgetConfig): TorchWidgetConfig =
        base.copy(
            displayName = name.ifBlank { base.displayName },
            actionKey = actionKey,
            params = params,
            sizePreset = sizePreset,
            appearance = appearance,
        )

    fun onDeleteWidget(widget: SavedTorchWidget) {
        viewModelScope.launch {
            // A non-host app can't pull a placed widget off the launcher,
            // so flag it `removed` rather than deleting the config — a
            // plain delete would let the provider self-heal it straight
            // back into the list. This drops it from the list and
            // repaints the home-screen instance inert; dragging it off
            // later fires onDeleted, which purges the config for real.
            widgetRepository.save(
                widget.appWidgetId,
                widget.config.copy(removed = true),
            )
            broadcastTorchWidgetUpdate(context, widget.appWidgetId)
            _widgetRemovedEvents.tryEmit(Unit)
        }
    }

    /** Discriminator for what kind of configuration the sheet should
     *  open with. `New` means a pin request; `Existing` means an edit
     *  of a previously-pinned widget. */
    sealed interface SheetTarget {
        val config: TorchWidgetConfig
        data class New(override val config: TorchWidgetConfig) : SheetTarget
        data class Existing(
            val appWidgetId: Int,
            override val config: TorchWidgetConfig,
        ) : SheetTarget
    }

    private companion object {
        /** How long to keep the combined flow subscribed after the
         *  last UI subscriber leaves. 5 s matches the convention
         *  used in `:feature:settings`. */
        const val SubscriptionTimeoutMillis: Long = 5_000L
    }
}
