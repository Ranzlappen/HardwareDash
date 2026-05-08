package com.gadget.diagnostics

/**
 * Logcat ring-buffer the helper reads via `logcat -b <wireName> -d`.
 * RADIO and SYSTEM are higher-sensitivity (IMSI fragments, tower IDs,
 * binder transactions) — the helper relies on the caller's choice of
 * buffer rather than redacting after the fact.
 */
enum class LogcatBuffer(val wireName: String) {
    MAIN("main"),
    RADIO("radio"),
    EVENTS("events"),
    SYSTEM("system"),
    CRASH("crash"),
}
