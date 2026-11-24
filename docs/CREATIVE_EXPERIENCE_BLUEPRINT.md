# Creative Experience Blueprint — الصلاة جامعة
Date: 2026-11-24
Branch: creative/app-experience-v1

## Product promise
A gentle Islamic reminder companion that helps a person remember, begin, and sustain acts of worship and care. It is not a religious scorecard, a fatwa engine, a charity processor, or a social surveillance app.

## Core navigation
1. **البداية** — the Journey identity, next prayer, today's focus, and a small daily reminder.
2. **العبادة** — obligatory prayer, sunnah, Qiyam, fasting, qada, adhkar, dua.
3. **الخير والناس** — giving, personal remembrance, kinship, friends, and useful good deeds.
4. **المعرفة والمتابعة** — Quran and reflection, learning, fiqh references, user-entered history and statistics.
Settings remain accessible from the existing bottom navigation/settings destination. Keep all existing screens reachable during migration.

## Daily reminder engine
A single "تذكرة اليوم" record chooses from verified content:
- Quran ayah with surah/ayah reference.
- Hadith with exact wording, collection, number, and grading/source.
- Short explanation clearly labeled as explanation, not prophetic wording.
- Practical action suggestion.
Avoid repeating the same reminder too often. Let the user choose categories, quiet hours, and frequency. Never imply a missed app log means the act was missed. If the user has not logged a prayer, use neutral copy: "لم تسجلها بعد — هل صليتها؟" not an accusation.
Do not send guilt-inducing alerts or fabricate religious urgency.

## Qiyam — guided worship tool
### Entry and timing
- Show the night window from Maghrib to Fajr, calculated using the app's local prayer-time settings.
- Show approximate start of the final third and explain the calculation: divide the interval from Maghrib to Fajr into three equal parts.
- State that Qiyam al-Layl is voluntary and its time extends through the night; the final third is a specially virtuous time, not the only valid time.
- Offer "ابدأ الآن" at any time during the night and an optional reminder before a user-selected target time.
- Never wake the user without explicit opt-in.

### Session flow
1. Intention prompt without forcing the user to declare or store intention.
2. Choose start location in Quran: surah and ayah.
3. Mushaf reader displays one ayah at a time, with correct RTL swipe direction, optional meaning, and discreet controls.
4. Session controls: previous/next ayah, pause, resume, mark ruku/finish, and end session.
5. Persist exact reading position and unique ayah keys; reopening resumes the session.
6. Finish screen offers optional rakats, witr, and private reflection; save or discard.
7. History summarizes what the user entered, not what Allah accepted. Never label a person as "قانت" or "من المقنطرين"; at most show the hadith wording as a source-linked learning note with a clear caveat.

### Qiyam reminders and learning
- Core reminder: "أفضل الصلاة بعد الفريضة صلاة الليل" (Sahih Muslim 1163).
- Final-third hadith: Sahih al-Bukhari 1145 and Sahih Muslim 758.
- Explain that two-by-two is the reported pattern for night prayer (Bukhari 990; Muslim 749) and that witr closes the night prayer (Bukhari 998; Muslim 751).
- Include concise, sourced learning cards for timing, witr, recitation, dua, and making the practice sustainable.
- No mandatory target of 10/100/1000 ayahs. Those narrations can be displayed as sourced learning, never as a personal reward guarantee.

## Prayer and qada
- Daily prayer log stays optional and supports correction/undo.
- When a user opens qada, present two equal, non-judgmental paths:
  A. "ابدأ تنظيم القضاء مباشرة" — user enters their chosen estimate and logs.
  B. "أريد خطة تدريجية" — choose a realistic pace and reminder frequency.
- Explain that qada rulings depend on the circumstances and scholarly school; the app does not determine the user's obligation or issue a personal fatwa.
- Challenge mode is a planning aid, not a religious ruling and not a public competition.
- Preserve all existing qada values and records. Do not recalculate silently.

## Challenges and seasonal planner
Use private, opt-in, user-defined plans with start/end dates and gentle reminders. Examples:
- المحافظة على تكبيرة الإحرام (personal goal, not a public leaderboard).
- Monday/Thursday fasting.
- Three days each lunar month (white days).
- Six days of Shawwal.
- Voluntary fasting in Sha'ban.
- Preparation for Ramadan.
Every challenge card must show: evidence/source, what the practice means, dates based on the user's local Hijri calendar, a realistic goal, reminder setting, log action, pause/skip, and a no-shame restart. Verify hadith wording and grading before publishing a card. Avoid treating a weak report as established Sunnah.

## Giving and remembrance
- The app does not accept, route, or solicit money in the user's name.
- Provide learning on charitable channels and their relative purposes, with no unverified charity directory.
- Personal remembrance list: names/labels, optional note, optional preferred reminder cadence, and user-controlled delete/export.
- A deceased loved one can be listed for a dua reminder. Do not imply the app performs charity or recitation on the user's behalf.
- If the user wants to log a charity they personally made, allow a private optional record; do not imply that an app log validates the act.
- Reminders for giving should be educational or user-scheduled, not pressure to donate.

## Kinship and relationships
- Private list of relatives/friends with relation label, optional note, last-contact date entered by the user, and desired cadence.
- Reminders are generated from user-selected cadence, not inferred surveillance.
- No call-log, SMS, contacts, or social-account access by default.
- A "تم التواصل" action records only the user's confirmation. Allow snooze, pause, and remove.
- Avoid assuming every family relationship is safe or appropriate; provide mute/pause/remove controls.

## Statistics
- Show only user-recorded activity.
- Separate obligatory prayer, voluntary prayer, fasting, qada, Quran reading, adhkar, kinship and giving.
- Trends are descriptive, not moral grades. No ranking, streak shame, badges implying spiritual rank, or reward predictions.
- Clearly label unknown/unrecorded days.

## Sources and content governance
- Each Quran quotation includes surah and ayah.
- Each hadith includes exact wording, narrator when useful, source collection, number, and grade or trusted source link.
- Clearly separate: Quran, Prophetic hadith, scholarly explanation, and project-authored copy.
- If grading differs, show the disagreement or omit the item until reviewed.
- Review religious content with qualified scholarship before release.

## Implementation sequence
1. Keep current branch and preserve user data; test existing changes.
2. Finish and test Qiyam guided session, then merge the separate sourced Quran-meaning/RTL fixes into this development branch after conflict review.
3. Replace qada gate with the two-choice onboarding and add opt-in challenge planner.
4. Finish remembrance/kinship data model, reminder cadence, and notification controls.
5. Expand verified daily reminder library and seasonal calendar.
6. Add export/delete controls for personal records and decide whether sensitive records are excluded from Android Auto Backup.
7. Run lint/build, unit tests, device tests, Arabic RTL/font scaling, reboot/timezone/day-rollover, notification permission, backup/restore, and data-preservation checks.

## Current implementation status
- Implemented in this branch: home next-prayer focus; personal remembrance list; kinship/friends list; educational giving categories.
- Not yet implemented: scheduled notifications for these lists, contact history cadence, challenge planner, Qiyam redesign, two-choice qada onboarding, daily reminder selection engine, privacy/backup redesign.
- Current personal lists use local SharedPreferences string sets. They are device-local, not encrypted, and do not yet support export/delete per-entry beyond the visible remove action. Treat as an early prototype, not a final privacy design.
- No release has been made. No production branch is changed.
