package dev.ranzlappen.gadget.core.widgetkit.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import kotlinx.coroutines.launch

/**
 * Boot-completion receiver for the kit. Fires every
 * [BootRearmHandler] bound into the
 * `Map<String, BootRearmHandler>` multibinding so each feature can
 * restore its widget-related runtime invariants (typically: re-arm
 * a foreground service that powers a placed home-screen widget the
 * launcher has reinstantiated).
 *
 * Declared in `:core:widgetkit`'s `AndroidManifest.xml`; resource
 * merging surfaces it into the app's merged manifest. `:app` already
 * declares the `android.permission.RECEIVE_BOOT_COMPLETED` permission.
 *
 * Each handler runs inside a single shared `goAsync` coroutine —
 * one receiver kept alive across every feature's rearm work, vs.
 * one receiver per feature. Failures in one handler are caught and
 * logged so they don't strand sibling features' rearm.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        Log.d(TAG, "boot.onReceive action=${intent.action}")

        val handlers = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootEntryPoint::class.java,
        ).bootRearmHandlers()
        if (handlers.isEmpty()) {
            Log.d(TAG, "no boot-rearm handlers bound")
            return
        }

        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                handlers.forEach { (featureId, handler) ->
                    runCatching { handler.onBootCompleted(context) }
                        .onFailure { Log.w(TAG, "boot rearm failed for $featureId", it) }
                }
                Log.d(TAG, "boot rearm fired ${handlers.size} handler(s)")
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Hilt entry point — surfaces the multibinding map to the
     *  system-instantiated receiver. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun bootRearmHandlers(): Map<String, @JvmSuppressWildcards BootRearmHandler>
    }

    private companion object {
        const val TAG: String = "WidgetKit.Boot"
    }
}
