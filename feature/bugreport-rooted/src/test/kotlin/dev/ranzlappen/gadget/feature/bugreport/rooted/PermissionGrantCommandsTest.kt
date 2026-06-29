package dev.ranzlappen.gadget.feature.bugreport.rooted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGrantCommandsTest {

    @Test
    fun `grant command targets the package and permission`() {
        assertEquals(
            "pm grant dev.ranzlappen.gadget.rooted android.permission.CAMERA",
            PermissionGrantCommands.grant("dev.ranzlappen.gadget.rooted", "android.permission.CAMERA"),
        )
    }

    @Test
    fun `valid permission tokens are accepted`() {
        assertTrue(PermissionGrantCommands.isValidPermission("android.permission.RECORD_AUDIO"))
    }

    @Test
    fun `blank or shell-metacharacter tokens are rejected`() {
        assertFalse(PermissionGrantCommands.isValidPermission(""))
        assertFalse(PermissionGrantCommands.isValidPermission("  "))
        assertFalse(PermissionGrantCommands.isValidPermission("android.permission.CAMERA; rm -rf /"))
        assertFalse(PermissionGrantCommands.isValidPermission("\$(reboot)"))
        assertFalse(PermissionGrantCommands.isValidPermission("a permission"))
    }
}
