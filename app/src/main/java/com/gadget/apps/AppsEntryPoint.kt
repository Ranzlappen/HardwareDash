package com.gadget.apps

import com.gadget.apps.icons.AppIconLoader
import com.gadget.apps.pin.PinFolderHelper
import com.gadget.apps.security.FolderLockManager
import com.gadget.data.db.apps.AppsDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Single Hilt entry point for the App-Organizer module, used by code that can't
 * receive `@Inject` dependencies — `AppWidgetProvider` subclasses, broadcast
 * receivers, and translucent Activities reached from `PendingIntent`s.
 *
 * Mirrors the `BackupManagerEntryPoint` / `FlipperManagerEntryPoint` shape from
 * `SettingsScreen.kt`. Consumers reach it via:
 *
 *     EntryPointAccessors.fromApplication(
 *         context.applicationContext,
 *         AppsEntryPoint::class.java,
 *     )
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppsEntryPoint {
    fun appRepository(): AppRepository
    fun webLinkRepository(): WebLinkRepository
    fun webLinkLauncher(): WebLinkLauncher
    fun appLauncher(): AppLauncher
    fun folderLockManager(): FolderLockManager
    fun pinFolderHelper(): PinFolderHelper
    fun appIconLoader(): AppIconLoader
    fun appsDao(): AppsDao
}
