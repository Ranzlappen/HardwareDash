package dev.ranzlappen.gadget.feature.usbdebug.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.usbdebug.R
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB Debugging's invocable-action surface for the automation engine.
 * Wraps the four rooted [UsbDebuggingController] methods 1:1 — all
 * `requiresRoot = true`, mirroring `TorchActionHandler` /
 * `VibrationActionHandler`'s shape. The standard flavor's controller
 * no-ops to [UsbDebuggingControllerResult.Unsupported] for every method,
 * which [toActionResult] maps onto a readable [ActionResult.Failure].
 */
@Singleton
class UsbDebugActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: UsbDebuggingController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_SWITCH_FUNCTION,
            label = context.getString(R.string.usbdebug_action_switch_function),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_FUNCTION, ActionParamType.Text, UsbFunctionType.MTP.wireName),
            ),
        ),
        ModuleAction(
            key = ACTION_DUMP_USB,
            label = context.getString(R.string.usbdebug_action_dump_usb),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_DUMP_SERIAL_SERVICE,
            label = context.getString(R.string.usbdebug_action_dump_serial_service),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_DUMP_USB_DEVICES_DEBUG,
            label = context.getString(R.string.usbdebug_action_dump_usb_devices_debug),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_SWITCH_FUNCTION -> {
                val function = parseFunction(params[PARAM_FUNCTION]) ?: UsbFunctionType.MTP
                controller.switchUsbFunction(function).toActionResult()
            }
            ACTION_DUMP_USB -> controller.dumpUsb().toActionResult()
            ACTION_DUMP_SERIAL_SERVICE -> controller.dumpSerialService().toActionResult()
            ACTION_DUMP_USB_DEVICES_DEBUG -> controller.dumpUsbDevicesDebug().toActionResult()
            else -> ActionResult.Unsupported
        }

    /** Matches by wire name (`"mtp"`) or enum name (`"MTP"`), case-insensitive
     *  — the rule builder's param editor round-trips [ActionParam.default]
     *  (a wire name), while a hand-authored rule JSON might use either. */
    private fun parseFunction(raw: String?): UsbFunctionType? {
        val value = raw?.trim() ?: return null
        return UsbFunctionType.entries.firstOrNull {
            it.wireName.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
        }
    }

    private fun UsbDebuggingControllerResult.toActionResult(): ActionResult = when (this) {
        is UsbDebuggingControllerResult.Ok -> ActionResult.Success
        is UsbDebuggingControllerResult.ResetCompleted -> ActionResult.Success
        is UsbDebuggingControllerResult.UsbFunctionSnapshot -> ActionResult.Success
        is UsbDebuggingControllerResult.UsbDumpExcerpt -> ActionResult.Success
        UsbDebuggingControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        UsbDebuggingControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is UsbDebuggingControllerResult.RateLimited ->
            ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is UsbDebuggingControllerResult.HardwareError -> ActionResult.Failure(message)
    }

    companion object {
        const val FEATURE_ID = "usbdebug"
        const val ACTION_SWITCH_FUNCTION = "usbdebug_switch_function"
        const val ACTION_DUMP_USB = "usbdebug_dump_usb"
        const val ACTION_DUMP_SERIAL_SERVICE = "usbdebug_dump_serial_service"
        const val ACTION_DUMP_USB_DEVICES_DEBUG = "usbdebug_dump_usb_devices_debug"
        const val PARAM_FUNCTION = "function"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface UsbDebugActionModule {

    @Binds
    @IntoMap
    @StringKey(UsbDebugActionHandler.FEATURE_ID)
    fun bindUsbDebugActionHandler(handler: UsbDebugActionHandler): ActionHandler
}
