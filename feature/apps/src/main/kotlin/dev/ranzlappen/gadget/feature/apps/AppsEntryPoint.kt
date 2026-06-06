package dev.ranzlappen.gadget.feature.apps

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.feature.apps.icons.AppIconLoader
import dev.ranzlappen.gadget.feature.apps.security.FolderLockManager

/**
 * Single Hilt entry point for the App-Organizer feature, used by code that
 * can't receive `@Inject` dependencies — `AppWidgetProvider` subclasses,
 * broadcast receivers, and translucent Activities reached from `PendingIntent`s
 * — plus reusable composables ([dev.ranzlappen.gadget.feature.apps.icons.AppIcon])
 * that are called from contexts without a ViewModel.
 *
 * Consumers reach it via:
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
    fun appLauncher(): AppLauncher
    fun folderLockManager(): FolderLockManager
    fun appIconLoader(): AppIconLoader
    fun appsDao(): AppsDao
}
