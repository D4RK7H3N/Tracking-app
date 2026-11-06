# Design: Finish & verify the native Android "Sagay Tourist Navigation" app

Date: 2026-11-07
Status: Approved (user confirmed in session)
Repo: `Tracking app` (parent). Target: untracked `SagayTouristNavigation/` subproject.

## Context

The repo already contains a committed **Flutter** app ("Sagay Tourist Navigator",
flutter_map + OSRM) and an **untracked native Android** project
(`SagayTouristNavigation/`, ~40 files, Kotlin/Compose/Hilt/Room) that matches
the paper's three-tier architecture. The paper's original build prompt asked
for Mapbox; the existing native project deliberately uses **osmdroid + OSRM
(keyless)**.

## Decisions (user-approved)

1. **Map stack:** keep osmdroid + OSRM (free, no API keys). Mapbox swap remains
   possible later via `di/NetworkModule.kt` (base URL) and the map layer.
2. **Scope:** finish and verify the existing native project (no rebuild).
3. **Seed data:** port the researched Flutter dataset (3 terminals, 17
   destinations) but keep the fare formula strictly as specified —
   base ₱15 for the first 3 km, +₱2 per started km (ceil), **no discount
   toggle**.
4. Commit the native project into the parent repo when verified.

## Current state (verified by code read)

Already complete and matching the prompt:

- State machine in `TripViewModel`: `NeedsPermission → Locating → MapReady →
  DestinationSelected → FetchingRoute → ResultShown` plus `GpsError`,
  `WaitingForInternet`, `RouteError`.
- Connectivity auto-retry: `ConnectivityObserver` (NetworkCallback flow) drives
  `isOnline`; `WaitingForInternet` trips are saved as `PENDING_ROUTE` in
  `trip_state` and resumed on reconnect or next app start, then marked `DONE`.
- Room entities `terminal`, `destination`, `fare_rule`, `trip_state`, seeded
  from `assets/seed.json` on first launch by `SeedLoader` (seed-if-empty).
- OSRM driving route via Retrofit + kotlinx.serialization; Mapbox-v5-compatible
  response schema; fare computed **only** from road km.
- Haversine sanity check: road < straight-line, or road > 3× straight-line ⇒
  `distanceWarning` shown; never changes the fare.
- Unit tests: FareCalculator (base, just-above, multi-step, 0 km), Haversine
  (known distances, zero, warning rule both directions, normal case).
- osmdroid user agent set; edge-to-edge Compose UI; searchable destination
  sheet; details card with distance, fare ("estimate only"), terminal, color
  chip, service areas.

## Changes to implement

### A. Build infrastructure

1. Bootstrap the Gradle wrapper (Gradle 8.10.2 per existing
   `gradle/wrapper/gradle-wrapper.properties`): add `gradle-wrapper.jar`,
   `gradlew`, `gradlew.bat`.
2. Resolve JDK compatibility: host JDK is 26; Gradle 8.10.2 requires ≤ 21/22.
   Point the build at a supported JDK (Android Studio JBR 21 if present, e.g.
   via `org.gradle.java.home` or invocation env) so `testDebugUnitTest` and
   `assembleDebug` run on this machine.
3. Ensure `.gitignore` excludes `build/`, `.gradle/`, `.kotlin/`, local
   `local.properties`.

### B. Code fixes

1. Fix 3 mojibake strings in `MapScreen.kt` (`â€¦` → `…`).
2. Add a **"locate me" FAB** on the map: recenters on the user and takes a
   fresh GPS fix (prompt requirement).
3. Replace the `TODO` in `FareCalculator` with a documented working-assumption
   comment (ceil = per-started-km rule, pending confirmation of Resolution No.
   2026-117; single clearly named function already isolates it).

### C. Seed data port (Flutter → native, no discount)

Replace `app/src/main/assets/seed.json` with the researched set from
`assets/data/sagay_data.json` (v5), schema-adapted:

- Terminals: T1 Public Market/Poblacion (Red `#D32F2F`), T2 Vito Government
  Center route (Green `#2E7D32`), T3 Old Sagay Port (Blue `#1565C0`), with
  their researched `serviceArea` lists.
- Destinations: all 17 Flutter entries (Carbin Reef, Suyac, Molocaboc, Museo,
  Margaha, Plaza, Market, Mall, City Hall, etc.).
- Fare rule: baseFare 15.0, baseKm 3.0, extraPerKm 2.0,
  source "Sagay City Resolution No. 2026-117" (no discounts).

**Boat handling (island destinations):** Carbin Reef, Suyac Island, and
Molocaboc Island are unreachable by road. Each carries `boat: true` plus the
serving port terminal id. Routing behavior:

- The road route is computed from the user's position to the **port terminal**
  (Old Sagay Port or Vito), never to the offshore point.
- `TripDetailsCard` shows "Boat ride required from ⟨terminal name⟩".
- The Haversine sanity check compares road km against the straight-line to the
  road endpoint (the port), not the island, so it does not fire falsely.
- Fare = road leg to the port only. This matches the Flutter app's boat-leg
  routing behavior.

### D. Tests to add

1. `FareCalculatorTest`: large distance (e.g. 12 km → 15 + 9×2 = ₱33) and exact
   step boundary (4.0 km → ₱17; 4.01 km → ₱19).
2. New `SeedParser` unit test: extract the pure JSON parsing (kotlinx
   serialization) out of the Android-bound `SeedLoader` into a unit-testable
   parser; test that the sample seed.json parses into the expected terminal and
   destination row counts and validates terminal-id references.

### E. Docs & delivery

1. Update `SagayTouristNavigation/README.md`: keyless stack note (already
   present), boat-destination behavior, locate-me button, exact build/test
   commands, how to replace seed data (uninstall/clear data to reseed), and the
   fare-rule caveat.
2. Run `gradlew :app:testDebugUnitTest` and `gradlew :app:assembleDebug`; fix
   any compile/test failures.
3. Commit `SagayTouristNavigation/` to the parent repo (artifacts excluded),
   including this design doc.

## Out of scope (unchanged)

Booking, payments, real-time tracking, accounts, turn-by-turn voice
navigation, offline maps, Mapbox migration, discount toggle, real fieldwork
data (paper limitation: all data is placeholder until TODA/city verification).

## Known risks

- Gradle/JDK mismatch on this host may require an installed JDK 17–21;
  Android Studio JBR is the expected source.
- OSRM demo server is rate-limited and unofficial (documented limitation;
  self-hosted OSRM recommended before release).
- Reseeding requires clearing app data (Room seed-if-empty) — documented in
  README.
