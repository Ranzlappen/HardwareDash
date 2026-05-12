#!/usr/bin/env bash
# =========================================================================
# scripts/new-feature.sh — scaffold a new feature module.
# =========================================================================
#
# Phase 0 / Batch 0: PLACEHOLDER.
#
# The full implementation lands alongside the convention plugins in
# Batch 1, once `gadget.android.feature` exists in
# `build-logic/convention/`.
#
# -------------------------------------------------------------------------
# Intended usage
# -------------------------------------------------------------------------
#
#   scripts/new-feature.sh <feature-name> [--rooted]
#
# Creates:
#
#   feature/<name>/build.gradle.kts            (applies gadget.android.feature)
#   feature/<name>/src/main/AndroidManifest.xml
#   feature/<name>/src/main/kotlin/dev/ranzlappen/gadget/feature/<name>/
#       <Pascal>Screen.kt
#       <Pascal>ViewModel.kt
#       <Pascal>NavDestination.kt
#   feature/<name>/src/test/kotlin/dev/ranzlappen/gadget/feature/<name>/
#       <Pascal>ViewModelTest.kt
#
# Appends `:feature:<name>` to settings.gradle.kts in the alphabetised
# feature block.
#
# With `--rooted`, also creates the parallel `feature/<name>-rooted/`
# module that the rooted flavor of :app pulls in via
# `rootedImplementation` only.

set -euo pipefail

echo "scripts/new-feature.sh: not yet implemented (Batch 0 placeholder)" >&2
echo "  Will scaffold a feature/* module once Batch 1 ships build-logic/." >&2
exit 2
