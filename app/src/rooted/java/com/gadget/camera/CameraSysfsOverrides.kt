package com.gadget.camera

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sysfs / system-property overrides for camera behaviour. Currently
 * implements only the shutter-sound toggle.
 *
 * **The privacy LED is never touched.** If a device exposes a
 * `/sys/class/leds/` brightness node for the privacy indicator, this
 * controller refuses to write to it regardless of the user's opt-in
 * state. The LED is a hardware-enforced consent signal and the rooted
 * flavor is not a tool to defeat it.
 */
@Singleton
class CameraSysfsOverrides @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun setShutterSoundEnabled(enabled: Boolean): CameraControllerResult {
        val value = if (enabled) "1" else "0"
        val results = SHUTTER_SOUND_PROPS.map { prop ->
            shell.exec("setprop $prop $value")
        }
        val anySuccess = results.any { it.isSuccess }
        if (!anySuccess) {
            // Try resetprop (Magisk) as a fallback for ro.* props.
            val fallback = SHUTTER_SOUND_PROPS.map { prop ->
                shell.exec("resetprop $prop $value")
            }
            if (fallback.none { it.isSuccess }) {
                return CameraControllerResult.HardwareError("setprop and resetprop both failed for shutter-sound")
            }
        }
        return CameraControllerResult.Ok
    }

    private companion object {
        // Multiple known props across vendors. Writing to a non-existent
        // prop is harmless; writing to one the device honors silences the
        // shutter sound.
        val SHUTTER_SOUND_PROPS = listOf(
            "audio.camerasound.force",
            "ro.camera.sound.forced",
            "persist.camera.shutter.disable",
        )
    }
}
