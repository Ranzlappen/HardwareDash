package dev.ranzlappen.gadget.feature.torch.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Foreground service that displays a draggable floating torch-toggle
 * button as a `TYPE_APPLICATION_OVERLAY` (`SYSTEM_ALERT_WINDOW`)
 * WindowManager view.
 *
 * Lifecycle:
 * - Started when the user enables "Floating torch button" in Settings
 *   (after granting overlay permission).
 * - [START_STICKY]: the OS restarts it after a process kill so the
 *   button survives memory pressure.
 * - Stops itself when `UserPreferences.floatingTorchButtonEnabled`
 *   flips to `false` (user disables in Settings).
 * - The [ACTION_STOP] intent action allows the notification's "Dismiss"
 *   action to stop the service without touching the preference.
 *
 * The button icon tracks [TorchController.state] live and toggles the
 * torch on tap. A drag threshold of 10 px distinguishes a drag from a
 * tap so the user can reposition the button without firing the torch.
 */
@AndroidEntryPoint
class TorchOverlayService : Service() {

    @Inject
    lateinit var torchController: TorchController

    @Inject
    lateinit var userPreferences: UserPreferencesRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var overlayButton: ImageButton? = null
    private val windowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var overlayParams: WindowManager.LayoutParams? = null

    // Touch-drag bookkeeping
    private var dragInitialX = 0
    private var dragInitialY = 0
    private var dragTouchX = 0f
    private var dragTouchY = 0f
    private var wasDragged = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        promoteToForeground()
        createOverlay()

        // Auto-stop when the user disables the feature
        serviceScope.launch {
            userPreferences.flow.collect { prefs ->
                if (!prefs.floatingTorchButtonEnabled) stopSelf()
            }
        }

        // Keep icon in sync with actual torch state
        serviceScope.launch {
            torchController.state.collect { state -> updateIcon(state.isOn) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP -> {
                // Notification "Dismiss" — treat as full disable so Settings
                // reflects the correct state (stopped = not enabled).
                CoroutineScope(Dispatchers.IO).launch {
                    userPreferences.setFloatingTorchButtonEnabled(false)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_FROM_SETTINGS -> {
                // Settings already wrote the pref; just stop the service.
                stopSelf()
                return START_NOT_STICKY
            }
        }
        // START_STICKY so OS restarts the overlay if the process is killed
        // while the preference is still true.
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        serviceJob.cancel()
        super.onDestroy()
    }

    // ── Overlay ──────────────────────────────────────────────────────────

    private fun createOverlay() {
        val sizePx = dipToPx(BUTTON_SIZE_DP)
        val padPx = dipToPx(BUTTON_PADDING_DP)

        val button = ImageButton(this).apply {
            setImageResource(R.drawable.ic_flashlight_off)
            contentDescription = getString(R.string.overlay_button_description)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(padPx, padPx, padPx, padPx)
            background = circleBackground(active = false)
            setOnTouchListener { _, event -> handleTouch(event) }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dipToPx(INITIAL_Y_DP)
        }

        overlayButton = button
        overlayParams = params
        windowManager.addView(button, params)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = overlayParams ?: return false
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragInitialX = params.x
                dragInitialY = params.y
                dragTouchX = event.rawX
                dragTouchY = event.rawY
                wasDragged = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - dragTouchX).toInt()
                val dy = (event.rawY - dragTouchY).toInt()
                if (abs(dx) > DRAG_THRESHOLD_PX || abs(dy) > DRAG_THRESHOLD_PX) wasDragged = true
                params.x = dragInitialX + dx
                params.y = dragInitialY + dy
                windowManager.updateViewLayout(overlayButton, params)
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!wasDragged) torchController.toggle()
                true
            }
            else -> false
        }
    }

    private fun updateIcon(isOn: Boolean) {
        val button = overlayButton ?: return
        button.setImageResource(if (isOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off)
        button.background = circleBackground(active = isOn)
    }

    private fun circleBackground(active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(
            if (active) {
                ContextCompat.getColor(this@TorchOverlayService, R.color.overlay_button_active)
            } else {
                ContextCompat.getColor(this@TorchOverlayService, R.color.overlay_button_inactive)
            },
        )
    }

    private fun removeOverlay() {
        overlayButton?.let { runCatching { windowManager.removeView(it) } }
        overlayButton = null
    }

    // ── Foreground notification ───────────────────────────────────────────

    private fun promoteToForeground() {
        ensureNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this,
            0,
            Intent(this, TorchOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_flashlight_on)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                R.drawable.ic_flashlight_off,
                getString(R.string.overlay_notification_dismiss),
                stopPi,
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun dipToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "torch_overlay"
        const val NOTIFICATION_ID = 0x54_4F_56_4C // "TOVL"
        const val ACTION_STOP = "dev.ranzlappen.gadget.feature.torch.OVERLAY_STOP"
        // Used by SettingsViewModel to start/stop the overlay without
        // a compile-time dependency on this class.
        const val ACTION_START = "dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_START"
        private const val ACTION_STOP_FROM_SETTINGS = "dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_STOP"

        private const val BUTTON_SIZE_DP = 56
        private const val BUTTON_PADDING_DP = 12
        private const val INITIAL_Y_DP = 300
        private const val DRAG_THRESHOLD_PX = 10
    }
}
