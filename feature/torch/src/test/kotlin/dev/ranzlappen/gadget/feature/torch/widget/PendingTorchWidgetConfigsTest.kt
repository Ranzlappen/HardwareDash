package dev.ranzlappen.gadget.feature.torch.widget

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PendingTorchWidgetConfigsTest {

    private val sample = TorchWidgetConfig(
        type = WidgetType.Strobe,
        displayName = "Strobe",
        rateHz = 8f,
        sosMode = true,
    )

    @Test
    fun `enqueue then claim returns the original config`() {
        val store = PendingTorchWidgetConfigs()
        val token = store.enqueue(sample)

        val claimed = store.claim(token)

        assertEquals(sample, claimed)
    }

    @Test
    fun `claim returns null for unknown token`() {
        val store = PendingTorchWidgetConfigs()
        assertNull(store.claim("not-a-real-token"))
    }

    @Test
    fun `claim is idempotent once consumed`() {
        val store = PendingTorchWidgetConfigs()
        val token = store.enqueue(sample)

        store.claim(token)
        // Second claim returns null — entry was popped.
        assertNull(store.claim(token))
    }

    @Test
    fun `every enqueue returns a fresh token`() {
        val store = PendingTorchWidgetConfigs()
        val a = store.enqueue(sample)
        val b = store.enqueue(sample.copy(displayName = "Other"))

        assertNotEquals(a, b)
        assertEquals(sample, store.claim(a))
        assertEquals(sample.copy(displayName = "Other"), store.claim(b))
    }
}
