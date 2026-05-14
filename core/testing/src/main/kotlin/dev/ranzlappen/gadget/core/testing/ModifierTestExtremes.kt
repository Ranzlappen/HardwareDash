package dev.ranzlappen.gadget.core.testing

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Test helpers that produce "Modifier extremes" — the layout-constraint
 * scenarios the design-system spec demands components survive without
 * breaking. Pair with parameterised tests:
 *
 * ```kotlin
 * @RunWith(Parameterized::class)
 * class ButtonsModifierTest(private val modifier: Modifier) {
 *     @get:Rule val composeTestRule = createComposeRule()
 *
 *     @Test
 *     fun renders_without_layout_crash() {
 *         composeTestRule.setContent {
 *             GadgetTestTheme {
 *                 GadgetPrimaryButton(
 *                     onClick = {},
 *                     text = "Test",
 *                     modifier = modifier,
 *                 )
 *             }
 *         }
 *         composeTestRule.onNodeWithText("Test").assertExists()
 *     }
 *
 *     companion object {
 *         @Parameterized.Parameters(name = "{0}")
 *         @JvmStatic
 *         fun cases(): List<Modifier> = ModifierExtremes.all()
 *     }
 * }
 * ```
 *
 * Pulled out as a shared helper because every component family in the
 * library has the same matrix to cover and the spec requires all
 * extremes to pass for each one. One source of truth means a new
 * extreme added here automatically gates every component test.
 */
object ModifierExtremes {

    /** Caller-driven full-width — `Modifier.fillMaxWidth()`. */
    val fillMaxWidth: Modifier = Modifier.fillMaxWidth()

    /** Content-driven — `Modifier.wrapContentWidth()`. */
    val wrapContent: Modifier = Modifier.wrapContentWidth()

    /** Bounded both ways — `widthIn(min, max)`. */
    fun boundedWidth(min: Dp = 64.dp, max: Dp = 240.dp): Modifier =
        Modifier.widthIn(min = min, max = max)

    /** Caller forces an exact width via `requiredWidthIn`. */
    fun exactWidth(value: Dp): Modifier =
        Modifier.requiredWidthIn(min = value, max = value)

    /**
     * The canonical extreme set. Add new entries here when the spec's
     * test matrix grows.
     */
    fun all(): List<Modifier> = listOf(
        fillMaxWidth,
        wrapContent,
        boundedWidth(),
        exactWidth(120.dp),
        exactWidth(480.dp),
    )
}
