# Consumer R8/ProGuard rules for :feature:apps.
#
# Merged into :app's release R8 run. Without them, R8 strips the synthetic
# `Companion.serializer()` / `$$serializer` members kotlinx.serialization
# generates for this module's @Serializable types (FolderRule + its variants,
# FolderRuleSet) — which surfaces as a SerializationException only in minified
# release builds (CI builds `assembleStandardRelease`, so this is reachable).
# Package-scoped so every current and future @Serializable type in the module
# is covered without an enumerated list drifting out of date.

-keepclassmembers class dev.ranzlappen.gadget.feature.apps.** {
    *** Companion;
}
-keepclasseswithmembers class dev.ranzlappen.gadget.feature.apps.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.ranzlappen.gadget.feature.apps.**$$serializer { *; }
