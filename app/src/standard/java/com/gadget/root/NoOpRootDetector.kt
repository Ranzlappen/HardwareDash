package com.gadget.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.core.RootDetection
import dev.ranzlappen.gadget.core.root.core.RootDetector

class NoOpRootDetector : RootDetector {
    override suspend fun detect(): RootDetection = RootDetection.None
}
