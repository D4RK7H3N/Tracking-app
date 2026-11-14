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
  4.01 km, 5 km, a large 12 km trip, and the fallback cap (short trips keep
  base/step fares, long trips clamp to the ceiling).
- `Haversine` — known Sagay-area distances, zero distance, and the road-vs-
  straight-line warning rule in both directions.
- `FareMatrix` — origin-zone detection (City Proper, Crossing Vito, Paraiso,
  Old Sagay, Bato), matrix look-ups per origin table, the published label
  variations ("SAN AGUSTIN" vs "SAN AGUSTIN (PROPER)"), and the 15–40 peso
  range of every published route.
- `SeedParser` — parses the bundled `seed.json` into 3 terminals and
  38 destinations (21 of them schools), checks that every destination's
  terminal id resolves, that the 8 island destinations carry the boat flag,
  the 39 fare-matrix rows and 5 origins, the fare values (15 / 3 km / +2),
  and that colours are valid `#RRGGBB` codes.

## The official fare matrix (Resolution No. 2026-117)

The city publishes **flat fares per route**, not a distance formula:

| Origin table | Routes | Range |
| --- | --- | --- |
| City Proper (Poblacion) | 27 | ₱15 – ₱40 |
| Crossing Vito | 2 | ₱15 – ₱23 |
| Paraiso | 6 | ₱15 – ₱33 |
| Brgy. Old Sagay | 1 | ₱15 |
| Brgy. Bato | 3 | ₱17 – ₱25 |

The app therefore:

1. detects the passenger's **origin zone** from GPS (`fare_origins` in
   `seed.json`, each with a radius);
2. looks the route up in that origin's table (`fare_matrix`);
3. shows the **official flat fare** and labels it
   *"Official fare matrix: ⟨origin⟩ → ⟨route⟩"*;
4. offers the published **20% student / senior citizen / PWD fare** as a
   toggle on the result card;
5. falls back to the road-distance estimate
   (₱15 first 3 km, +₱2 per started km, capped at the matrix ceiling ₱40)
   when the route is not in that origin's table.

Route labels in the published matrix are inconsistent, so lookups compare the
label, the label without its parenthetical, and its last path segment
("BRGY. PARAISO/FABRICA" also matches a "BRGY. FABRICA" row).

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
2. **Destinations** — `assets/seed.json` -> `destinations[]`. 38 entries
   (tourist spots + 21 DepEd schools from the OSM/Overpass data and the
   DepEd Division of Sagay City directory); coordinates from PhilAtlas /
   OSM / Wikipedia. Verify on site.
3. **Fare values** — `assets/seed.json` -> `fare_rule` (the distance fallback:
   `baseFare=15`, `baseKm=3`, `extraPerKm=2`, `maxFare=40`, source
   "Sagay City Resolution No. 2026-117") and `fare_matrix` / `fare_origins`
   (the 39 published routes and the 5 origin zones, including the 20%
   student / senior / PWD fares). The matrix is the city's temporary order —
   replace both when the official schedules are published.
4. **Fare rounding rule** — `domain/FareCalculator.kt` uses
   `ceil(roadKm - baseKm) * extraPerKm` (one step per **started** kilometer)
   for the fallback only, clamped to `maxFare`. If Resolution No. 2026-117
   specifies a different partial-kilometer or ceiling rule,
   change that one function and update `FareCalculatorTest`.
5. **Destination → matrix route** — `assets/seed.json` -> `destinations[].zone`
   maps each destination to a matrix route (e.g. "BRGY. VITO"). It was derived
   from OpenStreetMap coordinates and nearest-route matching; confirm with the
   city government.
6. After editing `seed.json`: restart the app. Bundled terminals, destinations,
   the fare rule and the matrix are refreshed automatically once per process
   without deleting saved trip state.

## Architecture

```
app/src/main/java/com/sagay/touristnav/
 ui/         MapScreen, DestinationSheet, TripDetailsCard, PermissionPrompt,
             TripViewModel, UiState
  domain/     FareCalculator, TripEstimator, Haversine,
              FareZoneDetector / FareMatrixLookup, models
  data/local/ AppDatabase, TerminalDao, DestinationDao, FareRuleDao,
              FareMatrixDao, FareOriginDao, TripStateDao, entities,
              SeedLoader, SeedParser
 data/remote/DirectionsApi (Mapbox Directions v5, driving)
 data/repo/  TripRepository
 platform/   LocationProvider (Fused), ConnectivityObserver
```

Runtime state machine:
`NeedsPermission -> Locating -> MapReady -> DestinationSelected ->
FetchingRoute -> ResultShown`, with `GpsError` (retry), `WaitingForInternet`
(trip persisted as `PENDING_ROUTE`, auto-resumed on reconnect or next app
start) and `RouteError` (retry) branches.

Fare comes from the **official Resolution No. 2026-117 matrix** whenever the
passenger's origin zone and the destination's route are in the same table;
otherwise it falls back to the road-distance estimate (₱15 first 3 km, +₱2 per
started km, capped at ₱40). The Haversine straight-line distance is used purely
as a sanity check: if the road distance is below the straight-line distance or
more than 3x it, the result card shows "Distance looks unusual, please verify".
The fare is always labeled "Estimate only".
