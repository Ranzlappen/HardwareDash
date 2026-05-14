package com.gadget.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gadget.MainActivity
import com.gadget.widget.CameraSnapshotWidgetProvider
import com.gadget.widget.DbMeterWidgetProvider
import com.gadget.widget.FlashlightWidgetProvider
import com.gadget.widget.LogNowWidgetProvider
import com.gadget.widget.NotifyWidgetProvider
import com.gadget.widget.PhoneRingWidgetProvider
import com.gadget.widget.StrobeWidgetProvider
import com.gadget.widget.VibrationWidgetProvider
import com.gadget.widget.VideoToggleWidgetProvider
import com.gadget.widget.VoiceRecordWidgetProvider

// One enum entry per tappable action available from a notification button.
// Lifted out of LockScreenScreen.kt so the builder, preview and any future
// callers can share the list.
enum class NotifActionEntry(
    val label: String,
    val broadcastAction: String?,
    val receiverClass: Class<*>?,
) {
    OPEN_APP("Open App", null, null),
    LOG_NOW("Log Now", "com.gadget.widget.ACTION_LOG_NOW", LogNowWidgetProvider::class.java),
    FLASHLIGHT("Flashlight Toggle", "com.gadget.widget.ACTION_FLASHLIGHT_TOGGLE", FlashlightWidgetProvider::class.java),
    STROBE("Strobe Toggle", "com.gadget.widget.ACTION_STROBE_TOGGLE", StrobeWidgetProvider::class.java),
    CAMERA_SNAPSHOT("Camera Snapshot", "com.gadget.widget.ACTION_CAMERA_SNAPSHOT", CameraSnapshotWidgetProvider::class.java),
    VIDEO_TOGGLE("Video Toggle", "com.gadget.widget.ACTION_VIDEO_TOGGLE", VideoToggleWidgetProvider::class.java),
    VOICE_RECORD("Voice Record Toggle", "com.gadget.widget.ACTION_VOICE_RECORD_TOGGLE", VoiceRecordWidgetProvider::class.java),
    VIBRATION("Vibration Toggle", "com.gadget.widget.ACTION_VIBRATION_TOGGLE", VibrationWidgetProvider::class.java),
    DB_METER("dB Meter Toggle", "com.gadget.widget.ACTION_DB_METER_TOGGLE", DbMeterWidgetProvider::class.java),
    PHONE_RING("Phone Ring", "com.gadget.widget.ACTION_RING_30S", PhoneRingWidgetProvider::class.java),
    SEND_NOTIFICATION("Send Notification", "com.gadget.widget.ACTION_NOTIFY_30S", NotifyWidgetProvider::class.java),
    ;

    fun buildPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        if (this == OPEN_APP) {
            return PendingIntent.getActivity(
                context, requestCode,
                Intent(context, MainActivity::class.java),
                flags,
            )
        }
        val intent = Intent(context, receiverClass!!).apply { action = broadcastAction }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}

enum class ProgressMode { OFF, INDETERMINATE, DETERMINATE }

enum class NotifStyle { NORMAL, BIG_TEXT, INBOX }

// Snapshot of every field the notification builder cares about.  buildNotification()
// is a pure function of (context, spec) - the only side effect is the platform
// PendingIntent registration that NotifActionEntry creates per action.
data class NotifSpec(
    val title: String,
    val body: String,
    val subtext: String = "",
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val visibility: Int = NotificationCompat.VISIBILITY_PUBLIC,
    val category: String? = null,
    val accentColor: Int? = null,
    val actions: List<NotifActionEntry> = emptyList(),
    val progressMode: ProgressMode = ProgressMode.OFF,
    val progressValue: Int = 0,
    val style: NotifStyle = NotifStyle.NORMAL,
    val ongoing: Boolean = false,
    val autoCancel: Boolean = true,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val timeoutSec: Int = 0,
    val badge: Int = 0,
    val quickReplyHint: String = "",
)

fun NotifSpec.channelId(): String =
    if (priority >= NotificationCompat.PRIORITY_HIGH) CH_HIGH else CH_CUSTOM

fun buildNotification(context: Context, spec: NotifSpec): android.app.Notification {
    val tapPi = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = NotificationCompat.Builder(context, spec.channelId())
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(spec.title)
        .setContentText(spec.body)
        .setPriority(spec.priority)
        .setVisibility(spec.visibility)
        .setContentIntent(tapPi)
        .setAutoCancel(spec.autoCancel)
        .setOngoing(spec.ongoing)
        .setSilent(!spec.sound)

    spec.subtext.takeIf { it.isNotBlank() }?.let(builder::setSubText)
    spec.category?.let(builder::setCategory)
    spec.accentColor?.let(builder::setColor)
    if (!spec.vibrate) builder.setVibrate(longArrayOf(0))
    if (spec.timeoutSec > 0) builder.setTimeoutAfter(spec.timeoutSec * 1000L)
    if (spec.badge > 0) builder.setNumber(spec.badge)

    spec.actions.forEachIndexed { i, entry ->
        val pi = entry.buildPendingIntent(context, 3001 + i)
        if (i == 0 && spec.quickReplyHint.isNotBlank()) {
            val remoteInput = androidx.core.app.RemoteInput.Builder("key_quick_reply")
                .setLabel(spec.quickReplyHint)
                .build()
            val action = NotificationCompat.Action.Builder(0, entry.label, pi)
                .addRemoteInput(remoteInput)
                .build()
            builder.addAction(action)
        } else {
            builder.addAction(0, entry.label, pi)
        }
    }

    when (spec.progressMode) {
        ProgressMode.OFF -> Unit
        ProgressMode.INDETERMINATE -> builder.setProgress(0, 0, true)
        ProgressMode.DETERMINATE -> builder.setProgress(100, spec.progressValue.coerceIn(0, 100), false)
    }

    when (spec.style) {
        NotifStyle.NORMAL -> Unit
        NotifStyle.BIG_TEXT -> builder.setStyle(NotificationCompat.BigTextStyle().bigText(spec.body))
        NotifStyle.INBOX -> {
            val inbox = NotificationCompat.InboxStyle()
            spec.body.lines().forEach(inbox::addLine)
            builder.setStyle(inbox)
        }
    }

    return builder.build()
}
