# Plan: Make the test harness trustworthy, then raise project coverage

| Field | Value |
|---|---|
| **Status** | Done |
| **Started** | 2026-08-17 |
| **Last updated** | 2026-08-17 |
| **Outcome** | `make test` 104/104. Project line coverage **56.1%** (1241 / 2214). `core` **90.2%**. |

## Goal

Before any large-scale feature or refactor work, the suite must actually run (`make test` green) and give a real coverage signal. Then raise **project-level line coverage to about 50%**, and keep **`jspectrumanalyzer.core` at ≥80%**.

Met. Do **not** chase 80% of the whole app by constructing `HackRFSweepSpectrumAnalyzer` (JNA + maximized `JFrame`).

## Current measured state

| Metric | Before Phase 0 | After Phase 0 | After Phases 2–3 |
|---|---|---|---|
| Test classes / `@Test` methods | 25 / 92 | 25 / 92 | **28 / 104** |
| `make test` | Failed to compile | 92 pass | **104 pass** |
| Project line coverage | 26.8% | 45.5% | **56.1%** |
| `core` | 45.1% | 86.9% | **90.2%** |
| `ui` | 38.6% | — | **74.7%** |
| Analyzer package | ~0% | ~0% | still ~0% (by design) |

## Rules (still apply)

- Java 8 tests only (`var` forbidden). No Mockito. Synthetic `DatasetSpectrum` / `FFTBins`. Reflection only for private time/graphics state.
- Do not construct `HackRFSweepSpectrumAnalyzer` in unit tests.
- Do not invent production APIs to match broken tests.
- Small testability extracts are OK (`GainPolicy`, `RuntimePerformanceWatch`, waterfall math).

## Checklist

### Phase 0 — Make `make test` a real gate — done

- [x] Surefire `@{argLine}` so JaCoCo instruments
- [x] Four test files compile on Java 8 / current APIs
- [x] Peak-fall assertion uses a large enough `dt`
- [x] `GraphicsToolkit` headless fallback
- [x] `getFrequencyBands` null-safe
- [x] Settings version `JLabel`
- [x] `make test` green + JaCoCo HTML

### Phase 1 — Finish core DSP — done

- [x] `FrequencyAllocations` / table draw / PersistentDisplay / DatasetSpectrum Hz ingest
- [x] `ModelValue*` real API
- [x] `DatasetSpectrumPeak` `createPeaksDataset` / `fillPeaksToXYSeries` / `setPeakFalloutMillis`

### Phase 2 — Settings / MVC / selectors — done

- [x] `MVCController` `JComboBox` binder
- [x] `FakeHackRFSettings` test double
- [x] `HackRFSweepSettingsUI(HackRFSettings)`: FFT bin string, pause label, peak-fall / decay visibility, hardware-status text
- [x] Quick Select `doClick()` for every band + start/end veto nudge
- [x] Frequency selector `+`/`-` digit buttons and min/max clamp

### Phase 3 — Testable slices of the God class — done

- [x] `GainPolicy` extracted from `recalculateGains` + table-driven tests
- [x] `RuntimePerformanceWatch` moved to `core` + tests
- [x] Waterfall `normalizePower` / `clampPixelX` / `translateXToFrequency` + tests (no `paintComponent` pixels)

### Later (not this plan)

- [ ] `processingThread` → `SpectrumSweepProcessor` (dedicated refactor)
- [ ] Native / JNA unit tests
- [ ] Analyzer ctor / `sweep` / `setupChart`
- [ ] `ScreenCapture.captureFrame` (`System.exit(0)` on finish)

## What this does *not* buy

End-to-end still needs `make start` with a real HackRF. Native C is untested. The Java DSP, settings, and Quick Select logic is now pinned.
