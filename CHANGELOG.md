# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `make info` / `make list-devices` — list attached HackRF USB devices, the libhackrf/USB API this app is pinned to, device firmware when openable, and whether a newer Great Scott Gadgets release exists
- `make firmware-update` — dry-run official GSG firmware flash; writes SPI only with `CONFIRM=1` (refuses Pro image on a One; not part of `build`/`test`)
- `make udev` — install persistent udev rules so WSL usbipd HackRF nodes stay writable after attach
- Comprehensive unit test suite (30 test classes) focused on core DSP logic
- `SpectrumSweepEngine` — analyzer sweep/processing path without Swing; hardware IT asserts the queue fills and `datasetSpectrum` updates
- First-class `docs/` documentation structure
- Root `README.md`, `AGENTS.md`, `CONTRIBUTING.md`
- Improved top-level `Makefile` with `make help`, `make test`, `make lint`, `make start`, categorized colored output
- Enhanced `src/hackrf-sweep/Makefile` with matching help and quality targets

### Changed
- Host libhackrf / SDK pin **v2024.02.1 → v2026.01.3** (USB API 1.16). Sweep-as-library patch rebased (`num_fft_bins`, `stdbool.h`). JNA ABI unchanged. `isKnownHackrfBoard` accepts HackRF Pro (board id 5). Min firmware remains 2024.02.1.
- Modernized build (Maven + cross-platform native)
- Brought in upstream improvements (Antenna LNA support, firmware v2024.02.1, min FFT bin fix, etc.)
- Preserved and integrated Quick Select feature from this fork

### Fixed
- Finish remaining pavsa/hackrf-spectrum-analyzer v2024.11.10 ports: JFreeChart 1.5 renderer API (`setDefault*` instead of removed `setBase*`) and Settings UI null-safety for no-arg/designer construction
- `make test` is a real gate: Java 8-compatible tests, JaCoCo agent no longer dropped by Surefire, headless `GraphicsToolkit` fallback, null-safe allocation-table range queries, and Settings version `JLabel` (AWT `Label` threw in headless)
- Extracted `GainPolicy` and `RuntimePerformanceWatch` from the analyzer for unit testing; waterfall palette/x mapping is now static and tested
- Settings UI, Quick Select bands, and frequency-selector digit buttons covered without constructing the native analyzer
- Gated hardware integration tests (`make test-hw`, `@Tag("hardware")`, `*IT`): USB present, firmware/USB API/board via libhackrf, live sweep into the analyzer dataset path, start/stop/restart, antenna power + LNA, restart after FFT bin / frequency change. Skipped when no HackRF; not run by `make test`

See the [docs/](docs/) directory for current usage and development information.
