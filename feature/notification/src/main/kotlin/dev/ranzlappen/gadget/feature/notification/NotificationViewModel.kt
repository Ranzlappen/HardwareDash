package dev.ranzlappen.gadget.feature.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.notifications.NotificationChannelRegistry
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.notification.control.LockScreenOverlayConfig
import dev.ranzlappen.gadget.feature.notification.control.NotificationController
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import dev.ranzlappen.gadget.feature.notification.control.StickyOverrideConfig
import dev.ranzlappen.gadget.feature.notification.listener.ActiveNotificationsBridge
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aggregating ViewModel for [NotificationScreen].
 *
 * Combines the in-app builder draft, a live re-read of the app's
 * notification channels, and [ActiveNotificationsBridge] into a single
 * [NotificationScreenState]. Standard-flavor actions (post/cancel a test
 * notification, open the listener-access settings page, re-read channels)
 * are handled directly with `NotificationManager`; rooted actions
 * (sticky-channel-importance override, programmatic listener grant, the
 * lock-screen overlay test, reset-all) dispatch through the injected
 * [NotificationController] — the standard/rooted seam bound per-flavor in
 * `:app` (`RootBindings`). This ViewModel never branches on
 * `BuildConfig.IS_ROOTED`; on the standard flavor those calls simply return
 * [NotificationControllerResult.Unsupported].
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: NotificationController,
    private val activeNotificationsBridge: ActiveNotificationsBridge,
    private val channelRegistry: NotificationChannelRegistry,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val isRootedFlavor = rootCapabilityRegistry.isRootedFlavor

    init {
        NotificationImportance.entries.forEach { channelRegistry.ensure(it.channelSpec(context)) }
        refreshChannels()
    }

    private val _draft = MutableStateFlow(BuilderDraft())

    private val _channels = MutableStateFlow(notificationManager.readChannelSummaries())

    /** One-shot results from every rooted controller invocation, surfaced as
     *  a snackbar by the screen (mirrors `TorchViewModel.rootToolEvents`). */
    private val _resultEvents = MutableSharedFlow<NotificationControllerResult>(extraBufferCapacity = 1)
    val resultEvents: SharedFlow<NotificationControllerResult> = _resultEvents.asSharedFlow()

    val state: StateFlow<NotificationScreenState> = combine(
        _draft,
        _channels,
        activeNotificationsBridge.listenerConnected,
        activeNotificationsBridge.activeCount,
    ) { draft, channels, listenerConnected, activeCount ->
        NotificationScreenState(
            isRootedFlavor = isRootedFlavor,
            channels = channels,
            listenerConnected = listenerConnected,
            activeNotificationCount = activeCount,
            builderTitle = draft.title,
            builderBody = draft.body,
            builderImportance = draft.importance,
            lastPostedNotificationId = draft.lastPostedNotificationId,
            stickyChannelId = draft.stickyChannelId,
            overlayMessage = draft.overlayMessage,
            overlayDurationMillis = draft.overlayDurationMillis,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SubscriptionTimeoutMillis),
        initialValue = NotificationScreenState.Initial,
    )

    fun onEvent(event: NotificationUiEvent) {
        when (event) {
            is NotificationUiEvent.BuilderTitleChange -> _draft.update { it.copy(title = event.title) }
            is NotificationUiEvent.BuilderBodyChange -> _draft.update { it.copy(body = event.body) }
            is NotificationUiEvent.BuilderImportanceChange ->
                _draft.update { it.copy(importance = event.importance) }
            NotificationUiEvent.PostTestNotification -> onPostTestNotification()
            NotificationUiEvent.CancelTestNotification -> onCancelTestNotification()
            NotificationUiEvent.RefreshChannels -> refreshChannels()
            NotificationUiEvent.OpenListenerSettings -> onOpenListenerSettings()
            is NotificationUiEvent.StickyChannelIdChange ->
                _draft.update { it.copy(stickyChannelId = event.channelId) }
            NotificationUiEvent.StickyOverrideRequest -> onStickyOverrideRequest()
            NotificationUiEvent.GrantListenerAccessRequest -> onGrantListenerAccessRequest()
            is NotificationUiEvent.OverlayMessageChange ->
                _draft.update { it.copy(overlayMessage = event.message) }
            is NotificationUiEvent.OverlayDurationChange ->
                _draft.update { it.copy(overlayDurationMillis = event.durationMillis) }
            NotificationUiEvent.ShowOverlayRequest -> onShowOverlayRequest()
            NotificationUiEvent.ResetAllRequest -> onResetAllRequest()
        }
    }

    /** Auto-revert path — called from the screen's `DisposableEffect` on
     *  dispose so every rooted mutation this screen made (sticky-importance
     *  override, the listener grant) reverts when the user navigates away,
     *  per [NotificationController.revertOnScreenExit]'s contract. */
    fun onScreenExit() {
        viewModelScope.launch { controller.revertOnScreenExit() }
    }

    private fun refreshChannels() {
        _channels.value = notificationManager.readChannelSummaries()
    }

    private fun onOpenListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun onPostTestNotification() {
        if (!hasPostNotificationsPermission()) return
        val draft = _draft.value
        val notification = NotificationCompat.Builder(context, draft.importance.channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(draft.title.ifBlank { context.getString(R.string.notification_builder_default_title) })
            .setContentText(draft.body.ifBlank { context.getString(R.string.notification_builder_default_body) })
            .setPriority(draft.importance.osImportance.toCompatPriority())
            .setAutoCancel(true)
            .build()
        runCatching { notificationManager.notify(TEST_NOTIFICATION_ID, notification) }
            .onSuccess { _draft.update { it.copy(lastPostedNotificationId = TEST_NOTIFICATION_ID) } }
    }

    private fun onCancelTestNotification() {
        notificationManager.cancel(TEST_NOTIFICATION_ID)
        _draft.update { it.copy(lastPostedNotificationId = null) }
    }

    private fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun onStickyOverrideRequest() {
        val channelId = _draft.value.stickyChannelId.trim()
        if (channelId.isEmpty()) return
        viewModelScope.launch {
            _resultEvents.tryEmit(controller.overrideStickyChannel(StickyOverrideConfig(channelId)))
            refreshChannels()
        }
    }

    private fun onGrantListenerAccessRequest() {
        viewModelScope.launch { _resultEvents.tryEmit(controller.grantListenerAccess()) }
    }

    private fun onShowOverlayRequest() {
        val draft = _draft.value
        viewModelScope.launch {
            _resultEvents.tryEmit(
                controller.showLockScreenOverlay(
                    LockScreenOverlayConfig(
                        message = draft.overlayMessage.ifBlank {
                            context.getString(R.string.notification_overlay_default_message)
                        },
                        durationMillis = draft.overlayDurationMillis,
                    ),
                ),
            )
        }
    }

    private fun onResetAllRequest() {
        viewModelScope.launch { _resultEvents.tryEmit(controller.resetAllNotificationMutations()) }
    }

    /** Local, unsaved draft of the builder + rooted-panel input fields.
     *  Nothing here is persisted — a simplified builder per this pass's
     *  scope; a named-preset save/reuse system is a stretch goal. */
    private data class BuilderDraft(
        val title: String = "",
        val body: String = "",
        val importance: NotificationImportance = NotificationImportance.Default,
        val lastPostedNotificationId: Int? = null,
        val stickyChannelId: String = "",
        val overlayMessage: String = "",
        val overlayDurationMillis: Long = NotificationScreenState.DEFAULT_OVERLAY_DURATION_MILLIS,
    )

    private fun Int.toCompatPriority(): Int = when (this) {
        NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
        NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    private companion object {
        /** How long to keep the combined flow subscribed after the last UI
         *  subscriber leaves. Matches the convention used across the app's
         *  other ViewModels (e.g. `TorchViewModel`). */
        const val SubscriptionTimeoutMillis: Long = 5_000L

        /** Stable id for the builder's own test notification so re-posting
         *  updates the same slot and Cancel targets exactly that one. */
        const val TEST_NOTIFICATION_ID = 0x4E4F5449 // "NOTI"
    }
}
