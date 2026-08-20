# Plans

Living implementation plans live here so later work (and agents) can pick them up without relying on chat history.

## Convention

- One Markdown file per plan: `docs/plans/<short-name>.md`.
- Keep a **Status** block at the top (`in progress` / `done` / `cancelled`) and a **checklist** that matches reality. Update both in the same change that implements or abandons a step. Do not leave checkboxes stale.
- When a plan finishes, leave the file in place and set Status to `done` with the date and measured outcome. Do not delete history.
- Link new plans from this index.

## Active

| Plan | Notes |
|---|---|
| [FM radio tuner](fm-radio-tuner.md) | In progress 2026-08-20. Listen mode stops the sweep; mono WFM via HackRF IQ. |

## Done

| Plan | Outcome |
|---|---|
| [Unit test coverage](unit-test-coverage.md) | Done 2026-08-17. `make test` 104/104. Project **56.1%** lines, `core` **90.2%**. |
| [Hardware integration tests](hardware-integration-tests.md) | Done 2026-08-17. Gated `make test-hw` (7 ITs, including `SpectrumSweepEngine` queue + dataset). `make test` stays radio-free. |
| [Java 21 + FlatLaf UI](java-21-ui.md) | Done 2026-08-18. Java 21 floor, FlatDarkLaf, library bumps. `make test` 119/119, `make test-hw` 7/7. |
