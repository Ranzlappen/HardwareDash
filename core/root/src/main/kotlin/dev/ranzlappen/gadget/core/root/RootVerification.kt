package dev.ranzlappen.gadget.core.root

/** Whether a rooted tool has been checked on a real rooted phone (v3 B13). */
enum class RootFeatureStatus { Verified, Experimental }

/**
 * The honest-root ledger (v3 B13). Every [RootFeatureKey] starts
 * [RootFeatureStatus.Experimental]: rooted tools render only when the user
 * turns on Settings → "Show experimental root tools", with an
 * "unverified" banner.
 *
 * Promote a key by adding it to [verified] once its line in
 * `docs/v3-smoke-test.md` passes on a rooted phone (note device + date in
 * the commit), then pass `verified = true` to the row / section showing it.
 * B14 removes tools still Experimental after one release.
 */
object RootVerification {

    /** Verified on: (none yet — test device: Samsung Galaxy A17, rooted). */
    val verified: Set<RootFeatureKey> = emptySet()

    fun status(key: RootFeatureKey): RootFeatureStatus =
        if (key in verified) RootFeatureStatus.Verified else RootFeatureStatus.Experimental

    fun isVerified(key: RootFeatureKey): Boolean = key in verified
}
