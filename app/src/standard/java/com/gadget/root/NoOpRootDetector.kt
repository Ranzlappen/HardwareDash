package com.gadget.root

import com.gadget.root.core.RootDetection
import com.gadget.root.core.RootDetector

class NoOpRootDetector : RootDetector {
    override suspend fun detect(): RootDetection = RootDetection.None
}
