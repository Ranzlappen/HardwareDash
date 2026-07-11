package dev.ranzlappen.gadget.feature.radios.ir

import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibraryBrand
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibraryRepository
import dev.ranzlappen.gadget.feature.radios.ir.library.IrLibrarySignal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [IrViewModel]'s non-passthrough logic:
 *  - seeding [IrState] from [IrHardware] / [IrLibraryRepository] /
 *    [RootCapabilityRegistry] at construction,
 *  - [IrViewModel.setRepeats]'s `coerceIn(1, 10)` clamp,
 *  - [IrViewModel.transmit]'s in-flight guard (a second call while a
 *    transmit is already running is a no-op) and its error/ok bookkeeping,
 *  - [IrViewModel.replay]'s "load pending fields from a saved signal, then
 *    transmit" chaining,
 *  - [IrViewModel.saveSignal]'s name-trim / blank-name-defaults-to-"Signal"
 *    logic,
 *  - [IrViewModel.pasteProto]'s Pronto / RAW / NEC protocol sniffing, and
 *  - [IrViewModel.importSignal]'s case-insensitive protocol parse with a
 *    NEC fallback on an unrecognised value.
 *
 * Pure passthrough setters (`setProtocol`, `setPayload`, `setCarrierHz`) are
 * skipped, matching this repo's established convention (see `BtViewModelTest` /
 * `NfcViewModelTest`). The library open/close/select/clear-selection flow is
 * exercised as a single combined flow test, mirroring `NfcViewModelTest`'s
 * template-picker flow test.
 */
class IrViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val hardware = mockk<IrHardware>()
    private val repository = mockk<IrSignalRepository>()
    private val libraryRepository = mockk<IrLibraryRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { hardware.hasEmitter() } returns true
        every { hardware.supportedFrequencies() } returns listOf(30_000..60_000)
        every { repository.signals } returns flowOf(emptyList())
        every { libraryRepository.brands } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun rootRegistry(isRootedFlavor: Boolean): RootCapabilityRegistry {
        val registry = mockk<RootCapabilityRegistry>(relaxed = true)
        every { registry.isRootedFlavor } returns isRootedFlavor
        return registry
    }

    private fun createViewModel(isRootedFlavor: Boolean = false): IrViewModel =
        IrViewModel(hardware, repository, libraryRepository, rootRegistry(isRootedFlavor))

    private fun brand(name: String) = IrLibraryBrand(
        brand = name,
        category = "TV",
        signals = listOf(IrLibrarySignal(name = "Power", protocol = "NEC", payload = "0x1")),
    )

    // ---- construction ----

    @Test
    fun `seeds isRootedFlavor from the root capability registry`() {
        assertTrue(createViewModel(isRootedFlavor = true).isRootedFlavor)
        assertFalse(createViewModel(isRootedFlavor = false).isRootedFlavor)
    }

    @Test
    fun `seeds hasEmitter, supportedFrequencies and libraryBrands at construction`() {
        val brands = listOf(brand("Acme"), brand("Zenith"))
        every { libraryRepository.brands } returns brands

        val state = createViewModel().state.value

        assertTrue(state.hasEmitter)
        assertEquals(listOf(30_000..60_000), state.supportedFrequencies)
        assertEquals(brands, state.libraryBrands)
    }

    @Test
    fun `seeds hasEmitter false when the device has no IR blaster`() {
        every { hardware.hasEmitter() } returns false
        every { hardware.supportedFrequencies() } returns emptyList()

        assertFalse(createViewModel().state.value.hasEmitter)
    }

    // ---- setRepeats ----

    @Test
    fun `setRepeats clamps to the 1 to 10 range`() {
        val viewModel = createViewModel()

        viewModel.setRepeats(99)
        assertEquals(10, viewModel.state.value.pendingRepeats)

        viewModel.setRepeats(0)
        assertEquals(1, viewModel.state.value.pendingRepeats)

        viewModel.setRepeats(-5)
        assertEquals(1, viewModel.state.value.pendingRepeats)

        viewModel.setRepeats(5)
        assertEquals(5, viewModel.state.value.pendingRepeats)
    }

    // ---- transmit ----

    @Test
    fun `transmit builds a pending signal from the current state and reports success`() = runTestWithModel { viewModel ->
        coEvery { hardware.transmit(any()) } returns null
        viewModel.setProtocol(IrProtocol.NEC)
        viewModel.setPayload("0x1234")
        viewModel.setCarrierHz(40_000)
        viewModel.setRepeats(3)

        viewModel.transmit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isTransmitting)
        assertTrue(state.lastTransmitOk)
        assertNull(state.lastTransmitError)
        coVerify {
            hardware.transmit(
                match {
                    it.protocol == IrProtocol.NEC &&
                        it.payload == "0x1234" &&
                        it.carrierHz == 40_000 &&
                        it.repeats == 3
                },
            )
        }
    }

    @Test
    fun `transmit records the hardware error and clears the ok flag on failure`() = runTestWithModel { viewModel ->
        coEvery { hardware.transmit(any()) } returns "No IR emitter on this device"

        viewModel.transmit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isTransmitting)
        assertFalse(state.lastTransmitOk)
        assertEquals("No IR emitter on this device", state.lastTransmitError)
    }

    @Test
    fun `transmit is a no-op while a transmit is already in flight`() = runTestWithModel { viewModel ->
        coEvery { hardware.transmit(any()) } returns null

        viewModel.transmit()
        // The isTransmitting flag flips synchronously before the coroutine
        // that actually calls hardware.transmit is ever run.
        assertTrue(viewModel.state.value.isTransmitting)

        viewModel.transmit()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { hardware.transmit(any()) }
    }

    // ---- replay ----

    @Test
    fun `replay loads the signal's fields into pending state and transmits it`() = runTestWithModel { viewModel ->
        coEvery { hardware.transmit(any()) } returns null
        val saved = IrSignal(
            id = "1",
            name = "TV Power",
            protocol = IrProtocol.PRONTO,
            payload = "0000 006C",
            carrierHz = 40_000,
            repeats = 4,
        )

        viewModel.replay(saved)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(IrProtocol.PRONTO, state.pendingProtocol)
        assertEquals("0000 006C", state.pendingPayload)
        assertEquals(40_000, state.pendingCarrierHz)
        assertEquals(4, state.pendingRepeats)
        assertTrue(state.lastTransmitOk)
        coVerify {
            hardware.transmit(
                match { it.protocol == IrProtocol.PRONTO && it.payload == "0000 006C" && it.repeats == 4 },
            )
        }
    }

    // ---- saveSignal ----

    @Test
    fun `saveSignal trims the name and saves the pending fields`() = runTestWithModel { viewModel ->
        coEvery { repository.save(any()) } returns Unit
        viewModel.setProtocol(IrProtocol.RAW)
        viewModel.setPayload("100,200")
        viewModel.setCarrierHz(38_000)
        viewModel.setRepeats(2)

        viewModel.saveSignal("  Kitchen Fan  ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.save(
                match {
                    it.name == "Kitchen Fan" &&
                        it.protocol == IrProtocol.RAW &&
                        it.payload == "100,200" &&
                        it.carrierHz == 38_000 &&
                        it.repeats == 2
                },
            )
        }
    }

    @Test
    fun `saveSignal defaults a blank name to Signal`() = runTestWithModel { viewModel ->
        coEvery { repository.save(any()) } returns Unit

        viewModel.saveSignal("   ")
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.save(match { it.name == "Signal" }) }
    }

    // ---- delete ----

    @Test
    fun `delete removes the signal by id`() = runTestWithModel { viewModel ->
        coEvery { repository.delete("1") } returns Unit
        val saved = IrSignal(id = "1", name = "TV Power", protocol = IrProtocol.NEC, payload = "0x1")

        viewModel.delete(saved)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.delete("1") }
    }

    // ---- pasteProto ----

    @Test
    fun `pasteProto recognises a learned Pronto header`() {
        val viewModel = createViewModel()

        viewModel.pasteProto("  0000 006C 0022 0002  ")

        val state = viewModel.state.value
        assertEquals(IrProtocol.PRONTO, state.pendingProtocol)
        assertEquals("0000 006C 0022 0002", state.pendingPayload)
    }

    @Test
    fun `pasteProto recognises digits-only payloads as RAW`() {
        val viewModel = createViewModel()

        viewModel.pasteProto("9000, 4500, 560, 560")

        assertEquals(IrProtocol.RAW, viewModel.state.value.pendingProtocol)
    }

    @Test
    fun `pasteProto falls back to NEC for a hex payload`() {
        val viewModel = createViewModel()

        viewModel.pasteProto("0x20DF10EF")

        assertEquals(IrProtocol.NEC, viewModel.state.value.pendingProtocol)
    }

    @Test
    fun `pasteProto prefers Pronto over the RAW digits pattern when both would match`() {
        val viewModel = createViewModel()

        // Starts with "0000" and is otherwise all digits/commas/spaces - the
        // Pronto check runs first and wins.
        viewModel.pasteProto("0000, 100, 200")

        assertEquals(IrProtocol.PRONTO, viewModel.state.value.pendingProtocol)
    }

    // ---- importSignal ----

    @Test
    fun `importSignal parses the protocol case-insensitively and saves it`() = runTestWithModel { viewModel ->
        coEvery { repository.save(any()) } returns Unit
        val librarySignal = IrLibrarySignal(name = "Power", protocol = "nec", payload = "0x1", carrierHz = 38_000, repeats = 1)

        viewModel.importSignal(librarySignal)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.save(match { it.name == "Power" && it.protocol == IrProtocol.NEC && it.payload == "0x1" }) }
    }

    @Test
    fun `importSignal falls back to NEC when the protocol string is unrecognised`() = runTestWithModel { viewModel ->
        coEvery { repository.save(any()) } returns Unit
        val librarySignal = IrLibrarySignal(name = "Mystery", protocol = "sirc", payload = "0x2")

        viewModel.importSignal(librarySignal)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.save(match { it.protocol == IrProtocol.NEC }) }
    }

    // ---- library open/close/select flow ----

    @Test
    fun `library open, select and close flow updates state`() {
        val acme = brand("Acme")
        val viewModel = createViewModel()

        viewModel.openLibrary()
        assertTrue(viewModel.state.value.showLibrary)

        viewModel.selectBrand(acme)
        assertEquals(acme, viewModel.state.value.selectedBrand)

        viewModel.clearBrandSelection()
        assertNull(viewModel.state.value.selectedBrand)

        viewModel.selectBrand(acme)
        viewModel.closeLibrary()
        assertFalse(viewModel.state.value.showLibrary)
        assertNull(viewModel.state.value.selectedBrand)
    }

    // ---- test helper ----

    private fun runTestWithModel(block: suspend (IrViewModel) -> Unit) = runTest {
        block(createViewModel())
    }
}
