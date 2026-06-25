package dev.ranzlappen.gadget.feature.youtubedownloader.cookies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [CookieLoginScreen]. Persists the captured cookie jar into the shared
 * `@Singleton` [CookieStore]; the downloader screen observes
 * [CookieStore.present] and reacts without any cross-screen plumbing.
 */
@HiltViewModel
class CookieLoginViewModel @Inject constructor(
    private val cookieStore: CookieStore,
) : ViewModel() {

    fun saveCookies(netscape: String) {
        viewModelScope.launch { cookieStore.write(netscape) }
    }
}
