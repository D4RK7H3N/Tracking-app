# Design: Official tricycle fare matrix (Resolution No. 2026-117)

Date: 2026-11-14
Status: Approved (user chose "Full matrix, all origins") and implemented
Applies to: `SagayTouristNavigation/` native Android app.

## Finding

The city publishes **flat fares per route**, not a distance formula. Source:
K5 Sagay News / Sagay City PIO posting of Resolution No. 2026-117
(Facebook, 31 March 2026), corroborated by Visayan Daily Star, Digicast
Negros and Panay News (base fare raised from ₱11 to ₱15, +₱2/km beyond the
first 3 km, plus per-route flat fares of ₱15–₱40 and a 20% student / senior
citizen / PWD fare).

Five origin tables:

| Origin | Routes | Range |
| --- | --- | --- |
| City Proper (Poblacion) | 27 | ₱15 – ₱40 |
| Crossing Vito | 2 | ₱15 – ₱23 |
| Paraiso | 6 | ₱15 – ₱33 |
| Brgy. Old Sagay | 1 | ₱15 |
| Brgy. Bato | 3 | ₱17 – ₱25 |

## Design

1. **Data** — `assets/seed.json` gains `fare_matrix` (39 rows: origin,
   destination route, regular fare, discounted fare) and `fare_origins`
   (zone, name, lat, lng, radiusKm). Each destination gains `zone`, the
   matrix route it belongs to (null when it is not on the matrix).
   Route coordinates come from OpenStreetMap/Overpass; destination → route
   mapping is nearest-route matching within 3 km plus explicit overrides for
   island destinations (their departure port).
2. **Storage** — new Room entities `fare_matrix` and `fare_origin` with
   DAOs; database version 4 with a `MIGRATION_3_4` that adds the tables and
   the `destination.fareZone` column without touching trip state.
3. **Domain** — `FareZoneDetector.detect()` returns the origin zone whose
   radius covers the passenger; `FareMatrixLookup.find()` resolves a route in
   that origin's table, tolerating the published label variations
   ("SAN AGUSTIN" vs "SAN AGUSTIN (PROPER)", "BRGY. PARAISO/FABRICA" vs
   "BRGY. FABRICA").
4. **Fare** — official matrix fare when the origin zone and destination route
   are in the same table; otherwise the road-distance fallback
   (₱15 / 3 km / +₱2 per started km, capped at the matrix ceiling ₱40).
   The result card labels which rule was used.
5. **Discount** — the published 20% student / senior citizen / PWD fare is a
   toggle on the result card; the route is cached, so toggling never re-requests.
6. **Seed** — `maxFare` in `fare_rule` raised from ₱20 to ₱40 (the matrix
   ceiling), because the matrix itself prices trips above ₱20.

## Verification

33 unit tests pass (fare calculator, Haversine, matrix lookup and zone
detection, seed integrity). Verified on a physical device: Vito = ₱40
(from City Proper), Suyac = ₱15 and ₱12 with the discount (from Old Sagay),
boat destinations route to the port with the boat note.

## Out of scope

Automatic origin-zone detection beyond the five published origins, and
per-route fares for trips whose pair is absent from the matrix (the
distance estimate is used and labelled as such).
