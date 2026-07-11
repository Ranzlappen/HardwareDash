package dev.ranzlappen.gadget.feature.vibration.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Quick Settings tile for continuous vibration — the vibration analogue of
 * torch's [FlashlightTileService]. A tap starts / stops a held continuous
 * vibration through the shared `@Singleton` [VibrationController], so the
 * tile, the in-app screen, the widgets, and automation all read the same
 * state.
 *
 * [TileService] is system-instantiated, so `@AndroidEntryPoint` doesn't
 * apply; the controller is reached via [EntryPointAccessors.fromApplication]
 * (the widget/tile recipe). The tile subscribes to `state` only during its
 * `onStartListening` ↔ `onStopListening` visibility window — no work while
 * the panel is closed. Active-state reads `isSustained` (a held continuous
 * command), not `isActive` (which is also true for a decaying one-shot), so
 * the tile reflects the real toggle state.
 */
class VibrateTileService : TileService() {

    private var listeningJob: Job? = null
    private var listeningScope: CoroutineScope? = null

    private val controller: VibrationController by lazy {
        EntryPointAccessors
            .fromApplication(applicationContext, VibrateTileEntryPoint::class.java)
            .vibrationController()
    }

    override fun onStartListening() {
        super.onStartListening()
        renderState()
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + job)
        controller.state
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
        val state = controller.state.value
        if (!state.isAvailable) return
        if (state.isSustained) {
            controller.stop()
        } else {
            controller.startContinuous(DEFAULT_TILE_AMPLITUDE_PERCENT)
        }
    }

    private fun renderState() {
        val state = controller.state.value
        qsTile?.apply {
            this.state = when {
                !state.isAvailable -> Tile.STATE_UNAVAILABLE
                state.isSustained -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            label = applicationContext.getString(R.string.vibration_tile_label)
            icon = Icon.createWithResource(
                applicationContext,
                if (state.isSustained) R.drawable.ic_vibration_on else R.drawable.ic_vibration_off,
            )
            updateTile()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VibrateTileEntryPoint {
        fun vibrationController(): VibrationController
    }

    private companion object {
        /** A firm-but-not-max default strength for the one-tap tile. */
        const val DEFAULT_TILE_AMPLITUDE_PERCENT = 70
    }
}
