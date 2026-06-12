package dev.ranzlappen.gadget.feature.vibration

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.monitoring.CollapseStateRepository
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinResult
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetCustomizationResult
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetCreator
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetFunctionCatalog
import dev.ranzlappen.gadget.feature.vibration.widget.broadcastVibrationWidgetUpdate
import dev.ranzlappen.gadget.feature.vibration.widget.customization.VibrationIconCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Aggregating ViewModel for [VibrationScreen]. Mirrors `TorchViewModel`:
 * combines the controller state, saved patterns, widget configs, the rooted
 * config + availability, and the persisted collapse state into a single
 * [VibrationScreenState], and dispatches every [VibrationUiEvent] to a typed
 * handler. Standard ops delegate to [VibrationController]; rooted ops to
 * [VibrationRootCapabilities]; pattern playback uses the controller too.
 */
@HiltViewModel
class VibrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: VibrationController,
    private val rootCapabilities: VibrationRootCapabilities,
    private val patternRepository: PatternRepository,
    private val rootToolsRepo: RootToolsConfigRepository,
    private val widgetRepository: WidgetConfigStore<VibrationWidgetConfig>,
    private val widgetCreator: VibrationWidgetCreator,
    private val functionCatalog: VibrationWidgetFunctionCatalog,
    private val iconCatalog: VibrationIconCatalog,
    private val collapseRepo: CollapseStateRepository,
) : ViewModel() {

    private val rootAvailability = MutableStateFlow(VibrationRootAvailability.Unavailable)

    init {
        viewModelScope.launch { rootAvailability.value = rootCapabilities.probe() }
    }

    /** One-shot rooted-tool results, surfaced as a snackbar by the screen. */
    private val _rootToolEvents = MutableSharedFlow<VibrationRootResult>(extraBufferCapacity = 1)
    val rootToolEvents: SharedFlow<VibrationRootResult> = _rootToolEvents.asSharedFlow()

    private val _pinUnsupportedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pinUnsupportedEvents: SharedFlow<Unit> = _pinUnsupportedEvents.asSharedFlow()

    private val _pinCapReachedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pinCapReachedEvents: SharedFlow<Unit> = _pinCapReachedEvents.asSharedFlow()

    private val _widgetRemovedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val widgetRemovedEvents: SharedFlow<Unit> = _widgetRemovedEvents.asSharedFlow()

    private val _sheetTarget = MutableStateFlow<SheetTarget?>(null)
    val sheetTarget: StateFlow<SheetTarget?> = _sheetTarget.asStateFlow()

    // ─── One-shot control pending state (optimistic, commit-on-release) ──
    private val pendingAmplitude = MutableStateFlow(VibrationScreenState.DEFAULT_ONESHOT_AMPLITUDE)
    private val pendingDurationMs = MutableStateFlow(VibrationScreenState.DEFAULT_ONESHOT_DURATION_MS)

    // ─── Pattern draft (drawn samples, intensity 0..1) ──────────────────
    private val patternDraft = MutableStateFlow<List<Float>>(emptyList())
    private val patternLoop = MutableStateFlow(false)

    // ─── Optimistic rooted-tool edit ────────────────────────────────────
    private val pendingRootTools = MutableStateFlow<VibrationRootToolsConfig?>(null)

    private data class OneShotInputs(val amplitude: Int, val durationMs: Long)
    private data class DraftInputs(val samples: List<Float>, val loop: Boolean)
    private data class RootToolsState(val config: VibrationRootToolsConfig, val maxAmplitudePercent: Int)

    private val oneShot: Flow<OneShotInputs> =
        combine(pendingAmplitude, pendingDurationMs, ::OneShotInputs)

    private val draft: Flow<DraftInputs> =
        combine(patternDraft, patternLoop, ::DraftInputs)

    private val rootToolsState: Flow<RootToolsState> = combine(
        rootToolsRepo.config,
        pendingRootTools,
        rootCapabilities.maxAmplitudePercentFlow,
    ) { persisted, pending, maxAmp ->
        RootToolsState((pending ?: persisted).coercedTo(maxAmp), maxAmp)
    }

    private data class Inputs(
        val vibration: VibrationState,
        val patterns: List<VibrationPattern>,
        val widgets: Map<Int, VibrationWidgetConfig>,
        val oneShot: OneShotInputs,
        val draft: DraftInputs,
    )

    private val inputs: Flow<Inputs> = combine(
        controller.state,
        patternRepository.patterns,
        widgetRepository.all,
        oneShot,
        draft,
        ::Inputs,
    )

    val state: StateFlow<VibrationScreenState> = combine(
        inputs,
        rootAvailability,
        rootToolsState,
        collapseRepo.expandedStates(VibrationSectionId.hoisted),
    ) { i, root, rootTools, expanded ->
        VibrationScreenState(
            vibration = i.vibration,
            oneShotAmplitudePercent = i.oneShot.amplitude,
            oneShotDurationMs = i.oneShot.durationMs,
            patternDraft = i.draft.samples,
            patternLoop = i.draft.loop,
            savedPatterns = i.patterns,
            widgets = i.widgets
                .toSortedMap()
                .filterValues { !it.removed }
                .map { (id, config) -> SavedVibrationWidget(id, config) },
            rootAvailability = root,
            rootTools = rootTools.config,
            maxAmplitudePercent = rootTools.maxAmplitudePercent,
            expandedSections = expanded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = VibrationScreenState.Initial,
    )

    fun onEvent(event: VibrationUiEvent) {
        when (event) {
            is VibrationUiEvent.PredefinedClick -> controller.playPredefined(event.effect)
            VibrationUiEvent.OneShot -> controller.oneShot(pendingAmplitude.value, pendingDurationMs.value)
            is VibrationUiEvent.OneShotAmplitudeChange -> pendingAmplitude.value =
                event.percent.coerceIn(VibrationWidgetConfig.MIN_AMPLITUDE_PERCENT, VibrationWidgetConfig.MAX_AMPLITUDE_PERCENT)
            is VibrationUiEvent.OneShotDurationChange -> pendingDurationMs.value =
                event.durationMs.coerceIn(VibrationWidgetConfig.MIN_DURATION_MS, VibrationWidgetConfig.MAX_DURATION_MS)
            VibrationUiEvent.OneShotCommit -> Unit // sliders are session-only; nothing persisted
            VibrationUiEvent.Stop -> controller.stop()

            is VibrationUiEvent.PatternDraftChange -> patternDraft.value = event.samples
            is VibrationUiEvent.PatternLoopChange -> patternLoop.value = event.loop
            VibrationUiEvent.PatternPlay -> playDraft()
            VibrationUiEvent.PatternClear -> patternDraft.value = emptyList()
            is VibrationUiEvent.PatternSaveRequest -> saveDraft(event.name)
            is VibrationUiEvent.PatternPlaySaved -> playPattern(event.pattern)
            is VibrationUiEvent.PatternDelete -> viewModelScope.launch { patternRepository.delete(event.pattern.id) }

            VibrationUiEvent.AddWidget -> _sheetTarget.value = SheetTarget.New(defaultWidgetConfig())
            is VibrationUiEvent.EditWidget ->
                _sheetTarget.value = SheetTarget.Existing(event.widget.appWidgetId, event.widget.config)
            is VibrationUiEvent.DeleteWidget -> onDeleteWidget(event.widget)
            is VibrationUiEvent.SheetConfirmed -> onSheetConfirmed(event.result)
            VibrationUiEvent.SheetDismissed -> _sheetTarget.value = null

            VibrationUiEvent.RootExtremeAmplitude -> runRootTool {
                val c = currentRootTools()
                rootCapabilities.extremeAmplitude(c.extremeAmplitudePercent, c.extremeBurstMs)
            }
            VibrationUiEvent.RootDirectPwm -> runRootTool {
                val c = currentRootTools()
                rootCapabilities.directPwm(List(c.pwmPulses) { PwmPulse(c.pwmOnMicros, c.pwmOffMicros) })
            }
            VibrationUiEvent.RootDualActuator -> runRootTool {
                val c = currentRootTools()
                val lra = List(c.pwmPulses) { PwmPulse(c.pwmOnMicros, c.pwmOffMicros) }
                rootCapabilities.dualActuator(lra, lra, c.dualPhaseOffsetMicros)
            }
            VibrationUiEvent.RootSustainedRumble -> runRootTool {
                val c = currentRootTools()
                rootCapabilities.sustainedRumble(c.rumbleDurationMs, c.rumbleAmplitudePercent)
            }
            is VibrationUiEvent.RootToolsChange ->
                pendingRootTools.value = event.config.coercedTo(state.value.maxAmplitudePercent)
            VibrationUiEvent.RootToolsCommit -> onRootToolsCommit()

            is VibrationUiEvent.SectionToggle -> viewModelScope.launch { collapseRepo.toggle(event.id) }
        }
    }

    // ─── Pattern helpers ────────────────────────────────────────────────

    /** Convert the drawn 0..1 intensity samples into a waveform and play it. */
    private fun playDraft() {
        val (timings, amplitudes) = draftToWaveform(patternDraft.value) ?: return
        controller.playPattern(timings, amplitudes, loop = patternLoop.value)
    }

    private fun saveDraft(name: String) {
        if (name.isBlank()) return
        val (timings, amplitudes) = draftToWaveform(patternDraft.value) ?: return
        viewModelScope.launch {
            patternRepository.save(
                VibrationPattern(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    timingsMillis = timings.toList(),
                    amplitudes = amplitudes.toList(),
                ),
            )
        }
    }

    private fun playPattern(pattern: VibrationPattern) {
        controller.playPattern(
            pattern.timingsMillis.toLongArray(),
            pattern.amplitudes.toIntArray(),
            loop = false,
        )
    }

    /**
     * Reduce the drawn intensity samples (0..1 over the fixed 2 s window) into
     * a `VibrationEffect.createWaveform` (timings + amplitudes) pair, or null
     * when nothing was drawn. Each sample becomes one fixed-width "on" segment
     * whose amplitude is the drawn intensity (segments at 0 intensity become a
     * silent gap of the same width via a 0 amplitude).
     */
    private fun draftToWaveform(samples: List<Float>): Pair<LongArray, IntArray>? {
        if (samples.isEmpty() || samples.none { it > 0f }) return null
        val segmentMs = PATTERN_WINDOW_MS / samples.size
        val timings = LongArray(samples.size) { segmentMs }
        val amplitudes = IntArray(samples.size) { i ->
            (samples[i].coerceIn(0f, 1f) * RAW_MAX).toInt()
        }
        return timings to amplitudes
    }

    // ─── Rooted tools ───────────────────────────────────────────────────

    private fun currentRootTools(): VibrationRootToolsConfig =
        (pendingRootTools.value ?: state.value.rootTools).coercedTo(state.value.maxAmplitudePercent)

    fun onRootToolsCommit() {
        val config = pendingRootTools.value ?: return
        viewModelScope.launch {
            rootToolsRepo.save(config)
            pendingRootTools.value = null
        }
    }

    private fun runRootTool(action: suspend () -> VibrationRootResult) {
        viewModelScope.launch { _rootToolEvents.tryEmit(action()) }
    }

    // ─── Widgets ────────────────────────────────────────────────────────

    /**
     * The widget functions offered in the customization sheet, **flavor-
     * filtered**: a `requiresRoot` function is dropped unless root is ready on
     * this device, so the standard flavor only ever lists runnable functions.
     */
    val functions: List<WidgetFunction>
        get() = functionCatalog.functions.filter { !it.requiresRoot || rootAvailability.value.rootReady }

    private fun defaultWidgetConfig() = VibrationWidgetConfig(
        displayName = context.getString(R.string.vibration_widget_default_name_vibrate),
        actionKey = VibrationWidgetConfig.FUNCTION_CONTINUOUS,
    )

    private fun requestPin(config: VibrationWidgetConfig) {
        viewModelScope.launch {
            when (widgetCreator.requestPin(config)) {
                WidgetPinResult.Requested -> Unit
                WidgetPinResult.LauncherUnsupported -> _pinUnsupportedEvents.tryEmit(Unit)
                WidgetPinResult.CapReached -> _pinCapReachedEvents.tryEmit(Unit)
            }
        }
    }

    private fun onSheetConfirmed(result: WidgetCustomizationResult) {
        when (val target = _sheetTarget.value) {
            is SheetTarget.New -> requestPin(target.config.applied(result))
            is SheetTarget.Existing -> {
                val updated = target.config.applied(result)
                viewModelScope.launch {
                    widgetRepository.save(target.appWidgetId, updated)
                    broadcastVibrationWidgetUpdate(context, target.appWidgetId)
                }
            }
            null -> Unit
        }
        _sheetTarget.value = null
    }

    /** Fold a [WidgetCustomizationResult] into this base config, producing the
     *  v2 widget config the sheet just edited (name/function/params/size/
     *  appearance), preserving the `removed` flag + schema version. */
    private fun VibrationWidgetConfig.applied(result: WidgetCustomizationResult): VibrationWidgetConfig =
        copy(
            displayName = result.name.ifBlank { displayName },
            actionKey = result.actionKey,
            params = result.params,
            sizePreset = result.sizePreset,
            appearance = result.appearance,
        )

    private fun onDeleteWidget(widget: SavedVibrationWidget) {
        viewModelScope.launch {
            widgetRepository.save(widget.appWidgetId, widget.config.copy(removed = true))
            broadcastVibrationWidgetUpdate(context, widget.appWidgetId)
            _widgetRemovedEvents.tryEmit(Unit)
        }
    }

    fun onSheetDismissed() {
        _sheetTarget.value = null
    }

    fun resolveWidgetIcon(key: String): WidgetIconSource = iconCatalog.resolveSource(key)

    suspend fun importCustomIcon(uri: Uri): String? = iconCatalog.importCustomIcon(uri)

    val iconChoices: List<WidgetIconChoice> = iconCatalog.entries.map { entry ->
        WidgetIconChoice(key = entry.key, drawable = entry.drawable, displayName = entry.displayName)
    }

    sealed interface SheetTarget {
        val config: VibrationWidgetConfig
        data class New(override val config: VibrationWidgetConfig) : SheetTarget
        data class Existing(val appWidgetId: Int, override val config: VibrationWidgetConfig) : SheetTarget
    }

    private companion object {
        const val SubscriptionTimeoutMillis: Long = 5_000L
        const val PATTERN_WINDOW_MS: Long = 2_000L
        const val RAW_MAX = 255
    }
}
