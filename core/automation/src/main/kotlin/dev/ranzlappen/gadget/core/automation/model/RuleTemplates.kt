package dev.ranzlappen.gadget.core.automation.model

/**
 * A prebuilt automation recipe the user can drop in from a picker and then
 * tweak (W7). A template is metadata plus a pure [create] that mints a
 * concrete [Rule] with a caller-supplied id — the ViewModel generates the
 * UUID so this stays deterministic and JVM-testable.
 *
 * Templates deliberately reference **stable, widely-available** metric keys
 * and feature/action ids (torch, battery, storage, logbook). A template
 * whose metric/action isn't registered on a device still saves; the builder
 * filters the actual action picker by `ModuleActionRegistry`, so an
 * unavailable action simply no-ops until the module is present.
 */
data class RuleTemplate(
    val templateId: String,
    val name: String,
    val description: String,
    private val build: (id: String) -> Rule,
) {
    /** Materialize this template as a saveable rule with the given [id]. */
    fun create(id: String): Rule = build(id)
}

/** The built-in template catalog surfaced in the rule builder. */
object RuleTemplates {

    val all: List<RuleTemplate> = listOf(
        RuleTemplate(
            templateId = "low_battery_torch_off",
            name = "Torch off on low battery",
            description = "Turn the flashlight off when the battery drops below 15%.",
        ) { id ->
            Rule(
                id = id,
                name = "Torch off on low battery",
                trigger = Trigger.MetricThreshold(
                    metricKey = "battery_level",
                    op = ComparisonOp.Lt,
                    value = 15f,
                    edge = Edge.Rising,
                    clearValue = 25f,
                ),
                actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
                cooldownSeconds = 60,
            )
        },
        RuleTemplate(
            templateId = "storage_low_logbook_note",
            name = "Log a note when storage is low",
            description = "Add a logbook entry when free storage falls below 2 GB.",
        ) { id ->
            Rule(
                id = id,
                name = "Log a note when storage is low",
                trigger = Trigger.MetricThreshold(
                    metricKey = "storage_free_gb",
                    op = ComparisonOp.Lt,
                    value = 2f,
                    edge = Edge.Rising,
                    clearValue = 4f,
                ),
                actions = listOf(
                    RuleAction(
                        featureId = "logbook",
                        actionKey = "logbook_add_entry",
                        params = mapOf("text" to "Free storage dropped below 2 GB"),
                    ),
                ),
                cooldownSeconds = 3600,
            )
        },
        RuleTemplate(
            templateId = "nightly_torch_off",
            name = "Torch off at night",
            description = "Turn the flashlight off at 11:00 PM every day.",
        ) { id ->
            Rule(
                id = id,
                name = "Torch off at night",
                trigger = Trigger.Schedule(timeOfDayMinutes = 23 * 60),
                actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
            )
        },
        RuleTemplate(
            templateId = "unplugged_torch_off",
            name = "Torch off when unplugged",
            description = "Turn the flashlight off whenever the charger is disconnected.",
        ) { id ->
            Rule(
                id = id,
                name = "Torch off when unplugged",
                trigger = Trigger.SystemEvent(SystemEventKind.PowerDisconnected),
                actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
                cooldownSeconds = 30,
            )
        },
    )

    fun byId(templateId: String): RuleTemplate? = all.firstOrNull { it.templateId == templateId }
}
