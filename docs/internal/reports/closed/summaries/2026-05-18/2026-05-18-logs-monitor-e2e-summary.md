# Logs Monitor E2E Summary

Date: 2026-05-18
Branch: `release/0.2.0`
Device: `emulator-5554` / Pixel 10 Pro XL / Android 16

## Scope

End-to-end check and hardening for the new Logs Monitor screen, plus a quick visual smoke across the main app screens. No commit or push was performed.

## Findings

- The first live monitor implementation pulled a large existing logcat backlog and wrote an unbounded persisted JSONL session. A short manual run produced `120394` persisted lines. This was bogus for a screen described as bounded and efficient.
- The Logs Monitor UI worked but the primary actions were icon-only, level chips did not visibly represent selection, and source/level labels were raw enum names.
- The first UI polish pass still clipped the right-edge filter chips on the initial viewport. That was random UI debt for a debug tool, so the chips were changed to wrap.
- Stop could report `ERROR` after an intentional process destroy because the service treated a cancelled logcat reader as a capture failure.
- Search only matched `rawLine`, which made parsed/redacted rows harder to find by displayed fields such as spoof type, tag, source, or level.
- Main app smoke did not expose obvious title alignment, major spacing, or text visibility regressions on Home, Groups, Group Detail, Apps tab, Settings, or Logs Monitor.

## Changes Made

- Live capture now starts from the current device logcat timestamp instead of replaying old buffers.
- The persisted live monitor JSONL file now trims back to the newest in-memory rows instead of growing without bound.
- Logs Monitor controls now show text labels for Start/Stop and Clear, plus explicit Source and Minimum level filter headers.
- Source and level chips now use user-facing labels such as `Xposed`, `LSPosed`, `Warn`, and `Fatal`.
- Minimum level chips now use selectable filter chips, so the active level is visible.
- Filter chips now wrap in the available width so all chip text is visible without edge clipping.
- Search now matches raw line, displayed message, tag, source, and level.
- Intentional Stop no longer overwrites `IDLE` with `ERROR` when the root logcat process is destroyed.

## Verification

1. Branch created -> verify: `git status --short --branch` showed `release/0.2.0`.
2. Focused pre-change tests -> verify: existing `LogMonitorStoreTest` and `LogsMonitorViewModelTest` passed before edits.
3. Root cause reproduced -> verify: Mobile MCP start capture showed old logcat backlog and `run-as` reported `120394` persisted monitor lines.
4. Regression tests -> verify: added tests for bounded persisted rows, live logcat command behavior, and parsed-field search; the parsed-field search test failed before the predicate fix.
5. Static/build gate -> verify: final `spotlessApply spotlessCheck detekt :app:testDebugUnitTest lint :app:assembleDebug --no-daemon` passed.
6. Mobile MCP logs monitor pass -> verify: Start changed status to `RUNNING`; Stop changed status to `IDLE`; Source filter narrowed rows to `Debug Xposed LSPosedFramework`; search for `ActivityManager` narrowed rows; Clear deleted the session file.
7. Persisted file check -> verify: after the fixed live capture, one target run produced `1070` JSONL rows including real `com.mantle.verify` Xposed spoof events; after reboot/final stop test, a fresh short run produced `432` rows and no stop-failure row; after Clear, `files/logs/monitor/` was empty.
8. Visual smoke -> verify: screenshots saved under `logs/device/2026-05-18-logs-monitor-ui/`.
9. Target evidence -> verify: saved JSONL contained `source:"XPOSED"` rows for `com.mantle.verify` spoof events including `IMEI`, `SERIAL`, `BLUETOOTH_MAC`, `WIFI_MAC`, `WIFI_SSID`, `ADVERTISING_ID`, `MEDIA_DRM_ID`, `SIM_OPERATOR_NAME`, and `IMSI`.
10. Export Logs check -> verify: a real in-app share export produced `logs/device/2026-05-18-logs-monitor-ui/devicemasker_support_export_check.zip` with 126 entries, `1227` parseable app JSONL lines, `1316` parseable Xposed JSONL lines, `diagnostics/hook_health.json` sourced from `parsed_xposed_export`, 11 LSPosed artifact entries, and 10 root command manifest lines.

## Evidence Files

- `logs/device/2026-05-18-logs-monitor-ui/home.png`
- `logs/device/2026-05-18-logs-monitor-ui/groups.png`
- `logs/device/2026-05-18-logs-monitor-ui/group-apps.png`
- `logs/device/2026-05-18-logs-monitor-ui/logs-monitor.png`

## Limits

- This is emulator evidence only, not physical-device evidence.
- This validates Logs Monitor capture/UI behavior and captured LSPosed/logcat spoof-event lines, not target-app spoof value correctness.
- Full hook success still requires target-app value evidence in addition to LSPosed/logcat hook lines.
