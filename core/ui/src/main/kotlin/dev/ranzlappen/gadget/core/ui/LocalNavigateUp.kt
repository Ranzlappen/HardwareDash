package dev.ranzlappen.gadget.core.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * "Navigate up" action for the screen being composed, or `null` when the
 * screen is a top-level root (Home, a section hub) and shows no back arrow.
 *
 * Provided by the app shell (`GadgetApp` in `:core:navigation`, which owns
 * the back stack) and read by [ModuleScreenScaffold], so every tool screen
 * gets an up arrow without each feature wiring an `onBack` callback.
 * Previews and tests leave it at the `null` default.
 */
val LocalNavigateUp = compositionLocalOf<(() -> Unit)?> { null }
