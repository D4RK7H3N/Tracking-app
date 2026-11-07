# Design: Splash + UI redesign ("Sunrise Route" identity)

Date: 2026-11-07
Status: Approved (visual companion session: splash direction B, theme B, details card V3)
Applies to: `SagayTouristNavigation/` native Android app.

## Decisions

1. **Splash = two stages.** The Android 12+ system splash (solid color + icon)
   blends into a branded Compose launch screen with full design freedom.
   - System splash: sunrise orange `#F4511E` background, white pin icon.
   - Launch screen (~1.6-2.0s, tap to skip): sunrise gradient
     `#FF8F00 → #F4511E → #D84315`, an animated route line drawing itself to a
     dropping pin, app name "Sagay Tourist Navigator", tagline
     "Your ride around Sagay City", then reveals the normal flow. The
     permission flow starts after the launch screen ends.
2. **Theme = "Calm Teal + Sunrise Accents"** (Material 3, light only):
   - Primary teal `#00695C`; sunrise accent `#E65100`/`#FF8F00` reserved for
     the FAB, fare, buttons, and selection highlights.
   - Surface `#F4FAF9` (cool mist), text `#263238`, muted `#78909C`,
     error `#C62828`.
   - Functional colors unchanged by theme: destination markers keep their
     terminal colors; user marker deep blue; route line deep blue `#0277BD`
     (readable on OSM tiles).
3. **Trip details card = "Route Banner" layout (V3):** sunrise-gradient strip
   with `road km · time` left and the peso fare right plus "estimate only";
   below: destination name + category, boat note (teal, when island trip),
   distance warning (red, when sanity check fails), terminal name + color
   chip, fare-basis source line, and an orange "Select new destination"
   button. The standalone straight-line stat is dropped (the warning covers
   the sanity-check case).
4. **Destination sheet:** same layout, theme colors (white search field with
   hairline border, terminal-color dots), 24dp top corners.
5. **Permission prompt:** themed calm-teal card, friendly copy, large buttons;
   behavior unchanged (permission request, denied, settings shortcut).
6. **Locate-me FAB:** orange rounded square, moved to **top-end below the
   status bar** so no bottom sheet can cover it (fixes the layering defect).
7. **Status cards** (Fetching / WaitingForInternet / RouteError) restyled to
   the theme. No logic changes anywhere: state machine, fare, routing,
   database untouched.

## Code changes

- New `ui/Theme.kt`: color tokens + `SagayTheme` (MaterialTheme wrapper).
- New `ui/LaunchScreen.kt`: gradient background, Canvas route-draw animation
  (PathMeasure), pin drop, text fade-in, tap-to-skip, auto-finish callback.
- `MainActivity.kt`: wrap content in `SagayTheme`; gate `MapScreen` behind the
  launch screen (`rememberSaveable` flag).
- `colors.xml` + `themes.xml` + `ic_splash_logo.xml`: system splash orange,
  white pin.
- `MapScreen.kt`: marker/route constants, FAB position + colors.
- `TripDetailsCard.kt`: V3 banner layout; status cards restyled.
- `DestinationSheet.kt`, `PermissionPrompt.kt`: theme restyle.

## Verification

Unit tests untouched and must stay green; `:app:assembleDebug` must succeed.
Device installation happens when the phone is reconnected (not part of this
change).

## Out of scope

Dark theme (paper specifies light theme is enough), booking/payments/tracking
features, logic changes.
