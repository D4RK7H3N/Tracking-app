# Native app bug fixes and launcher logo design

## Scope

Fix seven verified defects in the existing Kotlin/Compose app and replace Android's default launcher icon with the existing Sunrise Route brand. Preserve all unrelated uncommitted work and do not change the fare formula, destination data, visual theme, or app architecture.

## Data integrity and seeding

- Add a Room `MIGRATION_2_3` that executes `ALTER TABLE fare_rule ADD COLUMN maxFare REAL` and register it with the database builder. Existing `trip_state` rows must survive the 2-to-3 upgrade. Keep destructive fallback only for versions without an explicit migration.
- Change seed inserts to upsert bundled terminals, destinations, and the fare rule so an upgraded database receives `maxFare` and other current static values without deleting trips.
- Make `SeedLoader` serialize its once-per-process seed/refresh operation with a `Mutex`. The application can start it in the background, while the view model awaits the same operation before its initial destination and terminal reads. A fast GPS flow must never cache an empty database.
- Update the existing seed documentation to reflect automatic bundled-data refresh rather than requiring an uninstall.

## Trip and route state

- Use one cancellable route job for destination selection, GPS refresh, and network fetching. Selecting another destination cancels the previous job, so stale GPS results, route results, and errors cannot overwrite the newer selection. Cancellation exceptions must propagate rather than being converted into UI errors.
- Convert route fetching into a suspending operation called inside the owning coroutine. Retry, connectivity restoration, and pending-trip restoration start a new job only when no route job is active.
- Save pending trips with an upsert. When an offline selection changes, update the existing row's origin, destination, and timestamp as well as the corresponding in-memory fields. A restart must restore the latest selection rather than the first offline selection.
- Track the latest connectivity value. After the initial pending-row read, after location/data initialization, and on connectivity changes, call one guarded restoration path that resumes only when the pending data, map data, and online state are ready. This removes the startup race without introducing a second state machine.
- Gate permission-driven initial location acquisition so recomposing or rotating an already initialized screen does not reset route state. Explicit GPS retry and Locate Me retain their current behavior.

## Network error handling

- Treat `IOException` as an offline pending trip only when the latest connectivity state is offline.
- When connectivity remains online, show the existing `RouteError` card with its Retry action for timeouts, DNS failures, and other request failures. Do not claim the trip will retry on reconnect when no reconnect is required.
- Preserve automatic retry for genuinely offline trips when connectivity returns.

## Launcher logo

- Add adaptive launcher and round-icon resources using a calm-teal background and a vector rendering of the existing Sunrise Route route-and-pin artwork with the sunrise-yellow accent.
- Reference those resources from `android:icon` and `android:roundIcon` in the manifest.
- Keep the animated launch screen, splash theme, app name, and main map layout unchanged. No new bitmap artwork or image-generation dependency is needed.
- Change the Locate Me button from the leftover orange accent to calm teal. Every tap recenters immediately on the current position, while the existing background GPS refresh still runs; an unchanged GPS fix must not make the button appear unresponsive.

## Verification

1. Run `git diff --check`, the existing debug unit tests, and `assembleDebug` with the working JDK 17 environment.
2. Install the rebuilt APK on the connected Vivo V2344.
3. Verify launch, GPS/map/destination loading, and the branded launcher icon.
4. Exercise destination switching, rotation, offline pending-trip replacement, reconnect recovery, and online route failure/Retry.
5. Use the backed-up version-2 database to verify that upgrading preserves its pending trip and adds the fare cap.
6. Inspect `AndroidRuntime` logs for crashes. Report any external OSRM/network limitation separately from app defects.

## Non-goals

- No reactive-architecture rewrite, new navigation flow, account/backend work, or unrelated UI redesign.
- No new test libraries or production dependencies solely for these fixes.
- No commit unless separately requested by the user.
