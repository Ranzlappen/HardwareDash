# Consumer R8/ProGuard rules for :feature:torch.
#
# These are merged into :app's release R8 run. Without them, R8 strips the
# synthetic `Companion.serializer()` / `$$serializer` members that
# kotlinx.serialization generates for this module's @Serializable types
# (TorchWidgetConfig, WidgetAppearance, IconStyle, TapBehavior,
# ToggleFeedback, WidgetType, BackgroundMode, IconTint, TapAnimation,
# PendingTorchWidgetConfigs.PendingEntry) — which surfaces as a
# SerializationException crash only in minified release builds (CI builds
# `assembleStandardRelease`, so this is reachable). Package-scoped so every
# current and future @Serializable type in the module is covered without an
# enumerated list drifting out of date.

-keepclassmembers class dev.ranzlappen.gadget.feature.torch.** {
    *** Companion;
}
-keepclasseswithmembers class dev.ranzlappen.gadget.feature.torch.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.ranzlappen.gadget.feature.torch.**$$serializer { *; }
