package com.hardwaredash.widget

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.widget.Toast
import android.os.Handler
import android.os.Looper

/** Shared utility for widget actions that run without opening the app. */
object WidgetActionHandler {

    fun showToast(context: Context, msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleTorch(context: Context, on: Boolean): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cid = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cm.setTorchMode(cid, on)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun findFlashCameraId(context: Context): String? {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
}
