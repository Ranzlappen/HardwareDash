package com.gadget.root.core

import dev.ranzlappen.gadget.core.root.core.*
import android.content.Intent
import android.os.IBinder

/**
 * Privileged service that runs in a dedicated `:root` process via libsu's
 * `RootService` mechanism. Batch 2 declares the manifest entry and an empty
 * `onBind` so future batches can drop in typed AIDL/Binder handlers without
 * shipping a manifest change.
 *
 * Note: the supertype is referenced fully-qualified to avoid colliding with
 * the local [dev.ranzlappen.gadget.core.root.core.RootService] interface in this package.
 */
class GadgetRootService : com.topjohnwu.superuser.ipc.RootService() {
    override fun onBind(intent: Intent): IBinder? = null
}
