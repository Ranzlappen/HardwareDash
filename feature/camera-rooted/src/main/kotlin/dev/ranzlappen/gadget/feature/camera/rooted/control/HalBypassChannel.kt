package dev.ranzlappen.gadget.feature.camera.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.camera.control.CameraControllerResult
import javax.inject.Inject
import javax.inject.Singleton

private const val V4L2_LIST_COMMAND = "ls -1 /dev/video* 2>/dev/null"
private const val PRIVACY_LED_PATH_GLOB = "ls -1 /sys/class/leds/ 2>/dev/null"

/**
 * Best-effort raw v4l2 frame access via `/dev/video*`. Most modern Android
 * devices route capture through Camera HAL3 and do not expose useful v4l2
 * — the impl returns [CameraControllerResult.Unsupported] in that common
 * case rather than pretending to work.
 *
 * Before any access the impl checks the privacy-LED state on devices that
 * expose it via `/sys/class/leds/...camera...` and aborts if the LED is
 * queryable but not asserted. **The impl never writes to LED nodes** — the
 * privacy LED is inviolate.
 */
@Singleton
class HalBypassChannel @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun probeAndCapture(): CameraControllerResult {
        val ledStatus = checkPrivacyLed()
        if (ledStatus is PrivacyLedStatus.AssertableButOff) {
            return CameraControllerResult.HardwareError(
                "privacy LED at ${ledStatus.path} is not asserted; aborting HAL bypass",
            )
        }

        val nodes = enumerateV4l2Nodes()
        if (nodes.isEmpty()) {
            return CameraControllerResult.Unsupported
        }

        // Without device-specific knowledge of the v4l2 driver layout most
        // /dev/video* nodes on Android map to Camera HAL passthrough, not
        // raw sensor IO. We surface this honestly as Unsupported rather
        // than attempt a speculative ioctl that would either fail or — on
        // poorly-implemented HALs — desync the camera service.
        return CameraControllerResult.Unsupported
    }

    private suspend fun enumerateV4l2Nodes(): List<String> {
        val result = shell.exec(V4L2_LIST_COMMAND)
        if (!result.isSuccess) return emptyList()
        return result.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .filter { it.startsWith("/dev/video") }
    }

    private suspend fun checkPrivacyLed(): PrivacyLedStatus {
        val listing = shell.exec(PRIVACY_LED_PATH_GLOB)
        if (!listing.isSuccess) return PrivacyLedStatus.Absent
        val cameraLedDir = listing.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.contains("camera", ignoreCase = true) || it.contains("priv", ignoreCase = true) }
            ?: return PrivacyLedStatus.Absent
        val brightnessPath = "/sys/class/leds/$cameraLedDir/brightness"
        val read = shell.exec("cat \"$brightnessPath\"")
        val value = read.stdout.firstOrNull()?.trim()?.toIntOrNull()
            ?: return PrivacyLedStatus.Absent
        return if (value > 0) {
            PrivacyLedStatus.Asserted(brightnessPath)
        } else {
            PrivacyLedStatus.AssertableButOff(brightnessPath)
        }
    }
}

sealed class PrivacyLedStatus {
    data object Absent : PrivacyLedStatus()
    data class Asserted(val path: String) : PrivacyLedStatus()
    data class AssertableButOff(val path: String) : PrivacyLedStatus()
}
