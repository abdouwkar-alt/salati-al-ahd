# الصَّلَاةُ جَامِعَةٌ

**An Arabic-first prayer companion for Android.**

The official application name is **الصَّلَاةُ جَامِعَةٌ**. The repository name, `salati-al-ahd`, refers to the project's editorial theme: **العهد**.

## About the name

«الصلاة جامعة» is the established call used to gather people for prayer. The app's name is inspired by this expression and is not a claim that the app itself is a religious authority.

## The editorial meaning of «العهد»

The app uses «العهد» as a **project-specific devotional reflection**: a reminder of the servant's commitment to Allah, expressed through preserving prayer and following the guidance of the Prophet ﷺ.

This is our own editorial framing. It is **not presented as a quotation, formal interpretation, or explanation attributed to a hadith commentator, Qur'an exegete, or scholar**.

The hadith:

> «العهد الذي بيننا وبينهم الصلاة، فمن تركها فقد كفر»

is cited as a serious warning about the place of prayer. The app's use of the word «العهد» as a wider theme is a separate editorial reflection, not an assertion that the hadith literally defines a personal contract in the exact wording used by this project. The legal and theological implications of abandoning prayer are discussed differently by scholars; the app does not issue takfir or judge individuals.

## Current scope

- Prayer times and prayer reminders.
- Prayer status tracking and statistics.
- Qada (missed-prayer) tracking and estimation tools.
- Adhkar, duas, Sunnah and introductory fiqh learning material.
- Quran reflection journal.
- Local data backup and restore.

Feature availability and behavior may change between releases. See the documentation in `docs/`.

## Religious-content and accuracy notice

This application is an educational and personal-organization aid. Its religious material, calculations, reminders, and explanatory text are provided as a human effort and may contain omissions or errors.

- The application is **not a mufti, fatwa service, or substitute for qualified scholarship**.
- Content and calculations are an effort and may be mistaken; users should verify religious rulings and consequential matters with qualified scholars and trusted references.
- Differences between recognized schools of jurisprudence may exist. A displayed summary is not a complete treatment of every case.
- Prayer-time results depend on location, calculation method, device settings, and data availability. Users should compare them with a trusted local authority.
- Qada estimates are estimates based on the inputs and assumptions selected by the user; they are not a definitive religious ruling.
- Nothing in the app should be used to declare a particular person a disbeliever or to judge an individual's standing before Allah.

The developers and contributors are not responsible for decisions made solely on the basis of app output. To the extent permitted by applicable law, the app is provided "as is", without a guarantee that all content, calculations, or notifications will be error-free or uninterrupted. This notice does not remove any rights that cannot legally be excluded.

## Privacy and data

The Android app stores prayer records and journal data locally using app preferences. Quran text may be retrieved from AlQuran Cloud over the internet. Location permission is used for prayer-time calculation. Backup files are exported by the user and are not encrypted by the app; they may contain private records and should be stored carefully.

Do not share a backup file unless you intend to disclose its contents. Review Android backup and device-transfer behavior before relying on a backup as your only copy. See the privacy and release notes in `docs/`.

## Build

This repository contains the Android project. Open it with a compatible version of Android Studio and allow Gradle to sync. Build/debug instructions and the release signing process will be documented before a public release.

A debug APK is intended for testing, not as the final signed public release.

## Security

Do not install APKs from unofficial mirrors. For each release, use the APK attached to the corresponding GitHub Release, inspect the release notes, and verify its SHA-256 checksum. A checksum confirms file integrity relative to the published value; it does not by itself prove that an app is safe or malware-free.

Report security issues privately to the repository owner rather than publishing exploit details in an issue.

## License

The project source code is licensed under the MIT License. This license applies to original project code only; third-party libraries, Quran text/data, API services, and other assets remain subject to their own terms and licenses. See [LICENSE](LICENSE).
