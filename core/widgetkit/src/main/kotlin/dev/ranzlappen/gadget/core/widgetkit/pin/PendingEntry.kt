package dev.ranzlappen.gadget.core.widgetkit.pin

import kotlinx.serialization.Serializable

/**
 * On-disk wrapper for one in-flight pin request inside the kit's
 * [PendingWidgetConfigs] bridge. The pending bridge's serialization
 * surface is deliberately separate from the feature's
 * [dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig] surface — a
 * schema bump on one doesn't ripple into the other.
 *
 * `@Serializable` is generic: kotlinx.serialization generates
 * `PendingEntry.serializer(KSerializer<T0>)` automatically, so each
 * feature instantiates the serializer with its own config serializer:
 *
 * ```kotlin
 * PendingEntry.serializer(TorchWidgetConfig.serializer())
 * ```
 */
@Serializable
data class PendingEntry<T>(
    /** UUID token embedded in the pin-success PendingIntent so the
     *  receiver can claim this entry on callback. */
    val token: String,
    /** Wall-clock millis when the entry was enqueued, used by
     *  [PendingWidgetConfigs.purgeStale] to drop abandoned entries. */
    val savedAtMs: Long,
    /** The user's pre-pin config, carried across the launcher pin
     *  dialog round-trip. */
    val config: T,
)
