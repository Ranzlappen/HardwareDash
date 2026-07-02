// :feature:keepalive — persistent keep-alive controller contract + standard impl
// + the shared PersistentKeepAliveService foreground service.
//
// Screenless rooted-extras feature: the KeepAliveController contract, the
// standard impl (battery-opt-exemption intent + service start/stop), and the
// shared dataSync foreground service live here so both flavors bind one
// interface; the privileged impl (Doze whitelist + pm grant) ships in
// :feature:keepalive-rooted.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.keepalive"
}
