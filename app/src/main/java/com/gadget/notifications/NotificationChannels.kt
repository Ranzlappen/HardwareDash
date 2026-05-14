package com.gadget.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// Shared notification channel IDs used by the lock-screen / notification UI.
// Lifted out of LockScreenScreen.kt so the builder, preview and any future
// callers can reference them from one place.
const val CH_LOCKSCREEN = "hwd_lockscreen"
const val CH_DEFAULT    = "hwd_default"
const val CH_HIGH       = "hwd_high"
const val CH_PROGRESS   = "hwd_progress"
const val CH_CUSTOM     = "hwd_custom"
const val CH_GPS_SPOOF  = "hwd_gps_spoof"

fun ensureAllChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    listOf(
        NotificationChannel(CH_LOCKSCREEN, "Lock Screen Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        },
        NotificationChannel(CH_DEFAULT,  "Default",         NotificationManager.IMPORTANCE_DEFAULT),
        NotificationChannel(CH_HIGH,     "High / Heads-Up", NotificationManager.IMPORTANCE_HIGH),
        NotificationChannel(CH_PROGRESS, "Progress",        NotificationManager.IMPORTANCE_LOW),
        NotificationChannel(CH_CUSTOM,   "Custom",          NotificationManager.IMPORTANCE_HIGH),
        NotificationChannel(CH_GPS_SPOOF, "GPS Spoofing",   NotificationManager.IMPORTANCE_LOW).apply {
            description = "Active when GPS spoofing playback (GPX, KML, route) is running"
            setShowBadge(false)
        },
    ).forEach { nm.createNotificationChannel(it) }
}
