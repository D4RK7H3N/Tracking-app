# Design: Sagay route corrections from panelist feedback

Date: 2026-11-15
Status: Approved by the user on 2026-11-15
Applies to: `SagayTouristNavigation/` native Android app.

## Feedback and evidence

The panelist's recording asks for vehicle-specific icons, Sagay-local route colors, fares, estimated travel time, bus stops, and useful navigation details. It specifically distinguishes buses, which can have multiple stops, from tricycles, for which the app should not invent fixed stops. The Google Maps footage is a presentation reference, not a requirement to reproduce live public-transport schedules.

Research found the official temporary motorized-tricycle fare measure, Resolution No. 2026-117, and evidence that Old Sagay Port is a local transport and island jump-off point. No authoritative complete Sagay jeepney/tricycle route geometry, timetable, or GTFS feed was found. OpenStreetMap contains intercity bus relations in the area but not a complete local Sagay transit network. Therefore, this change uses owner-supplied terminal and stop information, and it does not fabricate alternatives, schedules, stops, or fares. Owner-supplied stop details remain subject to local verification and must not be described as officially confirmed.

Sources:

- Sagay City temporary fare announcement: https://www.facebook.com/sagaycity/posts/the-city-government-of-sagay-announces-the-implementation-of-new-temporary-fare-/1364698375692456/
- Panay News, temporary tricycle fare approval: https://www.panaynews.net/sagay-sp-approves-temporary-tricycle-fare-hike/
- Digicast Negros, Resolution No. 2026-117 coverage: https://digicastnegros.com/3rd-district-tricycle-drivers-to-receive-aid-sagay-implements-temporary-fare-hike/
- Old Sagay access and port jump-off guide: https://theawkwardtravelerdotcom.wordpress.com/2015/04/08/the-awkward-travelers-guide-to-carbin-reef/

The 2015 guide supports only the Old Sagay access/port relationship. Its historical prices and schedules must not be shown as current information.

## Chosen approach

Make a targeted correction to the existing single-trip road-navigation flow. Finish the in-progress migration from the bus-only `BusRoute` model to the generalized `Route` model, correct the seeded route presentation, and make fare, time, icon, and stop output vehicle-aware.

Do not add a multi-leg itinerary engine or external transit API in this change. The current OSRM polyline remains road-navigation guidance, not a claim that the app knows the exact path traveled by a local bus, tricycle, or boat.

## Route data

Apply the owner-supplied terminal assignments consistently:

| Terminal/service | Display color | Vehicle |
| --- | --- | --- |
| T1 / Poblacion | Red | Tricycle |
| T2 / Vito | Yellow | Reporm 2 motorized tricycle |
| T3 / Old Sagay land routes | White, with a contrasting outline where necessary | Bus |
| Vito and Old Sagay water services | Neutral boat styling, not a tricycle color | Boat |

Correct the seeded conflicts as follows:

- `R-POPLACION`: red tricycle.
- `R-OLD-SAGAY`: white bus; retain its existing owner-supplied ordered stops.
- `R-VITO`: yellow Reporm 2 tricycle; remove its fixed-stop sequence because the panelist explicitly distinguishes tricycle service from scheduled/stop-based bus service.
- `R-BULANON`: white bus; retain its existing owner-supplied ordered stops.
- Water routes: boat service with the appropriate departure port.

The Bato National High School association with T2/Vito is geographically unsupported by the available evidence. Remove its `routeId` and the unsupported `R-BATO` route rather than directing passengers through Vito. Bato retains direct road navigation and shows the generic message that local public-transport details need verification. For every land destination with a null `routeId`, suppress terminal/color/vehicle fallback in destination and trip details; otherwise Bato's legacy terminal field could still incorrectly display Vito guidance. Boat destinations may continue to use their terminal as the departure port. This uses the existing nullable-route field; do not add a route-status subsystem for one unverified record.

Do not change other destination/terminal or fare-matrix mappings without evidence. Preserve the seed note that exact mappings require confirmation with the Sagay City Tourism Office or franchised operators.

## Domain and persistence

Complete the generalized `Route`/`RouteStop` migration already present in the working tree:

- Keep one `RouteStop` declaration and one route entity/DAO family.
- Remove remaining bus-only names and references where they affect this migration.
- Preserve the existing Room migration and bundled-seed refresh behavior. Add only the schema changes required by the generalized route model, without deleting pending trips or other user state.
- Ensure repeated seed refreshes update corrected colors, vehicles, associations, and stops on existing installations.

No itinerary, leg, schedule, or alternative-route classes are required.

## Vehicle-specific UI

Use `VehicleType` to select the icon and labels throughout destination details, trip details, and terminal information:

- Bus: bus icon and bus label.
- Tricycle: tricycle icon and tricycle label.
- Reporm 2: Reporm 2 tricycle icon/artwork and the full local label.
- Boat: boat icon and boat label.

Display color as both a visual swatch/marker and text so white and color-ambiguous services remain understandable. A white route must have sufficient contrast against the map and cards.

Show the ordered stop block only when the selected land route is a bus route with owner-supplied stops. Tricycle and Reporm 2 details instead explain that no fixed stops are listed; do not render their reference points as a fixed-stop sequence. Boat details show the departure port and existing island/boat notice, not a fabricated marine stop sequence or timetable. Where local route or stop details appear, include a concise note that they are owner-supplied and should be confirmed locally before travel; do not label them officially verified.

Use Sagay-specific terminal, barangay, port, and vehicle terminology. Full Hiligaynon interface translation is not part of this fix.

## Fare and estimated time

Scope fare output to the service being described:

- For tricycle and Reporm 2 road journeys, use the official matrix when the detected origin/destination pair exists. Otherwise use the existing distance formula and label it as an estimate.
- Apply the 20% student/senior/PWD discount only to those motorized-tricycle fares.
- For bus journeys, retain road distance/time but show `Fare not verified` rather than applying the tricycle matrix or distance fallback to the bus.
- For island destinations, label any applicable matrix/fallback amount specifically as the tricycle fare to the departure port. Separately show `Boat fare not verified`; never present the land amount as the total island-trip price.
- Do not calculate a total fare by combining a verified land fare with an unknown bus or boat fare.

Label OSRM duration as estimated road-travel time. For island destinations, label it as estimated road time to the port and do not add an invented boat duration. Do not display live departures, arrivals, or schedule-based total trip times.

When offline or when routing fails, retain the existing pending-trip/error behavior. Cached fare and duration values must retain their labels and scope; unavailable values remain unavailable rather than falling back to a different vehicle's fare.

## Map behavior

Keep the direct OSRM road polyline and existing Locate Me/navigation behavior. Label the line and metrics as road guidance so users do not confuse them with an exact public-transport path.

Existing owner-supplied bus-stop markers may remain visible as informational markers, separate from the direct road line and covered by the local-confirmation note. Do not draw tricycle reference points as fixed passenger stops and do not draw an invented boat path. Selecting a destination must not produce alternative transit lines that lack supporting data.

## Error and edge cases

- No selected route or an unverified association, including Bato: direct road guidance remains available; local public-transport details show as needing verification.
- Bus route without stops: show route/vehicle information without an empty stop section.
- Boat destination: show port guidance and separate land/boat fare and time scopes.
- White route: preserve text labels and contrasting outlines.
- Offline island selection: preserve pending road-to-port routing and boat notice without claiming the boat is booked or scheduled.
- Existing database upgrade: preserve pending trips while refreshing corrected bundled route data.

## Verification

1. Run `git diff --check`, the debug unit tests, and `assembleDebug` with the working JDK 17 environment.
2. Add/update focused tests for route-to-terminal color/vehicle consistency, the removed Bato/Vito association, bus-only fixed-stop presentation rules, tricycle/Reporm 2 official and fallback fares, discount scoping, bus fare unavailability, and island land-fare versus boat-fare labels.
3. Verify that seed refresh updates an existing installation without losing pending-trip state.
4. Install on the connected Vivo phone and inspect Poblacion, Vito, Old Sagay, Bulanon, Bato, and one island destination.
5. Confirm correct icons, readable colors, bus-only stops, estimated-time wording, fare labels, boat notices, and direct road navigation.
6. Repeat representative checks offline and after reconnecting. Inspect Android logs for crashes.
7. Confirm that no schedule, alternative itinerary, fixed tricycle stop, boat duration, or unverified fare is presented as fact.

## Non-goals

- Multi-leg itineraries, transfers, alternative public-transport routes, or Google Maps-style live schedules.
- External transit APIs, GTFS infrastructure, new API keys, or paid services.
- New routes, stops, terminal mappings, fares, or timetables without reliable evidence.
- Full Hiligaynon translation or an unrelated visual redesign.
- Discarding or reverting unrelated uncommitted work.
