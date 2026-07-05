package com.gadget.backup

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point exposing the singleton [BackupManager] to non-Hilt callers.
 *
 * The Settings "Backup & restore" card ([com.gadget.backup.ui.BackupCard]) lives
 * in `:app` because [BackupManager] orchestrates state across modules that no
 * single leaf module can see together (`:core:data` checkpointing plus
 * `:feature:apps`' `LegacyAppsImporter`). The card reaches the singleton
 * through [get] and is dropped into Settings via the `backupSection` slot —
 * the same leaf-module-can't-see-`:app` seam the rooted toggles card uses.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupManagerEntryPoint {
    fun backupManager(): BackupManager

    companion object {
        fun get(context: Context): BackupManager =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BackupManagerEntryPoint::class.java,
            ).backupManager()
    }
}
