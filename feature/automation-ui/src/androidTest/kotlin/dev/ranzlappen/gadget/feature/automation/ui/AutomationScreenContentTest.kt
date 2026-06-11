package dev.ranzlappen.gadget.feature.automation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.testing.GadgetTestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule as JunitRule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the stateless [AutomationScreenContent] —
 * curated rules, Hilt-free (moduleInfo null, callbacks captured). Mirror
 * of `SensorsScreenContentTest`; runs via
 * `:feature:automation-ui:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AutomationScreenContentTest {

    @get:JunitRule
    val composeTestRule = createComposeRule()

    private val signals = listOf(
        MetricDescriptor(metricKey = "proximity", displayName = "Proximity", unit = "cm", max = 10f),
    )

    private val choices = listOf(
        ActionChoice(featureId = "torch", action = ModuleAction(key = "torch_off", label = "Off")),
    )

    private val rules = listOf(
        Rule(
            id = "1",
            name = "Pocket torch guard",
            trigger = Trigger.MetricThreshold(
                metricKey = "proximity",
                op = ComparisonOp.Lt,
                value = 5f,
            ),
            actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
        ),
        Rule(id = "2", name = "My scene", trigger = Trigger.Manual),
    )

    private fun setContent(
        rules: List<Rule>,
        exactAlarmAllowed: Boolean = true,
        onRunNow: (Rule) -> Unit = {},
    ) {
        composeTestRule.setContent {
            GadgetTestTheme {
                AutomationScreenContent(
                    rules = rules,
                    signals = signals,
                    actionChoices = choices,
                    exactAlarmAllowed = exactAlarmAllowed,
                    moduleInfo = null,
                    onSave = {},
                    onDelete = {},
                    onSetEnabled = { _, _ -> },
                    onRunNow = onRunNow,
                    onRequestExactAlarm = {},
                )
            }
        }
    }

    @Test
    fun rendersRuleNamesAndSummaries() {
        setContent(rules)
        composeTestRule.onNodeWithText("Pocket torch guard").assertIsDisplayed()
        composeTestRule.onNodeWithText("When Proximity < 5 cm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Runs: Torch · Off").assertIsDisplayed()
        composeTestRule.onNodeWithText("My scene").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Manual — tap Run now")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_renders() {
        setContent(emptyList())
        composeTestRule.onNodeWithText("No rules yet").assertIsDisplayed()
    }

    @Test
    fun newRuleFab_opensEditor() {
        setContent(emptyList())
        composeTestRule.onNodeWithText("New rule").performClick()
        composeTestRule.onNodeWithText("When (trigger)").assertIsDisplayed()
    }

    @Test
    fun runNow_invokesCallbackWithTheRowsRule() {
        var ran: Rule? = null
        setContent(rules, onRunNow = { ran = it })
        composeTestRule.onAllNodesWithContentDescription("Run now")[0].performClick()
        assertEquals("1", ran?.id)
    }

    @Test
    fun scheduleEditor_showsExactAlarmBadge_whenDenied() {
        setContent(emptyList(), exactAlarmAllowed = false)
        composeTestRule.onNodeWithText("New rule").performClick()
        composeTestRule.onNodeWithText("Schedule").performClick()
        composeTestRule
            .onNodeWithContentDescription("Exact time")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText(
                "Exact alarms aren’t allowed for Gadget — this rule falls back to a ±10 min window.",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }
}
