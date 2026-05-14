package dev.ranzlappen.gadget.feature.torch.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Quick Settings tile for the flashlight.
 *
 * [TileService] is system-instantiated, so `@AndroidEntryPoint`
 * Hilt injection doesn't apply. We reach the singleton
 * [TorchController] via [EntryPointAccessors.fromApplication] —
 * the same recipe widgets and other framework components use
 * across the codebase.
 *
 * Lifecycle notes:
 * - [onStartListening] / [onStopListening] bracket the tile being
 *   "visible" in the quick settings panel. We subscribe to the
 *   controller's `state` flow during this window so the tile icon
 *   + label reflect external toggles (in-app screen, widget) in
 *   real time.
 * - [onClick] fires when the user taps the tile in the panel.
 *   We `toggle()` on the controller's singleton, which propagates
 *   back into our `onTileStateChanged` callback via the same flow
 *   subscription.
 *
 * No persistent state in the tile itself — every snapshot comes
 * from the controller.
 */
class FlashlightTileService : TileService() {

    /** Job covering the lifetime of the current listening window. */
    private var listeningJob: Job? = null

    /** Coroutine scope tied to [listeningJob]. Re-created per window. */
    private var listeningScope: CoroutineScope? = null

    private val torchController: TorchController by lazy {
        EntryPointAccessors
            .fromApplication(applicationContext, FlashlightTileServiceEntryPoint::class.java)
            .torchController()
    }

    override fun onStartListening() {
        super.onStartListening()
        // Render the current state immediately so the tile doesn't
        // flicker into a wrong default while the first emission
        // arrives.
        renderState()
        // Subscribe to future state changes for the duration of the
        // listening window.
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + job)
        torchController.state
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
        // toggle() is synchronous (Camera2 setTorchMode is a fast
        // binder call). The TorchCallback updates the shared state
        // before this call returns; the listening-window observer
        // picks it up on the next tick and re-renders.
        torchController.toggle()
    }

    /**
     * Project [TorchController.state.value] onto the tile's
     * visual state. Called eagerly from `onStartListening` and on
     * every state-flow emission while listening.
     */
    private fun renderState() {
        val state = torchController.state.value
        qsTile?.apply {
            this.state = when {
                !state.isAvailable -> Tile.STATE_UNAVAILABLE
                state.isOn -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            label = applicationContext.getString(R.string.torch_tile_label)
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_flashlight_on)
            updateTile()
        }
    }

    /**
     * Hilt entry point — gives a system-instantiated TileService
     * access to the singleton [TorchController] without
     * `@AndroidEntryPoint`.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FlashlightTileServiceEntryPoint {
        fun torchController(): TorchController
    }
}
