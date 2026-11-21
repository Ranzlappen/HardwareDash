package dev.ranzlappen.gadget.feature.radios.nfc.tag

import android.nfc.Tag
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Hand-off for tags that reach the app through the activity (a tag tapped
 * while Gadget is closed, via the manifest's NFC intent filters) rather than
 * the NFC screen's reader mode. `MainActivity` offers the tag and opens the
 * NFC screen; its view model takes it. Replays the latest tag once so a
 * view model created after the offer still sees it.
 */
@Singleton
class NfcTagInbox @Inject constructor() {
    private val tags = MutableSharedFlow<Tag>(replay = 1, extraBufferCapacity = 1)
    val incoming: SharedFlow<Tag> = tags.asSharedFlow()

    fun offer(tag: Tag) {
        tags.tryEmit(tag)
    }

    /** Called once a tag is handled, so it isn't replayed to the next screen. */
    fun consumed() = tags.resetReplayCache()
}
