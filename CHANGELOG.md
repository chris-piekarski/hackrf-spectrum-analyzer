# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- MCP `spectrum_occupancy` (emitters above noise+8 dB, width, optional Wi-Fi `ch N` label) and `spectrum_history` (ring of summaries, new series on MHz/FFT change). `spectrum_summary` now includes `occupiedFraction` and `emitterCount`.

### Changed
- Docs and GitHub about/topics present the app as an **MCP interface for AI agents** on a live HackRF sweep (same JVM as the GUI). New [docs/mcp.md](docs/mcp.md).
- `src/hackrf-sweep` layout: Maven-standard Java tree (`src/main/{java,resources}`, `src/test/java`). Drop Eclipse CDT files, duplicate CSVs, unused Ant/JNAerator/32-bit/Zadig binaries. POM is indented, plugins version-pinned, `groupId` is `io.github.chris-piekarski`.

### Fixed
- Auto-gain was restarting the radio in a 32↔40 dB loop on Wi-Fi (quiet gap raised, a packet or a dropped burst reversed it). Each restart wiped the waterfall. Gain now only drops on real clip or a sustained hot streak; a disappeared burst is not treated as compression. The waterfall history is kept across gain-only retunes.

## [2.0.0] - 2026-08-19

### Added
- **Auto gain** (default on): live AGC picks LNA then VGA per Quick Select so the plot is not all-blue or all-red. Seeds by band (FM higher, 2.4 GHz lower), aims the peak near −28 dBm, drops immediately on clip, and holds Wi‑Fi bursts so quiet gaps do not pump. Uncheck **Auto** on the Gain row to take the sliders.
- Waterfall left-side time scale (`now`, `2s`, `5s`, …) aligned to the spectrum Y-axis gutter. Ages come from per-row timestamps so Pause does not drift the labels. Hover shows the row’s age next to the MHz readout.
- Opt-in MCP server (`make mcp` / `--mcp`) so local agents can read `spectrum_summary`, `spectrum_snapshot`, `radio_identity`, `sweep_config`, and `fm_stations` from the same JVM that holds the radio. Snapshots omit hop holes and are sampled at ≤10 Hz. Stdio proxy: `scripts/mcp-spectrum-proxy.py`.
- README status badges: Java 21, HackRF SDK v2026.01.3, min firmware, Linux|Windows, last commit.
- `FrequencyAxis`, `BandMark` layers, and a shared `BandHeaderPainter` so Wi-Fi / FM / Quick Select overlays share one MHz↔pixel map and header. `AnalyzerSettings` owns all `HackRFSettings` model values (radio vs display) so the analyzer frame no longer stores them.
- Spectrum plot Grafana-style frequency zoom: drag a span to zoom in (retunes the sweep), double-click or scroll down to zoom out, scroll up to zoom around the cursor. Start/end digits follow. Quick Select resets the zoom stack. Zoomed out past a single preset, Quick Select ranges are drawn as labeled vertical bands.
- FM overlay labels **live** sweep peaks as US station frequencies (e.g. **97.3**): local maxima ≥ 8 dB above the noise floor, snapped to the 47 CFR 73.201 200 kHz dial. A tracker raises confidence over ~0.4 s of repeated hits and holds the label ~1–2 s after the peak drops so IDs are readable. Empty channels are not marked.
- Hardware strip: **Restart** (re-open the sweep), **Stop** (release USB), radio serial picker, and **CLKOUT 10 MHz**. Pause still only freezes the plot.
- Spectrum plot draws occupied 20 MHz Wi-Fi bands (US ch 1–11 and 36–177). Quick Select **WiFi 2** is 2402–2472 (ch 1 start through ch 11 end; 2407 is ch 2’s start) and **WiFi 5** is 5170–5895. 2.4 GHz bands overlap; the axis is locked to the occupied envelope so channel 11 stays 20 MHz wide.
- `make stats` / `scripts/repo-stats.py` — regenerate [docs/stats.md](docs/stats.md) (first-party LOC, Java packages, tests, git, pins). `make mermaid` / `scripts/check-mermaid.sh` parse-checks every first-party Mermaid fence.
- `make info` / `make list-devices` — list attached HackRF USB devices, the libhackrf/USB API this app is pinned to, device firmware when openable, and whether a newer Great Scott Gadgets release exists
- `make firmware-update` — dry-run official GSG firmware flash; writes SPI only with `CONFIRM=1` (refuses Pro image on a One; not part of `build`/`test`)
- `make udev` — install persistent udev rules so WSL usbipd HackRF nodes stay writable after attach
- Comprehensive unit test suite (30 test classes) focused on core DSP logic
- `SpectrumSweepEngine` — analyzer sweep/processing path without Swing; hardware IT asserts the queue fills and `datasetSpectrum` updates
- First-class `docs/` documentation structure
- Root `README.md`, `AGENTS.md`, `CONTRIBUTING.md`
- Improved top-level `Makefile` with `make help`, `make test`, `make lint`, `make start`, categorized colored output
- Enhanced `src/hackrf-sweep/Makefile` with matching help and quality targets

### Fixed
- Waterfall stayed on a fixed −90…−25 palette after the spectrum Y-axis started auto-scaling, so a typical FM band (−85…−65 dBm) rendered as solid blue with no station streaks. Auto-scale now drives the waterfall colors from the same live window; turning auto-scale off restores the Chart-options sliders.

### Changed
- Auto-scale pad around the live noise/peak is **10 dB** (was 20) so a 15 dB FM contrast actually fills the plot.
- Java **8 → 21** (`--release 21`). FlatLaf 3.7.2 dark look-and-feel. JFreeChart 1.5.6, MigLayout 11.4.3, JNA 5.19.1, JUnit 5.13.4, JaCoCo 0.8.15. Launchers refuse older or headless JREs. `HackrfSweepLibrary` is hand-maintained (`make jnabridge` no longer runs JNAerator).
- Host libhackrf / SDK pin **v2024.02.1 → v2026.01.3** (USB API 1.16). Sweep-as-library patch rebased (`num_fft_bins`, `stdbool.h`). JNA ABI unchanged. `isKnownHackrfBoard` accepts HackRF Pro (board id 5). Min firmware remains 2024.02.1.
- Modernized build (Maven + cross-platform native)
- Brought in upstream improvements (Antenna LNA support, firmware v2024.02.1, min FFT bin fix, etc.)
- Preserved and integrated Quick Select feature from this fork

### Changed
- Operator-facing copy no longer brands the app as `hackrf_sweep`. README, getting-started, and usage talk about the spectrum analyzer; the window title is **Spectrum Analyzer**. The sidebar shows board, short serial, and firmware instead of “HackRF connected”.
- Quick Select ranges checked against FCC / ITU / 3GPP / ARRL Part 97. Wi-Fi 2 is 2400–2484 (not into Globalstar 2483.5–2495), Wi-Fi 5 is U-NII 5150–5895 (not 5030 MHz MLS), LTE-1/2 cover AWS+PCS and 600–900 MHz, U-TV is post-repack 470–608. Added US amateur **6m / 2m / 70cm / 33cm**. Hover a button for the citation.
- Moved RBW / FFT bins / fps / peak power off the waterfall HUD into a full-width status bar with readable labels (Resolution, FFT bins, Waterfall rate, Peak).

### Fixed
- Frequency zoom / Quick Select keep the last sweep on screen and debounce the radio apply (~120 ms) so a wheel flick is one USB restart, not one per tick. Chart series skip −150 dB hop holes and downsample to the plot width. Domain-axis updates run on the EDT.
- Spectrum **Auto-scale dB axis** is on by default (10 dB pad, edges locked to multiples of 10) so FM/Wi-Fi peaks are readable. Chart options still offers a fixed **−100…+20** window. Hop holes at −150 dB are ignored. Live follow holds through wobble and bursty peaks, expands only when a signal would clip, and shrinks at most one 10 dB tick every 3 s if that whole window stayed quiet.
- Narrow sweep windows (FM 88–108 is one 20 MHz hop) finished 400+ sweeps/s and flooded the waterfall plus Swing updates, so the plot looked frozen. Display work is capped at 30 fps; the radio still sweeps at full rate.
- Quick Select hover is an in-panel range line (`2402–2472 MHz`), not Swing/X11 tooltip windows. Moving to another button replaces the same line; unit tests dispatch enter/exit and assert a single hint.
- Wi-Fi 2 vertical bands were the 5 MHz numbering raster in a 2407–2467 window, so the left edge was channel 2’s occupied start (2417−10) and channel 1’s 20 MHz (2402–2422) was clipped. Overlay now draws occupied 20 MHz (ch 1 = 2402–2422, ch 11 = 2452–2472) and **WiFi 2** is 2402–2472.
- JVM SIGSEGV in `libawt.so` `BufImg_GetRasInfo` after long runs: persistent-display `setRGB` raced ChartPanel paint on an accelerated image. Draw on a heap buffer and publish a snapshot to the chart.
- Window would not shrink and the settings column was clipped at the bottom (pack/preferred height + no scroll). Frame is resizable, settings sit in a scroll pane, and the content pane has bottom padding.
- Quick Select applied start and end as two model updates, each restarting the native sweep (USB reset). The second start then retried in a tight loop and the spectrum froze. Presets now publish one range; {@code runSweepLoop} no longer auto-restarts {@code start()}.
- Finish remaining pavsa/hackrf-spectrum-analyzer v2024.11.10 ports: JFreeChart 1.5 renderer API (`setDefault*` instead of removed `setBase*`) and Settings UI null-safety for no-arg/designer construction
- `make test` is a real gate: Java 8-compatible tests, JaCoCo agent no longer dropped by Surefire, headless `GraphicsToolkit` fallback, null-safe allocation-table range queries, and Settings version `JLabel` (AWT `Label` threw in headless)
- Extracted `GainPolicy` and `RuntimePerformanceWatch` from the analyzer for unit testing; waterfall palette/x mapping is now static and tested
- Settings UI, Quick Select bands, and frequency-selector digit buttons covered without constructing the native analyzer
- Gated hardware integration tests (`make test-hw`, `@Tag("hardware")`, `*IT`): USB present, firmware/USB API/board via libhackrf, live sweep into the analyzer dataset path, start/stop/restart, antenna power + LNA, restart after FFT bin / frequency change. Skipped when no HackRF; not run by `make test`

See the [docs/](docs/) directory for current usage and development information.
