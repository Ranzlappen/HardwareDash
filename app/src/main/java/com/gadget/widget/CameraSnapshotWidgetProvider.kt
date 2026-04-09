package com.gadget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.gadget.R

class CameraSnapshotWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SNAPSHOT) {
            takeSnapshot(context)
        }
    }

    private fun takeSnapshot(context: Context) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val lifecycleOwner = HeadlessLifecycleOwner()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                val filename = "HWD_Snap_${System.currentTimeMillis()}.jpg"
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Gadget")
                }
                val output = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cv
                ).build()

                imageCapture.takePicture(
                    output,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                            provider.unbindAll()
                            lifecycleOwner.destroy()
                            WidgetActionHandler.showToast(context, "Saved: $filename")
                        }
                        override fun onError(exc: ImageCaptureException) {
                            provider.unbindAll()
                            lifecycleOwner.destroy()
                            WidgetActionHandler.showToast(context, "Capture failed: ${exc.message}")
                            Log.e("SnapshotWidget", "Capture error", exc)
                        }
                    }
                )
            } catch (e: Exception) {
                WidgetActionHandler.showToast(context, "Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    companion object {
        private const val ACTION_SNAPSHOT = "com.gadget.widget.ACTION_CAMERA_SNAPSHOT"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_menu_camera)
            views.setTextViewText(R.id.widget_action_label, "Snap")

            val intent = Intent(context, CameraSnapshotWidgetProvider::class.java).apply {
                action = ACTION_SNAPSHOT
            }
            val pi = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_action_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}

/** Minimal LifecycleOwner for headless CameraX binding (no Activity/Fragment). */
private class HeadlessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    init {
        registry.currentState = Lifecycle.State.STARTED
    }
    override val lifecycle: Lifecycle get() = registry
    fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
}
