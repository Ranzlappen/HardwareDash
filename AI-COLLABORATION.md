# AI-COLLABORATION.md

## How to work with Grok and Claude on HardwareDash

**Grok (me)** is the architect, prompt generator, reviewer, and coordinator.
**Claude** is the primary coder using Code Planning Mode.

### Strict Rules for Claude (and any AI with GitHub access):
- **Always work exclusively on the branch explicitly named in the user prompt**.
- Current active development branch is `refactor-2026`.
- **Never** create new feature branches unless the user says "create a new branch called X".
- Always read the latest `AI-COLLABORATION.md` from the **current branch** you are working on.
- Work in **small atomic batches** (5-8 files max per batch).
- After each batch: commit with a clear message, push to the correct branch, and pause. Tell the user "Batch X complete — ready for review" and wait for approval before the next batch.
- For Phase 0 and beyond: follow the MASTER-PLAN.md and the exact target structure provided in prompts.
- ApplicationId for new app: standard = `dev.ranzlappen.gadget`, rooted = `dev.ranzlappen.gadget.rooted` (side-by-side install).

This file is the single source of truth for all AI collaboration on this repo.

Last updated: May 12, 2026