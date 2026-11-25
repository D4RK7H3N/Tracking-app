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
- **Offline-first for data, online for tiles and routing.** The city data
  (terminals, destinations, fare matrix and rules) is **bundled in the APK** and
  needs no network. **Map tiles and road routing do need internet**; offline map
  packs store tiles locally for a bounded Sagay City area. If you pick a
  destination while offline, the trip is saved as `PENDING_ROUTE` and the road
  route is fetched automatically once connectivity returns.
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
  variations ("SAN AGUSTIN" vs "SAN AGUSTIN (PROPER)"), and the 15“40 peso
  range of every published route.
- `SeedParser` — parses the bundled `seed.json` into 3 terminals and
  38 destinations (21 of them schools), checks that every destination's
  terminal id resolves, that the 8 island destinations carry the boat flag,
  the 40 fare-matrix rows and 5 origins, the fare values (15 / 3 km / +2),
  that colours are valid `#RRGGBB` codes, and that the Old Sagay to Sagay
  City corridor row and access rule are bundled and name known routes.
- `VehicleType` — the vehicles boarding at each terminal, the vehicle id
  round trip, `Terminal.serviceLabel`, and that an unknown stored vehicle id is
  dropped instead of breaking the terminal.
- `Route` — route colours per route, the yellow corridor being a tricycle, white
  reserved for the water routes, and every terminal matching the routes it boards.
- `FareFormat` — published fares print as published: a whole peso amount stays
  whole and an amount with centavos keeps them.
- `TripEstimator` — fare scoping per vehicle: the tricycle and pink Reporm 2
  matrix fares and their 20% discounts, the island tricycle-to-port fare with
  an unverified boat fare, and the distance estimate outside the matrix. It
  also covers the Old Sagay to Sagay City corridor journey, its single fare,
  and a stale access rule being ignored.

## Route colours and vehicles

Every service in Sagay City is known by its colour, so the app shows the
route colour together with the vehicle that carries it:

| Route | Colour | Vehicle | Stops | Serves |
| --- | --- | --- | --- | --- |
| R-POPLACION | Red | Tricycle | none | Poblacion, Public Plaza, Public Market, Rizal |
| R-BATO | Pink | Tricycle | none | Brgy. Bato |
| R-VITO | Pink | Reporm 2 | 5 | Vito, Plaridel, Rafaela Barrera, Andres Bonifacio, Crossing Vito |
| R-BULANON | Pink | Reporm 2 | 3 | Taba-ao, Bulanon Proper, Talusan |
| R-OLD-SAGAY | Yellow | Tricycle | 5 | Sagay City Bus Terminal (Poblacion) - Old Sagay Elementary School - Margaha Beach - Museo sang Bata sa Negros - Old Sagay Port |
| R-WATER-OLD-SAGAY | White | Boat | none | Carbin Reef, Suyac, Panal and Macahulom from Old Sagay Port |
| R-WATER-VITO | White | Boat | none | Molocaboc Island from the Vito Fish Port |

The three terminals board these services:

| Terminal | Colour | Vehicles | Serves |
| --- | --- | --- | --- |
| T1 Sagay Public Market (Poblacion) | Red | Tricycle | Poblacion, Public Plaza, Public Market, Rizal |
| T2 Vito (Government Center route) | Pink | Reporm 2, Tricycle, Boat | Vito, Bato, Vito Fish Port (Molocaboc boats), National Highway |
| T3 Old Sagay Port | Yellow | Tricycle, Reporm 2, Boat | Old Sagay to Sagay City, Sagay City to Old Sagay, Taba-ao, Bulanon, Himoga-an Baybay, Carbin Reef and Suyac boats |

**White is reserved for the water routes.** The colour belongs to the
*route*, because it is the colour of the vehicle a tourist has to ask for, and
one terminal can board several colours.

The vehicle a route serves is **data, not code**: `seed.json` ->
`routes[].vehicle` is a vehicle id (`tricycle`, `reporm2`, `boat`).
Change one value and restart the app. The colour chip, the icon in
the destination list and the badge on the trip card all follow the route
colour, and the glyph switches to a dark ink on light routes so the white and
the yellow services stay readable.

The **Terminals & routes** button above the destination list opens a legend
of all routes — the cheat sheet for "which one is the red one?".
## Icons

The app uses **local vector drawables** for its icons rather than calling the
Material Icons composables directly. `material-icons-extended` **is** declared
on the classpath (`app/build.gradle.kts`), and because the release build does
not run R8 or resource shrinking, calling it from Compose would ship all 2 084
icons in the APK. The project therefore keeps only the handful of icons it uses
as local Android `<vector>` drawables.

`tools/extract-material-icons.ps1` reads the `material-icons-extended`
AAR and decompiles each icon's vector path out of the generated Kotlin
bytecode, emitting local Android `<vector>` drawables under
`app/src/main/res/drawable/`. The result is pixel-accurate Material art in
about one kilobyte per icon, and it is the convention the project already used
for `ic_my_location.xml`.

```
.\tools\extract-material-icons.ps1 `
    -Aar <path-to-material-icons-extended.aar> `
    -OutDir app\src\main\res\drawable `
    -Icons ElectricRickshaw,DirectionsBoat,Payments,Schedule,Route
```

Material Icons are Apache-2.0 licensed and every generated drawable carries
an attribution comment. `ic_tricycle_reporm2.xml` is hand-drawn for this
project — the Reporm 2 has a motorcycle front and an open passenger side,
unlike Material's closed `electric_rickshaw` — so the two tricycle services
are told apart at a glance.

## The official fare matrix (Resolution No. 2026-117)

The city publishes **flat fares per route**, not a distance formula:

| Origin table | Routes | Range |
| --- | --- | --- |
| City Proper (Poblacion) | 27 | â‚±15 “ â‚±40 |
| Crossing Vito | 2 | â‚±15 “ â‚±23 |
| Paraiso | 6 | â‚±15 “ â‚±33 |
| Brgy. Old Sagay | 1 | â‚±15 |
| Brgy. Bato | 3 | â‚±17 “ â‚±25 |

These are the **39 published rows**. The seed also carries **one additional
owner-supplied corridor row** (Old Sagay → Sagay City, ₱20 / ₱16) used by the
two-service rule below, for **40 stored rows in total**.

The app therefore:

1. detects the passenger's **origin zone** from GPS (`fare_origins` in
   `seed.json`, each with a radius);
2. looks the route up in that origin's table (`fare_matrix`);
3. shows the **official flat fare** and labels it
   *"Official fare matrix: âŸ¨originâŸ© â†’ âŸ¨routeâŸ©"*;
4. offers the published **20% student / senior citizen / PWD fare** as a
   toggle on the result card;
5. falls back to the road-distance estimate
   (â‚±15 first 3 km, +â‚±2 per started km, capped at the matrix ceiling â‚±40)
   when the route is not in that origin's table.

Route labels in the published matrix are inconsistent, so lookups compare the
label, the label without its parenthetical, and its last path segment
("BRGY. PARAISO/FABRICA" also matches a "BRGY. FABRICA" row).

The fare is scoped to the vehicle that carries the trip, because the matrix
prices **motorized tricycles** only:

| Service | What the card shows |
| --- | --- |
| Tricycle / Reporm 2 | The matrix fare, or the distance estimate when the pair is not in the matrix |
| Island / reef | The **tricycle fare to the port**, plus **Boat fare not verified** |
| No known route | **Fare not verified** — local public-transport details need confirmation |

A verified land fare is never combined with an unpublished boat fare into a
single total.

The 20% toggle is offered only for an official matrix fare. The distance
estimate keeps its single value, because no discounted estimate is
published, so a toggle there would promise something the card cannot
deliver. Published amounts are printed as published: a whole peso amount
stays whole and an amount with centavos keeps them (₱13.60, not ₱14).

## Old Sagay to Sagay City needs two services

Standing in Old Sagay and heading into Sagay City, the passenger rides the
**Yellow Tricycle** into the city and then the **Red Tricycle** to the
destination. The result card lists both with the point where the passenger
changes, and the map draws the Yellow route's stops, which include that
point.

The rule is owner-supplied and lives in the seed, so it can be corrected
without a code change:

```json
"route_access": [{
  "originZone": "OLD_SAGAY",
  "destinationRouteId": "R-POPLACION",
  "viaRouteId": "R-OLD-SAGAY",
  "changeAt": "Sagay City Bus Terminal (Poblacion)",
  "fareZone": "SAGAY CITY"
}]
```

The two services are one tricycle journey, so they are priced as one fare
and never added together. The `fareZone` names the matrix row that prices
it (`OLD_SAGAY` -> `SAGAY CITY`, ₱20 regular and ₱16
discounted). A rule naming a route the app does not know is ignored, so a
stale rule cannot invent a service.

## Building

```
.\gradlew.bat :app:assembleDebug   # APK at app/build/outputs/apk/debug/
```

`gradle.properties` (gitignored) may pin a machine-local JDK with
`org.gradle.java.home` (Gradle 8.10.2 needs JDK 17“21; Android Studio's JBR
works — see `gradle.properties.example`).

## Island destinations need a boat

Carbin Reef, Suyac Island, Molocaboc Island, Panal Reef and Macahulom Reef
are offshore. For these the app routes the **road leg to the serving port
terminal** (Old Sagay Port or Vito) — the driving profile cannot cross water —
and the result card shows *"Boat ride required from âŸ¨terminalâŸ©"*. The fare
covers the road leg to the port only.

## Routes have stops, direct rides do not

A direct ride inside one barangay is point-to-point, so no intermediate stops
are shown. A service that runs a corridor between two places lists its
owner-supplied waypoints, so the app shows them:

- `assets/seed.json` -> `routes[]` holds each route, its terminal, its
  colour, its vehicle and its ordered `stops[]` (the stops sit on the real
  road, taken from OpenStreetMap and matched against the OSRM route);
- the map draws a numbered marker per stop in the route colour, so the order is
  readable at a glance;
- the result card lists the stops under the vehicle badge, in the vehicle's
  own glyph.

The Yellow Tricycle between Sagay City Bus Terminal (Poblacion) and Old Sagay
Port is such a corridor: it lists the Old Sagay Elementary School, Margaha
Beach, Museo sang Bata sa Negros and Old Sagay Port stops.

Add more routes the same way; each route references a terminal id and the stops
are stored in travel order.

## REPLACE WITH REAL DATA (verified values pending fieldwork)

1. **Terminal coordinates, colours and vehicles** — `assets/seed.json` ->
   `terminals[]` and `routes[]`. The route colours (red tricycle, pink
   Reporm 2 / tricycle, yellow tricycle, white boat) were supplied by the
   app owner; the coordinates were researched from public sources. Verify all
   of it with the Sagay City Tourism Office / LTFRB-franchised operators.
2. **Destinations** — `assets/seed.json` -> `destinations[]`. 38 entries
   (tourist spots + 21 DepEd schools from the OSM/Overpass data and the
   DepEd Division of Sagay City directory); coordinates from PhilAtlas /
   OSM / Wikipedia. Verify on site.
3. **Fare values** — `assets/seed.json` -> `fare_rule` (the distance fallback:
   `baseFare=15`, `baseKm=3`, `extraPerKm=2`, `maxFare=40`, source
   "Sagay City Resolution No. 2026-117") and `fare_matrix` / `fare_origins`
   (the 40 stored matrix rows — 39 published plus one owner-supplied
    Old Sagay → Sagay City corridor row — and the 5 origin zones, including the 20%
   student / senior / PWD fares). The matrix is the city's temporary order —
   replace both when the official schedules are published.
4. **Fare rounding rule** — `domain/FareCalculator.kt` uses
   `ceil(roadKm - baseKm) * extraPerKm` (one step per **started** kilometer)
   for the fallback only, clamped to `maxFare`. If Resolution No. 2026-117
   specifies a different partial-kilometer or ceiling rule,
   change that one function and update `FareCalculatorTest`.
5. **Destination â†’ matrix route** — `assets/seed.json` -> `destinations[].zone`
   maps each destination to a matrix route (e.g. "BRGY. VITO"). It was derived
   from OpenStreetMap coordinates and nearest-route matching; confirm with the
   city government.
6. After editing `seed.json`: restart the app. Bundled terminals, destinations,
   the fare rule and the matrix are refreshed automatically once per process
   without deleting saved trip state.

## Architecture

```
app/src/main/java/com/sagay/touristnav/
 ui/         MapScreen, DestinationSheet, TripDetailsCard, TerminalLegendSheet,
             OnboardingScreen, FareCalculatorScreen, SettingsSheet,
             SagayIcons, PermissionPrompt, TripViewModel, UiState,
             Theme, Motion, MotionPrefs, OnboardingPrefs
  domain/     FareCalculator, TripEstimator, Haversine,
              FareZoneDetector / FareMatrixLookup, models
              (Terminal, VehicleType, Destination, FareRule, TripEstimate,
              Route, RouteStop)
  data/local/ AppDatabase, TerminalDao, DestinationDao, FareRuleDao,
              FareMatrixDao, FareOriginDao, RouteDao, TripStateDao,
              entities, SeedLoader, SeedParser
 data/remote/DirectionsApi (Mapbox Directions v5, driving)
 data/repo/  TripRepository
 platform/   LocationProvider (Fused), ConnectivityObserver
tools/       extract-material-icons.ps1 (Material icon -> vector drawable)
```

### New screens (v1.1.0)

- **Onboarding** — three-page first-run tour (search, colour-coded vehicle,
  official matrix fare), persisted in `OnboardingPrefs`.
- **Fare calculator** — offline tool: choose an origin zone and a route, see
  the regular and 20%-discounted official fare plus a group total. Boat and
  unknown routes are explicitly "not verified".
- **Settings & data** — OS/in-app reduce-motion control, clear trip history
  with confirmation, and the data-verification caveat.
- The palette is now Deep Ocean / Coral / Sunrise Gold / Warm Mist. The
  vehicle drawables and data-driven route colours are unchanged by design.

Runtime state machine:
`NeedsPermission -> Locating -> MapReady -> DestinationSelected ->
FetchingRoute -> ResultShown`, with `GpsError` (retry), `WaitingForInternet`
(trip persisted as `PENDING_ROUTE`, auto-resumed on reconnect or next app
start) and `RouteError` (retry) branches.

Fare comes from the **official Resolution No. 2026-117 matrix** whenever the
passenger's origin zone and the destination's route are in the same table;
otherwise it falls back to the road-distance estimate (â‚±15 first 3 km, +â‚±2 per
started km, capped at â‚±40). The Haversine straight-line distance is used purely
as a sanity check: if the road distance is below the straight-line distance or
more than 3x it, the result card shows "Distance looks unusual, please verify".
The fare is always labeled "Estimate only".
