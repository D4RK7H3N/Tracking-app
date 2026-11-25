# iOS build and installation

The Flutter app in this repository builds on GitHub Actions using a macOS
runner. A Windows machine cannot compile iOS apps, so the build always happens
in CI — either by pushing a `v...` tag or by running the **iOS build** workflow
manually from the Actions tab.

> **Distribution scope:** this workflow produces sideloadable and simulator
> builds only. There is **no App Store or TestFlight release**; even a signed
> IPA (below) is installed by sideloading, not submitted to Apple.

## Artifacts

Each run produces these files in the `ios-builds` workflow artifact, and a tag
build also attaches them to that tag's GitHub release:

| File | Purpose |
| --- | --- |
| `sagay-unsigned.ipa` | Unsigned device build. Sideload this onto your iPhone. |
| `sagay-simulator.zip` | Simulator build for Xcode's Simulator, for checking the UI without a phone. |
| `sagay-signed.ipa` | Only produced when signing secrets are configured (see below). |

## Installing the unsigned IPA

The unsigned build is not signed by Apple, so it cannot go on the App Store or
TestFlight. Sideload it with a free Apple ID:

1. Install **Sideloadly** (Windows or macOS) or **AltStore** on your computer.
2. Download `sagay-unsigned.ipa` from the workflow artifact or the GitHub
   release.
3. In Sideloadly, select the IPA, enter your free Apple ID, and choose your
   connected iPhone. AltStore users can open the `.ipa` and choose "Install".
4. Trust the app on the iPhone: **Settings → General → VPN & Device
   Management**, choose your Apple ID, then **Trust**.
5. A free Apple ID certificate expires after about **7 days**. Reconnect the
   phone and sideload again to renew; app data is kept if you reinstall over
   the top.

## Enabling a signed build

The signed job is skipped when the secrets are absent. To enable it, add these
repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Contents |
| --- | --- |
| `APPLE_CERTIFICATE` | Base64 of a PKCS#12 (`.p12`) distribution certificate |
| `APPLE_CERTIFICATE_PASSWORD` | The `.p12` import password |
| `PROVISIONING_PROFILE` | Base64 of the `.mobileprovision` file |
| `APPLE_TEAM_ID` | Apple Developer Team ID |

A paid Apple Developer Program membership is required for distribution
certificates. With these secrets present, CI also emits `sagay-signed.ipa`.
This is still a **sideloaded** build (installed with Sideloadly/AltStore or over
the same trust flow above) — it is not an App Store or TestFlight release.

## Local builds

The `ios/Runner` Xcode project, `Podfile` and `Info.plist` are being finalized
alongside these docs; the commands below apply once those files are in place.

On a Mac with Xcode:

```bash
flutter pub get
cd ios && pod install && cd ..
flutter build ipa --release
```

## Known limits

- The app requests **when-in-use** location only; background tracking is not
  enabled.
- Routing uses HTTPS, so no App Transport Security exceptions are needed.
- The bundle identifier is `ph.sagay.sagayTouristNavigator`. Sideloading tools
  may offer to replace it with their own prefix — accept their default if your
  free Apple ID requires it.
