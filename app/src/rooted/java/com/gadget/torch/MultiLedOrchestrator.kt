package com.gadget.torch

import dev.ranzlappen.gadget.core.root.core.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val SCREEN_MAX_BRIGHTNESS = 255
private const val DEFAULT_SCREEN_FALLBACK = 128

/**
 * Drives every available emitter at once: primary back LED, front LED, and
 * notification RGB LEDs. Optionally pushes screen brightness to maximum via
 * `settings put system` (root bypasses the WRITE_SETTINGS permission).
 *
 * Always restores all written nodes to their pre-override values in a
 * `finally` block — even if [activate]'s caller cancels.
 */
@Singleton
class MultiLedOrchestrator @Inject constructor(
    private val shell: RootShell,
    private val paths: TorchSysfsPaths,
) {
    suspend fun activate(durationMillis: Long, includeScreen: Boolean) {
        val set = paths.resolveAll()
        val originalScreenBrightness = if (includeScreen) currentScreenBrightness() else null
        try {
            coroutineScope {
                val nodes = buildList {
                    set.primary?.let(::add)
                    set.front?.let(::add)
                    addAll(set.notification)
                }
                nodes.map { node -> async(Dispatchers.IO) { writeMaxBrightness(node) } }.awaitAll()
            }
            if (includeScreen) setScreenBrightness(SCREEN_MAX_BRIGHTNESS)
            delay(durationMillis)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                set.primary?.let { writeBrightness(it, 0) }
                set.front?.let { writeBrightness(it, 0) }
                set.notification.forEach { writeBrightness(it, 0) }
                originalScreenBrightness?.let { setScreenBrightness(it) }
            }
        }
    }

    private suspend fun writeMaxBrightness(node: TorchLedNode) {
        val maxResult = shell.exec("cat \"${node.maxBrightnessPath}\"")
        val max = maxResult.stdout.firstOrNull()?.trim()?.toIntOrNull() ?: return
        shell.exec("echo $max > \"${node.brightnessPath}\"")
    }

    private suspend fun writeBrightness(node: TorchLedNode, value: Int) {
        shell.exec("echo $value > \"${node.brightnessPath}\"")
    }

    private suspend fun currentScreenBrightness(): Int {
        val result = shell.exec("settings get system screen_brightness")
        return result.stdout.firstOrNull()?.trim()?.toIntOrNull() ?: DEFAULT_SCREEN_FALLBACK
    }

    private suspend fun setScreenBrightness(value: Int) {
        shell.exec("settings put system screen_brightness $value")
    }
}
