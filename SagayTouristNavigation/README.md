# Sagay Tourist Navigation

Tourist navigation with distance-based tricycle fare computation for Sagay City,
Negros Occidental, Philippines.

- Kotlin, Jetpack Compose, MVVM (one `StateFlow<UiState>`), Hilt DI
- **osmdroid** map view with OpenStreetMap tiles (no API key needed)
- **OSRM public routing server** for road distance/duration/route geometry
  (same response schema as Mapbox Directions v5; swap back by changing the
  base URL in `di/NetworkModule.kt`)
- Room (SQLite) local database seeded from `app/src/main/assets/seed.json`
- Retrofit + OkHttp + kotlinx.serialization
- Google Fused Location Provider for GPS
- **Needs internet** for the map tiles and road routing. It is NOT an offline app.
  (Offline trips are saved and fetched automatically once internet returns.)
- **No API keys required.** To move to Mapbox or Google Maps later you will
  need their keys and (currently) a billing method on the provider account.

## Setup

1. Open the `SagayTouristNavigation` folder in Android Studio (Ladybug or newer).
   Let it sync; if it asks to create a Gradle wrapper, accept.
2. Add the two Mapbox tokens (see "API keys" below).
3. Run the `app` configuration on a device/emulator with API 28+.

## API keys

**None needed.** Map tiles come from OpenStreetMap (via osmdroid) and routing
from the OSRM public demo server — both keyless. If you later move routing to
Mapbox Directions, you will need a Mapbox account with billing and both a
public (`pk.`) and a secret downloads (`sk.`) token.

## Tests

```
.\gradlew.bat :app:testDebugUnitTest
```

- `FareCalculator` — 0 km, exactly 3 km, 3.1 km, the 4.0 km step boundary,
  4.01 km, 5 km, and a large 12 km trip.
- `Haversine` — known Sagay-area distances, zero distance, and the road-vs-
  straight-line warning rule in both directions.
- `SeedParser` — parses the bundled `seed.json` into 3 terminals and
  17 destinations, checks that every destination's terminal id resolves,
  that the 5 island destinations carry the boat flag, the fare values
  (15 / 3 km / +2), and that colours are valid `#RRGGBB` codes.

## Building

```
.\gradlew.bat :app:assembleDebug   # APK at app/build/outputs/apk/debug/
```

`gradle.properties` (gitignored) may pin a machine-local JDK with
`org.gradle.java.home` (Gradle 8.10.2 needs JDK 17–21; Android Studio's JBR
works — see `gradle.properties.example`).

## Island destinations need a boat

Carbin Reef, Suyac Island, Molocaboc Island, Panal Reef and Macahulom Reef
are offshore. For these the app routes the **road leg to the serving port
terminal** (Old Sagay Port or Vito) — the driving profile cannot cross water —
and the result card shows *"Boat ride required from ⟨terminal⟩"*. The fare
covers the road leg to the port only.

## REPLACE WITH REAL DATA (verified values pending fieldwork)

1. **Terminal coordinates and colors** — `assets/seed.json` -> `terminals[]`.
   Researched from public sources; verify colours and coordinates with the
   Sagay City Tourism Office / LTFRB-franchised operators.
2. **Destinations** — `assets/seed.json` -> `destinations[]`. 17 entries;
   coordinates from PhilAtlas / OSM / Wikipedia. Verify on site.
3. **Fare values** — `assets/seed.json` -> `fare_rule`
   (`baseFare=15`, `baseKm=3`, `extraPerKm=2`, source
   "Sagay City Resolution No. 2026-117"). This is the city's temporary fare
   order — replace when the official matrix is published.
4. **Fare rounding rule** — `domain/FareCalculator.kt` uses
   `ceil(roadKm - baseKm) * extraPerKm` (one step per **started** kilometer).
   If Resolution No. 2026-117 specifies a different partial-kilometer rule,
   change that one function and update `FareCalculatorTest`.
5. After editing `seed.json`: clear app data (or uninstall) so the seed
   reloads — the loader only seeds an empty database.

## Architecture

```
app/src/main/java/com/sagay/touristnav/
 ui/         MapScreen, DestinationSheet, TripDetailsCard, PermissionPrompt,
             TripViewModel, UiState
 domain/     FareCalculator, TripEstimator, Haversine, models
 data/local/ AppDatabase, TerminalDao, DestinationDao, FareRuleDao,
             TripStateDao, entities, SeedLoader
 data/remote/DirectionsApi (Mapbox Directions v5, driving)
 data/repo/  TripRepository
 platform/   LocationProvider (Fused), ConnectivityObserver
```

Runtime state machine:
`NeedsPermission -> Locating -> MapReady -> DestinationSelected ->
FetchingRoute -> ResultShown`, with `GpsError` (retry), `WaitingForInternet`
(trip persisted as `PENDING_ROUTE`, auto-resumed on reconnect or next app
start) and `RouteError` (retry) branches.

Fare is computed ONLY from the Mapbox road distance. The Haversine
straight-line distance is used purely as a sanity check: if the road distance
is below the straight-line distance or more than 3x it, the result card shows
"Distance looks unusual, please verify". The fare is always labeled
"Estimate only".
