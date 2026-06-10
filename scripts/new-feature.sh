#!/usr/bin/env bash
# =========================================================================
# scripts/new-feature.sh — scaffold a new feature module.
# =========================================================================
#
# Generates a `feature/<name>/` module wired to the design system and the
# `gadget.android.feature` convention plugin, ready to flesh out per the
# eight-step recipe in docs/migration-guide.md.
#
# -------------------------------------------------------------------------
# Usage
# -------------------------------------------------------------------------
#
#   scripts/new-feature.sh <name> [--rooted]
#
# <name> must be lowercase alphanumeric (a–z, 0–9), starting with a letter,
# with no hyphens or spaces (e.g. `sensors`, `gps`, `flipper`). Multi-word
# capabilities that need a hyphenated module name (e.g. `radios-nfc`) are not
# auto-scaffolded — copy an existing module by hand for those.
#
# Creates (base):
#   feature/<name>/build.gradle.kts                         (applies gadget.android.feature)
#   feature/<name>/src/main/AndroidManifest.xml
#   feature/<name>/src/main/kotlin/dev/ranzlappen/gadget/feature/<name>/
#       <Name>Screen.kt        — stateless content + Hilt entry on the design system
#       <Name>ViewModel.kt     — @HiltViewModel
#       <Name>Navigation.kt    — NavGraphBuilder.<name>Screen() + route const
#
# With --rooted, also creates the standard/rooted sibling pair (the
# torch/vibration model) so the standard APK is physically unable to compile
# against root code:
#   feature/<name>/...<Name>RootCapabilities.kt             (the flavor-agnostic interface)
#   feature/<name>-standard/...                             (no-op impl + Hilt @Binds)
#   feature/<name>-rooted/...                               (real impl seam + Hilt @Binds)
#
# Appends the `:feature:<name>` include line(s) to settings.gradle.kts.
#
# -------------------------------------------------------------------------
# Manual steps left after running (intentionally NOT automated — they touch
# shared files where a blind edit would be wrong):
#   1. Add a `GadgetDestination.<Name>` data object in
#      core/navigation/.../GadgetDestination.kt and append it to the
#      `modules` list (then fix every non-exhaustive `when (destination)`).
#   2. Call `<name>Screen()` from the `GadgetApp { … }` builder in :app.
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

if [[ ! "$NAME" =~ ^[a-z][a-z0-9]*$ ]]; then
    echo "error: <name> must be lowercase alphanumeric starting with a letter" >&2
    echo "       (got '$NAME'). Hyphenated names aren't auto-scaffolded." >&2
    exit 2
fi

if [[ -d "feature/$NAME" ]]; then
    echo "error: feature/$NAME already exists" >&2
    exit 2
fi

# --- derived identifiers --------------------------------------------------
# Pascal: capitalise the first letter (name is [a-z][a-z0-9]* so this is safe).
PASCAL="$(tr '[:lower:]' '[:upper:]' <<<"${NAME:0:1}")${NAME:1}"
UPPER="$(tr '[:lower:]' '[:upper:]' <<<"$NAME")"
PKG_DIR="feature/$NAME/src/main/kotlin/dev/ranzlappen/gadget/feature/$NAME"

echo "Scaffolding :feature:$NAME (rooted=$ROOTED) …"

# --- base module ----------------------------------------------------------
mkdir -p "$PKG_DIR"

cat > "feature/$NAME/build.gradle.kts" <<EOF
// :feature:$NAME — TODO one-line description of the capability.
//
// Scaffolded by scripts/new-feature.sh. Build out the real controller/UI
// per the eight-step recipe in docs/migration-guide.md.

plugins {
    id("gadget.android.feature")
}

android {
    namespace = "dev.ranzlappen.gadget.feature.$NAME"
}

dependencies {
    // :core:ui transitively brings :core:designsystem (DashCard, GadgetEmptyState,
    // the design-system component library + the preview matrix annotations).
    implementation(project(":core:ui"))
    // :core:navigation surfaces GadgetDestination + the route plumbing.
    implementation(project(":core:navigation"))
}
EOF

cat > "feature/$NAME/src/main/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest />
EOF

cat > "$PKG_DIR/${PASCAL}Screen.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME

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
package dev.ranzlappen.gadget.feature.$NAME

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
package dev.ranzlappen.gadget.feature.$NAME

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
fun NavGraphBuilder.${NAME}Screen() {
    composable(route = ${UPPER}_ROUTE) {
        ${PASCAL}Screen()
    }
}
EOF

# --- rooted sibling pair (optional) ---------------------------------------
if [[ "$ROOTED" == true ]]; then
    # Flavor-agnostic capability interface in the base module.
    cat > "$PKG_DIR/${PASCAL}RootCapabilities.kt" <<EOF
package dev.ranzlappen.gadget.feature.$NAME

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

    # ---- :feature:<name>-standard (no-op) --------------------------------
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

    # ---- :feature:<name>-rooted (real impl seam) -------------------------
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

# --- register module(s) in settings.gradle.kts ----------------------------
# Insert after the last existing ":feature:…" include entry (the feature
# include block is contiguous; order isn't strictly alphabetical so we append
# rather than sort).
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
register_includes

# --- done -----------------------------------------------------------------
echo "✓ created feature/$NAME"
if [[ "$ROOTED" == true ]]; then
    echo "✓ created feature/$NAME-standard, feature/$NAME-rooted"
fi
echo "✓ registered include(s) in settings.gradle.kts"
echo
echo "Verify it compiles:"
echo "  ./gradlew :feature:$NAME:assembleDebug"
echo
echo "Manual wiring left (see this script's header):"
echo "  1. Add GadgetDestination.$PASCAL in core/navigation + append to modules"
echo "  2. Call ${NAME}Screen() from the GadgetApp { … } builder in :app"
if [[ "$ROOTED" == true ]]; then
    echo "  3. In app/build.gradle.kts:"
    echo "       \"standardImplementation\"(project(\":feature:$NAME-standard\"))"
    echo "       \"rootedImplementation\"(project(\":feature:$NAME-rooted\"))"
fi
