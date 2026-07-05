package dev.ranzlappen.gadget.feature.keepalive.control

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.keepalive.service.PersistentKeepAliveService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor keep-alive. [enable] starts the foreground service
 * and surfaces [KeepAliveControllerResult.UserBatteryOptExemptionRequested]
 * so the UI knows to fire `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
 */
@Singleton
class StandardKeepAliveController @Inject constructor(
    @ApplicationContext private val context: Context,
) : KeepAliveController {

    override suspend fun enable(): KeepAliveControllerResult {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PersistentKeepAliveService::class.java),
        )
        return KeepAliveControllerResult.UserBatteryOptExemptionRequested
    }

    override suspend fun disable(): KeepAliveControllerResult {
        context.startService(
            Intent(context, PersistentKeepAliveService::class.java)
                .setAction(PersistentKeepAliveService.ACTION_STOP),
        )
        return KeepAliveControllerResult.Ok()
    }

    override suspend fun disableAndStopService(): KeepAliveControllerResult = disable()

    override suspend fun requestUserBatteryOptExemption(): KeepAliveControllerResult =
        KeepAliveControllerResult.UserBatteryOptExemptionRequested

    override suspend fun resetAllKeepAliveMutations(): KeepAliveControllerResult =
        KeepAliveControllerResult.ResetCompleted(restored = 0, failed = 0)

    /**
     * Convenience for the Settings toggle UI: builds the
     * IGNORE_BATTERY_OPTIMIZATIONS intent the user should be shown.
     * The UI is responsible for `startActivity`-ing it.
     */
    @Suppress("BatteryLife")
    fun buildBatteryOptExemptionIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
