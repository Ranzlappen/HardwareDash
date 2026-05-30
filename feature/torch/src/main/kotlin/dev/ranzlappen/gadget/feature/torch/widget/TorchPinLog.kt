package dev.ranzlappen.gadget.feature.torch.widget

/**
 * Torch's logcat tag for the home-screen widget pin flow + general
 * widget runtime traces. Pinned to the legacy `"TorchPinFlow"` string
 * (the value the old `PendingTorchWidgetConfigs.TAG` carried) so
 * existing `adb logcat -s TorchPinFlow:D` workflows keep filtering
 * the same stream end-to-end.
 *
 * Threaded through the kit's [dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs]
 * + [dev.ranzlappen.gadget.core.widgetkit.pin.BaseWidgetPinSuccessReceiver]
 * as the per-feature logcat tag (no other configuration knob — the kit
 * just calls `Log.d(tag, …)`).
 */
internal object TorchPinLog {
    const val TAG: String = "TorchPinFlow"
}
