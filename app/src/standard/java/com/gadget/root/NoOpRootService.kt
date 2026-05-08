package com.gadget.root

import com.gadget.root.core.RootService
import com.gadget.root.core.RootServiceHandle

class NoOpRootService : RootService {
    override suspend fun bind(): RootServiceHandle? = null
    override suspend fun unbind() = Unit
}
