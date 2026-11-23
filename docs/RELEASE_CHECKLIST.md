# Release preparation checklist

## Current status

- Repository: `abdouwkar-alt/salati-al-ahd`
- App display name: `الصَّلَاةُ جَامِعَةٌ`
- Repository name: `salati-al-ahd`
- Current migration: Android source and project documentation imported from `hibrah-marketing/salati-app`.
- Current workflow builds a **debug APK** only. It is not a signed public release.

## Before publishing

- [ ] Decide the Android application ID and confirm it will not change after release.
- [ ] Choose a version name and monotonically increasing version code.
- [ ] Configure a release signing key locally; keep the keystore and passwords out of Git.
- [ ] Store signing material only in GitHub Actions Secrets if building signed releases in CI.
- [ ] Add a dedicated release workflow that builds `assembleRelease`, signs the APK, and verifies the signature.
- [ ] Test the signed APK on real devices.
- [ ] Review app permissions, dependency tree, privacy behavior, and Android backup rules.
- [ ] Confirm third-party API/data attribution and terms.
- [ ] Publish release notes and SHA-256 checksum.
- [ ] Keep the source tag and binary artifact tied to the same commit.

## Signing secrets (names only)

Suggested GitHub Actions secret names:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never commit the keystore, passwords, signing properties, API credentials, or personal data.

## Distribution and verification

Publish APKs only through GitHub Releases for this repository. Provide the release tag, commit, version, signing certificate fingerprint, and SHA-256 checksum. A matching checksum proves that a downloaded file matches the published file; it does not prove that the software is free of vulnerabilities or malicious behavior.

Users can inspect the public source, review the permissions and workflow, compare the release commit, and verify the checksum/signature. Do not claim that this proves the app is completely safe.

## Licensing

The repository uses MIT for original project code. Third-party libraries, Quran text/data, API services, fonts, and artwork may have separate licenses and terms. Inventory and attribute those before public distribution.
