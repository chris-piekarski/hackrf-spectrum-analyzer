# AGENTS.md

This document provides guidance for AI coding agents (and human contributors) working on the **hackrf-spectrum-analyzer** repository.

## Project Overview

This is a Java desktop spectrum analyzer GUI optimized for the HackRF One SDR (USB device). It wraps the `hackrf_sweep` tool as a native shared library (via JNA) for high-performance wideband sweeps.

Key technologies:
- Java 21+ (Swing UI + FlatLaf + JFreeChart for plots)
- Native C (hackrf library v2026.01.3 + custom sweep-as-library patch)
- Maven for Java build
- Custom Makefile for cross-platform native + Java packaging (Linux + Windows)
- Supports real-time spectrum, waterfall, peak/persistent display, spur filter, frequency allocations, quick band selectors, and Antenna LNA (+14 dB) control.

The project is a maintained fork of [pavsa/hackrf-spectrum-analyzer](https://github.com/pavsa/hackrf-spectrum-analyzer) with added quick-select UI and significant test coverage improvements.

**Primary use case**: Users with a physical HackRF One USB device.

## Essential Commands

**Always start here:**

```bash
make help
```

This shows all available targets with descriptions and categories (colorized).

### Common Targets (from root)

- `make build` — Full build (natives + JAR + zip)
- `make test` — Run unit tests (Maven + JaCoCo). Does **not** require a HackRF.
- `make test-hw` — Hardware ITs (`*IT`, `@Tag("hardware")`): USB, firmware/USB API/board, `SpectrumSweepEngine` queue + dataset, start/stop/restart, LNA + antenna-power, FFT/freq restart. Skips if no radio.
- `make info` — List attached HackRF devices, the SDK/USB API this app is pinned to, and whether a newer GSG firmware/libhackrf exists. Alias: `make list-devices`
- `make firmware-update` — Dry-run official GSG SPI flash. Write only with `CONFIRM=1` (optional `VERSION=2026.01.3`). Not part of `build` / `test`.
- `make udev` — Install persistent udev rules (sudo once) so WSL usbipd nodes stay writable.
- `make lint` — Compile/lint checks
- `make stats` — Rewrite [docs/stats.md](docs/stats.md) (first-party LOC, packages, tests, git). Do not hand-edit that file.
- `make mermaid` — Parse-check every first-party Mermaid fence (`mmdc` when installed)
- `make start` — Build (if needed) + launch the Linux app
- `make clean` — Clean build artifacts
- `make run` — Alias for `start`

From inside `src/hackrf-sweep/` you can also run the detailed native build targets directly (`make help` there too).

### Testing & Coverage

```bash
make test
# or directly
cd src/hackrf-sweep && mvn clean test

# Coverage report is written by `make test` (JaCoCo report bound to the test phase)
# Open src/hackrf-sweep/target/site/jacoco/index.html
```

Unit tests cover core DSP (SpurFilter, PersistentDisplay, DatasetSpectrum*, allocations, EMA, firmware parse, SpectrumSweepEngine, RadioIdentity, …) plus UI helpers. They run without hardware. **Do not bake a class count into this file** — run `make stats` and cite [docs/stats.md](docs/stats.md).

### Building Details

See [docs/building.md](docs/building.md) for full instructions, including required packages (Ubuntu recommended for cross-build).

Native build requires:
- Linux host with mingw-w64 for Windows cross-compilation
- hackrf submodule pinned to `HACKRF_SDK_PIN` (v2026.01.3) and patched automatically (`src-c/0001-hackrf_sweep-to-library-conversion-v2026.01.3.patch`)

## Documentation

All first-class documentation lives under `docs/`:

- [docs/README.md](docs/README.md) — Documentation index
- [docs/getting-started.md](docs/getting-started.md)
- [docs/building.md](docs/building.md) — Build process & Makefile targets
- [docs/development.md](docs/development.md) — Dev workflow, testing, linting
- [docs/hackrf-setup.md](docs/hackrf-setup.md) — Hardware, udev, firmware, Zadig
- [docs/usage.md](docs/usage.md) — Running the analyzer, features, quick selects
- [docs/architecture.md](docs/architecture.md) — High-level design (core, native, UI)
- [docs/stats.md](docs/stats.md) — generated first-party stats (`make stats`)
- [docs/contributing.md](docs/contributing.md)
- [docs/plans/](docs/plans/README.md) — living implementation plans (status + checklists must stay current)

**Diagrams**: Use Mermaid fences in `docs/`. GitHub renders them with Mermaid 11. Prefer `flowchart`, `sequenceDiagram`, `classDiagram`, and `pie`. Do **not** use `deploymentDiagram` (removed in Mermaid 11). Quote sequence `Note` text if it contains `>`. After adding or changing a diagram, run `make mermaid`.

Root-level files:
- `README.md` — Project overview + quick links
- `AGENTS.md` — This file (for AI agents)
- `CONTRIBUTING.md` — Contribution guidelines
- `LICENSE`

**Never edit the old `Readme.md` or `src/hackrf-sweep/Readme.md` directly** — content has been migrated to the `docs/` structure and root `README.md`.

## Development Workflow

1. Run `make help` to explore available commands.
2. Make changes in `src/hackrf-sweep/src/main/java/...` (or native under `src-c/` / lib/hackrf).
3. Add or update unit tests for any new logic (especially in `core/` package).
4. Run `make test` and ensure coverage doesn't regress significantly.
5. Run `make lint`. After doc or layout changes run `make mermaid` and `make stats`.
6. Update relevant docs under `docs/`. Never hand-edit `docs/stats.md`.
7. Use `make start` to manually verify with a real HackRF when possible.
8. Commit with clear messages. Reference issues when applicable.

### Adding Features

- Core DSP changes (SpurFilter, peaks, spectrum datasets, etc.) **must** have corresponding unit tests.
- UI changes should be accompanied by updates to `docs/usage.md`.
- New Makefile targets must be added to both the root `Makefile` and the detailed `src/hackrf-sweep/Makefile`, with proper `##` descriptions for `make help`.
- When touching native code, ensure the patch in `src-c/` and build process still work.

### Code Style

- Java: Follow existing conventions (no major formatter enforced yet, but keep consistent with surrounding code).
- Makefiles: Use the established colorized help pattern with `##@ Category` sections and `## description` on targets.
- Documentation: Use clear Markdown, keep examples copy-pasteable. Prefer linking to `docs/` from root files.

## Working with AI Agents

- Always begin by running `make help` (both at root and in `src/hackrf-sweep/`) to understand current targets.
- Prefer editing files under `docs/` for documentation rather than root-level Readme files.
- When asked to add tests, prioritize the `core/` package and use existing patterns (synthetic data, reflection for time/graphics state where needed).
- After structural changes (new targets, new docs, major refactors), update this `AGENTS.md` and `docs/development.md`.
- Save implementation plans under `docs/plans/<name>.md` and list them in `docs/plans/README.md`. Keep the Status block and checkboxes in sync with what is actually done; do not leave stale items.
- Do not assume a full Java environment is available in all contexts — many verification steps require the user to run `mvn` / `make` locally.
- For coverage work, after adding tests run the JaCoCo report and report specific class/line improvements.

## Upstream

GitHub's "ahead/behind" count vs `pavsa/hackrf-spectrum-analyzer` is misleading: the histories share no commit SHAs (rewritten old commits). Do not rebase onto `upstream/master`. Port individual upstream bugfixes onto this tree. See [docs/development.md](docs/development.md#syncing-with-upstream).

## Known Limitations / Gotchas

- Full end-to-end testing requires a real HackRF One + proper udev permissions.
- The native build is Linux-only for cross-compilation (mingw).
- Some UI components are still difficult to unit test (Swing-heavy). Focus unit tests on `core/` logic.
- `HackrfSweepLibrary` is hand-maintained (`make jnabridge` does not run JNAerator). The UI requires a **headful** JDK 21+.

## Questions?

Open an issue or refer to the documentation under `docs/`.

Thank you for helping keep this tool high-quality for HackRF users!