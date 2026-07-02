package dev.ranzlappen.gadget.feature.torch.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Quick Settings tile for the flashlight **strobe** — the sibling of
 * [FlashlightTileService]. A tap starts a constant-rate strobe (the
 * feature's default Hz) via [StrobeService]; a second tap stops it.
 *
 * Like every system-instantiated component, [TileService] can't use
 * `@AndroidEntryPoint`, so the two singletons it needs are reached
 * through [EntryPointAccessors.fromApplication]:
 * - [StrobeRuntime] — the process-wide `running` `StateFlow` is the
 *   single source of truth for the tile's active/inactive state, so the
 *   tile tracks strobes started from *any* surface (in-app screen,
 *   widget, automation) in real time.
 * - [TorchController] — read only to gate the tile UNAVAILABLE on a
 *   flashless device (an emulator / flashless tablet), matching the
 *   flashlight tile.
 *
 * Start/stop plumbing mirrors `TorchViewModel`: a foreground-service
 * start (the QS-tile tap is a user-initiated FGS-allowlist window) with
 * a graceful catch for the rare refusal, and a plain `startService`
 * carrying [StrobeService.ACTION_STOP] to tear it down.
 */
class StrobeTileService : TileService() {

    /** Job covering the lifetime of the current listening window. */
    private var listeningJob: Job? = null

    /** Coroutine scope tied to [listeningJob]. Re-created per window. */
    private var listeningScope: CoroutineScope? = null

    private val entryPoint: StrobeTileEntryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, StrobeTileEntryPoint::class.java)
    }
    private val strobeRuntime: StrobeRuntime by lazy { entryPoint.strobeRuntime() }
    private val torchController: TorchController by lazy { entryPoint.torchController() }

    override fun onStartListening() {
        super.onStartListening()
        // Paint the current state immediately so the tile doesn't flash a
        // wrong default before the first flow emission lands.
        renderState()
        // Track strobe start/stop from any surface for the listening window.
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + job)
        strobeRuntime.running
            .onEach { renderState() }
            .launchIn(scope)
        listeningJob = job
        listeningScope = scope
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningJob = null
        listeningScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        // No flash unit → nothing to strobe; the tile is already
        // UNAVAILABLE, but guard the action path too.
        if (!torchController.state.value.isAvailable) return
        if (strobeRuntime.running.value) stopStrobe() else startStrobe()
    }

    private fun startStrobe() {
        val intent = Intent(applicationContext, StrobeService::class.java).apply {
            putExtra(StrobeService.EXTRA_RATE_HZ, TorchWidgetConfig.DEFAULT_RATE_HZ)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (API 31+) subtypes
            // IllegalStateException — the OS refuses an FGS start outside an
            // allowed window. Degrade quietly rather than crash the SystemUI
            // host process; the runtime stays `false`, so the tile is correct.
            Log.w(TAG, "Strobe FGS start refused from tile", e)
        }
    }

    private fun stopStrobe() {
        applicationContext.startService(
            Intent(applicationContext, StrobeService::class.java)
                .setAction(StrobeService.ACTION_STOP),
        )
    }

    /**
     * Project [StrobeRuntime.running] + [TorchController] availability onto
     * the tile's visual state. Called eagerly from `onStartListening` and on
     * every `running` emission while listening.
     */
    private fun renderState() {
        val available = torchController.state.value.isAvailable
        val running = strobeRuntime.running.value
        qsTile?.apply {
            state = when {
                !available -> Tile.STATE_UNAVAILABLE
                running -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            label = applicationContext.getString(R.string.strobe_tile_label)
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_strobe)
            updateTile()
        }
    }

    /**
     * Hilt entry point — gives a system-instantiated TileService access to
     * the singleton [StrobeRuntime] + [TorchController] without
     * `@AndroidEntryPoint`.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StrobeTileEntryPoint {
        fun strobeRuntime(): StrobeRuntime
        fun torchController(): TorchController
    }

    private companion object {
        const val TAG = "StrobeTileService"
    }
}
