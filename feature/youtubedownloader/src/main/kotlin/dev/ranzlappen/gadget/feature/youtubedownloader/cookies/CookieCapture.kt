package dev.ranzlappen.gadget.feature.youtubedownloader.cookies

import android.webkit.CookieManager

/**
 * Converts the WebView [CookieManager] session into a Netscape `cookies.txt`
 * jar that yt-dlp understands.
 *
 * Limitation: [CookieManager.getCookie] only exposes `name=value` pairs for a
 * URL — domain, path, secure and expiry are lost. We reconstruct conservative
 * defaults (host-wide domain, root path, secure, far-future expiry), which is
 * enough for YouTube/Google session auth cookies. HttpOnly cookies the WebView
 * declines to surface won't appear; that's the known gap for this approach.
 */
object CookieCapture {

    /** Hosts whose cookies authenticate YouTube playback + the account. */
    private val HOSTS = listOf("youtube.com", "google.com")

    /** Year-2037 expiry — comfortably beyond any session cookie's real TTL. */
    private const val FAR_FUTURE_EPOCH = "2145916800"

    /**
     * Build the cookie jar. Returns null when no auth cookies are present
     * (i.e. the user never actually signed in).
     */
    fun toNetscape(cookieManager: CookieManager): String? {
        val lines = LinkedHashSet<String>()
        for (host in HOSTS) {
            val raw = cookieManager.getCookie("https://www.$host") ?: continue
            for (pair in raw.split(';')) {
                val eq = pair.indexOf('=')
                if (eq <= 0) continue
                val name = pair.substring(0, eq).trim()
                val value = pair.substring(eq + 1).trim()
                if (name.isEmpty()) continue
                // domain  includeSubdomains  path  secure  expiry  name  value
                lines += ".$host\tTRUE\t/\tTRUE\t$FAR_FUTURE_EPOCH\t$name\t$value"
            }
        }
        if (lines.isEmpty()) return null
        return buildString {
            append("# Netscape HTTP Cookie File\n")
            append("# Captured by HardwareDash — treat as a password.\n")
            lines.forEach { append(it).append('\n') }
        }
    }
}
