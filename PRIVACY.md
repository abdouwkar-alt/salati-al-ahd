# Privacy notice — الصلاة جامعة

Last reviewed: 23 November 2026

This notice describes the Android app build prepared in this repository. It should be reviewed again whenever app features, dependencies, or external services change.

## No user account or app-owned cloud database
The app does not require an account and does not upload prayer logs, qada progress, dhikr counters, or Quran reflection notes to an app-owned server. These records are stored locally in Android app preferences.

## Location
The app requests approximate and/or precise location permission to calculate local prayer times. Coordinates are saved in local app preferences for scheduling. The app uses Google Play services Location to obtain the device location. The app does not send the coordinates to a Salati-operated server.

## Network services
- Quran reader: requests the Arabic Uthmani Quran text from AlQuran Cloud / Islamic Network. The request is made by the device; the provider may receive normal network information such as IP address and request metadata. The text is cached locally after validation.
- Update checks: WorkManager checks public GitHub Releases metadata for a newer version. If one is available and Android notification permission is granted, the app posts a notification linking to the GitHub release page.
- Prayer calculation and records are processed on the device.

## Notifications
The app schedules prayer and worship reminders locally. Android notification permission is requested on supported Android versions. Users can disable notifications in Android system settings or in the app's available notification settings.

## Backups and data retention
The app provides a user-initiated JSON backup and restore function. Backup files are not encrypted by the app and may include prayer records, qada progress, counters, settings, and private reflection notes. Location coordinates are excluded from the exported JSON. Store backup files securely and do not share them unless intended.

Android's app data normally remains on the device during an in-place app update when the package ID and signing certificate remain the same. Uninstalling the app or clearing its storage removes local app data. Moving from an older debug-signed build to the first public release may require exporting a backup, uninstalling the debug build, installing the signed release, and restoring the backup because Android does not permit an update across different signing certificates.

Android device backup and transfer behavior is controlled by Android system settings and device configuration. Do not rely on it as the only backup.

## Permissions
The app may request:
- Internet access for Quran text and release metadata.
- Approximate/precise location for prayer-time calculations.
- Notifications for prayer reminders and release updates.
- Boot/time/time-zone events to restore scheduled reminders after device changes.

## Children's privacy and contact
The app is a general religious and personal-organization tool and does not intentionally collect personal information from children on a server. For privacy questions or data concerns, contact the repository owner through the GitHub repository.

## Changes
This notice may change when the app's behavior or dependencies change. The release notes should identify material changes.
