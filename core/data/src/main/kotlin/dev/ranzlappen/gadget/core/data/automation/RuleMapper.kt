package dev.ranzlappen.gadget.core.data.automation

import dev.ranzlappen.gadget.core.automation.model.AutomationJson
import dev.ranzlappen.gadget.core.automation.model.Condition
import dev.ranzlappen.gadget.core.automation.model.ConditionLogic
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.serialization.builtins.ListSerializer

/**
 * Pure [RuleEntity] ↔ [Rule] mapping via [AutomationJson] — kept Room-free
 * so the JSON-column round-trip is JVM-unit-testable without a device.
 *
 * [RuleEntity.conditionLogic] is stored by enum **name** (decoded leniently:
 * an unknown value falls back to [ConditionLogic.All], the stricter fold).
 */
object RuleMapper {

    private val conditionsSerializer = ListSerializer(Condition.serializer())
    private val actionsSerializer = ListSerializer(RuleAction.serializer())

    fun toEntity(rule: Rule, createdAt: Long, updatedAt: Long, lastFiredAt: Long?): RuleEntity =
        RuleEntity(
            id = rule.id,
            name = rule.name,
            enabled = rule.enabled,
            triggerJson = AutomationJson.encodeToString(Trigger.serializer(), rule.trigger),
            conditionsJson = AutomationJson.encodeToString(conditionsSerializer, rule.conditions),
            conditionLogic = rule.conditionLogic.name,
            actionsJson = AutomationJson.encodeToString(actionsSerializer, rule.actions),
            cooldownSeconds = rule.cooldownSeconds,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastFiredAt = lastFiredAt,
        )

    fun toModel(entity: RuleEntity): Rule = Rule(
        id = entity.id,
        name = entity.name,
        enabled = entity.enabled,
        trigger = AutomationJson.decodeFromString(Trigger.serializer(), entity.triggerJson),
        conditions = AutomationJson.decodeFromString(conditionsSerializer, entity.conditionsJson),
        conditionLogic = runCatching { ConditionLogic.valueOf(entity.conditionLogic) }
            .getOrDefault(ConditionLogic.All),
        actions = AutomationJson.decodeFromString(actionsSerializer, entity.actionsJson),
        cooldownSeconds = entity.cooldownSeconds,
    )
}
