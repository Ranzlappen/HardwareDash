package dev.ranzlappen.gadget.feature.radios.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import io.mockk.Runs
import io.mockk.eq
import io.mockk.every
import io.mockk.just
import io.mockk.match
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [IrHardware] — the standard-tier `ConsumerIrManager` wrapper.
 * Not a thin passthrough: [IrHardware.hasEmitter] / [IrHardware.supportedFrequencies]
 * fold "no service", "no emitter" and a platform exception into safe
 * defaults, and [IrHardware.transmit] chains [IrCodecs.encode]'s own error
 * path in front of the actual `ConsumerIrManager.transmit` call. The manager
 * is resolved lazily from `Context.getSystemService`, so each test builds a
 * fresh [IrHardware] over its own mocked [Context].
 */
class IrHardwareTest {

    private fun hardware(manager: ConsumerIrManager?): IrHardware {
        val context = mockk<Context>()
        every { context.getSystemService(Context.CONSUMER_IR_SERVICE) } returns manager
        return IrHardware(context)
    }

    private fun hardwareWithNoService(): IrHardware {
        val context = mockk<Context>()
        every { context.getSystemService(Context.CONSUMER_IR_SERVICE) } returns null
        return IrHardware(context)
    }

    // ---- hasEmitter ----

    @Test
    fun `hasEmitter is true when the manager reports an emitter`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true

        assertTrue(hardware(manager).hasEmitter())
    }

    @Test
    fun `hasEmitter is false when the manager reports no emitter`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns false

        assertEquals(false, hardware(manager).hasEmitter())
    }

    @Test
    fun `hasEmitter is false when there is no CONSUMER_IR_SERVICE`() {
        assertEquals(false, hardwareWithNoService().hasEmitter())
    }

    // ---- supportedFrequencies ----

    @Test
    fun `supportedFrequencies maps every carrier range`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true
        val rangeA = mockk<ConsumerIrManager.CarrierFrequencyRange>()
        every { rangeA.minFrequency } returns 30_000
        every { rangeA.maxFrequency } returns 30_000
        val rangeB = mockk<ConsumerIrManager.CarrierFrequencyRange>()
        every { rangeB.minFrequency } returns 38_000
        every { rangeB.maxFrequency } returns 60_000
        every { manager.carrierFrequencies } returns arrayOf(rangeA, rangeB)

        val frequencies = hardware(manager).supportedFrequencies()

        assertEquals(listOf(30_000..30_000, 38_000..60_000), frequencies)
    }

    @Test
    fun `supportedFrequencies is empty when there is no emitter`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns false

        assertEquals(emptyList(), hardware(manager).supportedFrequencies())
    }

    @Test
    fun `supportedFrequencies is empty when there is no CONSUMER_IR_SERVICE`() {
        assertEquals(emptyList(), hardwareWithNoService().supportedFrequencies())
    }

    @Test
    fun `supportedFrequencies is empty when carrierFrequencies is null`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true
        every { manager.carrierFrequencies } returns null

        assertEquals(emptyList(), hardware(manager).supportedFrequencies())
    }

    @Test
    fun `supportedFrequencies swallows a platform exception as empty`() {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true
        every { manager.carrierFrequencies } throws RuntimeException("no such property")

        assertEquals(emptyList(), hardware(manager).supportedFrequencies())
    }

    // ---- transmit ----

    private fun signal(protocol: IrProtocol = IrProtocol.NEC, payload: String = "0x01") = IrSignal(
        id = "1",
        name = "Test",
        protocol = protocol,
        payload = payload,
        carrierHz = 38_000,
        repeats = 1,
    )

    @Test
    fun `transmit reports no ConsumerIrManager when the system has no service`() = runTest {
        val error = hardwareWithNoService().transmit(signal())

        assertEquals("No ConsumerIrManager on this device", error)
    }

    @Test
    fun `transmit reports no emitter when the device has none`() = runTest {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns false

        val error = hardware(manager).transmit(signal())

        assertEquals("No IR emitter on this device", error)
    }

    @Test
    fun `transmit surfaces the codec's own validation error without touching the manager`() = runTest {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true

        val error = hardware(manager).transmit(signal(protocol = IrProtocol.NEC, payload = "not-hex"))

        assertEquals("NEC payload must be hex", error)
        verify(exactly = 0) { manager.transmit(any(), any()) }
    }

    @Test
    fun `transmit forwards the encoded carrier and pattern to the manager and reports no error`() = runTest {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true
        every { manager.transmit(any(), any()) } just Runs
        val theSignal = signal(protocol = IrProtocol.NEC, payload = "0xF0")
        val expected = IrCodecs.encode(theSignal.protocol.name, theSignal.payload, theSignal.carrierHz, theSignal.repeats)
        val expectedEncoded = (expected as IrCodecs.Result.Ok).encoded

        val error = hardware(manager).transmit(theSignal)

        assertNull(error)
        verify {
            manager.transmit(
                eq(expectedEncoded.carrierHz),
                match<IntArray> { it.contentEquals(expectedEncoded.pattern) },
            )
        }
    }

    @Test
    fun `transmit reports a failure message when the manager throws`() = runTest {
        val manager = mockk<ConsumerIrManager>()
        every { manager.hasIrEmitter() } returns true
        every { manager.transmit(any(), any()) } throws RuntimeException("hardware busy")

        val error = hardware(manager).transmit(signal())

        assertEquals("Transmit failed: hardware busy", error)
    }
}
