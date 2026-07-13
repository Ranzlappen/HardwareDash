package dev.ranzlappen.gadget.feature.notification.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.permissions.FeaturePermissions
import dev.ranzlappen.gadget.core.permissions.SpecialPermission

/**
 * Contributes the notification feature's permission needs to the centralized
 * `:core:permissions` registry (W5, the first per-feature `@IntoMap`
 * contributor).
 *
 * This module owns the real `GadgetNotificationListenerService`
 * (`BIND_NOTIFICATION_LISTENER_SERVICE`), whose grant is a **special**
 * permission the app baseline deliberately omits — it maps to a Settings
 * screen, not a runtime dialog, and only this feature needs it. Surfacing it
 * here makes the notification-listener toggle visible in the Permissions
 * dashboard (live grant query + Settings deep-link already handled by
 * `SpecialPermissions.NotificationListener`) without the dashboard importing
 * the feature.
 *
 * `POST_NOTIFICATIONS` is intentionally **not** contributed — it already
 * lives in the registry's app-wide baseline, so re-declaring it here would
 * duplicate the row. The display name is a data-only English literal to match
 * the baseline catalog's convention (dashboard string localization is a
 * separate, whole-subsystem follow-up).
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationPermissionsModule {

    private const val FEATURE_ID = "notification"

    @Provides
    @IntoMap
    @StringKey(FEATURE_ID)
    fun provideNotificationPermissions(): FeaturePermissions = FeaturePermissions(
        featureId = FEATURE_ID,
        displayName = "Notifications",
        special = listOf(SpecialPermission.NotificationListener),
    )
}
