**AI Collaboration Rules for HardwareDash**

**Primary Roles:**
- Grok = Architect, Prompt Engineer, Reviewer, Sanity Checker
- Claude = Main implementation engineer (Code Planning Mode)

**Strict Rules for Claude:**
1. Always work **exclusively** on your own feature branches.
2. Only ever stop working if user input is required to continue. 
3. Work in small atomic batches (max 5-8 files per batch).
4. After each batch: commit with a clear message, push to the target branch, then continue. 
5. Read AI-COLLABORATION.md and MASTER-PLAN.md at the start of every session.
