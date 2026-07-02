package dev.ranzlappen.gadget.feature.notification.rooted.control

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.notification.control.LockScreenOverlayConfig
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal const val LOCK_SCREEN_OVERLAY_HARD_CEILING_MILLIS = 60_000L
private const val OVERLAY_FOOTER = "(Rooted overlay)"

/**
 * Adds a `TYPE_SYSTEM_ALERT` overlay above the keyguard for a bounded
 * duration. The overlay always shows the immutable "(Rooted overlay)"
 * footer so it cannot mimic system UI for phishing. Uses a finally
 * block under `NonCancellable` to guarantee `removeView` even if the
 * caller is cancelled mid-delay.
 */
@Singleton
class LockScreenOverlayHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun show(config: LockScreenOverlayConfig): NotificationControllerResult {
        val effectiveDuration = config.durationMillis.coerceAtMost(LOCK_SCREEN_OVERLAY_HARD_CEILING_MILLIS)
        val pseudoPath = "wm-overlay://lockscreen/${System.currentTimeMillis()}"
        mutationLog.register(pseudoPath, "active")
        return withContext(Dispatchers.Main.immediate) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return@withContext NotificationControllerResult.Unsupported.also {
                    mutationLog.unregister(pseudoPath)
                }
            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.CENTER
            }
            val view = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 32, 48, 32)
                addView(TextView(context).apply { text = config.message })
                addView(TextView(context).apply { text = OVERLAY_FOOTER })
            }
            return@withContext try {
                wm.addView(view, params)
                try {
                    delay(effectiveDuration)
                    NotificationControllerResult.Ok()
                } finally {
                    withContext(NonCancellable) {
                        runCatching { wm.removeView(view) }
                        mutationLog.unregister(pseudoPath)
                    }
                }
            } catch (t: Throwable) {
                mutationLog.unregister(pseudoPath)
                NotificationControllerResult.HardwareError(
                    "addView refused: ${t.message ?: t.javaClass.simpleName}",
                )
            }
        }
    }
}
