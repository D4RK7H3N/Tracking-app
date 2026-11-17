# Design: Old Sagay to Sagay City journey and scoped discount fares

Date: 2026-11-17
Status: Approved by the user on 2026-11-17
Applies to: `SagayTouristNavigation/` native Android app.
Supersedes: the fare section of `2026-11-15-sagay-route-feedback-design.md` (the
route-colour table in that document was already superseded by the owner's later
Red / Pink / Yellow / White direction).

## Problem

Three defects were found while checking the installed app on the owner's Vivo
phone with the owner standing in the Old Sagay area.

**1. The Old Sagay to Sagay City journey was shown as a single service.** The
destination list and the trip card name only the route that serves the
destination, so a passenger in Old Sagay heading for Sagay National High School
was told "Red Tricycle" and nothing else. That is wrong: the owner-supplied
service area of the Old Sagay Port Terminal reads "Old Sagay to Sagay City,
Sagay City to Old Sagay", and the Yellow route's first stop is "Sagay City Bus
Terminal (Poblacion)". The journey is Yellow from Old Sagay into the city, then
Red to the destination.

**2. The fare fell back to the distance estimate.** The published matrix holds
`CITY_PROPER -> BRGY. OLD SAGAY = 20 / 16` but no row for the Old Sagay
origin table leading into Sagay City, so the lookup missed and the trip was
priced with the distance formula: 7.4 km became an estimated ₱25. The owner
states the Old Sagay to Sagay City fare is ₱15-20, and chose a flat ₱20
(₱16 discounted) for every Sagay City destination.

**3. The discount toggle did nothing on an estimated fare.** The trip card
offers "Senior citizen / Student / PWD" for any tricycle fare, including the
distance estimate, but the estimate is deliberately not discounted because no
discounted estimate is published. On the phone the box was ticked while the
price stayed at ₱25, and its label still claimed "20% discount (matrix fare)".
Separately, discounted matrix fares were rounded to whole pesos, so the
published ₱13.60 was shown as ₱14.

## Decisions

The owner confirmed all four points.

| Question | Decision |
| --- | --- |
| What the card shows for Old Sagay to Sagay City | Both services: Yellow then Red, with a change point |
| How the fare is presented | One fare for the whole journey |
| Amount for the new rows | Flat ₱20 regular, ₱16 discounted, for every Sagay City destination |
| Discount fixes | Hide the toggle on distance estimates, and print published amounts exactly |

## Route data

### The access rule

The corridor rule lives in the bundled seed, not in Kotlin. The codebase
currently hard-codes no route, terminal or zone identifier, and the owner edits
`seed.json` directly; keeping the rule in data preserves that.

A new seed array `route_access` states, for an origin zone and a destination
route, which route is ridden first and where the passenger changes:

```json
"route_access": [{
  "originZone": "OLD_SAGAY",
  "destinationRouteId": "R-POPLACION",
  "viaRouteId": "R-OLD-SAGAY",
  "changeAt": "Sagay City Bus Terminal (Poblacion)",
  "fareZone": "SAGAY CITY"
}]
```

The rule fires only when the passenger's detected fare origin zone is
`OLD_SAGAY` **and** the destination's route is `R-POPLACION`. Every other
origin/destination pair keeps today's behaviour. The `fareZone` field names the
matrix destination to price the journey against, so the whole journey is priced
as one corridor rather than as two legs added together.

### The fare matrix row

The missing published row is added:

```json
{"origin": "OLD_SAGAY", "destination": "SAGAY CITY", "regular": 20, "discounted": 16}
```

No other matrix row changes. The City Proper table keeps its own values for the
same destinations, because a passenger already in the city pays the City Proper
fare.

## Domain and persistence

### New model

`RouteAccess` (domain) mirrors the entity:

| Field | Meaning |
| --- | --- |
| `originZone` | The fare origin zone the rule applies from |
| `destinationRouteId` | The route that serves the destination |
| `viaRouteId` | The route ridden first |
| `changeAt` | Where the passenger changes service |
| `fareZone` | The matrix destination used to price the journey |

### Persistence

Room goes from version 7 to 8 with a new `route_access` table:

```sql
CREATE TABLE IF NOT EXISTS `route_access` (
  `originZone` TEXT NOT NULL,
  `destinationRouteId` TEXT NOT NULL,
  `viaRouteId` TEXT NOT NULL,
  `changeAt` TEXT NOT NULL,
  `fareZone` TEXT NOT NULL,
  PRIMARY KEY(`originZone`, `destinationRouteId`))
```

The bundled seed is the only source of truth, so the migration creates the
table and the seed loader re-inserts its rows; nothing is copied from a legacy
table. Existing installations keep their pending trips.

The new table needs a `RouteAccessDao`, a `RouteAccessEntity`, a
`SeedRouteAccess` parser entry, a `toDomain` mapper, and a `routeAccess()`
accessor on `TripRepository`, which passes the list into the estimator.

### The trip estimate

`TripEstimate.route: Route?` becomes `TripEstimate.services: List<Route>`,
listed in the order the passenger boards them.

- No known route: `services` is empty.
- Ordinary trip: `services` holds the destination's route.
- Old Sagay to Sagay City: `services` holds the Yellow route then the Red
  route.

`TripEstimate.route` stays as a derived `services.lastOrNull()` so the
destination list, the map and the trip card keep their current meaning: the
route that serves the destination is the last service of the journey.

`TripEstimate.changeAt: String?` carries the change point, and is null for a
journey that needs no change. `TripEstimate.stopService: Route?` returns the
first service that has owner-supplied stops, and is null when no service has
any; this is what the map draws. For an ordinary trip it is the destination's
route, and for the corridor journey it is the Yellow route whose stops include
the change point.

### The estimator

`TripEstimator.estimate` takes the new `routeAccess` list. It resolves the
rule from the detected origin zone and the destination's route id. When the
rule fires:

- `services` becomes the via route followed by the destination's route;
- `changeAt` is set;
- the fare is looked up against the rule's `fareZone` instead of the
  destination's own fare zone.

The rest of the fare scoping is unchanged: the published matrix applies to
tricycle and Reporm 2 rides, a bus ride carries no fare, and an island trip
prices only the tricycle leg to the port.

## UI

### The trip card

With one service the card is unchanged: the coloured badge, the service label,
the terminal name and the terminal's service area.

With two services the card shows both badges joined by an arrow, each with its
own terminal name, and a line naming the change point:

```
[Yellow] Yellow Tricycle  ->  [Red] Red Tricycle
Old Sagay Port Terminal      Sagay Public Market Terminal (Poblacion)
Change at Sagay City Bus Terminal (Poblacion)
```

The stop block is rendered for the first service that has owner-supplied
stops, which in the corridor journey is the Yellow route, so its stops, which
include the change point, are listed. A service with no stops contributes no
stop block.

### The map

The stop markers and their colour come from `stopService` instead of the
destination's route, so the Old Sagay corridor journey shows the Yellow route's
stops in yellow.

### The destination list

Unchanged. The list names the service that serves each destination and does
not depend on where the passenger is standing.

## Fare and discount

The discount toggle is offered only when the fare is an official matrix fare.
A tricycle fare whose basis is the distance estimate, and an island trip whose
land leg is an estimate, keep their single value and show no toggle, because no
discounted estimate is published. The existing rule that the discount applies
to matrix fares only is unchanged; only the offer is now truthful.

Published amounts are printed exactly. Whole peso amounts print without
decimals (₱20, ₱16) and amounts with centavos print with them (₱13.60, not
₱14), in the banner headline and anywhere else a fare amount is shown.

## Error and edge cases

- Passenger outside every fare origin zone: no access rule fires, the journey
  is a single service and the fare falls back to the distance estimate.
- Passenger in Old Sagay going to a destination the Yellow route serves
  (Old Sagay Elementary School, Margaha Beach): one service, no change point.
- Passenger in Old Sagay going to a Vito or Bulanon destination: no access rule
  fires. The Old Sagay Port Terminal boards the Bulanon bus directly, so no
  change is invented.
- Access rule whose via route id is not in the seed: the rule is ignored and
  the journey falls back to the destination's route alone.
- Access rule with a missing matrix row for its `fareZone`: the journey still
  shows both services and the change point, and the fare falls back to the
  distance estimate.
- Destination with no route: unchanged, no service is claimed.
- Discount toggle left checked from an earlier trip, then a trip with an
  estimated fare is selected: the estimate is not discounted and no toggle is
  shown; the preference is kept for the next matrix fare.

## Verification

1. Run `git diff --check`, `:app:testDebugUnitTest` and `:app:assembleDebug`
   with the pinned JDK 21.
2. Add tests for: the access rule resolving to Yellow then Red with the change
   point; the journey fare being the ₱20 / ₱16 matrix row; no access rule
   outside Old Sagay; an unknown via route id being ignored; the discount
   toggle being offered only for official matrix fares; exact peso formatting.
3. Verify the seed refresh on an existing installation keeps pending trips and
   picks up the new table.
4. Install on the connected Vivo phone and check, standing in Old Sagay: the
   Yellow then Red journey to Sagay National High School, the ₱20 fare, the
   change point, the discounted ₱16, and a bus destination showing "Not
   verified".
5. Check an island destination shows the tricycle-to-port fare and "Boat fare
   not verified", and that a distance estimate shows no discount toggle.

## Non-goals

- A general multi-leg itinerary engine. The journey is exactly the two
  services the owner has confirmed, and no other corridor is modelled.
- New routes, terminals or stops.
- Changing the City Proper fare table or any other origin table.
- Discounting the distance estimate.
- Totalling a land fare with an unpublished bus or boat fare.
