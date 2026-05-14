package dev.ranzlappen.gadget.feature.torch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfigRepository
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetCreator
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aggregating ViewModel for [TorchScreen].
 *
 * Combines three reactive sources into a single [TorchScreenState]:
 * - [TorchController.state] — live hardware snapshot.
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }] —
 *   the persisted slider value that becomes the default rate at
 *   widget-pin time.
 * - [TorchWidgetConfigRepository.all] — every persisted widget config.
 *
 * Event handlers cover the screen's full surface area:
 * - [onToggleClick] — passthrough to the controller.
 * - [onRateChange] — debounced commit to
 *   [UserPreferencesRepository.setDefaultStrobeRateHz]. The slider
 *   fires onValueChange at ~60 Hz while the user drags; without
 *   debouncing we'd write to DataStore on every emission. 150 ms
 *   is the sweet spot: fast enough to feel live, slow enough to
 *   coalesce a 1-second drag into ~7 writes.
 * - [onAddFlashlight] / [onAddStrobe] — kick off the
 *   [TorchWidgetCreator] pin flow with the current default rate.
 * - [onEditWidget] — overwrite a saved config.
 * - [onDeleteWidget] — purge a saved config. Note: doesn't remove
 *   the widget from the home screen — that's the user's gesture.
 *   Removing the config here just orphans the on-screen widget,
 *   which falls back to its provider's default rendering.
 */
@HiltViewModel
class TorchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TorchController,
    private val userPreferences: UserPreferencesRepository,
    private val widgetRepository: TorchWidgetConfigRepository,
    private val widgetCreator: TorchWidgetCreator,
) : ViewModel() {

    /** Public read-only torch hardware snapshot for callers that
     *  only need the toggle state (e.g. unit tests of click
     *  behaviour without standing up the full combined flow). */
    val torchState: StateFlow<TorchState> = controller.state

    @OptIn(FlowPreview::class)
    val state: StateFlow<TorchScreenState> = combine(
        controller.state,
        userPreferences.flow.map { it.defaultStrobeRateHz },
        widgetRepository.all,
    ) { torch, rateHz, widgets ->
        TorchScreenState(
            torch = torch,
            defaultStrobeRateHz = rateHz,
            widgets = widgets
                .toSortedMap()
                .map { (id, config) -> SavedTorchWidget(id, config) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = TorchScreenState.Initial,
    )

    /**
     * One-shot signal raised when the user requests a widget pin but
     * the active launcher doesn't support
     * [android.appwidget.AppWidgetManager.requestPinAppWidget]. The
     * screen observes this and shows a transient message.
     */
    private val _pinUnsupportedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pinUnsupportedEvents: SharedFlow<Unit> = _pinUnsupportedEvents.asSharedFlow()

    /**
     * Transient "open the configuration sheet" signal. The
     * [TorchScreen] composable owns the visible-or-not state of the
     * sheet; this flow only signals "the user clicked Add strobe"
     * and "the user clicked Edit on widget X" so the screen can
     * stash the right initial config and flip the visible flag.
     */
    private val _sheetTarget = MutableStateFlow<SheetTarget?>(null)
    val sheetTarget: StateFlow<SheetTarget?> = _sheetTarget.asStateFlow()

    /** Debounced commit pipeline for the strobe-rate slider. */
    private val rateChangeFlow = MutableSharedFlow<Float>(extraBufferCapacity = 1)

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            rateChangeFlow
                .debounce(RateChangeDebounceMillis)
                .collect { rate ->
                    userPreferences.setDefaultStrobeRateHz(rate)
                }
        }
    }

    fun onToggleClick() {
        controller.toggle()
    }

    fun onRateChange(newRateHz: Float) {
        rateChangeFlow.tryEmit(
            newRateHz.coerceIn(TorchWidgetConfig.MIN_RATE_HZ, TorchWidgetConfig.MAX_RATE_HZ),
        )
    }

    fun onAddFlashlight() {
        val name = context.getString(R.string.torch_widget_default_name_flashlight)
        val config = TorchWidgetConfig(
            type = WidgetType.Flashlight,
            displayName = name,
        )
        if (!widgetCreator.requestPin(config)) {
            _pinUnsupportedEvents.tryEmit(Unit)
        }
    }

    fun onAddStrobeRequested() {
        val name = context.getString(R.string.torch_widget_default_name_strobe)
        val config = TorchWidgetConfig(
            type = WidgetType.Strobe,
            displayName = name,
            rateHz = state.value.defaultStrobeRateHz,
        )
        _sheetTarget.value = SheetTarget.New(config)
    }

    fun onEditWidget(widget: SavedTorchWidget) {
        _sheetTarget.value = SheetTarget.Existing(widget.appWidgetId, widget.config)
    }

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
                }
            }
            null -> Unit
        }
        _sheetTarget.value = null
    }

    fun onDeleteWidget(widget: SavedTorchWidget) {
        viewModelScope.launch {
            widgetRepository.delete(widget.appWidgetId)
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

        /** Slider debounce window in ms. See [onRateChange] KDoc. */
        const val RateChangeDebounceMillis: Long = 150L
    }
}
