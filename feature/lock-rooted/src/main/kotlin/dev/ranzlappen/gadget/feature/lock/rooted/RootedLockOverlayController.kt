package dev.ranzlappen.gadget.feature.lock.rooted

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val OVERLAY_PADDING_H_PX = 48
private const val OVERLAY_PADDING_V_PX = 32

/**
 * Draws a bounded `TYPE_APPLICATION_OVERLAY` above the secure keyguard. The
 * migration of the legacy `com.gadget.notification.LockScreenOverlayHelper`,
 * hardened to the rooted seam: every show first clears [RootSafetyGate], then
 * self-grants `SYSTEM_ALERT_WINDOW` via root appops (the genuinely privileged
 * step — a normal app cannot draw over a *secure* keyguard), then adds the
 * window and removes it in a `NonCancellable` finally so a cancelled coroutine
 * can never leave a window latched on screen.
 *
 * The overlay always carries the immutable "(Rooted overlay)" footer so it
 * cannot be used to mimic system UI for phishing.
 */
@Singleton
class RootedLockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) {
    suspend fun showSecureOverlay(message: String, durationMillis: Long): LockOverlayResult =
        when (val gate = safetyGate.check(RootFeatureKey.LockSecureOverlay)) {
            RootGateDecision.Allowed -> showGranted(message, durationMillis).also { result ->
                if (result is LockOverlayResult.Ok) {
                    safetyGate.recordInvocation(RootFeatureKey.LockSecureOverlay)
                }
            }
            RootGateDecision.BlockedByUser -> LockOverlayResult.OptedOut
            is RootGateDecision.BlockedByLimiter -> LockOverlayResult.RateLimited(gate.retryAfterMillis)
            RootGateDecision.Unsupported -> LockOverlayResult.Unsupported
        }

    private suspend fun showGranted(message: String, durationMillis: Long): LockOverlayResult {
        val effectiveDuration = LockOverlayCommands.clampDuration(durationMillis)

        // The one root-only step: grant ourselves overlay permission so the
        // window is accepted above a secure keyguard. Best-effort — if the
        // shell is unavailable the addView below fails loudly and is reported.
        shell.exec(LockOverlayCommands.grantOverlayPermission(context.packageName))

        return withContext(Dispatchers.Main.immediate) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return@withContext LockOverlayResult.Unsupported

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.CENTER }

            val view = buildOverlayView(message)

            try {
                windowManager.addView(view, params)
                try {
                    delay(effectiveDuration)
                    LockOverlayResult.Ok
                } finally {
                    withContext(NonCancellable) {
                        runCatching { windowManager.removeView(view) }
                    }
                }
            } catch (t: Throwable) {
                LockOverlayResult.Error(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun buildOverlayView(message: String): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(OVERLAY_PADDING_H_PX, OVERLAY_PADDING_V_PX, OVERLAY_PADDING_H_PX, OVERLAY_PADDING_V_PX)
            addView(TextView(context).apply { text = message })
            addView(TextView(context).apply { text = OVERLAY_FOOTER })
        }

    companion object {
        /** Immutable anti-phishing footer; never sourced from a rule param. */
        const val OVERLAY_FOOTER = "(Rooted overlay)"
    }
}
