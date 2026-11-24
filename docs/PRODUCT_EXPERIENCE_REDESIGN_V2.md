# Product Experience Redesign V2 — الصلاة جامعة

Status: Active redesign branch. This document is the product and UX acceptance contract for the next development prototype.
Branch: redesign/product-experience-v2
Base: creative/app-experience-v1
Production/main and the published release must remain untouched until explicit owner approval.

## 1. Product truth

الصلاة جامعة is a guided Islamic life companion. "وعي" is the intellectual and educational heart. Worship tools, daily planning, reminders, relationships and personal tracking support that heart.

Product promise: افهم دينك، وافهم واقعك، وابنِ خُلُقك، واجعل العبادة حاضرة في يومك.

The app must help a person understand what a section is, why it matters, what they can do in it, and how to continue. A screen is not complete merely because it exists or has buttons.

Never use spiritual scores, public leaderboards, guilt mechanics, forced streaks, or claims about acceptance of worship. An unrecorded action is unknown, not proof it did not happen.

## 2. Problems confirmed in the current prototype

- The daily routine implementation is primarily a reminder CRUD screen, not an explained daily-life experience.
- Awareness currently exposes path labels and module placeholders but not a full lesson-learning loop with evidence, explanation, real-world connection, comprehension check and optional action.
- The home combines multiple entry mechanisms and duplicate shortcuts; the user has to discover the information architecture.
- Navigation lacks a clear first-use orientation and section-level onboarding.
- Existing feature additions were layered onto the old screen structure rather than being reconciled into one coherent product.
- Qiyam has overlapping session and tracking surfaces; the session must become the primary capture path and statistics a secondary summary.
- Product documentation records these gaps; the prototype must now satisfy them, not simply repeat the roadmap.

## 3. Experience architecture

Use four clear primary destinations with persistent labels and one consistent navigation model:

1. البداية — today's orientation, next prayer, one clear next action, a short daily reminder, and a compact visual route into the app.
2. وعي — central learning hub: continue learning, choose a learning path, lesson cards, saved material, and Gaza as a deliberate, separate news experience.
3. العبادة — prayer, sunnah, Qiyam, Quran, fasting, qada, adhkar and duas. Group by user intent, not by technical screen names.
4. الخير والناس — giving education, people in dua, kinship and useful good deeds.

Settings are a clearly labeled utility destination, not a competing content hub. Do not create duplicate entry buttons for the same destination. Each feature has one canonical home; contextual shortcuts may deep-link to it but must not look like duplicate features.

Keep all existing useful features reachable during migration. Preserve preference keys, package ID, signing identity and stored records. Do not silently migrate or delete data.

## 4. First launch and guided orientation

Build a skippable, replayable first-use tour. Do not request location or notification permissions before explaining value and obtaining contextual consent.

Tour sequence:
1. Welcome: what الصلاة جامعة is and what it is not.
2. The four destinations and what belongs in each.
3. البداية: next prayer and daily focus; how prayer logging works and that no log does not mean no prayer.
4. وعي: how a lesson is structured and how sources are labeled.
5. العبادة: how to find a worship tool and understand its evidence and practical steps.
6. الخير والناس: private reminders and personal lists; no contact surveillance or money collection.
7. Personalization: choose what to see and how often; all optional.
8. Permissions: request location only for prayer-time accuracy, notifications only when enabling a reminder, and usage access only if the user explicitly chooses screen-time support.

Each destination also gets a short contextual introduction the first time it opens, with a "كيف أستخدم هذا القسم؟" help action available later. The tour must be visual, concise, readable, RTL-correct and actionable; not a wall of text.

## 5. Screen design quality contract

Establish a small shared design system before redesigning screens:
- Arabic-first RTL layout, typography hierarchy and readable line height.
- Consistent spacing, surface elevation, corner radii, icons and illustration style.
- One dominant primary action per screen; secondary actions visibly quieter.
- Use meaningful visual metaphors and editorial composition rather than a stack of identical cards.
- Touch targets at least 48dp, accessible labels, scalable type and contrast.
- Clear loading, empty, error, offline, permission-denied and saved states.
- Every control must have a clear label and visible outcome. Remove controls that duplicate another control or do not produce a useful result.
- Avoid horizontal chip strips as the default navigation pattern; use legible section cards/list rows with a short explanation and a clear next step.
- No decorative interaction that adds complexity without helping the task.

## 6. "وعي" — the central learning product

The Awareness home must answer: What can I learn here? Where should I start? What did I last learn? What will I gain from this lesson?

Learning paths:
- الإيمان وشعب الإيمان
- الأخلاق النبوية والقرآنية
- السيرة النبوية
- الأنبياء والرسل في القرآن
- الصحابة والصحابيات
- فقه الواقع وأسئلة العصر
- غزة: الخبر والإنسان

Every real lesson follows this editorial structure:
1. سؤال واضح أو موقف واقعي.
2. الأصل: exact Quran, hadith, athar, historical source or contemporary source.
3. Source type, citation and hadith grade where applicable.
4. Explanation, visibly labeled as explanation and not revelation.
5. Connection to a contemporary situation.
6. One optional practical action or a comprehension question.
7. Save, note and resume controls.

The first prototype must include at least one complete, genuinely written sample lesson in each of: شعب الإيمان, الأخلاق, السيرة, and فقه الواقع. Do not fill the lesson library with empty module labels or generic YouTube search links. Video recommendations require editorial review and clear publisher/source metadata; search results are not endorsements.

Add a short "كيف نعرض المصادر؟" explainer. Distinguish Quran, Prophetic hadith, scholarly explanation, historical report and project-authored editorial content. Do not issue personal fatwas or make unsupported historical claims.

Gaza is not a generic news feed. It is a bounded, optional space that distinguishes news, humanitarian reporting, analysis and verification, with source/date and original link, no graphic imagery by default and no endless feed.

## 7. "روتين يومك" — redesign as a guided day, not a reminder manager

Do not make "routine" a confusing standalone form. Present a day as a flexible sequence of meaningful moments:
- الاستيقاظ
- الصلاة والذكر
- العمل أو الدراسة
- الطعام
- الأسرة والناس
- التعلم والقرآن
- التهيؤ للنوم

Each moment explains:
- لماذا تظهر هذه المحطة؟
- ماذا يمكنك أن تفعل؟ (optional actions)
- هل هو فرض، سنة ثابتة، مستحب، عادة مباحة، أم هدف شخصي؟
- ما الدليل إن نُسب فعل إلى الشرع؟
- ما الذي يحتاج إلى اختيار المستخدم أو ضبطه؟

The home view should show a small "محطتك التالية" and the whole day as a clear timeline/route. The user can add, reorder, hide or disable personal reminders. Do not imply every meal, work block or sleep is automatically worship. No mandatory tasks, no guilt, and no requirement to read a dua to silence an alarm.

Separate "إدارة المنبهات" from "فهم روتين اليوم". Alarm CRUD belongs in a simple focused editor with examples, repeat days, snooze, pause and delete. Do not mix its technical controls into the educational explanation.

## 8. Worship tools and duplicate-feature cleanup

- Prayer: clear daily prayer view, optional user log, correction/undo, neutral unrecorded state.
- Qiyam: one guided session flow is the primary capture path; show night window and final-third explanation; one-ayah reader with correct RTL swipe; pause/resume; exact saved position; finish screen with optional rakats/witr/reflection. History/statistics are summaries, not a competing tracker.
- Qada: two equally clear options: organize directly or choose a gradual plan. Explain the app is an organizer, not a personal fatwa.
- Quran: preserve validated Quran data and saved reading position; explain first download and offline state.
- Sunnah, adhkar and dua: group by user need and context, show source and classification, and avoid duplicate entry points.
- Fasting/challenges: private, opt-in planning with evidence, local Hijri dates, pause/skip/restart and no leaderboard.

## 9. "الخير والناس"

- Giving is educational and private; the app does not collect or route money.
- People in dua: user-entered labels and optional notes, private and deletable.
- Kinship: user-entered relationships and chosen reminder cadence; user confirms contact. No contacts/SMS/call log access by default.
- Every reminder can be paused, snoozed, muted or removed.
- Do not assume every family relationship is safe.

## 10. Privacy and trust

Before expanding personal lists or reminders, review Android Auto Backup, plaintext JSON export, local storage and deletion. Explain what remains on-device and what permissions expose. Do not silently alter backup compatibility or erase records. Usage access is optional and separate; never inspect message content or block other apps.

## 11. Build and acceptance sequence

Phase A — Product structure and design foundation:
- Map every existing screen, action, preference key and destination.
- Mark each as keep, merge, redesign, defer or remove, with reason.
- Implement shared design tokens/components and the four-destination navigation.
- Deliver the first-use tour and section explainers.

Phase B — Core experiences:
- Redesign البداية and daily routine.
- Build the Awareness learning journey and four complete sample lessons.
- Consolidate Qiyam.
- Reorganize worship and الخير والناس without duplicate controls.

Phase C — Validation:
- Run lint/build and inspect all navigation paths.
- Test real device: RTL, font scaling, back behavior, permissions, network failures, reboot, timezone/date changes, saved data, backup/restore and notification controls.
- Provide a guided test script that explains each screen and expected outcome.
- Do not publish or merge to main. Do not issue a production release without explicit approval.

## 12. Acceptance criteria

A new user can complete the tour and explain where to find each major task without searching.
A first-time user understands the daily routine before configuring reminders.
A user can complete a lesson and distinguish the source text from explanation.
Every primary action has one clear result and no duplicate button competes with it.
Every screen has a clear purpose, concise explanation and useful empty/error states.
No feature is presented as complete if it is only a placeholder.
The interface feels like one intentional product, not a collection of added pages.
