package dev.ranzlappen.gadget.core.designsystem.a11y

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Accessibility CompositionLocals.
 *
 * Two per-tree booleans that downstream components consult to degrade
 * their animation / glass behaviour when the user has opted into a
 * more accessible UI.
 *
 * Both default to `false` (full animations, full transparency) when
 * no provider is set, so callers that don't wrap their content in
 * [GadgetTheme] still get sensible behaviour.
 */

/**
 * `true` when animations should be suppressed (instant transitions,
 * no spring scale, no shimmer).
 *
 * The default [GadgetTheme] populates this from the system
 * `Settings.Global.ANIMATOR_DURATION_SCALE` setting — when the user
 * sets "Animator duration scale" to "off" in Developer options or via
 * a third-party a11y app, every Compose subtree under [GadgetTheme]
 * sees `true` here automatically.
 *
 * Consumer apps can override locally:
 *
 * ```kotlin
 * CompositionLocalProvider(LocalReducedMotion provides true) { … }
 * ```
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * `true` when glassy / translucent surfaces should be opaque (higher
 * alpha, no gradient bleed-through).
 *
 * Android has no system-wide "reduce transparency" toggle (unlike
 * iOS), so this CompositionLocal defaults to `false` and stays
 * default unless an app explicitly wires it from a user preference
 * (e.g. Settings → Accessibility → "Reduce transparency").
 *
 * Components reading this should swap a [GlassIntensity.Standard]
 * surface to [GlassIntensity.Subtle] (more opaque) when `true`,
 * not eliminate the glass entirely — the visual hierarchy still
 * needs the surface distinction.
 */
val LocalReducedTransparency = compositionLocalOf { false }

/**
 * Snapshot of the system animator-duration-scale setting. Returns
 * `true` when animations are disabled (scale == 0f), `false`
 * otherwise.
 *
 * Snapshot-only: the value is captured at the call site's first
 * composition and cached via [remember]. A live observation would
 * require a `ContentObserver` on the [Settings.Global] URI; queued
 * for a future a11y batch if needed. In practice the setting changes
 * rarely during app runtime — re-launch picks up the change.
 *
 * Used by [GadgetTheme] to seed [LocalReducedMotion] so consumer
 * components don't have to read the system setting themselves.
 */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DefaultAnimatorScale,
        )
        scale == 0f
    }
}

/** Fallback when the system setting is unreadable (shouldn't happen). */
private const val DefaultAnimatorScale: Float = 1f
