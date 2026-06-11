package dev.ranzlappen.gadget.core.automation.model

import kotlinx.serialization.json.Json

/**
 * The single [Json] configuration every automation persistence path uses
 * (the `automation.db` JSON columns, backup round-trips, tests). Mirrors
 * the `:core:widgetkit` shared-Json convention:
 *
 *  - `ignoreUnknownKeys` — a record written by a newer app version (extra
 *    fields) still decodes on an older one.
 *  - `encodeDefaults = false` — defaulted fields stay off the wire, so
 *    adding a defaulted field never rewrites existing records' shape.
 *
 * The polymorphic class discriminator stays the kotlinx default (`"type"`),
 * carrying the pinned `@SerialName` FQNs of [Trigger] / [Condition].
 */
val AutomationJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
