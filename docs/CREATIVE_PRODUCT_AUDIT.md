# Creative Product Audit — الصلاة جامعة
Date: 2026-11-24
Branch: `creative/app-experience-v1`
Base: `test/home-journey-v2`
Status: First-pass source audit and redesign direction. No production release changes.

## Product direction
Build a calm, trustworthy worship companion: a clear daily entry point, a memorable journey identity, and focused destinations that never turn worship into a score or competition. Preserve the Journey doors and all existing features; improve hierarchy, consistency, accessibility, and data safety around them.

## What is already strong
- Distinctive Arabic identity, warm cream/green palette, and the Journey metaphor.
- Prayer times are calculated locally from coordinates; user records remain local by default.
- Quran reader validates the expected 114 surahs and 6,236 ayahs before accepting downloaded content, then caches it.
- Backup/restore is user-initiated and includes schema/package validation and a confirmation step.
- Religious content often displays source references and explicitly avoids judging sincerity or acceptance.
- Qiyam flow has a dedicated reader, session finish screen, verse-position tracking, and optional reflection.

## Main product and UX findings
1. **Home is memorable but not yet operational enough.** The Journey is a strong identity, but the first screen does not foreground the next prayer, countdown, today's prayer times, or the most useful immediate action. The user must open the prayer door to reach the daily task.
2. **Navigation has mixed mental models.** The home screen opens destinations directly, while a separate bottom bar exposes only a subset (home/start, statistics, settings). Section navigation and back-stack state are managed inside the very large `SalatiScreen`, making the system harder to reason about and test.
3. **MainActivity is a maintenance bottleneck.** The main Compose screen owns location-derived state, prayer/qada state, navigation, notification preferences, and rendering for many unrelated destinations. This raises regression risk when adding features.
4. **Visual language is not yet systematic.** Individual screens use repeated green/cream values and cards, but spacing, typography hierarchy, section headers, empty states, and action emphasis vary. Build a small shared design system before broad restyling.
5. **Qiyam has overlapping tracking surfaces.** The reader/session finish flow and the separate tracker both expose verse counts, rakats, witr, sessions, and history. Clarify one source of truth and make the reader/session the primary capture flow; keep the tracker as a summary, not a competing data-entry path.
6. **Quran availability is network-dependent on first load.** The reader caches a validated copy after download, but a fresh install still needs internet. Explain this clearly and give a helpful offline/error state.
7. **Privacy needs an explicit product decision.** Android Auto Backup is enabled in the manifest; exported JSON is plaintext and can include personal reflection data. Make the privacy choice visible and consider excluding sensitive records from system backup or offering encrypted export. Do not silently change existing user data or backup compatibility.
8. **Location onboarding is too early and broad.** Location permission is requested after the first-launch notice, although the user may want to explore first. Offer manual location / later permission and make the reason clear.
9. **Prayer-time confidence needs better disclosure.** The current calculation uses fixed solar-angle assumptions and labels times approximate. Add calculation method, offsets, and manual correction only after the behavior is designed and tested against a trusted local timetable.
10. **Accessibility and edge states need a dedicated pass.** Verify touch targets, screen reader labels, font scaling, long Arabic labels, loading/empty/error states, and RTL swipe behavior across all destinations.

## Proposed experience architecture
- **الرئيسية:** compact daily focus (next prayer, countdown, today's five times, one clear prayer action) above the Journey entry points. Keep the existing Journey art as the app's signature, but do not make users traverse it to find time-critical prayer information.
- **المتابعة:** prayer log, qada calculator/progress, statistics, and Qiyam summaries.
- **التعلّم:** Quran/mushaf/reflection, prophetic duas, Quranic duas, adhkar, prayer sunnahs, practical sunnahs, wudu and prayer fiqh.
- **الإعدادات:** location and calculation method, notifications, backup/restore, privacy, app information and sources.
Use four stable top-level destinations and contextual links; preserve all existing feature destinations and saved preference keys.

## Design principles
- Quiet hierarchy: one primary action per screen; secondary actions are visually quieter.
- Progressive disclosure: show the next useful choice, not every feature at once.
- No streak pressure, guilt language, ranking, or spiritual scoring.
- Keep Quran/hadith wording exact and place references beside the content.
- Distinguish transmitted evidence, scholarly explanation, and project-authored reflection.
- Preserve the Journey metaphor without forcing it to carry every navigation task.
- Arabic-first RTL, generous line-height, scalable type, and minimum 48dp interactive targets.

## Technical and data safeguards
- Keep package ID `com.salati.app`, current signing identity, SharedPreferences keys, and existing backup schema stable during UI work.
- No destructive preference migration or storage refactor in this visual iteration.
- Any future Room migration must be one-time, idempotent, verified against counts/samples, and reversible until accepted.
- Do not publish a release or change `main` during this development phase.
- CI build success is not a substitute for real-device acceptance testing.

## Work sequence
1. Establish a coherent daily home hierarchy while retaining the Journey.
2. Standardize reusable typography, surfaces, spacing, controls, and state messaging.
3. Simplify navigation into four stable areas without removing any current destination.
4. Consolidate Qiyam capture and summary behavior without losing stored records.
5. Review Quran reading/reflection and learning content for clarity and source display.
6. Review settings, location, notification permission timing, backup and privacy disclosures.
7. Run static/build checks, then device tests for RTL, font scaling, navigation, notification permissions, timezone/date rollover, backup/restore, and data preservation.
8. Only after owner acceptance, prepare a separately signed release candidate. Do not merge or publish before explicit approval.

## Audit limits
This is a source-based first-pass review of the development branch and selected app modules. It is not a complete test of every screen, hadith citation, device behavior, network request, or security property. Findings must be validated as implementation progresses.
