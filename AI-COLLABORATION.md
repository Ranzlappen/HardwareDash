# AI-COLLABORATION.md

> **Status: DRAFT** — created in Batch 0 of the monorepo refactor because
> the file referenced from `MASTER-PLAN.md` did not previously exist on
> this branch. The rules below were distilled from `MASTER-PLAN.md`,
> `CLAUDE.md`, and the explicit Batch-0 protocol issued at the start of
> the refactor. Owner: please correct in review.

This document governs how AI assistants (Claude Code, Grok, sanity-check
models) interact with this repository during the `refactor-2026`
modular-monorepo refactor.

## 1. Branch isolation

- All refactor work happens on **`refactor-2026`**. Until the refactor
  lands, `main` is frozen as the production source. No feature merges
  back to `main` until the refactor is signed off.
- A parallel auxiliary branch
  **`claude/android-monorepo-refactor-BOmij`** is kept fast-forwarded
  with every push to `refactor-2026`, because the
  Claude-Code-on-the-web harness was provisioned to push there. Treat
  it as a mirror — do not commit independently on it.
- Never force-push either branch. Never push to `main`.

## 2. Batch protocol (Claude → review → push)

Each round trip on `refactor-2026` is one **batch**:

1. Claude proposes a batch in plan/code mode, listing **every file** it
   will create, modify, or delete and the **full content** of each file
   (or a template + module list when the same file shape repeats).
2. Claude commits and pushes the batch to `refactor-2026` (and mirrors
   to the harness branch) with a clear `Phase X / Batch Y:` prefixed
   commit message.
3. Claude pauses with `Batch X complete — ready for review` and **waits
   for approval** before starting the next batch.
4. Grok (or the human reviewer) reviews the batch on GitHub. Approval
   may be implicit (a `proceed to Batch X+1` instruction) or explicit
   (review comments resolved).

## 3. Batch size

- **Hard cap:** 5–8 files per batch, OR **one logical group** when a
  single group genuinely spans more files (e.g. Batch 0's skeleton).
  The principle is reviewability — if the diff can't be reviewed in
  one sitting, the batch is too big.
- **Never** bundle "while I'm in there" refactors with the batch's
  primary change. Side-quests get their own batch.
- A batch must leave the tree compilable on CI. Empty skeletons get a
  minimal Android-library `build.gradle.kts` so `./gradlew tasks`
  succeeds.

## 4. Plan-file hygiene

Mirroring the rule in `CLAUDE.md`:

- `/root/.claude/plans/<name>.md` is a **per-task scratchpad**, never
  an append-only log. Each session truncates and replaces.
- Shipped batches live in **git history**, the **PR description**, and
  the `MASTER-PLAN.md` changelog — never in the plan file.
- If a plan file balloons past ~500 lines, truncate at the start of the
  next session.

## 5. Naming

- Project internal name remains **Gadget** (display name; legacy
  `com.gadget.*` namespace).
- New applicationIds for side-by-side install (rolled out in a later
  Phase-0 batch):
  - standard → `dev.ranzlappen.gadget`
  - rooted   → `dev.ranzlappen.gadget.rooted`
- New module namespaces under
  `dev.ranzlappen.gadget.<core|feature>.<name>`. Legacy
  `com.gadget.*` packages remain in place under `app/src/main/` until
  each is migrated by its owning batch.

## 6. Compilability invariant

- Every batch leaves CI green. A red CI on `refactor-2026` **blocks**
  the next batch — the fix is the next thing Claude does, not the next
  feature.
- New empty modules apply a real plugin (`com.android.library`) so the
  module configures successfully. Empty body, but real plugins.

## 7. Standard-APK leak gate (inherited)

CI's `Assert standard APK has no rooted leakage` step from
`.github/workflows/build-apk.yml` remains in force throughout the
refactor. Rooted code introduced by any batch must be scoped to a
`feature/*-rooted/` module (or the existing `app/src/rooted/` source
set) and depended on only from `rootedImplementation` configurations.

If a batch breaks the leak gate, treat that the same as a red CI: fix
before continuing.

## 8. Convention plugins (Batch 1+)

- The first convention plugin (`gadget.android.library`) lands in
  **Batch 1**.
- Once it exists, every new module skeleton uses the convention
  plugin — no inline `alias(libs.plugins.android.library)` blocks in
  `feature/*` or `core/*` build files outside of `build-logic/`.
- Batch 0 modules are explicitly grandfathered; Batch 1 sweeps them
  over to the convention plugin.

## 9. Kotlin / Compose / AGP

- Kotlin **1.9.0** (matches current `gradle/libs.versions.toml`).
  Migration to Kotlin 2.0+ and the standalone Compose Compiler Gradle
  plugin is a later, scoped batch (probably Phase 2).
- AGP **8.6.1**. Bumps require their own batch.
- Compose-using modules apply `composeOptions` via the convention
  plugin, not inline.

## 10. What Claude does NOT do

- Never push directly to `main`.
- Never force-push `main` or `refactor-2026`.
- Never amend a published commit. Always add a new commit.
- Never bypass commit hooks (`--no-verify`, `--no-gpg-sign`). If a
  hook fires, fix the hook's complaint.
- Never commit secrets, signing keys, or upload-keystore material.
- Never `rm -rf app/src/rooted/` or any source set to "exclude" code
  from a flavor; AGP source-set scoping already does this and the
  deletion would race the Gradle build cache.
- Never branch on `BuildConfig.IS_ROOTED` directly. Always flow
  flavor differences through the `RootCapabilityRegistry` /
  `RootSafetyGate` Hilt seam.

## 11. Issues, PRs, and GitHub comments

- Comment frugally on GitHub. Reply only when a reply is genuinely
  necessary (a reviewer suggestion that can't be done, a CI failure
  that needs human input).
- Do not open a PR until the user asks. The Phase-0 batches land
  directly on `refactor-2026`; a single PR `main ← refactor-2026`
  opens once the refactor is complete.
- When in doubt, ask via `AskUserQuestion`.

---

When this document and `CLAUDE.md` disagree, `CLAUDE.md` wins — it is
the user-authored project rulebook and survives across branches. This
file (`AI-COLLABORATION.md`) is the refactor-scoped operational
protocol.
