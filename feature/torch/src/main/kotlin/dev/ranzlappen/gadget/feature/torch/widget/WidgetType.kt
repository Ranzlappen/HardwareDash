package dev.ranzlappen.gadget.feature.torch.widget

import kotlinx.serialization.Serializable

/**
 * Variant of a torch-related home-screen widget.
 *
 * Used inside [TorchWidgetConfig] to discriminate between
 * [FlashlightWidgetProvider] (binary on/off toggle backed by
 * `TorchController.toggle()`) and [StrobeWidgetProvider] (foreground-
 * service-backed strobe).
 *
 * `@Serializable` so the type roundtrips through
 * `kotlinx.serialization` JSON inside [FeaturePreferences]'s
 * encoded values.
 */
@Serializable
enum class WidgetType { Flashlight, Strobe }
