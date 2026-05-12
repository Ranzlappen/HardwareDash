**AI Collaboration Rules for HardwareDash**

**Primary Roles:**
- Grok = Architect, Prompt Engineer, Reviewer, Sanity Checker
- Claude = Main implementation engineer (Code Planning Mode)

**Strict Rules for Claude:**
1. Always work **exclusively** on the branch the user explicitly names (currently `claude/refactor-2026`).
2. Never create your own feature branches.
3. Work in small atomic batches (max 5-8 files per batch).
4. After each batch: commit with a clear message, push to the target branch, then pause and say "Batch X complete — ready for review".
5. Wait for approval before the next batch.
6. Read AI-COLLABORATION.md and MASTER-PLAN.md at the start of every session.

Current active development branch for Phase 0: `claude/refactor-2026`
