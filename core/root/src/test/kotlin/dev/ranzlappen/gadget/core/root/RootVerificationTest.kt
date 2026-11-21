package dev.ranzlappen.gadget.core.root

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class RootVerificationTest {

    private val registry = RootFeatureRegistry()

    @Test
    fun `every verified key is a registered rooted feature`() {
        val registered = registry.allDescriptors().map { it.key }.toSet()
        assertTrue(registered.containsAll(RootVerification.verified), "unregistered: ${RootVerification.verified - registered}")
    }

    @Test
    fun `unlisted keys are experimental, listed keys verified`() {
        registry.allDescriptors().forEach { descriptor ->
            val expected = if (descriptor.key in RootVerification.verified) {
                RootFeatureStatus.Verified
            } else {
                RootFeatureStatus.Experimental
            }
            assertEquals(expected, RootVerification.status(descriptor.key))
            assertEquals(expected == RootFeatureStatus.Verified, RootVerification.isVerified(descriptor.key))
        }
    }
}
