# Release Notes — Sagay Tourist Navigation

## v1.0.0 (versionCode 1) — 2026-11-11

First branded build of the native Android app: a calm-teal tourist navigator for
Sagay City that routes visitors from their location to the correct jeepney
terminal (and boat port for islands), with distance, duration, and fare estimates.

- **Package:** `com.sagay.touristnav`
- **Requires:** Android 9+ (minSdk 28), targets Android 15 (targetSdk 35)
- **Artifact:** `Sagay-Tourist-Navigator-debug.apk` (~28 MB, debug build)

### What's New

- **Branded launcher icon** — adaptive route-and-pin logo on the calm-teal
  background, replacing the default Android icon.
- **Map zoom controls** — stacked + / − buttons on the map's left edge.
- **21 school destinations** added (38 destinations total), sourced from
  OSM/Overpass and the DepEd Division of Sagay City directory.
- **Fare cap** — fares are clamped to a ₱20 ceiling (₱15 first 3 km, +₱2 per
  started km beyond, max ₱20).

### Bug Fixes

- **Saved trips survive updates** — database upgrade (migration 2→3) no longer
  wipes pending trip state.
- **Seed refresh without data loss** — bundled terminals, destinations, and fare
  rule reload once per process; editing `seed.json` now applies on restart
  without clearing app data.
- **Route selection race** — selecting a new destination cancels the previous
  in-flight route fetch so stale results can't overwrite the current one.
- **Rotation stability** — screen rotation no longer resets state or re-triggers
  location permission/locate.
- **Startup race** — pending-trip restoration is guarded until data and
  connectivity are ready.
- **Pending trip updates in place** — re-saving a pending trip updates the same
  row instead of creating duplicates.
- **Clearer offline handling** — online route timeouts surface a retryable error;
  genuine offline failures save the trip and wait for connectivity.
- **Locate Me button** — now recenters immediately and uses the correct teal
  theme color (was unresponsive/orange).

### Improvements

- APK output named `Sagay-Tourist-Navigator` instead of the generic `app-debug`.
- Fare basis line now shows the ₱20 maximum.
- Expanded unit tests: fare cap boundaries, school destinations, and boat flags.

### Notes & Known Limitations

- This is a **debug build**, not a Play Store–signed release.
- Terminal colors/coordinates and school-to-terminal routing are researched from
  public sources and should be verified with the Sagay City Tourism Office /
  LTFRB-franchised operators before public release.
- The fare rule is a working rule pending the exact wording of Sagay City
  Resolution No. 2026-117.
- Island/reef destinations route by road to the boat terminal only; the boat leg
  is not priced.