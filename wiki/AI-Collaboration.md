# AI Collaboration

The central working manual for AI agents on HardwareDash. **Direction** (what
to build next, principles, lessons from v2) lives in
[`MASTER-PLAN.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/MASTER-PLAN.md)
in the repo; this page is the **workflow**. The repo's `CLAUDE.md` is a short
bootstrap pointing at both.

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
2. **`MASTER-PLAN.md`** (repo root, versioned with the branch) — direction,
   principles, the current batch.
3. **This wiki** — the AI Collaboration, [Module Authoring
   Contract](Module-Authoring-Contract), [Design System](Design-System), and
   [Roadmap & Status](Roadmap-and-Status) pages.
4. **Git** (the code itself + commit history + PR descriptions) — the
   authoritative record of what shipped.
5. The repo `CLAUDE.md` bootstrap (a pointer, not content).

When the wiki and the code disagree, **the code is what ships** — fix the
wiki (and say so), don't code to a stale doc.

## Read the wiki before coding

Before planning or coding any task:

1. `MASTER-PLAN.md` (repo root).
2. [AI Collaboration](AI-Collaboration) (this page).
3. [Module Authoring Contract](Module-Authoring-Contract).
4. [Design System](Design-System).
5. [Roadmap & Status](Roadmap-and-Status).

Read a subsystem page (widgets / monitoring / automation / flavors) only when
the task touches it. [Torch Blueprint](Torch-Blueprint) and [Feature Migration
Guide](Feature-Migration-Guide) are v2 references — useful for *how* a seam
works, not as a shape every module must copy.

## Modes

### Planning mode

- Produce a concrete, reviewable plan **before** writing code: file map,
  what lands now vs. deferred, the seams touched, the user-visible result,
  and the CI traps that apply.
- Durable plans go into `MASTER-PLAN.md` (a batch line with acceptance
  criteria). A per-session scratchpad (`/root/.claude/plans/<name>.md` when it
  exists) is **replaced, never appended**; truncate past ~500 lines.
- Use the maintainer's plan template (below).

### Coding mode

- Work **exclusively** on the assigned feature branch (`claude/<topic>`).
  Never push to `main` or another branch without explicit permission.
- Small atomic batches (≈5–8 files). After each batch: commit with a clear
  message, push, check the CI verdict (`ci-refactor.yml` runs on pushes to
  `claude/v3-**`), then pause.
- Only stop when **user input is genuinely required** to continue (the
  "pause after batch" protocol below).
- New code lives under `dev.ranzlappen.gadget.**`. **Never** import
  `com.gadget.**` (a CI-enforced review-blocker).
- Use the design system, not raw Material components. No raw `dp`. No
  `BuildConfig.IS_ROOTED` branching. No feature-to-feature dependency.
- Build the user-visible job first; add MetricSource / ActionHandler /
  widgets / rooted rows only when they earn their place (see the
  [contract](Module-Authoring-Contract)).
- Respect the [Module Authoring Contract](Module-Authoring-Contract) and
  the [Troubleshooting](Troubleshooting) CI traps (there's no local SDK —
  CI is the first real compile).

### Review mode

- Check the diff against the [review checklist](#review-checklist) below.
- Enforce architecture (dependency direction), safety (root gating, leak
  gate), and consistency (design tokens, a11y contract).
- Ask "can a person do the job now?" — the change should move a line in
  `docs/v3-smoke-test.md` closer to ticked, or be infrastructure the plan
  explicitly calls for.

## "Pause after batch" protocol

After each atomic batch, **commit + push + pause for review** rather than
barrelling through the whole feature. The maintainer (or reviewer)
approves before the next batch starts. Exception: when the maintainer has
explicitly said "don't stop until finished," run the batches back-to-back
but still commit at clean boundaries so each is reviewable in isolation.

## Branch & PR conventions

- **Branch:** `claude/<topic>` (one batch series per branch). Develop,
  commit, and push only to the assigned branch. v3 work happens on
  `claude/v3-pragmatic-overhaul`; archives (`legacy-main`,
  `archive/v2-modular-2026`) are never written to.
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
- Adding a seam, base class or module with fewer than two real consumers.
- Adding `assert_*` ActionHandler actions (use the engine's metric
  conditions), or MetricSources / rooted rows nobody will look at.
- Splitting one screen's worth of function across several modules.
- Marking something ✅ that nobody ran on a phone (use ⚪ Unverified).
- Appending to the plan scratchpad instead of replacing it.
- Pushing to `main` or an unassigned branch.
- Skipping the widget pin-reliability halves (when a widget exists).

## Templates

### Batch plan template

```
# <batch id> <title> — plan
## User-visible result
- what a person can do after this batch that they couldn't before
## Now / deferred
- …
## Files (by step)
1. …
## Data kept stable
- MetricSource keys / DataStore keys / rule @SerialNames touched (if any)
## Seams touched / CI traps that apply
- …
## Docs to update
- MASTER-PLAN checkbox, smoke-test line, wiki page(s)
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
- [ ] Tests added/updated for new pure logic
- [ ] Smoke-test line(s) to check on device
- [ ] MASTER-PLAN + wiki pages updated
## Follow-ups / deferred
- <issue links>
```

### Done checklist

The acceptance checklist is the [Module Authoring
Contract](Module-Authoring-Contract) required core. A feature is done when
its smoke-test line is ticked on a real device.

## Review checklist

- [ ] Dependency direction respected (`app → feature → core`; no
      feature↔feature; no `*-rooted` in standard).
- [ ] No `com.gadget.**` imports in new code.
- [ ] Design tokens from `LocalGadgetTheme.current`; no raw `dp`; glass via
      `GlassSurface`; a11y contract met.
- [ ] Root gating via `:core:root` seam, not `BuildConfig.IS_ROOTED`.
- [ ] Any new `MetricSource` / `ActionHandler` / widget / rooted row has a
      stated user benefit (no `assert_*` actions).
- [ ] Widget pin reliability halves present (if widgets).
- [ ] Persisted keys (metric keys, DataStore keys, rule JSON) unchanged or
      migrated.
- [ ] Tests for new pure logic; LightDark preview; CI traps pre-checked.
- [ ] MASTER-PLAN + relevant wiki pages updated.

---

> _Last reviewed: 2026-11-18 (v3 reset) · Source: `MASTER-PLAN.md`,
> `CLAUDE.md` · Related: all._
