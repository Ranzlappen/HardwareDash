package dev.ranzlappen.gadget.feature.vibration.widget

/**
 * Vibration's logcat tag for the home-screen widget pin flow + general widget
 * runtime traces. `adb logcat -s VibrationPinFlow:D` filters the full
 * enqueue → callback → claim → save → render stream end-to-end.
 *
 * Threaded through the kit's
 * [dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs] +
 * [dev.ranzlappen.gadget.core.widgetkit.pin.BaseWidgetPinSuccessReceiver] as
 * the per-feature logcat tag.
 */
internal object VibrationPinLog {
    const val TAG: String = "VibrationPinFlow"
}
