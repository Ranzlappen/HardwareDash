package dev.ranzlappen.gadget.core.ui.module

import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind

/**
 * The last-run status of one rooted action surfaced through a
 * [RootActionRow] inside a [RootToolsSection] (the #94 / W6 epic).
 *
 * Every dormant rooted feature (battery, gps, audio, radios-*, …) maps its
 * own `*ControllerResult` sealed type onto this shared holder in its
 * `ViewModel`, so the in-screen root surface stays a single shape rather than
 * a per-feature copy of `(message, isError, running)`. [statusKind] derives
 * from [isError] so the row tint stays consistent everywhere.
 */
data class RootActionState(
    val message: String? = null,
    val isError: Boolean = false,
    val running: Boolean = false,
) {
    /** The [GadgetStatusKind] a [RootActionRow] should tint this row with. */
    val statusKind: GadgetStatusKind
        get() = if (isError) GadgetStatusKind.Error else GadgetStatusKind.Success
}
