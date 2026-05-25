package dev.ranzlappen.gadget.feature.torch

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfigRepository
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetCreator
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import dev.ranzlappen.gadget.feature.torch.widget.broadcastTorchWidgetUpdate
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// (Unused-import cleanup: `FlowPreview` + `debounce` removed —
// the slider now commits exactly once via onValueChangeFinished
// so no debounce pipeline is needed.)

/**
 * Aggregating ViewModel for [TorchScreen].
 *
 * Combines four reactive sources into a single [TorchScreenState]:
 * - [TorchController.state] — live hardware snapshot.
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }] —
 *   the persisted slider value that becomes the default rate at
 *   widget-pin time.
 * - [TorchWidgetConfigRepository.all] — every persisted widget config.
 * - A polled [StrobeService.isRunning] flag (250 ms cadence) — drives
 *   the in-app strobe toggle button label / pressed state.
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
 * - [onAddFlashlight] / [onAddStrobeRequested] — kick off the
 *   [TorchWidgetCreator] pin flow.
 * - [onEditWidget] / [onSheetConfirmed] / [onSheetDismissed] —
 *   widget configuration sheet round-trip.
 * - [onDeleteWidget] — purge a saved config.
 */
@HiltViewModel
class TorchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TorchController,
    private val userPreferences: UserPreferencesRepository,
    private val widgetRepository: TorchWidgetConfigRepository,
    private val widgetCreator: TorchWidgetCreator,
    private val iconCatalog: WidgetIconCatalog,
) : ViewModel() {

    /** Public read-only torch hardware snapshot. */
    val torchState: StateFlow<TorchState> = controller.state

    /** Latest slider value the user landed on; written through to
     *  DataStore by [onRateCommit]. */
    private val pendingRateHz = MutableStateFlow<Float?>(null)

    /** Polled hot signal mirroring [StrobeService.isRunning]. 250 ms
     *  cadence is plenty for a button-label flip and stays cheap
     *  (one @Volatile read per tick). */
    private val strobeRunning: StateFlow<Boolean> = flow {
        while (true) {
            emit(StrobeService.isRunning)
            delay(StrobeRunningPollMillis)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = StrobeService.isRunning,
    )

    val state: StateFlow<TorchScreenState> = combine(
        controller.state,
        userPreferences.flow.map { it.defaultStrobeRateHz },
        widgetRepository.all,
        strobeRunning,
        userPreferences.flow.map { it.morseText },
    ) { torch, rateHz, widgets, running, morseText ->
        TorchScreenState(
            torch = torch,
            defaultStrobeRateHz = pendingRateHz.value ?: rateHz,
            widgets = widgets
                .toSortedMap()
                // Drop widgets the user deleted in-app (kept on disk as
                // `removed` only so the provider stops self-healing them).
                .filterValues { !it.removed }
                .map { (id, config) -> SavedTorchWidget(id, config) },
            strobeRunning = running,
            morseText = morseText,
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

    /** Tap-to-toggle constant strobe (mirrors the strobe widget). */
    fun onStrobeToggle() {
        if (StrobeService.isRunning) stopStrobeService() else startStrobeService(morseText = null)
    }

    /** Momentary constant strobe — runs only while the button is held. */
    fun onStrobeHold(active: Boolean) {
        if (active) startStrobeService(morseText = null) else stopStrobeService()
    }

    /** Tap-to-toggle Morse playback of the persistent [TorchScreenState.morseText]. */
    fun onMorseToggle() {
        if (StrobeService.isRunning) stopStrobeService() else startStrobeService(morseText = state.value.morseText)
    }

    /** Momentary Morse playback — loops the message only while held. */
    fun onMorseHold(active: Boolean) {
        if (active) startStrobeService(morseText = state.value.morseText) else stopStrobeService()
    }

    /** Persist the in-app Morse message. */
    fun onMorseTextChange(text: String) {
        viewModelScope.launch { userPreferences.setMorseText(text) }
    }

    private fun startStrobeService(morseText: String?) {
        val startIntent = Intent(context, StrobeService::class.java).apply {
            putExtra(StrobeService.EXTRA_RATE_HZ, state.value.defaultStrobeRateHz)
            if (!morseText.isNullOrBlank()) putExtra(StrobeService.EXTRA_MORSE_TEXT, morseText)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        } else {
            context.startService(startIntent)
        }
    }

    private fun stopStrobeService() {
        context.startService(
            Intent(context, StrobeService::class.java).setAction(StrobeService.ACTION_STOP),
        )
    }

    fun onAddFlashlight() {
        // Both Add flows open the configuration sheet now that
        // appearance + tap + feedback are per-widget knobs — having
        // flashlight pin directly while strobe opens a sheet was an
        // inconsistency users noticed (the appearance picker would
        // never reach the flashlight path).
        val name = context.getString(R.string.torch_widget_default_name_flashlight)
        val config = TorchWidgetConfig(
            type = WidgetType.Flashlight,
            displayName = name,
        )
        _sheetTarget.value = SheetTarget.New(config)
    }

    fun onAddStrobeRequested() {
        val name = context.getString(R.string.torch_widget_default_name_strobe)
        val config = TorchWidgetConfig(
            type = WidgetType.Strobe,
            displayName = name,
            rateHz = state.value.defaultStrobeRateHz,
            // Pre-fill the Morse box so flipping on Morse mode plays
            // "SOS" out of the box without the user typing anything.
            morseText = StrobeService.DEFAULT_MORSE_TEXT,
        )
        _sheetTarget.value = SheetTarget.New(config)
    }

    fun onEditWidget(widget: SavedTorchWidget) {
        _sheetTarget.value = SheetTarget.Existing(widget.appWidgetId, widget.config)
    }

    /** Resolve a widget icon key to its drawable resource for the
     *  configuration sheet's live appearance preview. */
    fun resolveWidgetIcon(key: String): Int = iconCatalog.resolve(key)

    /** Icons the configuration sheet offers in its icon picker. */
    val iconChoices: List<WidgetIconCatalog.Entry> = iconCatalog.entries

    fun onSheetDismissed() {
        _sheetTarget.value = null
    }

    fun onSheetConfirmed(updated: TorchWidgetConfig) {
        when (val target = _sheetTarget.value) {
            is SheetTarget.New -> {
                if (!widgetCreator.requestPin(updated)) {
                    _pinUnsupportedEvents.tryEmit(Unit)
                }
            }
            is SheetTarget.Existing -> {
                viewModelScope.launch {
                    widgetRepository.save(target.appWidgetId, updated)
                    // Repaint the placed widget now — saving alone leaves
                    // the launcher's cached RemoteViews stale until the
                    // next tap.
                    broadcastTorchWidgetUpdate(context, updated.type, target.appWidgetId)
                }
            }
            null -> Unit
        }
        _sheetTarget.value = null
    }

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
            broadcastTorchWidgetUpdate(context, widget.config.type, widget.appWidgetId)
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

        /** Polling cadence for [StrobeService.isRunning]. 250 ms is
         *  imperceptible to the user but cheap enough to leave
         *  always-on while the screen is visible. */
        const val StrobeRunningPollMillis: Long = 250L
    }
}
