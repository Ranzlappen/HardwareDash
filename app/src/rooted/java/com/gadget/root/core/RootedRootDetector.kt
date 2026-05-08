package com.gadget.root.core

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor detector. Probes installed root managers (Magisk first, then
 * KernelSU, then APatch) and asks libsu for an actual privileged shell. The
 * result is cached for the lifetime of the process — re-probing would
 * re-trigger the user's su prompt on every navigation event.
 */
@Singleton
class RootedRootDetector @Inject constructor(
    private val magiskProbe: MagiskProviderProbe,
    private val kernelSuProbe: KernelSuProviderProbe,
    private val apatchProbe: ApatchProviderProbe,
) : RootDetector {

    @Volatile private var cached: RootDetection? = null
    private val lock = Mutex()

    override suspend fun detect(): RootDetection {
        cached?.let { return it }
        return lock.withLock {
            cached?.let { return@withLock it }
            val result = withContext(Dispatchers.IO) {
                runCatching { detectInternal() }
                    .onFailure { Timber.w(it, "Root probe failed") }
                    .getOrDefault(RootDetection.None)
            }
            cached = result
            result
        }
    }

    private fun detectInternal(): RootDetection {
        val providerInfo = magiskProbe.probe()
            ?: kernelSuProbe.probe()
            ?: apatchProbe.probe()

        // Negotiate a shell off-main. libsu blocks here while su asks the user
        // to grant access; on no-root devices it returns a non-root shell
        // immediately.
        val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)

        return when {
            isRoot && providerInfo != null -> RootDetection.Available(providerInfo)
            isRoot && providerInfo == null ->
                RootDetection.Available(RootProviderInfo(RootProvider.Unknown, null, null))
            !isRoot && providerInfo != null -> RootDetection.AvailableButDenied(providerInfo.provider)
            else -> RootDetection.None
        }
    }
}
