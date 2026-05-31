#!/usr/bin/env bash
# =========================================================================
# scripts/verify-standard-apk.sh — local mirror of the CI leak gate.
# =========================================================================
#
# Developer-side mirror of the `Assert standard APK has no rooted leakage`
# step in `.github/workflows/build-apk.yml` (the CI step is the source of
# truth). Run it on a locally-assembled standard APK to catch rooted
# leakage before pushing — same checks, human-readable output instead of
# GitHub `::error::` annotations.
#
# -------------------------------------------------------------------------
# Usage
# -------------------------------------------------------------------------
#
#   scripts/verify-standard-apk.sh path/to/standard.apk
#
# Exit codes:
#   0 — clean
#   1 — found rooted strings, rooted assets, or root-tier permissions
#   2 — usage error (missing / nonexistent APK argument)
#
# -------------------------------------------------------------------------
# What it checks (mirroring the CI gate)
# -------------------------------------------------------------------------
#
#   Dex strings:
#     topjohnwu, libsu, /system/{bin,xbin}/su, chainfire, hiddenapibypass,
#     me.weishu.kernelsu, me.bmax.apatch, org.lsposed., /data/adb/{magisk,modules,ksu}
#
#   Asset names:
#     lsposed, magisk, spoofer, .magisk., /su/, /ksu/, kernelsu, apatch
#
#   Permissions:
#     WRITE_SECURE_SETTINGS, MOUNT_UNMOUNT*, INSTALL_PACKAGES,
#     DELETE_PACKAGES, READ_LOGS, MANAGE_USERS, CHANGE_CONFIGURATION,
#     MASTER_CLEAR, REBOOT, ACCESS_SUPERUSER
#
# The manifest is decoded in THREE encodings (UTF-8, NUL-stripped, and
# UTF-16 LE) because Android's binary XML compresses string pools per
# locale (see commit fabd036 on main: "ci(leak-gate): extract manifest in
# 3 encodings to catch UTF-16 LE strings"). The UTF-16 LE pass is essential,
# not optional — it catches strings the other two miss.

set -euo pipefail

APK="${1:-}"

if [ -z "$APK" ] || [ ! -f "$APK" ]; then
  echo "usage: scripts/verify-standard-apk.sh path/to/standard.apk" >&2
  if [ -n "$APK" ]; then
    echo "  no such file: $APK" >&2
  fi
  exit 2
fi

LABEL=$(basename "$APK")

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

echo "── Inspecting ${LABEL}: ${APK} ──"

# ─── Dex ─────────────────────────────────────────────────────────────────
unzip -p "$APK" '*.dex' > "$WORK/dexes.bin" 2>/dev/null || true
if [ ! -s "$WORK/dexes.bin" ]; then
  echo "::error: could not extract any dex from $APK" >&2
  exit 1
fi

# ─── Manifest ────────────────────────────────────────────────────────────
unzip -p "$APK" AndroidManifest.xml > "$WORK/manifest.bin" 2>/dev/null || true
if [ ! -s "$WORK/manifest.bin" ]; then
  echo "::error: could not extract AndroidManifest.xml from $APK" >&2
  exit 1
fi

# ─── 1. Rooted strings in dex ────────────────────────────────────────────
HITS=$(strings -a -n 6 "$WORK/dexes.bin" \
  | grep -iE 'topjohnwu|libsu|/system/(bin|xbin)/su|chainfire|hiddenapibypass|me\.weishu\.kernelsu|me\.bmax\.apatch|org\.lsposed\.|/data/adb/(magisk|modules|ksu)' \
  | sort -u || true)
if [ -n "$HITS" ]; then
  echo "${LABEL} contains rooted strings:"
  echo "$HITS"
  exit 1
fi

# ─── 2. Rooted asset names ───────────────────────────────────────────────
ASSETS=$(unzip -l "$APK" | awk '{print $NF}' \
  | grep -iE 'lsposed|magisk|spoofer|\.magisk\.|/su/|/ksu/|kernelsu|apatch' \
  | sort -u || true)
if [ -n "$ASSETS" ]; then
  echo "${LABEL} contains rooted assets:"
  echo "$ASSETS"
  exit 1
fi

# ─── 3. Root-tier permissions in the manifest (three encodings) ──────────
{
  strings -a -n 8 "$WORK/manifest.bin"
  tr -d '\0' < "$WORK/manifest.bin" | strings -a -n 8
  strings -a -e l -n 8 "$WORK/manifest.bin"
} > "$WORK/manifest.txt"
echo "Manifest text extraction: $(wc -l < "$WORK/manifest.txt") lines"

PERMS=$(grep -iE 'WRITE_SECURE_SETTINGS|MOUNT_UNMOUNT|INSTALL_PACKAGES|DELETE_PACKAGES|READ_LOGS|MANAGE_USERS|CHANGE_CONFIGURATION|MASTER_CLEAR|REBOOT|ACCESS_SUPERUSER' "$WORK/manifest.txt" \
  | sort -u || true)
if [ -n "$PERMS" ]; then
  echo "${LABEL} declares root-tier permissions:"
  echo "$PERMS"
  exit 1
fi

echo "✅ ${LABEL} clean — no rooted leakage detected"
exit 0
