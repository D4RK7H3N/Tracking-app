# Sagay Tourist Navigator

An **offline-first, backend-free** Android app that helps tourists get around
**Sagay City, Negros Occidental, Philippines** using tricycles.

- Flutter stable · Dart null-safety · Material 3
- **No server, no database, no login** — all city data ships inside the app as
  a JSON asset (`assets/data/sagay_data.json`).

## What it does

1. Shows your current GPS location on an OpenStreetMap map (no API key).
2. Lets you pick a destination from a searchable list (by name or category).
3. Shows the **road distance** to it (OSRM public routing API; falls back to a
   straight-line estimate labelled **"Estimated (offline)"** when there is no
   internet).
4. Shows an **estimated tricycle fare** computed from a simple stepped formula
   (plus an optional Senior/PWD discount toggle).
5. Shows **which tricycle terminal** serves that destination and its assigned
   **colour**, so you can find the right tricycle queue at a glance.

Out of scope by design: booking, payments, real-time tricycle tracking, user
accounts, turn-by-turn voice navigation.

## ⚠️ ALL DATA VALUES ARE PLACEHOLDERS

Every fare amount, terminal, colour assignment, area list, destination name
and coordinate in `assets/data/sagay_data.json` is a **clearly fake
placeholder** used for development. Replace them with:

- the **official city fare matrix** (base fare, step size, extra per step,
  discounts), and
- **surveyed coordinates** for the real tricycle terminals, their official
  colour codes, and the destinations they serve.

See "Replacing the data" below.

## Project structure

```
lib/
  main.dart                      App entry, providers, theme
  models/
    terminal.dart                Terminal + "#RRGGBB" colour parsing
    destination.dart             Destination
    fare_rule.dart               FareRule (formula) + Discount + FarePolicy
    app_data.dart                Root JSON model + terminal lookups
  services/
    data_service.dart            Loads the bundled JSON asset
    location_service.dart        GPS + permissions, normalised outcomes
    distance_service.dart        OSRM road distance + Haversine fallback
    fare_service.dart            Fare computation + discount + formatting
  state/app_state.dart           Central ChangeNotifier (provider)
  screens/
    home_screen.dart             Map + locate-me + searchable destination sheet
    result_screen.dart           Distance / fare / terminal / mini map
  widgets/
    color_chip.dart              Big terminal colour swatch
    result_card.dart             Card layout for result blocks
assets/data/sagay_data.json      THE data file you will replace
test/                            Unit tests (fare, haversine, data integrity)
```

## Setup

```bash
# 1. Install Flutter stable and verify
flutter doctor

# 2. Fetch packages
flutter pub get

# 3. Run on a connected device or emulator (Android-first)
flutter run
```

Dependencies (pinned by `pubspec.yaml`):

| Package      | Used for                                   |
| ------------ | ------------------------------------------ |
| geolocator   | GPS + runtime location permissions         |
| flutter_map  | OpenStreetMap map rendering (no API key)   |
| latlong2     | Latitude/longitude types                   |
| http         | OSRM road-distance request                 |
| provider     | Simple state management                    |

## Replacing the data

Edit **`assets/data/sagay_data.json`** only — no code changes are needed if
you keep the schema:

```json
{
  "version": 1,
  "fare_rule": {
    "base_fare": 15,          // ₱ charged for trips up to base_distance_km
    "base_distance_km": 2.0,
    "step_km": 1.5,           // each full step beyond the base adds:
    "extra_per_step": 10,     //   base_fare + ceil(extra_km / step_km) * extra_per_step
    "road_factor": 1.3,       // offline estimate multiplier (see below)
    "discounts": [
      { "label": "Senior Citizen / PWD", "percent": 20 }
    ]
  },
  "terminals": [
    {
      "id": "T1",             // any stable string id
      "name": "Sagay City Plaza Terminal",
      "lat": 10.9451, "lng": 123.4799,
      "color": "#E53935",     // the tricycle colour assigned to this terminal
      "color_name": "Red",    // human-readable label shown on the big chip
      "areas": ["Poblacion", "Public Market"]
    }
  ],
  "destinations": [
    {
      "id": "D1",
      "name": "Carbin Reef Sandbar",
      "category": "Beach / Island",   // used by search
      "lat": 10.956, "lng": 123.518,
      "terminal_id": "T1"             // must match a terminal id
    }
  ]
}
```

Notes:

- Every `terminal_id` in `destinations` must exist in `terminals` — the test
  suite checks this (`flutter test`).
- `discounts`: the **first** entry becomes the optional toggle on the result
  screen. Leave the array empty (`[]`) to hide the toggle entirely.
- Colours must be `#RRGGBB`.

## Calibrating `road_factor`

`road_factor` scales the straight-line (Haversine) distance into an
approximate **road** distance for the offline fallback:

```
road_distance ≈ straight_line_distance × road_factor
```

To calibrate:

1. Pick 5–10 real origin/destination pairs around Sagay City.
2. Measure the actual driving distance for each (e.g. with OSRM's demo
   server, or a car's odometer / Google Maps route distance).
3. For each pair compute `actual_km / straight_line_km`.
4. Use the **average** (or median) ratio as `road_factor`. Typical values for
   small Philippine cities land between **1.2 and 1.5**; the bundled
   placeholder uses 1.3.
5. Re-run `flutter test` — the data test asserts `road_factor >= 1`.

A well-calibrated factor keeps offline estimates within a few hundred metres
of the real route for short tricycle rides.

## Running the tests

```bash
flutter test
```

The suite covers:

- **FareRule** — within the base distance, exactly at the base distance, just
  above the base, multiple steps, exact step boundary, just below a boundary,
  discount math, peso rounding, JSON parsing.
- **Haversine** — zero distance, 0.01° latitude ≈ 1.11 km, 1° longitude at
  the equator ≈ 111.2 km, a Sagay-area sanity check, symmetry.
- **Data integrity** — the bundled JSON parses, all `terminal_id`s resolve,
  colours are `#RRGGBB`, fare values are sane.

## Swapping the routing provider

The OSRM base URL is a single constant in
`lib/services/distance_service.dart` (`DistanceService.defaultBaseUrl`). To
switch to e.g. OpenRouteService, change that constant and adapt the small
response parser (`_parseRoadDistanceKm`) to the new API's response shape.

## Running on Android

`AndroidManifest.xml` already declares `INTERNET`,
`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`; `minSdk` is set to 23.
Just run `flutter run` with a device connected.

## Known limitations

- **All data values are placeholders** and must be replaced with surveyed
  data and the official city fare matrix (see above).
- The OSRM demo server is rate-limited and not meant for production; swap in
  a self-hosted OSRM or OpenRouteService before wide release.
- Offline distances are straight-line × `road_factor` estimates, not real
  routes — always shown with an "Estimated (offline)" label.
- The fare is an estimate only; actual tricycle fares may vary (traffic,
  night surcharges, driver discretion). The app displays a disclaimer.
- The map uses standard OpenStreetMap tiles; a dedicated tile source with
  proper attribution policy may be preferable at scale.
- Only the first discount entry in the JSON is offered as a toggle.
