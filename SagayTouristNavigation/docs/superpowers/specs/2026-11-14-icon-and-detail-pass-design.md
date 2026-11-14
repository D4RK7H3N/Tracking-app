# Sagay Tourist Navigation — Icon & Detail Pass

Date: 2026-11-14
Status: approved by the user, pending implementation

## Goal

Make the app tell a tourist, at a glance, **which vehicle to board, what colour
it is, what the fare is, and how long the trip takes** — using the icon set
presented in the lecture (Bus, Tricycle, Reporm 2 Tricycle, Red, Fare,
Estimated Time).

## Starting point (measured, not assumed)

- `TripEstimator` already computes `fareBasis`, `fareIsOfficial`,
  `fareDiscounted` and `originZoneName`. `TripDetailsCard` rendered none of
  them; it printed a hardcoded string instead. That string also claimed
  "max ₱20" while `seed.json` carries `maxFare: 40`.
- A concurrent session on the same project has since landed the fare matrix
  (39 rows), the five fare origins, `FareZoneDetector`, Room migration 3→4
  and a working 20% student/senior/PWD discount toggle. The stale hardcoded
  fare line and the hidden `fareBasis` are fixed as part of that work.
- The app has **no vehicle concept**: nothing tells a tourist to ask for the
  red tricycle rather than the bus.
- `material-icons-extended` is not a dependency. Only `material-icons-core` is
  on the classpath, and `isMinifyEnabled = false`, so adding the extended
  artifact would ship all 2 084 icons in the APK.

## Icon sourcing — no new dependency

`tools/extract-material-icons.ps1` reads the `material-icons-extended` AAR and
decompiles each icon's vector path out of the generated Kotlin bytecode
(`javap -c`), emitting Android `<vector>` drawables.

Why this instead of the dependency:

- The release build does not run R8 or resource shrinking, so the dependency
  would add every icon to the APK.
- The project already uses this convention: `ic_my_location.xml` is Material's
  `MyLocation` path, hand-copied into a local drawable.

The extractor reproduces Material's genuine path data, so the icons are
pixel-accurate. Material Icons are Apache-2.0; each generated drawable carries
an attribution comment.

Icons shipped: `directions_bus`, `electric_rickshaw`, `directions_boat`,
`payments`, `schedule`, `route`, plus `palette`, `local_taxi`, `pedal_bike`,
`two_wheeler`, `directions_bike`, `airport_shuttle`, `access_time`, `wallet`,
`directions_transit`, `groups`.

Custom drawables: `ic_tricycle_reporm2.xml` (hand-drawn Reporm 2 silhouette).

### The "Red" icon

Red is a **route colour**, not a separate vehicle. One tricycle silhouette is
tinted at runtime to the serving terminal's route colour, so the red, yellow
and white tricycles come from one drawable instead of three near-identical
files. Contrast is preserved by rendering the glyph inside a tinted circular
badge with a hairline border when the route colour is too light to read on a
white card (white bus, yellow tricycle).

## Data model

`domain/models/VehicleType.kt` (new):

| id | display name | icon |
|---|---|---|
| `bus` | Bus | `ic_directions_bus` |
| `tricycle` | Tricycle | `ic_electric_rickshaw` |
| `reporm2` | Reporm 2 Tricycle | `ic_tricycle_reporm2` |
| `boat` | Boat | `ic_directions_boat` |

`Terminal` gains `vehicles: List<VehicleType>`.

### Terminal table (final)

| id | name | colour | vehicles | notes |
|---|---|---|---|---|
| T1 | Sagay Public Market Terminal (Poblacion) | **Red** `#D32F2F` | Tricycle | |
| T2 | Vito Terminal (Government Center route) | **Yellow** `#F9A825` | Reporm 2 Tricycle | Vito Fish Port (Molocaboc boats) |
| T3 | Old Sagay Port Terminal | **White** `#FFFFFF` | Bus, Boat | route *Old Sagay to Sagay City* / *Sagay City to Old Sagay*; Carbin Reef and Suyac boats |

The vehicles per terminal live in `seed.json` as one JSON array per terminal,
so a correction is a one-line edit and an app restart.

## Storage

- `terminal.vehicles TEXT NOT NULL DEFAULT ''` — comma-separated vehicle ids.
- Room migration 4→5: `ALTER TABLE terminal ADD COLUMN vehicles TEXT NOT NULL DEFAULT ''`.
- `SeedParser.SeedTerminal.vehicles: List<String>`, `SeedLoader` writes it,
  `EntityMappers` maps it to `List<VehicleType>`.
- Unknown vehicle ids are dropped rather than crashing the seed load.

## UI

### `TripDetailsCard` — icon-led

1. **Banner** (unchanged): road km · estimated time, and the fare in pesos.
2. **Detail rows**, each with an icon:
   - `ic_route` — road distance in km.
   - `ic_schedule` — estimated time, labelled an estimate.
   - `ic_payments` — fare, plus an "official matrix" badge when
     `fareIsOfficial`, and a "20% discount applied" badge when `fareDiscounted`.
   - Vehicle icon in the route colour + service name ("Red Tricycle").
   - Colour chip + terminal name + colour name + service area.
   - `ic_directions_boat` for the boat leg.
   - Origin zone (`originZoneName`), e.g. "Fare zone: City Proper (Poblacion)".
   - Distance warning keeps its existing message.

### `DestinationSheet`

Each row shows the vehicle icon (tinted to the route colour) in place of the
plain colour dot, and the supporting line carries the terminal name next to
the category.

### Terminal legend sheet (new)

A bottom sheet listing every terminal: colour chip, vehicle icon, terminal
name, route colour name, and service area. This is the tourist cheat sheet —
"ask for the red tricycle". Reachable from a button above the destination
sheet.

## Tests

- `SeedParserTest` — vehicle arrays parse; unknown ids are dropped.
- `VehicleType` mapping — ids round-trip; the terminal's vehicles map to the
  right drawable id.
- Existing fare, Haversine and matrix tests must keep passing.

## Verification

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\gradlew.bat :app:assembleDebug`
3. Install on the connected device (`10HEAKFRM700057`) with adb and screenshot
   the result card and the legend sheet.

## Out of scope

- Turn-by-turn navigation, booking, payments, accounts, offline maps.
- Reworking the fare rules or the matrix contents.
- Any change to the fare logic.
