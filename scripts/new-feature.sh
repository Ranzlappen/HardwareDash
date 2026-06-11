#!/usr/bin/env bash
# =========================================================================
# scripts/new-feature.sh — scaffold a new feature module.
# =========================================================================
#
# Two auto-detected modes:
#
#   * BASE mode — `feature/<name>/` does NOT exist yet. Creates the whole
#     module (build.gradle.kts + manifest + sources) and appends the
#     `:feature:<name>` include to settings.gradle.kts. Optional --rooted
#     also scaffolds the standard/rooted sibling pair (torch/vibration model).
#
#   * SKELETON-FILL mode — `feature/<name>/` exists with a `build.gradle.kts`
#     but no Kotlin sources (the Batch-0 empty skeletons). Generates **sources
#     only**: it reads `namespace = "…"` from the existing build file (so it
#     handles hyphenated module names like `radios-bt` for free), never
#     overwrites the build file, and does NOT touch settings.gradle.kts (the
#     include is already registered). If the skeleton's build file has no
#     `dependencies { }` block, a minimal one (`:core:ui` + `:core:navigation`)
#     is appended so the generated screen compiles. If the module already has
#     Kotlin sources, the script refuses loudly.
#
# -------------------------------------------------------------------------
# Usage
# -------------------------------------------------------------------------
#
#   scripts/new-feature.sh <name> [--rooted]
#
# BASE-mode <name> must be lowercase alphanumeric (a–z, 0–9) starting with a
# letter (no hyphens — the namespace is derived from it). SKELETON-FILL mode
# additionally accepts hyphenated names (the namespace comes from the file).
#
# Generates these sources (both modes), under the module's namespace package:
#       <Pascal>Screen.kt        — stateless content + Hilt entry on the design system
#       <Pascal>ViewModel.kt     — @HiltViewModel
#       <Pascal>Navigation.kt    — NavGraphBuilder.<camel>Screen() + route const
#
# --rooted (BASE mode only) also creates the standard/rooted sibling pair.
#
# -------------------------------------------------------------------------
# Manual steps left after running (intentionally NOT automated — they touch
# shared files where a blind edit would be wrong):
#   1. Add a `GadgetDestination.<Pascal>` data object in
#      core/navigation/.../GadgetDestination.kt and append it to the
#      `modules` list (then fix every non-exhaustive `when (destination)`).
#   2. Call `<camel>Screen()` from the `GadgetApp { … }` builder in :app.
#   3. (--rooted only) Wire the siblings in app/build.gradle.kts:
#         "standardImplementation"(project(":feature:<name>-standard"))
#         "rootedImplementation"(project(":feature:<name>-rooted"))
# -------------------------------------------------------------------------

set -euo pipefail

# --- locate the repo root (this script lives in <root>/scripts) -----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# --- args -----------------------------------------------------------------
NAME="${1:-}"
ROOTED=false
for arg in "${@:2}"; do
    case "$arg" in
        --rooted) ROOTED=true ;;
        *) echo "error: unknown flag '$arg'" >&2; exit 2 ;;
    esac
done

if [[ -z "$NAME" ]]; then
    echo "usage: scripts/new-feature.sh <name> [--rooted]" >&2
    exit 2
fi

# Loose name check (segments are lowercase-alphanumeric, hyphen-separated).
if [[ ! "$NAME" =~ ^[a-z][a-z0-9]*(-[a-z0-9]+)*$ ]]; then
    echo "error: <name> must be lowercase alphanumeric segments separated by" >&2
    echo "       single hyphens, starting with a letter (got '$NAME')." >&2
    exit 2
fi

FEATURE_DIR="feature/$NAME"

# --- mode detection -------------------------------------------------------
if [[ -d "$FEATURE_DIR" ]]; then
    if [[ ! -f "$FEATURE_DIR/build.gradle.kts" ]]; then
        echo "error: $FEATURE_DIR exists but has no build.gradle.kts — not a" >&2
        echo "       recognised skeleton. Refusing to touch it." >&2
        exit 2
    fi
    if find "$FEATURE_DIR/src" -name '*.kt' 2>/dev/null | grep -q .; then
        echo "error: $FEATURE_DIR already has Kotlin sources — nothing to fill." >&2
        echo "       Refusing to overwrite existing code." >&2
        exit 2
    fi
    MODE=skeleton
else
    MODE=base
fi

# BASE mode derives the namespace from the name, so it can't contain hyphens.
if [[ "$MODE" == base && "$NAME" == *-* ]]; then
    echo "error: hyphenated names ('$NAME') aren't supported in base mode" >&2
    echo "       (the namespace is derived from the name). Create the skeleton" >&2
    echo "       module first, then re-run to fill it." >&2
    exit 2
fi
if [[ "$MODE" == skeleton && "$ROOTED" == true ]]; then
    echo "error: --rooted is only supported in base mode. Add the" >&2
    echo "       :feature:$NAME-{rooted,standard} siblings by hand for an" >&2
    echo "       existing skeleton." >&2
    exit 2
fi

# --- derived identifiers (hyphen-aware) -----------------------------------
# PASCAL: each hyphen segment capitalised + joined  (radios-bt -> RadiosBt)
# CAMEL : first segment lower, rest capitalised      (radios-bt -> radiosBt)
# UPPER : hyphens -> underscores, uppercased          (radios-bt -> RADIOS_BT)
IFS='-' read -ra _SEGS <<<"$NAME"
PASCAL=""
CAMEL=""
for _i in "${!_SEGS[@]}"; do
    _s="${_SEGS[$_i]}"
    _cap="$(tr '[:lower:]' '[:upper:]' <<<"${_s:0:1}")${_s:1}"
    PASCAL+="$_cap"
    if [[ "$_i" -eq 0 ]]; then CAMEL+="$_s"; else CAMEL+="$_cap"; fi
done
UPPER="$(tr '[:lower:]-' '[:upper:]_' <<<"$NAME")"

# Namespace: from the name (base) or read from the existing build file (skeleton).
if [[ "$MODE" == base ]]; then
    NAMESPACE="dev.ranzlappen.gadget.feature.$NAME"
else
    NAMESPACE="$(grep -oP 'namespace\s*=\s*"\K[^"]+' "$FEATURE_DIR/build.gradle.kts" | head -1 || true)"
    if [[ -z "$NAMESPACE" ]]; then
        echo "error: couldn't read 'namespace = \"…\"' from $FEATURE_DIR/build.gradle.kts" >&2
        exit 2
    fi
fi
PKG_DIR="$FEATURE_DIR/src/main/kotlin/${NAMESPACE//.//}"

echo "Scaffolding :feature:$NAME (mode=$MODE, rooted=$ROOTED) …"

# --- shared source generator ----------------------------------------------
generate_sources() {
    mkdir -p "$PKG_DIR"

    cat > "$PKG_DIR/${PASCAL}Screen.kt" <<EOF
package $NAMESPACE

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Hilt entry point for the $PASCAL feature screen. Resolves the ViewModel
 * and renders the stateless [${PASCAL}ScreenContent] — keeping the content
 * (and its previews/tests) Hilt-free, per the module blueprint.
 */
@Composable
fun ${PASCAL}Screen(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") viewModel: ${PASCAL}ViewModel = hiltViewModel(),
) {
    ${PASCAL}ScreenContent(modifier = modifier)
}

/**
 * Stateless $PASCAL screen content. Build the real functional slot here
 * (controls, monitoring, ModuleInfo) following the torch/vibration reference.
 */
@Composable
internal fun ${PASCAL}ScreenContent(modifier: Modifier = Modifier) {
    ModuleScreenScaffold(
        title = "$PASCAL",
        modifier = modifier,
        functional = {
            GadgetEmptyState(
                title = "$PASCAL coming soon",
                subtitle = "Scaffolded by scripts/new-feature.sh — build the real UI here.",
            )
        },
    )
}

@GadgetPreviewLightDark
@Composable
private fun ${PASCAL}ScreenPreview() = GadgetThemedPreview { ${PASCAL}ScreenContent() }
EOF

    cat > "$PKG_DIR/${PASCAL}ViewModel.kt" <<EOF
package $NAMESPACE

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel backing the $PASCAL screen. Inject the feature's controller /
 * repositories here and expose an immutable view-state StateFlow.
 */
@HiltViewModel
class ${PASCAL}ViewModel @Inject constructor() : ViewModel()
EOF

    cat > "$PKG_DIR/${PASCAL}Navigation.kt" <<EOF
package $NAMESPACE

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Route string for the $PASCAL destination. Kept module-local so this module
 * compiles standalone; once you add \`GadgetDestination.$PASCAL\` (see the
 * script's "manual steps"), switch this to \`GadgetDestination.$PASCAL.route\`.
 */
const val ${UPPER}_ROUTE = "$NAME"

/**
 * Wire :feature:$NAME into the Gadget NavGraph. Call from the
 * \`GadgetApp { … }\` builder in :app.
 */
fun NavGraphBuilder.${CAMEL}Screen() {
    composable(route = ${UPPER}_ROUTE) {
        ${PASCAL}Screen()
    }
}
EOF
}

# --- write the manifest if absent -----------------------------------------
write_manifest_if_absent() {
    local manifest="$FEATURE_DIR/src/main/AndroidManifest.xml"
    if [[ ! -f "$manifest" ]]; then
        mkdir -p "$FEATURE_DIR/src/main"
        cat > "$manifest" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF
    fi
}

if [[ "$MODE" == base ]]; then
    # --- base module: write build file + manifest -------------------------
    mkdir -p "$FEATURE_DIR"
    cat > "$FEATURE_DIR/build.gradle.kts" <<EOF
// :feature:$NAME — TODO one-line description of the capability.
//
// Scaffolded by scripts/new-feature.sh. Build out the real controller/UI
// per the eight-step recipe in docs/migration-guide.md.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "$NAMESPACE"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard, GadgetEmptyState,
    // the design-system component library + the preview matrix annotations).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination + the route plumbing.
    implementation(project(":core:navigation"))
}
EOF
    write_manifest_if_absent
    generate_sources
else
    # --- skeleton-fill: sources only, never overwrite the build file ------
    write_manifest_if_absent
    generate_sources
    # Ensure the design-system deps the generated screen needs are present.
    if ! grep -q 'dependencies' "$FEATURE_DIR/build.gradle.kts"; then
        cat >> "$FEATURE_DIR/build.gradle.kts" <<EOF

dependencies {
    // Added by scripts/new-feature.sh (skeleton-fill) so the generated screen
    // compiles. :core:ui brings the design system; :core:navigation the routes.
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))
}
EOF
        echo "  · appended a dependencies { } block (:core:ui + :core:navigation)"
    else
        echo "  · WARNING: existing dependencies { } block left untouched — confirm"
        echo "    it has :core:ui + :core:navigation, or the generated screen won't compile."
    fi
fi

# --- rooted sibling pair (base mode only) ---------------------------------
if [[ "$ROOTED" == true ]]; then
    cat > "$PKG_DIR/${PASCAL}RootCapabilities.kt" <<EOF
package $NAMESPACE

/**
 * Root-only capability surface for $PASCAL, bound per flavor:
 *   * standard → no-op in :feature:$NAME-standard (always unavailable)
 *   * rooted   → real impl in :feature:$NAME-rooted (gated through :core:root)
 *
 * The feature stays flavor-agnostic and never imports libsu or com.gadget.root.
 * Inject this interface directly into [${PASCAL}ViewModel]; Hilt resolves the
 * binding at :app assembly per the active flavor.
 */
interface ${PASCAL}RootCapabilities {
    /** True only on the rooted flavor when root access + the capability are present. */
    suspend fun isAvailable(): Boolean
}
EOF

    STD_PKG="feature/$NAME-standard/src/main/kotlin/dev/ranzlappen/gadget/feature/$NAME/standard"
    mkdir -p "$STD_PKG/di"
    cat > "feature/$NAME-standard/build.gradle.kts" <<EOF
// :feature:$NAME-standard — standard-flavor no-op $PASCAL root surface.
//
// Mirror of :feature:$NAME-rooted; pulled in exclusively by the standard
// flavor of :app via \`standardImplementation\`. Inert by construction — no
// :core:root, no libsu.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.$NAME.standard"
}

dependencies {
    implementation(project(":feature:$NAME"))
}
EOF
    cat > "feature/$NAME-standard/src/main/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF
    cat > "$STD_PKG/Standard${PASCAL}RootCapabilities.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME.standard

import dev.ranzlappen.gadget.feature.$NAME.${PASCAL}RootCapabilities
import javax.inject.Inject

/** No-op standard-flavor impl: $PASCAL root capabilities are never available. */
class Standard${PASCAL}RootCapabilities @Inject constructor() : ${PASCAL}RootCapabilities {
    override suspend fun isAvailable(): Boolean = false
}
EOF
    cat > "$STD_PKG/di/Standard${PASCAL}Module.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME.standard.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.$NAME.${PASCAL}RootCapabilities
import dev.ranzlappen.gadget.feature.$NAME.standard.Standard${PASCAL}RootCapabilities
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class Standard${PASCAL}Module {
    @Binds
    @Singleton
    abstract fun bind${PASCAL}RootCapabilities(
        impl: Standard${PASCAL}RootCapabilities,
    ): ${PASCAL}RootCapabilities
}
EOF

    ROOT_PKG="feature/$NAME-rooted/src/main/kotlin/dev/ranzlappen/gadget/feature/$NAME/rooted"
    mkdir -p "$ROOT_PKG/di"
    cat > "feature/$NAME-rooted/build.gradle.kts" <<EOF
// :feature:$NAME-rooted — rooted-only $PASCAL capability surface.
//
// Sibling to :feature:$NAME, pulled in by the rooted flavor of :app via
// \`rootedImplementation\`. The standard APK is physically unable to compile
// against this module — see CLAUDE.md's "Standard-APK leak gate".

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.$NAME.rooted"
}

dependencies {
    implementation(project(":feature:$NAME"))
    // Root-safety framework — route every privileged call through RootSafetyGate
    // + a RootFeatureKey (clamp to a hard ceiling, restore state in a
    // NonCancellable finally). See the torch-rooted reference.
    implementation(project(":core:root"))

    // libsu — the rooted shell binder. Scoped to this rooted-flavor module only.
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
}
EOF
    cat > "feature/$NAME-rooted/src/main/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF
    cat > "$ROOT_PKG/Rooted${PASCAL}RootCapabilities.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME.rooted

import dev.ranzlappen.gadget.feature.$NAME.${PASCAL}RootCapabilities
import javax.inject.Inject

/**
 * Rooted-flavor $PASCAL capabilities. TODO: probe real root availability and
 * route every privileged call through :core:root's RootSafetyGate + a
 * RootFeatureKey, reusing the feature's privileged sysfs controller (see the
 * RootedTorchRootCapabilities reference).
 */
class Rooted${PASCAL}RootCapabilities @Inject constructor() : ${PASCAL}RootCapabilities {
    override suspend fun isAvailable(): Boolean = false // TODO: real root probe
}
EOF
    cat > "$ROOT_PKG/di/Rooted${PASCAL}Module.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME.rooted.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.$NAME.${PASCAL}RootCapabilities
import dev.ranzlappen.gadget.feature.$NAME.rooted.Rooted${PASCAL}RootCapabilities
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class Rooted${PASCAL}Module {
    @Binds
    @Singleton
    abstract fun bind${PASCAL}RootCapabilities(
        impl: Rooted${PASCAL}RootCapabilities,
    ): ${PASCAL}RootCapabilities
}
EOF
fi

# --- register module(s) in settings.gradle.kts (base mode only) -----------
# Skeleton modules are already registered, so skeleton-fill skips this.
register_includes() {
    local settings="settings.gradle.kts"
    local last
    last="$(grep -n '^[[:space:]]*":feature:' "$settings" | tail -1 | cut -d: -f1)"
    if [[ -z "$last" ]]; then
        echo "error: couldn't find the :feature: include block in $settings" >&2
        exit 1
    fi
    local entries="    \":feature:$NAME\","
    if [[ "$ROOTED" == true ]]; then
        entries+=$'\n'"    \":feature:$NAME-rooted\","$'\n'"    \":feature:$NAME-standard\","
    fi
    local tmp
    tmp="$(mktemp)"
    head -n "$last" "$settings" > "$tmp"
    printf '%s\n' "$entries" >> "$tmp"
    tail -n +"$((last + 1))" "$settings" >> "$tmp"
    mv "$tmp" "$settings"
}
if [[ "$MODE" == base ]]; then
    register_includes
fi

# --- done -----------------------------------------------------------------
echo "✓ filled feature/$NAME sources (${PASCAL}Screen / ${PASCAL}ViewModel / ${PASCAL}Navigation)"
if [[ "$MODE" == base ]]; then
    echo "✓ created feature/$NAME (build.gradle.kts + manifest)"
    echo "✓ registered include in settings.gradle.kts"
fi
if [[ "$ROOTED" == true ]]; then
    echo "✓ created feature/$NAME-standard, feature/$NAME-rooted"
fi
echo
echo "Verify it compiles:"
echo "  ./gradlew :feature:$NAME:assembleDebug"
echo
echo "Manual wiring left (see this script's header):"
echo "  1. Add GadgetDestination.$PASCAL in core/navigation + append to modules"
echo "  2. Call ${CAMEL}Screen() from the GadgetApp { … } builder in :app"
if [[ "$ROOTED" == true ]]; then
    echo "  3. In app/build.gradle.kts:"
    echo "       \"standardImplementation\"(project(\":feature:$NAME-standard\"))"
    echo "       \"rootedImplementation\"(project(\":feature:$NAME-rooted\"))"
fi
