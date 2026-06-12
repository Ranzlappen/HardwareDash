# HardwareDash / Gadget — Wiki source

This directory is the **source of truth for the GitHub Wiki**. Every file
here is a wiki page: GitHub wikis are a flat git repository where each
`*.md` file is a page and the filename (minus extension) is the page
title. `_Sidebar.md` and `_Footer.md` are special pages that render as
the wiki sidebar and footer.

> **Why the docs live here as files:** the wiki is published by pushing
> these files to the wiki's own git repository
> (`https://github.com/Ranzlappen/HardwareDash.wiki.git`). Keeping the
> source in the main repo under `wiki/` lets the content be reviewed in a
> normal PR before it goes live, and lets CI sync it.

## Publishing to the GitHub Wiki

The wiki is a separate git repo. To publish (one-time setup: enable the
wiki under **Settings → Features → Wikis** and create any first page in
the UI so the repo exists):

```bash
# Clone the wiki repo (note the .wiki.git suffix)
git clone https://github.com/Ranzlappen/HardwareDash.wiki.git /tmp/hd-wiki

# Copy every page over (flat — no subdirectories; GitHub wiki is flat)
cp wiki/*.md /tmp/hd-wiki/

# Commit + push
cd /tmp/hd-wiki
git add -A
git commit -m "Sync wiki from main repo wiki/ directory"
git push origin master   # the wiki's default branch is 'master'
```

A CI job can automate this on every push to `main` that touches `wiki/`.

## Page index

| Page | Audience | Purpose |
|---|---|---|
| `Home.md` | everyone | Landing page + audience-based navigation |
| `Roadmap-and-Status.md` | maintainers | Phases, migrated features, open issues |
| `Architecture.md` | contributors | Module graph, dependency direction |
| `Module-Catalog.md` | contributors | Every Gradle module, indexed |
| `Feature-Catalog.md` | product/users | Every user-facing capability |
| `Design-System.md` | contributors | Tokens, theming, a11y, responsive rules |
| `Component-Catalog.md` | contributors | Every `:core:ui` composable |
| `Asset-Catalog.md` | contributors | Drawables, strings, widget XML, icons |
| `Feature-Migration-Guide.md` | contributors | The 8-step migration playbook |
| `Module-Authoring-Contract.md` | contributors | The acceptance checklist for a new module |
| `Torch-Blueprint.md` | contributors | The canonical advanced feature example |
| `Widgets-Tiles-and-Surfaces.md` | contributors | `:core:widgetkit` + pinning + RemoteViews |
| `Flavors-and-Root-Safety.md` | contributors | standard/rooted model + safety gates |
| `Automation-Engine.md` | contributors | Triggers/conditions/actions/runtime |
| `Monitoring-Framework.md` | contributors | `MetricSource`, monitor containers, charts |
| `Testing-and-CI.md` | contributors | Tests, preview matrix, leak gate, CI |
| `Troubleshooting.md` | contributors | CI-only traps and failure guide |
| `AI-Collaboration.md` | AI agents | Roles, modes, source-of-truth hierarchy |
| `AI-Prompt-Library.md` | AI agents | Reusable prompts |
| `Decision-Records.md` | maintainers | ADR home |
| `Glossary.md` | everyone | Shared vocabulary |

## Maintenance rules

1. All long-form documentation lives in the wiki — the repo keeps only a
   tiny `README.md` pointer and a tiny `CLAUDE.md` bootstrap.
2. Every wiki page carries a footer with **last reviewed**, **source
   paths**, and **related modules**.
3. Every feature-migration PR updates the relevant wiki pages.
4. Every new public component updates `Component-Catalog.md`.
5. Every new asset category updates `Asset-Catalog.md`.
6. Every new AI workflow rule updates `AI-Collaboration.md`.
7. Every major architectural choice updates `Decision-Records.md`.
8. Review the wiki after every major phase or feature batch.
