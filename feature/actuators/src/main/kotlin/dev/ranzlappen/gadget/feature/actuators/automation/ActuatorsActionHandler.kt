package dev.ranzlappen.gadget.feature.actuators.automation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.actuators.monitor.ActuatorsRuntime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActuatorsActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: ActuatorsRuntime,
) : ActionHandler {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_HAPTIC_CLICK, label = "Trigger haptic click"),
        ModuleAction(key = ACTION_HAPTIC_HEAVY, label = "Trigger heavy haptic"),
        ModuleAction(key = ACTION_ASSERT_AVAILABLE, label = "Assert vibrator available"),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        val vib = vibrator ?: return ActionResult.Failure("No vibrator on this device")
        return when (actionKey) {
            ACTION_HAPTIC_CLICK -> {
                @Suppress("MissingPermission")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
                runtime.notifyTriggered(CLICK_PULSE_MS)
                ActionResult.Success
            }
            ACTION_HAPTIC_HEAVY -> {
                @Suppress("MissingPermission")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(150)
                }
                runtime.notifyTriggered(HEAVY_PULSE_MS)
                ActionResult.Success
            }
            ACTION_ASSERT_AVAILABLE -> {
                if (vib.hasVibrator()) ActionResult.Success
                else ActionResult.Failure("Vibrator not available")
            }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "actuators"
        const val ACTION_HAPTIC_CLICK = "actuators_haptic_click"
        const val ACTION_HAPTIC_HEAVY = "actuators_haptic_heavy"
        const val ACTION_ASSERT_AVAILABLE = "actuators_assert_available"

        // Approximate pulse durations fed to ActuatorsRuntime — mirrors the
        // same milliseconds used for the pre-O VibrationEffect fallbacks
        // above, since predefined effects (EFFECT_CLICK/EFFECT_HEAVY_CLICK)
        // don't expose an actual duration.
        private const val CLICK_PULSE_MS = 50L
        private const val HEAVY_PULSE_MS = 150L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ActuatorsActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(ActuatorsActionHandler.FEATURE_ID)
    abstract fun bindActuatorsActionHandler(impl: ActuatorsActionHandler): ActionHandler
}
