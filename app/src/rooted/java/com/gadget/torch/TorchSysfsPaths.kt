package com.gadget.torch

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Device-specific LED sysfs path resolver. The kernel exposes the flashlight
 * driver under different names depending on SoC vendor:
 *
 *   - Qualcomm: `/sys/class/leds/torch-light{0,1}/`
 *   - MediaTek: `/sys/class/leds/flashlight/`
 *   - Pixel/AOSP: `/sys/class/leds/led:flash_torch/`
 *
 * [resolvePrimary] probes each candidate via `test -e` under root and caches
 * the first hit for the lifetime of the process. [resolveAll] additionally
 * enumerates the front LED, aux LEDs, and notification RGB nodes — used by
 * the multi-LED orchestrator in sub-batch 3c.
 */
@Singleton
class TorchSysfsPaths @Inject constructor(
    private val shell: RootShell,
) {
    private var cachedPrimary: TorchLedNode? = null
    private var primaryProbed = false

    suspend fun resolvePrimary(): TorchLedNode? {
        if (!primaryProbed) {
            cachedPrimary = PRIMARY_CANDIDATES.firstOrNull { existsAt(it.directory) }
            primaryProbed = true
        }
        return cachedPrimary
    }

    suspend fun resolveAll(): TorchLedSet {
        val primary = resolvePrimary()
        val front = FRONT_CANDIDATES.firstOrNull { existsAt(it.directory) }
        val notification = NOTIFICATION_CANDIDATES.filter { existsAt(it.directory) }
        return TorchLedSet(
            primary = primary,
            front = front,
            notification = notification,
        )
    }

    private suspend fun existsAt(directory: String): Boolean {
        val result = shell.exec("test -e \"$directory/brightness\"")
        return result.isSuccess
    }

    private companion object {
        val PRIMARY_CANDIDATES = listOf(
            TorchLedNode("/sys/class/leds/torch-light0", "qcom-torch0"),
            TorchLedNode("/sys/class/leds/flashlight", "mtk-flashlight"),
            TorchLedNode("/sys/class/leds/led:flash_torch", "pixel-flash"),
            TorchLedNode("/sys/class/leds/led:torch_0", "aosp-torch0"),
        )

        val FRONT_CANDIDATES = listOf(
            TorchLedNode("/sys/class/leds/torch-light1", "qcom-torch1-front"),
            TorchLedNode("/sys/class/leds/led:flash_torch_front", "pixel-flash-front"),
        )

        val NOTIFICATION_CANDIDATES = listOf(
            TorchLedNode("/sys/class/leds/red", "notif-red"),
            TorchLedNode("/sys/class/leds/green", "notif-green"),
            TorchLedNode("/sys/class/leds/blue", "notif-blue"),
            TorchLedNode("/sys/class/leds/white", "notif-white"),
        )
    }
}

data class TorchLedNode(
    val directory: String,
    val label: String,
) {
    val brightnessPath: String get() = "$directory/brightness"
    val maxBrightnessPath: String get() = "$directory/max_brightness"
}

data class TorchLedSet(
    val primary: TorchLedNode?,
    val front: TorchLedNode?,
    val notification: List<TorchLedNode>,
)
