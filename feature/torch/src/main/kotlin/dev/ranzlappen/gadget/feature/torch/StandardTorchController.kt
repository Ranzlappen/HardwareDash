package dev.ranzlappen.gadget.feature.torch

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor torch controller backed by Camera2's
 * `CameraManager.setTorchMode(...)`.
 *
 * Resolves the back-camera's flash unit at construction:
 * - Scans every camera ID via [CameraManager.getCameraIdList].
 * - Picks the first one with `FLASH_INFO_AVAILABLE = true` and
 *   `LENS_FACING = LENS_FACING_BACK`.
 * - If none match → [TorchState.isAvailable] stays `false` forever,
 *   and `toggle()` / `setOn(...)` become no-ops.
 *
 * Registers a [CameraManager.TorchCallback] so OS-level torch state
 * changes (notification-panel tile toggled by another app, the
 * legacy stock QS tile, etc.) flow into [state]. The callback runs
 * for the singleton's lifetime; in production the `@Singleton`
 * instance lives for the whole process, so there's no automatic
 * unregister. The class implements [java.io.Closeable] purely so a
 * unit test (which constructs a controller directly) can release the
 * CameraManager subscription via [close] and not leak a callback
 * across tests.
 *
 * Rooted-flavor extras (DutyCycleStrobe / MultiLed / Thermal
 * override) ship in a sibling `:feature:torch-rooted` module
 * after `RootCapabilityRegistry` + `RootSafetyGate` port — see
 * https://github.com/Ranzlappen/HardwareDash/issues/94.
 */
@Singleton
class StandardTorchController @Inject constructor(
    @ApplicationContext context: Context,
) : TorchController, java.io.Closeable {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val flashCameraId: String? = resolveFlashCameraId(context, cameraManager)

    // Number of discrete strength levels the flash unit supports (API 33+).
    // 0 or 1 means no adjustable brightness; > 1 means setBrightness() is functional.
    private val maxBrightnessLevel: Int = resolveMaxBrightnessLevel(cameraManager, flashCameraId)

    private val _state = MutableStateFlow(
        TorchState(
            isOn = false,
            isAvailable = flashCameraId != null,
            error = if (flashCameraId == null) TorchError.NoFlashUnit else null,
            brightnessSupported = maxBrightnessLevel > 1,
        ),
    )
    override val state: StateFlow<TorchState> = _state.asStateFlow()

    // Completed the first time the OS torch callback delivers state for our
    // camera after registration (Android invokes the callback with the current
    // mode for every flash-capable camera right after registerTorchCallback).
    // currentState() awaits this so a cold-process caller (a widget tap) reads
    // the real hardware state instead of the seeded `isOn = false`.
    private val firstStateDelivered = CompletableDeferred<Unit>()

    // Declared before the `init` block that registers it — Kotlin
    // runs `init` blocks and property initializers in declaration
    // order, so forward-referencing `torchCallback` from `init`
    // hits an uninitialized field.
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                _state.update { current -> current.copy(isOn = enabled, error = null) }
                firstStateDelivered.complete(Unit)
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == flashCameraId) {
                _state.update { current ->
                    current.copy(isAvailable = false, error = TorchError.HardwareError)
                }
                firstStateDelivered.complete(Unit)
            }
        }
    }

    init {
        // Subscribe to OS-wide torch callbacks so external state
        // changes (other apps, the legacy stock QS tile, hardware
        // power button gestures on some OEMs) keep [_state] in
        // sync without us polling. Callback runs on the main
        // thread by default.
        cameraManager.registerTorchCallback(torchCallback, null)
    }

    override fun toggle() {
        setOn(!_state.value.isOn)
    }

    override fun setOn(on: Boolean) {
        val cameraId = flashCameraId ?: return
        try {
            cameraManager.setTorchMode(cameraId, on)
            // Don't update _state here — the TorchCallback will fire
            // and update synchronously. Avoids racing two paths.
        } catch (e: CameraAccessException) {
            _state.update { current -> current.copy(error = mapException(e)) }
        } catch (e: IllegalArgumentException) {
            // Camera ID became invalid mid-flight (rare).
            _state.update { current ->
                current.copy(isAvailable = false, error = TorchError.HardwareError)
            }
        }
    }

    override fun setBrightness(level: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val cameraId = flashCameraId ?: return
        if (maxBrightnessLevel <= 1) return
        val clamped = level.coerceIn(0f, 1f)
        // Strength levels are 1-based (1 = minimum, maxBrightnessLevel = maximum).
        val targetLevel = (clamped * maxBrightnessLevel).toInt().coerceIn(1, maxBrightnessLevel)
        try {
            @Suppress("NewApi") // guarded by SDK_INT check above
            cameraManager.turnOnTorchWithStrengthLevel(cameraId, targetLevel)
            _state.update { it.copy(brightness = clamped) }
        } catch (e: CameraAccessException) {
            _state.update { it.copy(error = mapException(e)) }
        }
    }

    override suspend fun currentState(): TorchState {
        // On a flashless device the callback never fires for our (null) camera,
        // so don't wait. Otherwise await the first authoritative delivery,
        // bounded so a misbehaving OEM can never stall the widget broadcast.
        if (flashCameraId != null) {
            withTimeoutOrNull(STATE_READY_TIMEOUT_MS) { firstStateDelivered.await() }
        }
        return _state.value
    }

    private fun mapException(e: CameraAccessException): TorchError = when (e.reason) {
        CameraAccessException.CAMERA_DISABLED -> TorchError.PermissionDenied
        else -> TorchError.HardwareError
    }

    /**
     * Release the OS torch-callback subscription. Not called in
     * production (the singleton is process-lived), but lets a test that
     * builds a controller directly avoid leaking the callback.
     */
    override fun close() {
        cameraManager.unregisterTorchCallback(torchCallback)
    }

    private companion object {
        /**
         * Upper bound on how long [currentState] waits for the first OS torch
         * callback. The callback is posted to the main thread right after
         * registration and normally lands within a few ms; this ceiling only
         * guards a pathological device so it can never hang a widget broadcast
         * (well inside the broadcast's ~10s goAsync window).
         */
        const val STATE_READY_TIMEOUT_MS = 500L

        /**
         * Read the maximum strength level for the given camera's flash unit
         * (API 33+). Returns 0 on older API levels or if the characteristic
         * is absent. A value > 1 means `turnOnTorchWithStrengthLevel` will work.
         */
        fun resolveMaxBrightnessLevel(manager: CameraManager, cameraId: String?): Int {
            if (cameraId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 0
            return try {
                @Suppress("NewApi")
                manager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 0
            } catch (_: CameraAccessException) {
                0
            }
        }

        /**
         * Find the first back-facing camera that advertises a flash
         * unit. Returns `null` on flashless devices / emulators.
         *
         * Wrapped in try/catch because CameraManager throws on a
         * surprisingly wide range of OEM oddities (e.g. some
         * devices fail to enumerate during very early app startup).
         * Failing to resolve a camera ID is the same as not having
         * a flash unit — both surface as [TorchError.NoFlashUnit].
         */
        fun resolveFlashCameraId(context: Context, manager: CameraManager): String? {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                return null
            }
            return try {
                manager.cameraIdList.firstOrNull { id ->
                    val characteristics = manager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val isBackFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK
                    hasFlash && isBackFacing
                }
            } catch (e: CameraAccessException) {
                null
            }
        }
    }
}
