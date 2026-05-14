package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Rule
import org.junit.Test

/**
 * Behaviour contract for the loading-state primitives. Each renders
 * without crashing under both indeterminate and determinate modes
 * (where applicable) and respects the caller-supplied modifier.
 *
 * These tests verify the renderable surface exists; deeper checks on
 * progress-arc geometry or shimmer animation are out of scope —
 * pixel-perfect rendering belongs in a screenshot-test batch.
 */
class LoadingStatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun circular_indeterminate_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetCircularProgress(modifier = Modifier.testTag(CircularTag))
            }
        }
        composeTestRule.onNodeWithTag(CircularTag).assertExists()
    }

    @Test
    fun circular_determinate_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetCircularProgress(
                    modifier = Modifier.testTag(CircularTag),
                    progress = 0.6f,
                )
            }
        }
        composeTestRule.onNodeWithTag(CircularTag).assertExists()
    }

    @Test
    fun linear_indeterminate_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetLinearProgress(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(LinearTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(LinearTag).assertExists()
    }

    @Test
    fun linear_determinate_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetLinearProgress(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(LinearTag),
                    progress = 0.3f,
                )
            }
        }
        composeTestRule.onNodeWithTag(LinearTag).assertExists()
    }

    @Test
    fun shimmerBlock_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .testTag(ShimmerTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(ShimmerTag).assertExists()
    }

    @Test
    fun shimmerBlock_sized_renders() {
        composeTestRule.setContent {
            GadgetTestTheme {
                GadgetShimmerBlock(
                    modifier = Modifier
                        .size(64.dp)
                        .testTag(ShimmerTag),
                )
            }
        }
        composeTestRule.onNodeWithTag(ShimmerTag).assertExists()
    }

    private companion object {
        const val CircularTag = "circular-progress"
        const val LinearTag = "linear-progress"
        const val ShimmerTag = "shimmer-block"
    }
}
