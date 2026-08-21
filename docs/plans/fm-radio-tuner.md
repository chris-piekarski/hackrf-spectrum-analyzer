# FM radio tuner (listen via HackRF)

**Status:** in progress (unit tests green; native `.so` has `hackrf_fm_lib_*`; live listen needs relaunch)  
**Started:** 2026-08-20

## Goal

The operator can **hear broadcast FM** through the attached HackRF One: pick a live **97.3** tag, the radio leaves sweep mode, demodulates mono WFM, and plays audio on the host.

## Hard constraint

One RF path. **Listening stops the sweep.** Pause still only freezes the plot. Stop still releases USB.

## Checklist

- [x] `docs/plans/fm-radio-tuner.md` listed as active
- [x] `WfmDemodulator` + unit tests (synthetic IQ, offset LO, 1 kHz tone)
- [x] `AudioSink` + Java Sound (tests use a fake sink)
- [x] `src-c/hackrf_fm.c` + JNA + Makefile `SOURCES`
- [x] `RadioMode` on `AnalyzerSettings` + launcher sequencing
- [x] Hardware strip Listen + frequency + volume; header-tag click; HUD
- [x] MCP `sweep_config` `radioMode` / `listenMHz` (read-only; no `fm_listen`)
- [x] Docs: usage, architecture, mcp, hackrf-setup, CHANGELOG, AGENTS
- [x] `make test` 287/287
- [ ] Hardware IT listen start/stop then resume sweep (needs exclusive USB)
- [x] Live listen on FM Quick Select with speakers (relaunch the GUI)
- [x] Analog knob jumps detected stations; spectrum highlights the tuned station

## Non-goals (v1)

Stereo, RDS, NBFM/AM/SSB, simultaneous spectrum+audio, GNU Radio, MCP write tools, WAV.

## DSP

4 MS/s int8 IQ → mix +100 kHz offset → decimate ×10 → polar discriminator → 75 µs de-emphasis → 48 kHz PCM.
