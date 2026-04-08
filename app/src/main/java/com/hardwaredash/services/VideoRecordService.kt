package com.hardwaredash.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class VideoRecordService : Service() {

    private var recording: Recording? = null
    private var lifecycleOwner: HeadlessLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        ensureChannel()
        val notification = Notification.Builder(this, CH_VIDEO)
            .setContentTitle("Recording Video")
            .setContentText("Tap to stop")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getService(
                    this, 0,
                    Intent(this, VideoRecordService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)

        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val lco = HeadlessLifecycleOwner()
                lifecycleOwner = lco

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)
                val selector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lco, selector, videoCapture)

                val filename = "HWD_Vid_${System.currentTimeMillis()}.mp4"
                val cv = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HardwareDash")
                }
                val outputOptions = MediaStoreOutputOptions.Builder(
                    contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(cv).build()

                recording = recorder.prepareRecording(this, outputOptions)
                    .start(ContextCompat.getMainExecutor(this)) { event ->
                        when (event) {
                            is VideoRecordEvent.Finalize -> {
                                val msg = if (event.hasError()) "Video error: ${event.cause?.message}"
                                          else "Saved: $filename"
                                WidgetActionHandler.showToast(this, msg)
                                isRunning = false
                                provider.unbindAll()
                                lco.destroy()
                            }
                        }
                    }
                isRunning = true
                WidgetActionHandler.showToast(this, "Recording video...")
            } catch (e: Exception) {
                WidgetActionHandler.showToast(this, "Camera error: ${e.message}")
                Log.e("VideoRecordService", "Start failed", e)
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        recording?.stop()
        lifecycleOwner?.destroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_VIDEO, "Video Recording", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_START = "com.hardwaredash.VIDEO_START"
        const val ACTION_STOP = "com.hardwaredash.VIDEO_STOP"
        private const val CH_VIDEO = "hwd_video"
        private const val NOTIF_ID = 7002
        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, VideoRecordService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } else {
                context.startService(Intent(context, VideoRecordService::class.java).apply { action = ACTION_STOP })
                false
            }
        }
    }
}

private class HeadlessLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    init { registry.currentState = Lifecycle.State.STARTED }
    override val lifecycle: Lifecycle get() = registry
    fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }
}
