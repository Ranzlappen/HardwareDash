package com.gadget.root

import dev.ranzlappen.gadget.core.root.core.RootService
import dev.ranzlappen.gadget.core.root.core.RootServiceHandle

class NoOpRootService : RootService {
    override suspend fun bind(): RootServiceHandle? = null
    override suspend fun unbind() = Unit
}
