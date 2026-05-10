package com.gadget.gps.spoof

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mode-agnostic emission loop. Both flavors construct one of these and use
 * [start]/[stop] from their controller; the flavor-specific controllers add
 * permission grants (rooted: AppOp via libsu) and the safety-gate wrapper
 * before delegating here.
 *
 * The engine owns:
 *   - The MutableStateFlow<SpoofState> exposed by the controllers.
 *   - The Job that runs the emission loop.
 *   - The TestProviderManager lifecycle (start/stop on session boundaries).
 *
 * It does NOT own:
 *   - The legal acknowledgement check (controller responsibility).
 *   - The RootSafetyGate check (rooted controller responsibility).
 *   - The AppOp grant (rooted controller responsibility, before [start]).
 *   - The foreground service start (this triggers it via
 *     [LocationSpoofService.start] for non-Static configs).
 */
@Singleton
internal class SpoofEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val testProviders: TestProviderManager,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var emissionJob: Job? = null

    private val _state = MutableStateFlow<SpoofState>(SpoofState.Idle)
    val state: StateFlow<SpoofState> = _state.asStateFlow()

    /**
     * Starts emitting. The caller is responsible for verifying that the
     * test-provider AppOp / Mock Location App selection is in place — if
     * not, this method will surface the SecurityException as
     * [SpoofResult.Failed].
     */
    suspend fun start(
        config: SpoofConfig,
        activeModes: Set<SpoofMode>,
        sessionLimitMs: Long = GpsSpoofController.DEFAULT_SESSION_LIMIT_MS,
    ): SpoofResult {
        // Idempotent: a force-stop may have left state Idle but providers registered.
        emissionJob?.cancel()

        return try {
            testProviders.start()
        } catch (e: SecurityException) {
            return SpoofResult.Unsupported(
                reason = "Mock-location grant missing",
                helperIntentAction = android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            )
        }.let {
            launchEmission(config, activeModes, sessionLimitMs)
            SpoofResult.Ok
        }
    }

    private fun launchEmission(
        config: SpoofConfig,
        activeModes: Set<SpoofMode>,
        sessionLimitMs: Long,
    ) {
        val startedAt = System.currentTimeMillis()
        emissionJob = scope.launch {
            try {
                when (config) {
                    is SpoofConfig.Static -> runStatic(config, activeModes, startedAt, sessionLimitMs)
                    is SpoofConfig.GpxPlayback -> runGpx(config, activeModes, startedAt, sessionLimitMs)
                    is SpoofConfig.KmlPlayback -> runKml(config, activeModes, startedAt, sessionLimitMs)
                    is SpoofConfig.Route -> runRoute(config, activeModes, startedAt, sessionLimitMs)
                }
            } catch (t: Throwable) {
                _state.value = SpoofState.Error(
                    reason = t.message ?: t.javaClass.simpleName,
                    retriable = true,
                )
            } finally {
                withContext(NonCancellable) { teardown() }
            }
        }
    }

    private suspend fun runStatic(
        cfg: SpoofConfig.Static,
        modes: Set<SpoofMode>,
        startedAt: Long,
        sessionLimitMs: Long,
    ) {
        _state.value = SpoofState.Running(
            activeModes = modes,
            sourceLabel = "Static",
            currentLat = cfg.lat,
            currentLon = cfg.lon,
            startedAtMs = startedAt,
            sessionLimitMs = sessionLimitMs,
        )
        // Re-emit every second so consumers see "fresh" locations.
        val deadline = startedAt + sessionLimitMs
        while (System.currentTimeMillis() < deadline) {
            testProviders.emitStatic(cfg)
            delay(EMIT_INTERVAL_MS)
        }
    }

    private suspend fun runGpx(
        cfg: SpoofConfig.GpxPlayback,
        modes: Set<SpoofMode>,
        startedAt: Long,
        sessionLimitMs: Long,
    ) {
        val waypoints = parseInputStream(cfg.source, GpxParser.MAX_BYTES) { stream ->
            GpxParser.parse(stream)
        }
        runWaypoints(
            waypoints = waypoints,
            interpolation = SpoofConfig.Route.Interpolation.Linear,
            defaultSpeedMps = cfg.defaultSpeedMps,
            speedMultiplier = cfg.speedMultiplier,
            loop = cfg.loop,
            label = "GPX",
            modes = modes,
            startedAt = startedAt,
            sessionLimitMs = sessionLimitMs,
        )
    }

    private suspend fun runKml(
        cfg: SpoofConfig.KmlPlayback,
        modes: Set<SpoofMode>,
        startedAt: Long,
        sessionLimitMs: Long,
    ) {
        val waypoints = parseInputStream(cfg.source, KmlParser.MAX_BYTES) { stream ->
            KmlParser.parse(stream)
        }
        runWaypoints(
            waypoints = waypoints,
            interpolation = SpoofConfig.Route.Interpolation.Linear,
            defaultSpeedMps = cfg.defaultSpeedMps,
            speedMultiplier = cfg.speedMultiplier,
            loop = cfg.loop,
            label = "KML",
            modes = modes,
            startedAt = startedAt,
            sessionLimitMs = sessionLimitMs,
        )
    }

    private suspend fun runRoute(
        cfg: SpoofConfig.Route,
        modes: Set<SpoofMode>,
        startedAt: Long,
        sessionLimitMs: Long,
    ) {
        runWaypoints(
            waypoints = cfg.waypoints,
            interpolation = cfg.interpolation,
            defaultSpeedMps = cfg.defaultSpeedMps,
            speedMultiplier = 1f,
            loop = cfg.loop,
            label = "Route",
            modes = modes,
            startedAt = startedAt,
            sessionLimitMs = sessionLimitMs,
        )
    }

    private suspend fun runWaypoints(
        waypoints: List<Waypoint>,
        interpolation: SpoofConfig.Route.Interpolation,
        defaultSpeedMps: Float,
        speedMultiplier: Float,
        loop: Boolean,
        label: String,
        modes: Set<SpoofMode>,
        startedAt: Long,
        sessionLimitMs: Long,
    ) {
        val engine = RouteEngine(
            waypoints = waypoints,
            interpolation = interpolation,
            defaultSpeedMps = defaultSpeedMps,
            speedMultiplier = speedMultiplier,
            loop = loop,
        )

        // Kick off the foreground service so playback survives Doze.
        LocationSpoofService.start(context)

        val deadline = startedAt + sessionLimitMs
        while (System.currentTimeMillis() < deadline) {
            val elapsed = System.currentTimeMillis() - startedAt
            val sample = engine.sample(elapsed)
            testProviders.emitSample(sample)
            _state.value = SpoofState.Running(
                activeModes = modes,
                sourceLabel = label,
                currentLat = sample.waypoint.lat,
                currentLon = sample.waypoint.lon,
                startedAtMs = startedAt,
                sessionLimitMs = sessionLimitMs,
            )
            if (sample.isFinal) break
            delay(EMIT_INTERVAL_MS)
        }
    }

    private suspend fun parseInputStream(
        uri: android.net.Uri,
        maxBytes: Long,
        block: (java.io.InputStream) -> List<Waypoint>,
    ): List<Waypoint> = withContext(Dispatchers.IO) {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            if (afd.length in 1..maxBytes || afd.length == android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH) {
                afd.createInputStream().use { stream ->
                    return@withContext block(stream)
                }
            }
            throw IllegalArgumentException("File too large (${afd.length} bytes; max $maxBytes)")
        } ?: throw IllegalArgumentException("Cannot open URI: $uri")
    }

    suspend fun stop(): SpoofResult {
        emissionJob?.cancel()
        emissionJob = null
        teardown()
        LocationSpoofService.stop(context)
        return SpoofResult.Ok
    }

    private fun teardown() {
        _state.value = SpoofState.Idle
        try {
            testProviders.stop()
        } catch (_: SecurityException) {
            // Lost the slot mid-session; nothing further to do.
        }
    }

    companion object {
        private const val EMIT_INTERVAL_MS = 1_000L
    }
}
