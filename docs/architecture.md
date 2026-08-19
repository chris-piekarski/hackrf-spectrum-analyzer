# Architecture

## High-Level Overview

```mermaid
flowchart TD
    subgraph JavaApp["Java Application (Swing)"]
        UI["UI Layer<br/>Waterfall, Charts, Settings, Quick Select"]
        Core["Core DSP Layer<br/>SpurFilter, PersistentDisplay,<br/>DatasetSpectrum*, EMA, Allocations"]
        MCP["MCP snapshot store"]
    end

    NativeBridge["Native Bridge (JNA)"]
    NativeLib["Native sweep library<br/>(libhackrf-sweep.so / .dll)"]
    HackRF["libhackrf + USB (libusb)"]
    Agent["Local MCP client"]

    UI --> Core
    MCP --> Core
    Agent --> MCP
    Core --> NativeBridge
    NativeBridge --> NativeLib
    NativeLib --> HackRF
```

## Key Components

### Core DSP (`jspectrumanalyzer/core/`)
- `DatasetSpectrum`, `DatasetSpectrumPeak`
- `SpectrumSweepEngine`, `SpurFilter`, `PersistentDisplay`
- `AnalyzerSettings` (all `HackRFSettings` model values; radio vs display)
- `FrequencyAxis`, `BandMark`, `WifiBandLayer`, `FmBandLayer` (plot overlays do not invent their own MHz↔pixel map)
- `EMA`, `FFTBins`, `PowerCalibration`, `RadioIdentity`
- Frequency allocation tables

These are the best candidates for unit testing (and have the majority of our test coverage).

### Native Integration
- `src-c/0001-hackrf_sweep-to-library-conversion-v2026.01.3.patch` — Turns the upstream `hackrf_sweep` tool into a reusable library.
- `HackRFSweepNativeBridge.java` + hand-maintained `HackrfSweepLibrary.java` (`make jnabridge` does not regenerate it)
- The build process resets the hackrf submodule to `HACKRF_SDK_PIN` (v2026.01.3) and applies the patch.

### UI Layer
- Swing + FlatLaf + JFreeChart.
- `WaterfallPlot`, `HackRFSweepSettingsUI`, Quick Select (`QuickSelectPreset`), `SweepStatusBar`, radio identity (board / serial / firmware). Spectrum overlays share `FrequencyAxis` + `BandHeaderPainter`: Wi-Fi (`WifiBandLayer`), live US FM (`FmBandLayer` + `FmStationTracker`), and zoomed-out Quick Select (`QuickSelectBandLayer`). Frequency zoom (`SpectrumZoom` + `SpectrumZoomHistory`) retunes the sweep like a Grafana time-range drag.

### Build System
- Root `Makefile` — convenience targets (`make help`, `make test`, `make start`, etc.).
- `src/hackrf-sweep/Makefile` — the real engine (cross-compiles natives + invokes Maven).
- Maven (`pom.xml`) — Java compilation, dependency management, fat JAR assembly.

## Design Goals

- Keep the performance-critical sweep loop in optimized native code.
- Make the Java side as "pure" as possible for the signal processing so it can be unit tested.
- Support both Linux native development and Windows end-users from a single build.

## Data Flow (Simplified)

```mermaid
sequenceDiagram
    participant Native as Native sweep library
    participant Bridge as JNA bridge
    participant Analyzer as Analyzer
    participant Engine as SpectrumSweepEngine
    participant DSP as Core DSP
    participant UI as Charts and waterfall
    participant MCP as MCP snapshot store

    Native->>Bridge: FFT power batches
    Bridge->>Analyzer: newSpectrumData
    Analyzer->>Engine: accept bins
    Engine->>DSP: filter peaks persist
    Engine->>UI: hooks update displays
    Engine->>MCP: snapshot store copy
```

## Testing Strategy

- Unit tests live under `src/test/java` and focus on `core/`.
- No hardware is required for the unit test suite.
- Graphics and time-dependent behavior use reflection to control internal state where necessary.

See [development.md](development.md) and [testing section in root README](https://github.com/chris-piekarski/hackrf-spectrum-analyzer) for more.

## Core DSP Class Diagram (Simplified)

```mermaid
classDiagram
    class DatasetSpectrum {
        +addNewData(FFTBins)
        +getSpectrumArray()
        +cloneMe()
    }
    class DatasetSpectrumPeak {
        +refreshPeakSpectrum()
        +calculateSpectrumPeakPower()
    }
    class SpurFilter {
        +filterDataset()
        +isFilterCalibrated()
        -calibrate()
    }
    class PersistentDisplay {
        +drawSpectrumFloat()
        +setImageSize()
    }
    class EMA {
        +calculate()
        +addNewValue()
    }
    class PowerCalibration {
        +correctPower()
    }
    class FrequencyAllocationTable {
        +lookupBand()
        +getFrequencyBands()
        +drawAllocationTable()
    }
    class FrequencyBand
    class FFTBins
    class SpectrumSweepEngine {
        +accept()
        +runProcessingLoop()
        +runSweepLoop()
    }
    class RadioIdentity {
        +statusHtml()
        +shortSerial()
    }

    DatasetSpectrum <|-- DatasetSpectrumPeak
    SpurFilter --> DatasetSpectrum
    PersistentDisplay --> DatasetSpectrum
    PersistentDisplay --> EMA
    PowerCalibration --> FFTBins
    FrequencyAllocationTable --> FrequencyBand
    SpectrumSweepEngine --> DatasetSpectrumPeak
    SpectrumSweepEngine --> SpurFilter
```

## Deployment Diagram

```mermaid
flowchart TD
    Dev["Linux build host<br/>Makefile, Maven, sweep-as-library patch"]
    Out["Build output<br/>fat JAR, libhackrf-sweep.so, hackrf-sweep.dll, launchers"]
    Linux["Linux user<br/>.sh launcher + JRE + radio"]
    Win["Windows user<br/>.cmd launcher + JRE + WinUSB"]
    Dev -->|make build| Out
    Out --> Linux
    Out --> Win
```

## Java Package Structure (Core Focus)

```mermaid
flowchart LR
    UI["jspectrumanalyzer.ui<br/>settings, waterfall, Quick Select"]
    Core["jspectrumanalyzer.core<br/>engine, DSP, RadioIdentity"]
    Bridge["jspectrumanalyzer.nativebridge<br/>JNA + device query"]
    Native["libhackrf-sweep"]
    UI --> Core
    UI --> Bridge
    Core --> Bridge
    Bridge --> Native
```
