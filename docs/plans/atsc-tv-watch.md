# ATSC 1.0 TV watch (via HackRF)

**Status:** in progress (Watch demodulates 8VSB → MPEG-TS → ffmpeg video/audio; hardware IT still open)  
**Started:** 2026-08-21

## Goal

The operator can **tune and watch US ATSC 1.0** the same way they listen to FM: pick a live **ch 14** tag (or the TV tuner), the radio leaves sweep mode, demodulates 8VSB to an MPEG-2 transport stream, and **shows video + plays AC-3** in this JVM.

OTA TV in 2026 is digital. Analog NTSC is not this plan.

## Hard constraint

One RF path. **Watching stops the sweep.** Pause still only freezes the plot. Stop still releases USB. FM listen and TV watch cannot run together.

## Checklist

- [x] `docs/plans/atsc-tv-watch.md` listed as active
- [x] `TvChannelPlan` + occupancy detect + unit tests
- [x] Overlay tags + header click Watch
- [x] TV tuner (Tune / Seek / Watch)
- [x] `RadioMode.WATCH` + MCP `tvChannel`
- [x] Watch parks IQ at 20 MS/s (reuse `hackrf_fm_lib_*`; analog BB filter 10 MHz)
- [x] HUD lock / no-lock
- [x] Native 8VSB → MPEG-TS (`atsc_rx_*` in `libhackrf-sweep`)
- [x] FFmpeg video in waterfall slot + AC-3 on `AudioSink`
- [x] Watch waterfall is a live IQ **VIDEO · ±10 MHz** raster (Listen-style), not a blank card
- [x] Watch uses the same waterfall strip as Listen (VIDEO · ±10 MHz); split unchanged
- [x] Docs: usage, architecture, mcp, CHANGELOG
- [x] 16:9 preview under the TV tuner Watch button (waterfall stays VIDEO · ±10 MHz)
- [ ] Hardware IT Watch start/stop then resume sweep
- [ ] Live ATSC lock: Reed-Solomon still fails on indoor 8-bit IQ (PAT never appears)

## Non-goals (v1)

ATSC 3.0, analog NTSC/PAL, cable QAM, GNU Radio runtime, MCP write `tv_watch`, EPG, recording, simultaneous spectrum+video.

## DSP

20 MS/s int8 IQ → RRC (GNU Radio `atsc_rx_filter`) → FPLL → DC block → AGC → 8VSB (GNU Radio `gr-dtv`, GPL-3, vendored C + libfec) → MPEG-TS → host FFmpeg MPEG-2 + AC-3. Decoded frames go in the TV-tuner preview; the waterfall is the parked IQ strip.
