#!/usr/bin/env bash
# =========================================================================
# scripts/verify-standard-apk.sh — local mirror of the CI leak gate.
# =========================================================================
#
# Phase 0 / Batch 0: PLACEHOLDER.
#
# The full implementation will be ported from the `Assert standard APK has
# no rooted leakage` step in `.github/workflows/build-apk.yml`. Until that
# port lands (planned alongside the CI overhaul in a later batch), this
# script exits non-zero with code 2 so any caller that accidentally relies
# on it fails loudly rather than silently passing.
#
# -------------------------------------------------------------------------
# Intended interface (once implemented)
# -------------------------------------------------------------------------
#
#   scripts/verify-standard-apk.sh path/to/standard.apk
#
# Exit codes:
#   0 — clean
#   1 — found rooted strings, root-tier permissions, or libsu in the APK
#   2 — not yet implemented (current state)
#
# -------------------------------------------------------------------------
# What it will check (mirroring the CI gate verbatim)
# -------------------------------------------------------------------------
#
#   Dex strings:
#     topjohnwu, libsu, /system/bin/su, /system/xbin/su,
#     chainfire, hiddenapibypass
#
#   Asset names:
#     lsposed, magisk, spoofer, .magisk., /su/
#
#   Permissions:
#     WRITE_SECURE_SETTINGS, MOUNT_UNMOUNT*, INSTALL_PACKAGES,
#     DELETE_PACKAGES, READ_LOGS, MANAGE_USERS, CHANGE_CONFIGURATION,
#     MASTER_CLEAR, REBOOT, ACCESS_SUPERUSER
#
# The implementation MUST decode the manifest in three encodings (UTF-8,
# UTF-16 LE, UTF-16 BE) because Android's binary XML compresses string
# pools per locale (see commit fabd036 on main: "ci(leak-gate): extract
# manifest in 3 encodings to catch UTF-16 LE strings").

set -euo pipefail

echo "scripts/verify-standard-apk.sh: not yet implemented (Batch 0 placeholder)" >&2
echo "  See .github/workflows/build-apk.yml > 'Assert standard APK has no rooted leakage'" >&2
exit 2
