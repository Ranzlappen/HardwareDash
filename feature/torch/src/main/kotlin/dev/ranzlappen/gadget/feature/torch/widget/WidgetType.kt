package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetProvider
import kotlinx.serialization.Serializable

/**
 * Variant of a torch-related home-screen widget.
 *
 * Used inside [TorchWidgetConfig] to discriminate between
 * [FlashlightWidgetProvider] (binary on/off toggle backed by
 * `TorchController.toggle()`) and [StrobeWidgetProvider] (foreground-
 * service-backed strobe).
 *
 * Each variant carries the backing [AppWidgetProvider] subclass via
 * [providerClass] so call sites that previously needed `when (type)`
 * switches (`broadcastTorchWidgetUpdate`, `TorchWidgetCreator
 * .requestPin`) can read the class directly off the type (P2-17).
 *
 * **kotlinx.serialization note.** The `providerClass` property is
 * `@kotlinx.serialization.Transient`-free because the generated enum
 * serializer encodes the discriminator by name only; constructor args
 * outside the serialized form are ignored automatically (the JSON
 * value is just `"Flashlight"` or `"Strobe"`).
 */
@Serializable
enum class WidgetType(val providerClass: Class<out AppWidgetProvider>) {
    Flashlight(FlashlightWidgetProvider::class.java),
    Strobe(StrobeWidgetProvider::class.java),
}
