package dev.ranzlappen.gadget.feature.flipper.rooted

import org.junit.Assert.assertEquals
import org.junit.Test

class FlipperRootCommandsTest {

    @Test
    fun `relaxUsbNode chmods the given device node`() {
        assertEquals(
            "chmod 666 /dev/bus/usb/001/002",
            FlipperRootCommands.relaxUsbNode("/dev/bus/usb/001/002"),
        )
    }
}
