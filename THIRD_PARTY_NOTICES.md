# Third-party notices

## Original project code
Original application code in this repository is licensed under the MIT License; see [LICENSE](LICENSE). This license does not relicense third-party components, external content, or artwork.

## Android and application libraries
The app directly uses the following AndroidX / Jetpack libraries:
- AndroidX Activity Compose 1.10.1
- Jetpack Compose BOM 2025.02.00 (Compose UI, Material 3, and Material Icons Extended)
- AndroidX Core KTX 1.15.0
- AndroidX WorkManager KTX 2.10.0

AndroidX / Jetpack open-source components are generally distributed under the Apache License 2.0. Preserve their notices and review the actual resolved dependency graph for each release. License text: https://www.apache.org/licenses/LICENSE-2.0

The app also uses:
- Google Play services Location 21.3.0 — governed by the applicable Google Play services / Google SDK terms and any notices accompanying the SDK. Documentation: https://developers.google.com/android/guides/setup

## Quran text
Arabic Uthmani Quran text is fetched at runtime from AlQuran Cloud (Islamic Network), endpoint edition `quran-uthmani`. The app displays the text without intentional alteration and identifies AlQuran Cloud / Islamic Network as its source in the Quran reader.

AlQuran Cloud's current terms state that Quran text may be used, stored, and displayed, must remain exactly as provided (including spelling and diacritics), and should be credited when republished. The API is provided without warranty and subject to its rate limits and current terms:
- Terms: https://alquran.cloud/terms-and-conditions
- Credits: https://alquran.cloud/credits

This app fetches Quran text at runtime; it does not publish the raw API response as a separate downloadable dataset. Translations, tafsir, recitations, and other editions can have separate rights and are not covered by this notice.

## App artwork
The launcher artwork must be supplied by the project owner or used with permission. Record the creator/source and any applicable license here when the final release icon is approved.

## Release inventory
The Android release workflow should retain a resolved dependency report and verify all notices before publishing. This file is an inventory of known direct dependencies and content sources, not a substitute for reviewing transitive dependencies or the rights in separately supplied assets.
