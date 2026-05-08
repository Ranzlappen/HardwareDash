package com.gadget.keepalive

/**
 * Doze-bypass scope. Always restricted to the app's own package — the
 * impl rejects anything else regardless of caller input.
 */
data class DozeBypassConfig(
    val enable: Boolean,
)

/**
 * `pm grant` against a hard allow-list of normal-protection-level
 * permissions only. The impl rejects every permission outside the
 * allow-list.
 */
data class PmGrantConfig(
    val grantOrRevoke: PmGrantVerb,
    val permissions: List<String>,
)

enum class PmGrantVerb {
    GRANT,
    REVOKE,
}
