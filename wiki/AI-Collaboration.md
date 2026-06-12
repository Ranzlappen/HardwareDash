# AI Collaboration

The central working manual for AI agents on HardwareDash. Replaces the
former `AI-COLLABORATION.md` + the `CLAUDE.md` workflow sections. The repo
keeps only a tiny `CLAUDE.md` bootstrap that points here.

## Roles

| Role | Who | Responsibility |
|---|---|---|
| **Maintainer** | Matthias (project owner) | Direction, final review, approves/pushes, decides scope and trade-offs. |
| **Implementer** | Claude (Code Planning Mode) | Detailed planning + implementation; works on a named feature branch; pauses for review. |
| **Reviewer** | Grok / ChatGPT | Reviews plans, enforces architecture / safety / consistency, sanity-checks. |
| **Codex (if used)** | Codex | Secondary implementation for delegated slices. |

## The source-of-truth hierarchy

Read top-down; a higher source wins on conflict:

1. **The maintainer's explicit instruction** in the current task.
2. **This wiki** — the AI Collaboration, [Module Authoring
   Contract](Module-Authoring-Contract), [Design System](Design-System),
   [Feature Migration Guide](Feature-Migration-Guide), and [Roadmap &
   Status](Roadmap-and-Status) pages.
3. **Git** (the code itself + commit history + PR descriptions) — the
   authoritative record of what shipped.
4. The tiny repo `CLAUDE.md` bootstrap (a pointer, not content).

When the wiki and the code disagree, **the code is what ships** — fix the
wiki (and say so), don't code to a stale doc.

## Read the wiki before coding

Before planning or coding any task:

1. [AI Collaboration](AI-Collaboration) (this page).
2. [Module Authoring Contract](Module-Authoring-Contract).
3. [Design System](Design-System).
4. [Feature Migration Guide](Feature-Migration-Guide).
5. [Roadmap & Status](Roadmap-and-Status).

For a feature migration, also read [Torch Blueprint](Torch-Blueprint) and
the relevant subsystem page (widgets / monitoring / automation / flavors).

## Modes

### Planning mode

- Produce a concrete, reviewable plan **before** writing code: file map,
  the lean v1 cut (what migrates now vs. deferred), the seams touched, and
  the CI traps that apply.
- The plan scratchpad (`/root/.claude/plans/<name>.md`) is a **per-task
  scratchpad, not a log** — each session **replaces** its contents with the
  current plan, never appends. Prior plans live in git + the PR + this
  wiki. Truncate if it balloons past ~500 lines.
- Use the maintainer's plan template (below).

### Coding mode

- Work **exclusively** on the assigned feature branch (`claude/<topic>`).
  Never push to `main` or another branch without explicit permission.
- Small atomic batches (≈5–8 files). After each batch: commit with a clear
  message, push, then continue.
- Only stop when **user input is genuinely required** to continue (the
  "pause after batch" protocol below).
- New code lives under `dev.ranzlappen.gadget.**`. **Never** import
  `com.gadget.**` (a CI-enforced review-blocker).
- Use the design system, not raw Material components. No raw `dp`. No
  `BuildConfig.IS_ROOTED` branching. No feature-to-feature dependency.
- Respect the [Module Authoring Contract](Module-Authoring-Contract) and
  the [Troubleshooting](Troubleshooting) CI traps (there's no local SDK —
  CI is the first real compile).

### Review mode

- Check the diff against the [review checklist](#review-checklist) below.
- Enforce architecture (dependency direction), safety (root gating, leak
  gate), and consistency (design tokens, a11y contract).
- Verify the done checklist in the [Feature Migration
  Guide](Feature-Migration-Guide) is satisfied.

## "Pause after batch" protocol

After each atomic batch, **commit + push + pause for review** rather than
barrelling through the whole feature. The maintainer (or reviewer)
approves before the next batch starts. Exception: when the maintainer has
explicitly said "don't stop until finished," run the batches back-to-back
but still commit at clean boundaries so each is reviewable in isolation.

## Branch & PR conventions

- **Branch:** `claude/<topic>` (one batch series per branch). Develop,
  commit, and push only to the assigned branch.
- **Commits:** clear, descriptive, one logical change each — small enough
  to review, large enough to compile. Don't put model identifiers or
  internal tooling notes in commit messages.
- **PRs:** one per batch series onto `main`. Do **not** open a PR unless
  the maintainer asks. Use the [PR template](#pr-description-template).
- **Don't** delete repo docs before the wiki replacement is complete; do
  documentation moves in two PRs (wiki creation, then repo cleanup).

## How to update the wiki after code changes

Per the maintenance rules, every change updates the relevant page **in the
same PR**:

- New/changed feature → [Roadmap & Status](Roadmap-and-Status),
  [Feature Catalog](Feature-Catalog), [Module Catalog](Module-Catalog).
- New public component → [Component Catalog](Component-Catalog).
- New asset category → [Asset Catalog](Asset-Catalog).
- New AI workflow rule → this page.
- Major architectural choice → [Decision Records](Decision-Records).
- New CI trap discovered → [Troubleshooting](Troubleshooting).

Every wiki page carries a footer with **last reviewed**, **source paths**,
and **related modules** — update the date when you touch a page.

## Anti-patterns for AI agents

- Coding to a stale doc instead of the code.
- Importing `com.gadget.**` in new code.
- Adding a feature-to-feature dependency (route through `core/*`).
- Branching on `BuildConfig.IS_ROOTED`.
- Raw `dp` / raw `Surface(color=…)` glass / default text wrapping.
- Overbuilding a simple feature into a torch (see [Torch
  Blueprint](Torch-Blueprint) — copy the shape, not every seam).
- Appending to the plan scratchpad instead of replacing it.
- Pushing to `main` or an unassigned branch.
- Declaring done without monitoring + automation readiness.
- Skipping the widget pin-reliability halves.

## Templates

### Migration plan template

```
# Migrate <feature> — plan
## Lean v1 cut (now)
- …
## Deferred (issues)
- …
## Files (by step)
1. build.gradle.kts deps
2. Controller + Hilt module
3. ViewModel + ScreenContent + Screen
4. Navigation (GadgetDestination + GadgetApp wiring)
5. Surfaces (tile/widget/service) — if any
6. MetricSource + ActionHandler
7. Tests + previews
## Seams touched / CI traps that apply
- …
## Wiki pages to update
- …
```

### PR description template

```
## What
<one-paragraph summary of the batch>
## Why
<link to roadmap item / issue>
## Changes
- <module>: <change>
## Verification
- [ ] CI green (standard + rooted)
- [ ] Leak gate / no-legacy-import passing
- [ ] Tests added/updated
- [ ] Wiki pages updated
## Follow-ups / deferred
- <issue links>
```

### Done checklist

The acceptance checklist lives in the [Feature Migration Guide → Done
checklist](Feature-Migration-Guide) and the [Module Authoring
Contract](Module-Authoring-Contract). A migration isn't done until it's
both monitoring- and automation-ready.

## Review checklist

- [ ] Dependency direction respected (`app → feature → core`; no
      feature↔feature; no `*-rooted` in standard).
- [ ] No `com.gadget.**` imports in new code.
- [ ] Design tokens from `LocalGadgetTheme.current`; no raw `dp`; glass via
      `GlassSurface`; a11y contract met.
- [ ] Root gating via `:core:root` seam, not `BuildConfig.IS_ROOTED`.
- [ ] `MetricSource` + `ActionHandler` bound `@IntoMap`.
- [ ] Widget pin reliability halves present (if widgets).
- [ ] Tests + preview matrix; CI traps pre-checked.
- [ ] Relevant wiki pages updated.

---

> _Last reviewed: 2026-06-12 · Source: `AI-COLLABORATION.md`, `CLAUDE.md`
> (workflow + plan hygiene), `MASTER-PLAN.md` · Related: all._
