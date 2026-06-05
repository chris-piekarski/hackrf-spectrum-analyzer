# Documentation

This directory contains the first-class documentation for the **hackrf-spectrum-analyzer** project.

## Quick Navigation

- [Getting Started](getting-started.md)
- [Building & Running](building.md)
- [Development Guide](development.md) (including testing and linting)
- [HackRF Hardware Setup](hackrf-setup.md)
- [Usage & Features](usage.md)
- [Architecture](architecture.md)
- [Contributing](contributing.md)

## Diagrams

This documentation uses [Mermaid](https://mermaid.js.org/) for UML-style diagrams (flowcharts, sequence diagrams, class diagrams, deployment diagrams, etc.). These render natively on GitHub.

Current diagrams cover:
- Architecture (high-level, data flow, class, deployment, package)
- Build pipeline
- Development & test workflows
- Contributing process
- Getting started / HackRF setup flows
- Usage interaction loop

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