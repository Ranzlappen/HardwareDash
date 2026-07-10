package dev.ranzlappen.gadget.feature.notification

import androidx.compose.runtime.Immutable

/** Every user-initiated event [NotificationScreenContent] can dispatch. */
@Immutable
sealed interface NotificationUiEvent {

    // ─── Standard: builder + channel inspector ─────────────────────────
    data class BuilderTitleChange(val title: String) : NotificationUiEvent
    data class BuilderBodyChange(val body: String) : NotificationUiEvent
    data class BuilderImportanceChange(val importance: NotificationImportance) : NotificationUiEvent
    data object PostTestNotification : NotificationUiEvent
    data object CancelTestNotification : NotificationUiEvent
    data object RefreshChannels : NotificationUiEvent
    data object OpenListenerSettings : NotificationUiEvent

    // ─── Rooted: sticky override / listener grant / overlay / reset ───
    data class StickyChannelIdChange(val channelId: String) : NotificationUiEvent
    data object StickyOverrideRequest : NotificationUiEvent
    data object GrantListenerAccessRequest : NotificationUiEvent
    data class OverlayMessageChange(val message: String) : NotificationUiEvent
    data class OverlayDurationChange(val durationMillis: Long) : NotificationUiEvent
    data object ShowOverlayRequest : NotificationUiEvent
    data object ResetAllRequest : NotificationUiEvent
}
