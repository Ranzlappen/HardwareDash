package com.gadget.root.core

import dev.ranzlappen.gadget.core.root.core.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable wrapper around the privileged [GadgetRootService] binder.
 * Batch 2 returns null on bind — the service is declared in the rooted
 * manifest and the class exists, but actual binding (and the typed remote
 * facade) lands in a later batch alongside the first feature that needs it.
 */
@Singleton
class RootedRootService @Inject constructor() : RootService {
    override suspend fun bind(): RootServiceHandle? = null
    override suspend fun unbind() = Unit
}
