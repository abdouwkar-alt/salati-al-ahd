# Privacy and data handling notes

## Reviewed implementation

- Prayer records, settings, and Quran reflection journal are stored locally in Android app preferences.
- Quran text may be retrieved from AlQuran Cloud using internet access.
- Location permission is used to calculate prayer times.
- User-initiated backup export creates a JSON file through Android's document picker.
- The app's JSON backup is not encrypted by the app and may include personal prayer/journal data.
- The reviewed Android manifest enables Android backup.

## User-facing disclosure

The app should clearly tell users:
1. Which information stays on the device.
2. That Quran content retrieval requires internet access.
3. Why location permission is requested.
4. That backup JSON is unencrypted and should be stored/shared carefully.
5. How to export and restore data, and what restore overwrites.
6. Whether Android system backup/device transfer includes app data.

## Required follow-up

- Inspect the full dependency graph and runtime network requests.
- Confirm whether any third-party SDK collects diagnostics/telemetry.
- Decide whether to exclude prayer logs and journal entries from Android Auto Backup/device transfer.
- Consider an optional encrypted backup format.
- Keep this document synchronized with actual app behavior. This is not a legal privacy policy and does not replace a jurisdiction-specific review.
