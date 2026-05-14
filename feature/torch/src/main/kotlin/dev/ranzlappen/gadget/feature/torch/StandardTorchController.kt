package dev.ranzlappen.gadget.feature.torch

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
 * for the singleton's lifetime — there's no `unregister` because
 * `@Singleton` instances live for the process.
 *
 * Rooted-flavor extras (DutyCycleStrobe / MultiLed / Thermal
 * override) ship in a sibling `:feature:torch-rooted` module
 * after `RootCapabilityRegistry` + `RootSafetyGate` port — see
 * https://github.com/Ranzlappen/HardwareDash/issues/94.
 */
@Singleton
class StandardTorchController @Inject constructor(
    @ApplicationContext context: Context,
) : TorchController {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val flashCameraId: String? = resolveFlashCameraId(context, cameraManager)

    private val _state = MutableStateFlow(
        TorchState(
            isOn = false,
            isAvailable = flashCameraId != null,
            error = if (flashCameraId == null) TorchError.NoFlashUnit else null,
        ),
    )
    override val state: StateFlow<TorchState> = _state.asStateFlow()

    // Declared before the `init` block that registers it — Kotlin
    // runs `init` blocks and property initializers in declaration
    // order, so forward-referencing `torchCallback` from `init`
    // hits an uninitialized field.
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                _state.update { current -> current.copy(isOn = enabled, error = null) }
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == flashCameraId) {
                _state.update { current ->
                    current.copy(isAvailable = false, error = TorchError.HardwareError)
                }
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

    private fun mapException(e: CameraAccessException): TorchError = when (e.reason) {
        CameraAccessException.CAMERA_DISABLED -> TorchError.PermissionDenied
        else -> TorchError.HardwareError
    }

    private companion object {
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
