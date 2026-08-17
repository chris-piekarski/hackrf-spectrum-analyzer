# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive unit test suite (25 test classes) focused on core DSP logic
- First-class `docs/` documentation structure
- Root `README.md`, `AGENTS.md`, `CONTRIBUTING.md`
- Improved top-level `Makefile` with `make help`, `make test`, `make lint`, `make start`, categorized colored output
- Enhanced `src/hackrf-sweep/Makefile` with matching help and quality targets

### Changed
- Modernized build (Maven + cross-platform native)
- Brought in upstream improvements (Antenna LNA support, firmware v2024.02.1, min FFT bin fix, etc.)
- Preserved and integrated Quick Select feature from this fork

### Fixed
- Finish remaining pavsa/hackrf-spectrum-analyzer v2024.11.10 ports: JFreeChart 1.5 renderer API (`setDefault*` instead of removed `setBase*`) and Settings UI null-safety for no-arg/designer construction

See the [docs/](docs/) directory for current usage and development information.
