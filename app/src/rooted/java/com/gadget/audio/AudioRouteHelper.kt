package com.gadget.audio

import android.content.Context
import android.media.AudioManager
import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Force-routing helper. Records the prior routing state
 * (`isSpeakerphoneOn`, `isBluetoothScoOn`, `mode`) in the mutation log
 * under `audio-policy://routing/<timestamp>` so the revert path can
 * restore it even after process kill.
 *
 * The actual route flip uses both `cmd audio set-route` (via the
 * privileged shell) and the framework `AudioManager` setters — because
 * the framework setters may revert the cmd-shell change once the user
 * leaves the screen, registering the prior state in the mutation log
 * is the robust path.
 */
@Singleton
class AudioRouteHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
    @ApplicationContext private val context: Context,
) {
    suspend fun applyRoute(target: AudioRoutingTarget): RouteOutcome {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return RouteOutcome.Unavailable
        val priorTarget = currentTarget(audioManager)
        val pseudoPath = "audio-policy://routing/${System.currentTimeMillis()}"
        mutationLog.register(pseudoPath, priorTarget.name)
        when (target) {
            AudioRoutingTarget.SPEAKER -> {
                shell.exec("cmd audio set-route SPEAKER")
                audioManager.isSpeakerphoneOn = true
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            AudioRoutingTarget.EARPIECE -> {
                shell.exec("cmd audio set-route EARPIECE")
                audioManager.isSpeakerphoneOn = false
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            AudioRoutingTarget.BLUETOOTH_SCO -> {
                shell.exec("cmd audio set-route BLUETOOTH_SCO")
                audioManager.isSpeakerphoneOn = false
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            AudioRoutingTarget.WIRED_HEADSET -> {
                shell.exec("cmd audio set-route WIRED_HEADSET")
                audioManager.isSpeakerphoneOn = false
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
        }
        return RouteOutcome.Applied(priorTarget = priorTarget, appliedTarget = target)
    }

    private fun currentTarget(audioManager: AudioManager): AudioRoutingTarget = when {
        audioManager.isBluetoothScoOn -> AudioRoutingTarget.BLUETOOTH_SCO
        audioManager.isSpeakerphoneOn -> AudioRoutingTarget.SPEAKER
        else -> AudioRoutingTarget.EARPIECE
    }
}

sealed class RouteOutcome {
    data object Unavailable : RouteOutcome()
    data class Applied(
        val priorTarget: AudioRoutingTarget,
        val appliedTarget: AudioRoutingTarget,
    ) : RouteOutcome()
}
