package dev.ranzlappen.gadget.feature.youtubedownloader.cookies

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the on-disk Netscape `cookies.txt` captured from the in-app YouTube
 * login. Kept in app-private [Context.getFilesDir] storage (never world-
 * readable) and handed to yt-dlp via `--cookies <path>`.
 *
 * Treat the file like a password: it grants access to the signed-in session.
 */
@Singleton
class CookieStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val file: File get() = File(context.filesDir, COOKIE_FILE)

    private val _present = MutableStateFlow(file.exists() && file.length() > 0L)

    /** Whether usable cookies are currently stored. */
    val present: StateFlow<Boolean> = _present.asStateFlow()

    /** The cookies file if it exists and is non-empty, else null. */
    fun fileOrNull(): File? = file.takeIf { it.exists() && it.length() > 0L }

    /** Persist a Netscape-format cookie jar, replacing any previous one. */
    suspend fun write(netscape: String) {
        withContext(Dispatchers.IO) { file.writeText(netscape) }
        _present.value = file.length() > 0L
    }

    /** Forget the stored session. */
    suspend fun clear() {
        withContext(Dispatchers.IO) { file.delete() }
        _present.value = false
    }

    private companion object {
        const val COOKIE_FILE = "youtube_cookies.txt"
    }
}
