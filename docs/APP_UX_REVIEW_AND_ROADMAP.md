# UI/UX review and release improvements

Date: 2026-11-23

## Current observations

The main screen currently exposes ten destinations as horizontally scrolling FilterChips: prayer, statistics, adhkar, duas, qada, practical Sunnah, wudu fiqh, prayer fiqh, Quran reflection, and settings.

This is functional but creates navigation friction:
- Ten destinations are difficult to scan and remember.
- A horizontal chip row hides destinations off-screen and makes discovery depend on swiping.
- Primary daily actions compete visually with reference/learning sections.
- The main activity is a large composable with many responsibilities; changing it broadly just before release increases regression risk.

## Recommended information architecture

Do not remove any feature. Group the existing destinations into four persistent primary areas:

1. **الرئيسية** — next prayer, today's prayer times, quick prayer recording, next reminder.
2. **المتابعة** — prayer log, qada, statistics.
3. **التعلّم** — adhkar, duas, practical Sunnah, wudu fiqh, prayer fiqh, Quran reflection.
4. **الإعدادات** — location, calculation preferences, notifications, backup/restore, about and legal notices.

Use a bottom navigation bar for the four primary areas and a clear section list/grid inside «التعلّم». Keep the selected destination and back behavior predictable. This is a proposed next UI iteration, not a claim that it has already been implemented.

## Main screen hierarchy

Recommended order:
1. Compact greeting/date and location status.
2. Prominent next-prayer card with readable countdown.
3. Today's prayer times in a compact, consistently aligned row/grid.
4. One primary action: record the current/most recent prayer.
5. Small progress summary and shortcuts to qada/statistics.

Avoid placing all learning links on the home screen. Make the main action visually dominant, but do not use guilt-inducing colors or streak mechanics that turn worship into a competition.

## Interaction and accessibility

- Give every icon-only action a content description and a sufficiently large touch target (prefer 48dp).
- Ensure Arabic RTL alignment, readable Arabic line-height, and no clipped diacritics at large font/display sizes.
- Test small screens and large font settings; long Arabic labels must wrap or scroll without hiding controls.
- Use clear confirmation for destructive actions: reset data, restore backup, delete journal entries.
- For restore, show exactly what will be replaced and require explicit confirmation.
- Make notification permission requests contextual and explain why they are needed.
- Provide visible empty states and actionable error states for missing location/network/permission.
- Ensure disabled/loading buttons communicate their state and cannot trigger duplicate actions.
- Add accessibility semantics for prayer status and statistics, not color alone.

## Content presentation

- Separate sourced religious text from the app team's own explanation.
- Show source references next to hadith and Quran material, not only in a general sources page.
- Label summaries as summaries; do not imply that a brief display covers every madhhab or case.
- Keep «رسالة العهد» explicitly identified as the project's own editorial reflection, not a scholarly commentary.
- Never use the app's prayer tracking data to infer a user's piety or judge them.

## Release-blocking verification

Before public release:
- Build and install a release-signed APK on at least two real Android devices and Android versions.
- Test location denied/granted, location unavailable, timezone/date rollover, daylight-saving changes where applicable, reboot, battery optimization, and notification permission denied/granted.
- Test all prayer status transitions and qada arithmetic, including changing a previously recorded status and restoring a backup.
- Test backup export/import with non-ASCII Arabic text and malformed/old-version JSON.
- Review Android Auto Backup/device transfer behavior because local prayer logs and Quran journal entries can be personal.
- Verify the privacy notice against actual runtime network traffic and the full dependency tree.
- Verify all displayed hadith/fiqh citations and Quran text against trusted references.
- Verify release APK signature and publish SHA-256 checksum.

## Known technical/privacy risks to resolve

- App backup export is JSON and not encrypted by the app; warn users and consider encrypted export in a later version.
- The app requests internet access for Quran content retrieval; do not describe it as fully offline.
- Android system backup is enabled in the reviewed manifest. Decide explicitly whether personal records should be included in system backup/device transfer and configure backup rules accordingly.
- No malware-free guarantee can be made from source review alone. Publish source, signed artifact, checksum, permissions, and build provenance so users can inspect the release.
