package com.gadget.microphone

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Probes ALSA / TinyALSA sysfs to locate the microphone capture devices
 * and the mixer control device. Caches the result for the lifetime of
 * the process.
 *
 * The probe is deliberately permissive: it returns whatever `/proc/asound`
 * reports rather than hardcoding vendor-specific paths, so the same code
 * works on Qualcomm, MediaTek, Exynos, and Tensor SoCs.
 */
@Singleton
class MicSysfsPaths @Inject constructor(
    private val shell: RootShell,
) {
    private var cached: AlsaSurface? = null
    private var probed = false

    suspend fun resolve(): AlsaSurface {
        if (!probed) {
            cached = probeOnce()
            probed = true
        }
        return cached ?: AlsaSurface.EMPTY
    }

    private suspend fun probeOnce(): AlsaSurface {
        val captureDevices = listCaptureDevices()
        val mixer = locateMixer()
        return AlsaSurface(captureDevices = captureDevices, mixerDevice = mixer)
    }

    private suspend fun listCaptureDevices(): List<String> {
        val ls = shell.exec("ls -1 /dev/snd 2>/dev/null")
        if (!ls.isSuccess) return emptyList()
        return ls.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .filter { it.matches(Regex("pcmC\\d+D\\d+c")) }
            .map { "/dev/snd/$it" }
    }

    private suspend fun locateMixer(): String? {
        val ls = shell.exec("ls -1 /dev/snd 2>/dev/null")
        if (!ls.isSuccess) return null
        return ls.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.matches(Regex("controlC\\d+")) }
            ?.let { "/dev/snd/$it" }
    }
}

data class AlsaSurface(
    val captureDevices: List<String>,
    val mixerDevice: String?,
) {
    val anyAvailable: Boolean get() = captureDevices.isNotEmpty() || mixerDevice != null

    companion object {
        val EMPTY = AlsaSurface(emptyList(), null)
    }
}
