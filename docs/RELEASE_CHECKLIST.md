# Public Android release checklist

## Release identity
- Repository: `abdouwkar-alt/salati-al-ahd`
- Package ID: `com.salati.app` — keep stable for all updates.
- Current prepared version: `1.3.0`, versionCode `4`.
- Public downloads are published as GitHub Release assets, not short-lived Actions artifacts.

## Signing and data continuity
- [ ] Create one long-lived Android release keystore.
- [ ] Store it securely offline in at least two protected locations.
- [ ] Configure GitHub Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- [ ] Never commit the keystore or passwords.
- [ ] Keep the same release keystore and alias for every future update. Android accepts an in-place update only when the package ID and signing certificate match.
- [ ] Existing debug builds may have a different signing certificate. Before moving from a debug APK to the first release APK, export the in-app JSON backup, install the signed release, then restore the backup. Do not uninstall before exporting.
- [ ] Test an in-place update from one signed release to the next and verify prayer records, qada progress, settings, and journal entries remain.

## CI and release
- [ ] Android CI passes on the release-preparation branch and pull request.
- [ ] Android lint and assembleDebug pass.
- [ ] Release workflow builds `assembleRelease`, verifies the APK signature, creates a SHA-256 checksum, and publishes the APK to GitHub Releases.
- [ ] Install and test the signed APK on a real Android device.
- [ ] Confirm update-check notification permission is granted and that the notification opens the matching GitHub Release.
- [ ] Verify the tag, release commit, APK version, certificate fingerprint, and SHA-256 checksum all match.

## Branch protection
Configure GitHub Settings → Rules → Rulesets (or Branches → Add branch protection rule) for `main`:
- Require a pull request before merging.
- Require at least one approval (or require code-owner review).
- Require the Android CI status check.
- Require branches to be up to date before merging.
- Block force pushes and branch deletion.
- Do not allow bypassing the rules for routine changes.
The repository includes `.github/CODEOWNERS`; code-owner review only becomes mandatory when the corresponding GitHub rule is enabled.

## Licensing and privacy
- [ ] Review [LICENSE](../LICENSE) and [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
- [ ] Keep the Quran text unmodified and credit AlQuran Cloud / Islamic Network.
- [ ] Review the resolved Gradle dependency graph and all transitive notices.
- [ ] Publish a clear privacy notice covering local prayer/journal data, location, network requests, and unencrypted JSON backups.
- [ ] Test backup and restore before distribution.

## Update behavior
The app checks the latest GitHub Release when opened and periodically while the device has network connectivity. If a newer release is available, it posts a notification when Android notification permission is granted. The notification opens the GitHub Release page. Android requires the user to approve installation; the app cannot silently install an APK. Data remains on the device during a normal in-place update signed with the same release key.
