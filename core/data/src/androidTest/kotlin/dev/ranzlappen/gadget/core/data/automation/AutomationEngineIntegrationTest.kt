package dev.ranzlappen.gadget.core.data.automation

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.automation.ModuleActionRegistry
import dev.ranzlappen.gadget.core.automation.engine.MetricThresholdGate
import dev.ranzlappen.gadget.core.automation.engine.RuleEvaluator
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/**
 * Batch-F end-to-end engine integration (the design doc's batch-3.3
 * acceptance, CI-runnable form): the **canonical rule** — "if proximity
 * < 5 cm then torch off" — persisted through the real Room
 * [AutomationDatabase], edge-detected by [MetricThresholdGate], evaluated by
 * [RuleEvaluator], and dispatched through a real [ModuleActionRegistry] into
 * a recording torch [ActionHandler]; the fire is recorded via
 * [RuleRepository.markFired] and the persisted cooldown suppresses an
 * immediate re-fire.
 *
 * The handler is a **recording fake** rather than the real
 * `TorchActionHandler` — deliberately: `:core:data` must not import a
 * feature module (the same invariant the engine itself has), and a CI
 * emulator has no flash unit to observe anyway. The real-torch end of the
 * acceptance ("watch it fire, reboot, watch it re-arm") is the Batch-H
 * milestone demo on a physical device. What this test pins is everything
 * the engine owns: persist → arm → edge → evaluate → budget-free dispatch
 * path → cooldown persistence.
 */
@RunWith(AndroidJUnit4::class)
class AutomationEngineIntegrationTest {

    private lateinit var database: AutomationDatabase
    private lateinit var repository: RoomRuleRepository
    private lateinit var registry: ModuleActionRegistry
    private lateinit var torchHandler: RecordingTorchHandler

    private val evaluator = RuleEvaluator()

    private val canonicalRule = Rule(
        id = "proximity-torch-off",
        name = "if proximity < 5cm then torch off",
        trigger = Trigger.MetricThreshold(
            metricKey = "proximity",
            op = ComparisonOp.Lt,
            value = 5f,
            edge = Edge.Rising,
            clearValue = 8f,
        ),
        actions = listOf(RuleAction(featureId = "torch", actionKey = "off")),
        cooldownSeconds = 30,
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AutomationDatabase::class.java,
        ).build()
        repository = RoomRuleRepository(database.ruleDao())
        torchHandler = RecordingTorchHandler()
        registry = ModuleActionRegistry(mapOf("torch" to torchHandler))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun canonicalRule_persists_fires_dispatches_andCooldownSuppressesRefire() = runTest {
        // 1. Persist through the real Room DB and read back intact.
        repository.save(canonicalRule)
        val persisted = repository.rule(canonicalRule.id)
        assertNotNull(persisted)
        assertEquals(canonicalRule, persisted)

        // 2. Edge detection: first sample (10cm, outside) arms; 4cm fires.
        val trigger = persisted!!.trigger as Trigger.MetricThreshold
        var gate = MetricThresholdGate.initialState(trigger, firstSample = 10f)
        assertTrue(gate.armed)
        val step = MetricThresholdGate.step(trigger, gate, sample = 4f)
        gate = step.state
        assertTrue("crossing below 5cm must fire", step.fire)

        // 3. Evaluate + dispatch through the real registry (the engine's path).
        val now = System.currentTimeMillis()
        val actions = evaluator.evaluate(
            rule = persisted,
            firedTrigger = persisted.trigger,
            readings = mapOf("proximity" to 4f),
            now = LocalTime.NOON,
            rootAvailable = false,
            sinceLastFiredMillis = repository.lastFiredAt(persisted.id)?.let { now - it },
        )
        assertEquals(persisted.actions, actions)
        actions.forEach { registry.dispatch(it.featureId, it.actionKey, it.params) }
        repository.markFired(persisted.id, now)
        assertEquals(listOf("off"), torchHandler.dispatched)

        // 4. The fire is durable: cooldown clock persisted via Room.
        assertEquals(now, repository.lastFiredAt(persisted.id))

        // 5. Hysteresis: dithering 6cm (between value and clearValue) does
        //    not re-arm; 9cm does; 4cm fires again at the gate level…
        assertFalse(MetricThresholdGate.step(trigger, gate, 6f).also { gate = it.state }.fire)
        assertFalse(MetricThresholdGate.step(trigger, gate, 9f).also { gate = it.state }.fire)
        val refire = MetricThresholdGate.step(trigger, gate, 4f)
        assertTrue(refire.fire)

        // 6. …but the PERSISTED cooldown suppresses the dispatch: an
        //    automated re-fire 1ms later evaluates to nothing.
        val refireActions = evaluator.evaluate(
            rule = persisted,
            firedTrigger = persisted.trigger,
            readings = mapOf("proximity" to 4f),
            now = LocalTime.NOON,
            rootAvailable = false,
            sinceLastFiredMillis = repository.lastFiredAt(persisted.id)?.let { (now + 1) - it },
        )
        assertEquals(emptyList<RuleAction>(), refireActions)
        assertEquals(listOf("off"), torchHandler.dispatched) // still exactly one dispatch
    }

    @Test
    fun unknownFeature_dispatchesUnsupported_notAnException() = runTest {
        val result = registry.dispatch("nonexistent", "off")
        assertEquals(ActionResult.Unsupported, result)
    }

    /** Records dispatched action keys; never touches hardware. */
    private class RecordingTorchHandler : ActionHandler {
        val dispatched = mutableListOf<String>()
        override val featureId: String = "torch"
        override val actions: List<ModuleAction> = listOf(
            ModuleAction(key = "off", label = "Torch off"),
            ModuleAction(key = "on", label = "Torch on"),
        )

        override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult {
            dispatched += actionKey
            return ActionResult.Success
        }
    }
}
