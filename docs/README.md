# Documentation

This directory contains the first-class documentation for the **hackrf-spectrum-analyzer** project.

## Quick Navigation

- [Getting Started](getting-started.md)
- [Building & Running](building.md)
- [Development Guide](development.md) (including testing and linting)
- [HackRF Hardware Setup](hackrf-setup.md)
- [Usage](usage.md)
- [Architecture](architecture.md)
- [Repository stats](stats.md) — first-party LOC, packages, tests (`make stats`)
- [Contributing](contributing.md)
- [Plans](plans/README.md) — living implementation plans (keep status/checklists current)

## Diagrams

This documentation uses [Mermaid](https://mermaid.js.org/) for UML-style diagrams (flowcharts, sequence diagrams, class diagrams, deployment diagrams, etc.). These render natively on GitHub.

Current diagrams cover:
- Architecture (high-level, data flow, class, package, build-to-user)
- Build pipeline
- Development and test workflows
- Contributing process
- Getting started / radio setup
- Usage interaction loop
- Generated pies in [stats.md](stats.md)

`make mermaid` extracts every fence and parses it with mermaid-cli when `mmdc` is on `PATH` (Mermaid 11, matching current GitHub). Avoid `deploymentDiagram` (dropped in Mermaid 11) and unquoted `>` in sequence `Note` lines.

## Root-Level Files

See these files in the repository root:

- `README.md` — Project overview and quick start
- `AGENTS.md` — Guidance for AI coding agents
- `CONTRIBUTING.md` — Contribution process
- `LICENSE`

## Building the Docs

These are plain Markdown files. They are rendered nicely on GitHub and are meant to be read directly or via the repo's GitHub Pages / wiki if set up later.

When making changes:
- Update the relevant doc under `docs/`
- Keep examples up-to-date with current `make` targets (run `make help`)
- Update `AGENTS.md` and root `README.md` when adding significant new processes or targets

For the most up-to-date build instructions, always prefer `make help` over static docs.