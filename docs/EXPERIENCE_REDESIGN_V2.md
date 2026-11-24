# Experience Redesign V2 — الصلاة جامعة
Status: active development / not a release
Branch: redesign/experience-v2
Parent: creative/app-experience-v1 @ 7b3efb69f9b8889745e21e5b1acb64a21aabfa9f

## Product decision
Rebuild the experience around a single promise: help a person understand, remember, begin, and sustain worship and good conduct. "وعي" is the intellectual and educational heart; worship tools and daily-life utilities support it. Do not ship a collection of disconnected feature pages.

## Problems confirmed in the first prototype
- The home page and section doors do not teach the navigation model or explain what each destination is for.
- "وعي" is currently a list of path labels/modules and discovery links, not a lesson product. Search links are not editorial recommendations.
- Daily routine exposes reminders and preferences without a guided setup, explanation, sensible defaults, or a clear outcome.
- Several actions overlap with existing entry points and duplicate navigation.
- Qiyam has a reader/session and a separate tracker surface; session capture should be the source of truth and the tracker a summary.
- The current screens use repeated cards/chips and low-information controls. Visual hierarchy and action priority are inconsistent.
- Several features are only scaffolds while the UI can make them look complete.

## Experience architecture
Four primary destinations, always visible and stable:
1. **الرئيسية** — a calm daily briefing: next prayer and today's times; one relevant next action; today's verified reminder; resume last learning/reading activity; shortcuts only to the most-used destinations. Preserve Journey artwork as a signature navigation/brand moment, not as the only way to reach urgent information.
2. **وعي** — guided learning paths with a real lesson structure: question, source, source type/grade, explanation, present-day connection, optional practice/reflection, and references. Start with a curated, reviewed starter curriculum rather than empty topic labels or unreviewed search results.
3. **العبادة** — prayer, sunnah, Quran, qiyam, fasting and qada. Each has an introductory explanation and a primary task. Qiyam reader/session is the single capture flow; history is summary only. Qada starts with two clear choices: organize directly or make a gradual plan.
4. **الخير والناس** — kinship, personal remembrance, giving and daily-life reminders, each explained in plain language and explicitly user-controlled.

Settings are global and secondary; remove duplicate shortcuts when a destination already exists in primary navigation.

## First-run experience
- Welcome screen: what the app is, what it is not, and what is optional.
- Three-step optional orientation: (1) understand the four areas, (2) choose what matters now, (3) configure prayer location/notifications only when needed.
- Every step has Skip, Back, and "why we ask". Do not request location, notifications, Usage Access, or contacts merely to browse.
- First visit to each major area shows a compact "what this section does / how to use it / source and limits" intro with "ابدأ" and "تخطي الشرح"; replay from help.
- Add contextual empty states and first-action guidance. No dead-end blank pages.

## Daily home hierarchy
- One primary focus, one secondary action, and one reminder maximum.
- Prayer focus displays next prayer, time remaining and today's five times, with a clear tap target to open prayer.
- Prayer log language is neutral: "لم تسجلها بعد — هل صليتها؟" and correction/undo is easy.
- "تذكرة اليوم" distinguishes Quran, hadith, scholarly explanation, and app-authored reflection. Show reference adjacent to the text and one practical optional action.
- Resume card continues the last Quran/lesson/qiyam session; do not add duplicate "continue" buttons elsewhere.

## Visual system
- Arabic-first RTL with tested layout direction, generous readable line-height, dynamic font scaling and 48dp minimum touch targets.
- Establish design tokens for type scale, spacing, radii, elevation, surfaces, dividers and semantic colors.
- Use editorial composition: strong page title and short explanatory lead, one hero action, grouped content, clear section hierarchy. Avoid card-per-row, chip walls and decorative buttons.
- Interactive controls must have explicit labels, visible selected/disabled states and feedback. Icons never replace ambiguous labels.
- Use motion only to explain state or continuity; honor reduced motion and avoid animation that delays tasks.
- Test at small Android widths and large system font settings.

## Section definitions and user guidance
- **وعي:** explain each path, who/what it helps understand, lesson duration and learning mode. A lesson must contain substance, not only a video search query. Add progress as "lessons opened/completed" only, never a spiritual score.
- **روتين اليوم:** frame as optional personal reminders, not a religious obligation list. Explain each category, sample use, setup preview, snooze/pause/delete, and what permissions/data are used. Distinguish alarm exactness from approximate reminders.
- **قيام الليل:** explain its time window and voluntary nature; the last third is a virtuous time, not the only valid time. Guided flow: choose start point, read ayah-by-ayah, pause/resume, finish, optional rakat/witr/reflection. Session is source of truth; history summarizes.
- **القضاء:** two equally clear paths (direct organization / gradual plan), preserve existing values, no personal fatwa.
- **الخير والناس:** distinguish learning about giving from recording a personal action; app never collects donations or contacts people. Relationship reminders are user-scheduled, pausable and removable.
- **غزة:** curated headline links with source/date and separation of news/report/analysis; no endless feed or shocking imagery.

## Content integrity and safety
- Quran and hadith exact wording with references. Hadith grade/source shown; do not label search results as endorsed content.
- Separate transmitted text, scholarly explanation, historical report, contemporary analysis and product copy.
- No fatwa engine, religious ranking, streak punishment, guilt notifications or acceptance-of-deeds claims.
- No contacts, SMS, call logs or social content access by default.
- Before expanding personal lists, decide export/delete and Android Auto Backup treatment; do not silently alter existing user data or schema.

## Implementation order
1. Replace navigation and first-run guidance with the four-area architecture, preserving all existing destinations and preference keys.
2. Build the shared visual system and redesign home hierarchy.
3. Rebuild "وعي" as a guided learning experience with real starter lessons and explicit source metadata; keep Gaza as a separate curated feed inside it.
4. Rebuild daily routine setup and section onboarding; eliminate duplicate/unclear controls.
5. Consolidate Qiyam session capture and qada entry flow.
6. Redesign "الخير والناس" and privacy controls.
7. Add usability checks: navigation discoverability, task completion, empty/error states, RTL/swipe, large fonts, permission denial, offline behavior, data preservation.
8. Generate a debug APK for device testing. Do not merge or publish a release until owner acceptance.

## Acceptance criteria
- A first-time user can explain the four destinations and start a useful action without external instructions.
- Each visible button has a unique, understandable purpose; duplicate actions are removed.
- Every major section explains what it is, why it exists, how to use it, and its limits.
- No feature is presented as complete if it is only a scaffold.
- Main daily tasks are reachable in at most two deliberate taps.
- Existing user records and saved positions survive the redesign.
- Build/lint success plus a real-device usability pass are both required.
