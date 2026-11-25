# App Feature Expansion and iOS Build — Design

**Date:** 2026-11-18
**Status:** Approved for implementation

> **Status update (2026-11-25):** Delivery phases 3–7 (UI motion, trip history
> + favourites, journey guidance, offline map packs, and fare tools expansion)
> are **now implemented** in the native app; the code is being finalized by
> another agent. The current behaviour is summarized in the root
> [README](../../README.md). Read the phase lists below as the original design,
> not as outstanding work.

## Context

The repository contains two implementations of the Sagay Tourist Navigator:

- `SagayTouristNavigation/` — the shipping **native Android** app (Kotlin,
  Jetpack Compose, MVVM, Hilt, Room, osmdroid, OSRM). It holds the current
  source of truth for city data: `assets/seed.json` with 3 terminals,
  38 destinations, 5 fare origins, 40 official fare-matrix rows, 1
  `route_access` rule and 7 routes. 77 unit tests pass.
- The repository root — the original **Flutter** prototype (Dart). It has a
  full `ios/Runner` Xcode project but outdated v2 data: no fare matrix, no
  route colours, no `route_access`, and the old distance-formula fare.

The owner requested more working functions, an iOS build, and richer motion
("flex animations") across **both** apps, kept in lockstep.

## Approved decisions

| Decision | Choice |
| --- | --- |
| Scope | Both apps, implemented together for each feature |
| Approach | Shared contract + parallel native implementations; no rewrite |
| Navigation depth | Journey guidance: progress, remaining distance/time, change-point and arrival prompts; no voice or turn maneuvers |
| iOS build | GitHub Actions macOS runner; unsigned release IPA + simulator build, optional signed job gated on secrets |
| Apple signing | Not available — no TestFlight/App Store; sideload unsigned IPA with a free Apple ID |
| Motion | Animated layouts + page transitions, including the map `+`/`−` zoom controls |
| Features | Trip history + favourites, journey guidance, offline map packs, fare tools expansion |
| Sequence | Seven phases (below), each with its own spec → plan → implementation |

## 1. Architecture — shared contract

- Keep both apps and their existing stacks. Nothing is rewritten.
- Add `shared/sagay-contract/schema.json`: versioned JSON Schema (v3) for
  terminals, routes (with stops and colours), destinations, fare origins, the
  official fare matrix, `route_access`, and the new records: favourites, trip
  history, map-pack manifests, fare-tool preferences.
- Add `shared/sagay-contract/cases/`: golden input→expected-output JSON
  vectors for fare lookup, journey legs, discount scope, change points, group
  fares and export rows. Both apps' test suites consume the **same** files —
  as Kotlin test resources and Dart test assets — so parity is mechanical.
- Add `tools/sync_seed.dart`: generates the Flutter app's data assets from the
  native app's `seed.json`, so the two never drift.
- No backend. Everything remains bundled/offline-first.

## 2. Motion system

One shared motion spec in both apps:

| Token | Duration | Use |
| --- | --- | --- |
| Micro | 120 ms | Press feedback, toggles, the map `+`/`−` buttons |
| Layout | 220 ms | Sheet expand/collapse, cards revealing legs, lists reflowing |
| Page | 320 ms | Screen changes, shared-element handoff |

- **Animated layouts**: destination sheet expands/collapses smoothly; trip
  cards grow to reveal journey legs and stops; lists reflow when the estimate
  changes; favourite and expand toggles animate instead of jumping.
- **Map zoom**: the `+` and `−` controls animate on press (scale/press
  feedback) and zoom the map smoothly rather than jumping a zoom level.
- **Page transitions**: search → result → journey guidance uses a consistent
  forward slide + fade, reversed on back; the destination colour chip carries
  across screens as a shared element; the launch screen dissolves into the map.
- Compose: `animateContentSize`, `AnimatedContent`, `AnimatedVisibility`,
  `SharedTransitionLayout`, Navigation transitions, `animateFloatAsState` for
  zoom feedback. Flutter: `AnimatedContainer`, `AnimatedSize`,
  `AnimatedSwitcher`, `Hero`, custom `PageRoute`, `AnimationController` for
  zoom feedback.
- Both apps honour OS reduce-motion settings and cover them in tests.

## 3. Feature behaviour

### 3.1 Trip history + favourites
- Save planned/completed trips locally with origin, destination, fare, vehicle
  scope, date and status; Room entity/DAO in Android, local DB in Flutter.
- Star destinations for a quick-access list on the map home.
- Stats: trip count, total spent, most-used route.
- Delete individual entries and clear all, with confirmation. No account/sync.

### 3.2 Journey guidance
- Start from any result. Live progress along the planned route, remaining
  distance/time to the change point and destination, and prompts at
  "Change at Sagay City Bus Terminal (Poblacion)" and on arrival.
- Uses GPS + bundled polyline offline; OSRM refines when online.
- No voice, no turn-by-turn maneuvers.

### 3.3 Offline map packs
- Bounded Sagay City tile area (bbox + zoom levels) downloaded with progress,
  stored on-device, used automatically when offline; shows download size,
  storage used and a delete option.
- The standard OSM tile server discourages bulk downloads, so the pack uses a
  configurable tile source that permits offline use; a small pre-bundled pack
  is the fallback.
- Android: osmdroid offline tile provider. Flutter: flutter_map local tile
  provider via a tile-caching package.

### 3.4 Fare tools expansion
- Multi-passenger totals with per-passenger discounts.
- Shareable trip summary (text/image) via the OS share sheet.
- CSV export of trip history.

## 4. iOS build pipeline

- Modernise `ios/Runner`: bundle id, display name, version matched to
  `pubspec.yaml`, app icon and launch screen from current assets, location
  permission strings in `Info.plist`, network config for OSRM.
- Add `.github/workflows/ios-build.yml`: manual `workflow_dispatch` plus
  release tags; `macos-latest`; Flutter + Xcode setup; `flutter pub get`,
  `pod install`; unsigned release IPA and simulator build uploaded as workflow
  artifacts and attached to the GitHub release.
- Optional **signed** release job gated on repository secrets
  (`APPLE_CERTIFICATE`, `APPLE_CERTIFICATE_PASSWORD`, `PROVISIONING_PROFILE`,
  etc.). Without secrets it is skipped; adding them later enables a signed
  build with no code changes.
- Sideloading instructions (AltStore/Sideloadly with a free Apple ID, ~7-day
  re-sign) go into the release notes and the iOS README.
- No TestFlight/App Store distribution without Apple signing.

## 5. Testing, errors and delivery

- **Parity**: both apps run the same golden vectors in CI; Kotlin unit tests
  and Flutter tests run on every change.
- **Motion**: widget/screenshot tests for sheet expansion, `+`/`−` zoom
  animation and page transitions; reduce-motion path covered.
- **Features**: history CRUD, favourites, guidance at the change point,
  offline pack download/delete, group-fare totals, share and CSV export.
- **Errors**: no location permission → prompt; offline with no map pack →
  straight-line fallback, clearly labelled; OSRM failure → cached/offline
  route; low storage → block download with a message; share/export failure →
  visible error, no data loss.
- **Delivery phases**:
  1. Foundation (shared contract, data port, parity harness)
  2. iOS pipeline skeleton
  3. UI motion (animated layouts, page transitions, `+`/`−` zoom)
  4. Trip history + favourites
  5. Journey guidance
  6. Offline map packs
  7. Fare tools expansion
- Final validation: native APK + unsigned IPA attached to a GitHub release,
  README download links updated, and a real-device walkthrough of the key
  flows.

## Constraints and risks

- This host is Windows: the iOS workflow runs on GitHub's macOS runners, so
  its result can only be verified from CI logs, not locally.
- Flutter's v2 data must be fully replaced before feature parity is possible;
  this is part of phase 1, not an optional extra.
- Offline tile policy must be respected; an unavailable source falls back to
  the bundled small pack.
- Adding Flutter dependencies requires `flutter pub get`; offline availability
  of packages must be checked when each phase starts.
