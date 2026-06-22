package dev.ranzlappen.gadget.feature.lock.automation

import android.app.KeyguardManager
import android.content.Context
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActionHandler {

    private val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(key = ACTION_ASSERT_LOCKED, label = "Assert device is locked"),
        ModuleAction(key = ACTION_ASSERT_UNLOCKED, label = "Assert device is unlocked"),
        ModuleAction(key = ACTION_ASSERT_SECURE, label = "Assert device has screen lock"),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
        val kg = keyguard ?: return ActionResult.Failure("KeyguardManager unavailable")
        return when (actionKey) {
            ACTION_ASSERT_LOCKED -> {
                if (kg.isKeyguardLocked) ActionResult.Success
                else ActionResult.Failure("Device is not locked")
            }
            ACTION_ASSERT_UNLOCKED -> {
                if (!kg.isKeyguardLocked) ActionResult.Success
                else ActionResult.Failure("Device is locked")
            }
            ACTION_ASSERT_SECURE -> {
                if (kg.isDeviceSecure) ActionResult.Success
                else ActionResult.Failure("No screen lock configured")
            }
            else -> ActionResult.Unsupported
        }
    }

    companion object {
        const val FEATURE_ID = "lock"
        const val ACTION_ASSERT_LOCKED = "lock_assert_locked"
        const val ACTION_ASSERT_UNLOCKED = "lock_assert_unlocked"
        const val ACTION_ASSERT_SECURE = "lock_assert_secure"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LockActionModule {
    @Binds
    @Singleton
    @IntoMap
    @StringKey(LockActionHandler.FEATURE_ID)
    abstract fun bindLockActionHandler(impl: LockActionHandler): ActionHandler
}
