import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart' show Position;
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';

import '../models/destination.dart';
import '../models/fare_origin.dart' show detectFareOrigin;
import '../models/fare_scope.dart';
import '../models/journey.dart' show JourneyPlan;
import '../models/route.dart';
import '../models/fare_rule.dart' show Discount;
import '../models/terminal.dart';
import '../services/distance_service.dart' show DistanceResult, DistanceService;
import '../services/fare_service.dart';
import '../services/haversine.dart' show haversineKm, isRoadDistanceSuspicious;
import '../services/map_tiles.dart' show TileSource;
import '../services/trip_store.dart';
import '../state/app_state.dart';
import '../theme/motion.dart';
import '../widgets/color_chip.dart';
import '../widgets/compass_indicator.dart';
import '../widgets/map_tile_layer.dart';
import '../widgets/result_card.dart';
import 'guidance_screen.dart';

const FareService _fareService = FareService();

/// Result screen for the selected destination: distance, estimated fare,
/// the colour-coded tricycle terminal, and a mini map.
class ResultScreen extends StatefulWidget {
  const ResultScreen({super.key});

  @override
  State<ResultScreen> createState() => _ResultScreenState();
}

class _ResultScreenState extends State<ResultScreen> {
  /// Optional discount from the fare rule, toggled by the user.
  bool _applyDiscount = false;
  int _passengers = 1;

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final destination = appState.selectedDestination;
    final data = appState.data;

    if (destination == null || data == null) {
      return Scaffold(
        appBar: AppBar(),
        body: const Center(child: Text('No destination selected.')),
      );
    }

    final terminal = data.terminalForDestination(destination);
    final theme = Theme.of(context);
    final position = appState.location.position;

    // The origin decides which matrix row and which access rule apply.
    final origin = position == null
        ? null
        : detectFareOrigin(position.latitude, position.longitude, data.fareOrigins);
    final journey = data.journeyFor(origin?.zone ?? '', destination);

    // Fare (null until a distance is available). Scoping decides whether the
    // amount is a tricycle fare, an island land leg, or not verified at all.
    final distance = appState.distanceResult;
    final FareQuote? quote = distance == null
        ? null
        : _fareService.quote(
            data: data,
            destination: destination,
            roadKm: distance.km,
            discounted: _applyDiscount,
            originZone: origin?.zone,
          );

    String shareText(FareQuote result) {
      final scope = result.scope;
      final priceable = scope is MotorizedTricycleFare || scope is IslandFare;
      final serviceLabels = journey.services
          .map((service) => service.serviceLabel)
          .join(' + ');
      return <String>[
        'Sagay Tourist Navigator',
        'Destination: ${destination.name}',
        'Service: ${serviceLabels.isEmpty ? 'not published' : serviceLabels}',
        if (journey.changeAt != null) 'Change at: ${journey.changeAt}',
        if (priceable)
          '${scope is IslandFare ? 'Tricycle fare to the port' : 'Fare per passenger'}: '
              '${_fareService.formatFare(scope.amount)}'
        else
          'Fare per passenger: Not verified',
        if (scope is IslandFare) 'Boat fare not verified',
        if (scope.canDiscount && _passengers > 1) ...[
          'Passengers: $_passengers',
          'Group total: ${_fareService.formatFare(
                _fareService.groupTotal(scope.amount, _passengers),
              )}',
        ],
        'Estimate only. Actual fares may vary.',
      ].join('\n');
    }

    void startGuidance() {
      final routePoints = appState.distanceResult?.routePoints ?? const <LatLng>[];
      if (routePoints.isEmpty) return;
      final changeName = journey.changeAt;
      RouteStop? changeStop;
      if (changeName != null) {
        for (final service in journey.services) {
          for (final stop in service.stops) {
            if (stop.name == changeName) {
              changeStop = stop;
              break;
            }
          }
        }
      }
      Navigator.of(context).push(
        SagayPageRoute<void>(
          context: context,
          child: GuidanceScreen(
            destinationName: destination.name,
            points: routePoints,
            changeAtName: changeName,
            changeAtLat: changeStop?.lat,
            changeAtLng: changeStop?.lng,
          ),
        ),
      );
    }

    void saveTrip() async {
      final current = quote;
      if (current == null) return;
      final messenger = ScaffoldMessenger.of(context);
      // Do not put an unverified amount into trip history as if it were a
      // published fare (mirrors the native app's zero-value unverified fare).
      if (current.scope is UnverifiedFare) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Fare not verified; trip not saved.')),
        );
        return;
      }
      final result = current.estimate;
      final now = DateTime.now();
      await context.read<TripStore>().addTrip(
            TripRecord(
              id: now.microsecondsSinceEpoch.toString(),
              destinationId: destination.id,
              destinationName: destination.name,
              originZone: origin?.zone ?? '',
              vehicleLabel: journey.services
                  .map((service) => service.serviceLabel)
                  .join(' + '),
              fare: result.amount,
              discounted: result.discounted,
              passengers: _passengers,
              total: _fareService.groupTotal(result.amount, _passengers),
              savedAt: now,
            ),
          );
      if (mounted) {
        messenger.showSnackBar(
          const SnackBar(content: Text('Trip saved to history')),
        );
      }
    }

    return Scaffold(
      appBar: AppBar(title: Text(destination.name)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: Chip(
              avatar: const Icon(Icons.category, size: 16),
              label: Text(destination.category),
              visualDensity: VisualDensity.compact,
            ),
          ),
          Text(
            destination.name,
            style: theme.textTheme.headlineSmall
                ?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),

          // Distance card with loading / error / success states.
          ResultCard(
            icon: Icons.near_me,
            title: 'Distance',
            child: _DistanceSection(appState: appState),
          ),

          // Fare card: title and contents depend on the vehicle scope.
          ResultCard(
            icon: Icons.payments_outlined,
            title: switch (quote?.scope) {
              IslandFare() => 'Island Fare',
              UnverifiedFare() => 'Fare',
              _ => 'Estimated Tricycle Fare',
            },
            child: _FareSection(
              quote: quote,
              onSaveTrip: saveTrip,
              onStartGuidance: startGuidance,
              passengers: _passengers,
              onChangePassengers: (value) =>
                  setState(() => _passengers = value.clamp(1, 20)),
              onShare: (result) => SharePlus.instance.share(
                    ShareParams(text: shareText(result), title: 'Trip summary'),
                  ),
              applyDiscount: _applyDiscount,
              onToggleDiscount: (value) =>
                  setState(() => _applyDiscount = value),
              hasDistance: distance != null,
            ),
          ),

          // Two-service corridors (e.g. Old Sagay -> Poblacion) are shown as
          // a compact service row with the change point.
          if (journey.services.length > 1)
            ResultCard(
              icon: Icons.route,
              title: 'Two-service journey',
              child: _ServiceRow(journey: journey),
            ),

          // Terminal card with the big colour chip.
          ResultCard(
            icon: Icons.place,
            title: 'Tricycle Terminal',
            child: _TerminalSection(
              terminal: terminal,
              destinationId: destination.id,
            ),
          ),

          // Mini map: user -> destination, plus the terminal.
          Card(
            margin: const EdgeInsets.only(bottom: 12),
            elevation: 1.5,
            clipBehavior: Clip.antiAlias,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            child: SizedBox(
              height: 230,
              child: _MiniMap(
                appState: appState,
                destination: destination,
                terminal: terminal,
                tiles: appState.mapTiles.source,
              ),
            ),
          ),

          const SizedBox(height: 4),
          Text(
            'Estimate only. The actual fare may vary.',
            textAlign: TextAlign.center,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

/// Distance block: spinner while fetching, error + retry on failure,
/// distance in km (1 decimal) with the "Estimated (offline)" label when the
/// value came from the straight-line fallback instead of a road route.
class _DistanceSection extends StatelessWidget {
  const _DistanceSection({required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final state = appState.distanceState;

    if (state == DistanceLoadState.loading) {
      return const Row(
        children: [
          SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(strokeWidth: 2.5),
          ),
          SizedBox(width: 12),
          Text('Calculating road distance…'),
        ],
      );
    }

    if (state == DistanceLoadState.error) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.error_outline, size: 20, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  appState.distanceError ?? 'Distance is unavailable.',
                  style: const TextStyle(fontSize: 14),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          FilledButton.tonalIcon(
            onPressed: appState.retryDistance,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
          ),
        ],
      );
    }

    final result = appState.distanceResult;
    if (result == null) {
      return const Text('—');
    }

    // For sea destinations the km value covers only the road leg to the
    // boat terminal — say so explicitly. The same road endpoint is used for
    // the straight-line sanity check (the offshore point would false-alarm).
    final selected = appState.selectedDestination;
    final terminal = selected == null
        ? null
        : appState.data?.terminalForDestination(selected);
    final boatTerminalName = terminal?.name;
    final position = appState.location.position;
    var suspicious = false;
    if (position != null && selected != null) {
      final roadEnd = selected.boat && terminal != null
          ? LatLng(terminal.lat, terminal.lng)
          : LatLng(selected.lat, selected.lng);
      final straightKm = haversineKm(
        position.latitude,
        position.longitude,
        roadEnd.latitude,
        roadEnd.longitude,
      );
      suspicious = isRoadDistanceSuspicious(result.km, straightKm);
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Text(
              result.km.toStringAsFixed(1),
              style: theme.textTheme.displaySmall
                  ?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(width: 6),
            Text('km', style: theme.textTheme.titleMedium),
          ],
        ),
        if (!result.isRoadRoute) const _OfflineLabel(),
        if (result.durationSeconds != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              _travelTimeLabel(result),
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        // Display-only sanity warning: the fare is never recalculated here.
        if (suspicious)
          Container(
            margin: const EdgeInsets.only(top: 8),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: theme.colorScheme.errorContainer,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.warning_amber_rounded,
                    size: 16, color: theme.colorScheme.error),
                const SizedBox(width: 6),
                Text(
                  'Distance looks unusual, please verify',
                  style: TextStyle(
                    color: theme.colorScheme.error,
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        if (selected?.boat == true)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              'Tricycle to ${boatTerminalName ?? "the boat terminal"}, then a '
              'boat ride to ${selected!.name}.',
              style: theme.textTheme.bodySmall,
            ),
          ),
      ],
    );
  }
}

/// Travel-time line for a road route (OSRM duration) or the offline estimate.
String _travelTimeLabel(DistanceResult result) {
  final seconds = result.durationSeconds!;
  final estimatePrefix = result.isRoadRoute ? '' : 'Estimated ';
  if (seconds < 60) return '${estimatePrefix}under 1 min travel time';
  final minutes = (seconds / 60).round();
  if (minutes < 60) return '$estimatePrefix$minutes min travel time';
  final hours = seconds ~/ 3600;
  final rest = ((seconds % 3600) / 60).round();
  return '$estimatePrefix$hours h $rest min travel time';
}

/// Visible "Estimated (offline)" badge for straight-line fallback distances.
class _OfflineLabel extends StatelessWidget {
  const _OfflineLabel();

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.orange.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.wifi_off, size: 16),
          SizedBox(width: 6),
          Text(
            'Estimated (offline)',
            style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
          ),
        ],
      ),
    );
  }
}

/// Fare block: headline, vehicle-scoped explanation, and the 20% discount
/// toggle only when the amount is an official tricycle matrix fare.
class _FareSection extends StatelessWidget {
  const _FareSection({
    required this.quote,
    required this.onSaveTrip,
    required this.onStartGuidance,
    required this.passengers,
    required this.onChangePassengers,
    required this.onShare,
    required this.applyDiscount,
    required this.onToggleDiscount,
    required this.hasDistance,
  });

  final FareQuote? quote;
  final VoidCallback onSaveTrip;
  final VoidCallback onStartGuidance;
  final int passengers;
  final ValueChanged<int> onChangePassengers;
  final ValueChanged<FareQuote> onShare;
  final bool applyDiscount;
  final ValueChanged<bool> onToggleDiscount;
  final bool hasDistance;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final current = quote;
    final scope = current?.scope;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (current == null || scope == null)
          Text(
            hasDistance
                ? 'Calculating…'
                : 'A GPS location is needed to estimate the fare.',
            style: theme.textTheme.bodyMedium,
          )
        else ...[
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Flexible(
                child: AnimatedSwitcher(
                  duration: Motion.duration(context, Motion.layout),
                  transitionBuilder: (child, animation) => FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: Tween<Offset>(
                        begin: const Offset(0, 0.25),
                        end: Offset.zero,
                      ).animate(animation),
                      child: child,
                    ),
                  ),
                  child: Text(
                    scope.headline(_fareService.formatFare),
                    key: ValueKey(
                      '${current.estimate.amount}-${current.estimate.discounted}-${scope.label}',
                    ),
                    style: theme.textTheme.displaySmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: scope is UnverifiedFare
                          ? theme.colorScheme.error
                          : theme.colorScheme.primary,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  scope.label,
                  style: theme.textTheme.bodyMedium,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            scope.basis,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          // Native parity: only an official matrix fare can be discounted.
          if (scope.canDiscount)
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: applyDiscount,
              onChanged: onToggleDiscount,
              title: Text(
                'Apply ${Discount.matrix.label} '
                '(${Discount.matrix.percent.round()}% off)',
              ),
            ),
          if (scope.canDiscount) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('Passengers'),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.remove),
                  tooltip: 'Fewer passengers',
                  onPressed: () => onChangePassengers(passengers - 1),
                ),
                Text('$passengers',
                    style: Theme.of(context).textTheme.titleMedium),
                IconButton(
                  icon: const Icon(Icons.add),
                  tooltip: 'More passengers',
                  onPressed: () => onChangePassengers(passengers + 1),
                ),
              ],
            ),
            if (passengers > 1)
              Text(
                'Group total: ${_fareService.formatFare(_fareService.groupTotal(scope.amount, passengers))}',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: Theme.of(context).colorScheme.primary,
                    ),
              ),
          ],
          const SizedBox(height: 6),
          FilledButton.icon(
            onPressed: onStartGuidance,
            icon: const Icon(Icons.navigation),
            label: const Text('Start journey guidance'),
          ),
          const SizedBox(height: 6),
          OutlinedButton.icon(
            onPressed: onSaveTrip,
            icon: const Icon(Icons.bookmark_add_outlined),
            label: const Text('Save to trip history'),
          ),
          const SizedBox(height: 6),
          OutlinedButton.icon(
            onPressed: () => onShare(current),
            icon: const Icon(Icons.share),
            label: const Text('Share trip summary'),
          ),
        ],
      ],
    );
  }
}

/// Compact two-service row: one badge per service plus the change point.
class _ServiceRow extends StatelessWidget {
  const _ServiceRow({required this.journey});

  final JourneyPlan journey;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 6,
          children: [
            for (final service in journey.services)
              Chip(
                avatar: CircleAvatar(
                  backgroundColor: service.uiColor,
                  radius: 6,
                ),
                label: Text(service.serviceLabel),
                visualDensity: VisualDensity.compact,
              ),
          ],
        ),
        if (journey.changeAt != null) ...[
          const SizedBox(height: 8),
          Row(
            children: [
              Icon(Icons.swap_horiz, size: 18, color: theme.colorScheme.primary),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  'Change at ${journey.changeAt}',
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ],
      ],
    );
  }
}

/// Terminal block: name, big colour chip with the colour name, and the
/// list of areas the terminal serves.
class _TerminalSection extends StatelessWidget {
  const _TerminalSection({required this.terminal, required this.destinationId});

  final Terminal? terminal;
  final String destinationId;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    if (terminal == null) {
      return const Text(
        'Terminal information for this destination is missing from the '
        'city data.',
      );
    }
    final t = terminal!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(t.name, style: theme.textTheme.titleLarge),
        const SizedBox(height: 12),
        Hero(
          tag: 'destination-color-$destinationId',
          child: ColorChip(
            color: t.uiColor,
            label: t.colorName.isEmpty ? t.color : t.colorName,
          ),
        ),
        const SizedBox(height: 12),
        const Row(
          children: [
            Icon(Icons.map, size: 16),
            SizedBox(width: 6),
            Text('Serves:', style: TextStyle(fontWeight: FontWeight.w600)),
          ],
        ),
        const SizedBox(height: 6),
        Wrap(
          spacing: 8,
          runSpacing: 4,
          children: t.areas
              .map(
                (area) => Chip(
                  label: Text(area, style: const TextStyle(fontSize: 12)),
                  visualDensity: VisualDensity.compact,
                ),
              )
              .toList(),
        ),
      ],
    );
  }
}

/// Small static map showing the user, the destination and the terminal,
/// with a line from the user to the destination. The camera fits all
/// available points.
class _MiniMap extends StatelessWidget {
  const _MiniMap({
    required this.appState,
    required this.destination,
    required this.terminal,
    required this.tiles,
  });

  final AppState appState;
  final Destination destination;
  final Terminal? terminal;
  final ValueNotifier<TileSource> tiles;

  @override
  Widget build(BuildContext context) {
    final Position? user = appState.location.position;
    final destPoint = LatLng(destination.lat, destination.lng);
    final terminalPoint =
        terminal == null ? null : LatLng(terminal!.lat, terminal!.lng);
    final userPoint =
        user == null ? null : LatLng(user.latitude, user.longitude);

    final points = <LatLng>[
      destPoint,
      ?terminalPoint,
      ?userPoint,
    ];

    // Route line: real road geometry from the routing API when available,
    // straight line otherwise.
    final routeFromApi =
        appState.distanceResult?.routePoints ?? const <LatLng>[];
    // Offline fallback: straight line to the boat terminal (sea spots) or
    // to the destination itself.
    final fallbackTarget = terminalPoint ?? destPoint;
    final polylinePoints = routeFromApi.isNotEmpty
        ? routeFromApi
        : <LatLng>[?userPoint, fallbackTarget];

    // Sea destinations: dashed line for the boat leg (terminal -> spot).
    final boatLeg = destination.boat && terminalPoint != null;

    // Fit the camera around markers AND the full route line.
    final fitPoints = <LatLng>{...points, ...polylinePoints}.toList();

    final MapOptions options;
    if (fitPoints.length >= 2) {
      options = MapOptions(
        initialCameraFit: CameraFit.bounds(
          bounds: LatLngBounds.fromPoints(fitPoints),
          padding: const EdgeInsets.all(40),
        ),
      );
    } else {
      options = MapOptions(initialCenter: destPoint, initialZoom: 15);
    }

    final terminalColor = terminal?.uiColor ?? Colors.grey;

    return Stack(
      children: [
        FlutterMap(
          options: options,
          children: [
        MapTileLayer(tiles: tiles),
        if (userPoint != null)
          PolylineLayer(
            polylines: [
              Polyline(
                // Follow the actual road route when we have one (online);
                // otherwise fall back to a straight line.
                points: polylinePoints,
                strokeWidth: 4,
                color: Colors.blue,
              ),
              // Boat leg over the sea, drawn dashed so it is never mistaken
              // for a road.
              if (boatLeg)
                Polyline(
                  points: [terminalPoint, destPoint],
                  strokeWidth: 4,
                  color: const Color(0xFF00838F),
                  pattern: StrokePattern.dashed(segments: [6, 6]),
                ),
            ],
          ),
        MarkerLayer(
          markers: [
            if (userPoint != null)
              Marker(
                point: userPoint,
                width: 34,
                height: 34,
                child: Transform.rotate(
                  // Point the arrow towards the destination (map is
                  // north-up, so rotate by the geographic bearing).
                  angle: DistanceService.bearingDeg(userPoint, destPoint) *
                      math.pi /
                      180.0,
                  child: const Icon(
                    Icons.navigation,
                    color: Color(0xFF1565C0),
                    size: 32,
                    shadows: [Shadow(color: Colors.black38, blurRadius: 4)],
                  ),
                ),
              ),
            Marker(
              point: destPoint,
              width: 34,
              height: 34,
              child: Icon(
                Icons.location_on,
                color: terminalColor,
                size: 34,
                shadows: const [Shadow(color: Colors.black38, blurRadius: 4)],
              ),
            ),
            if (terminalPoint != null)
              Marker(
                point: terminalPoint,
                width: 34,
                height: 34,
                child: CircleAvatar(
                  backgroundColor: terminalColor,
                  child: const Icon(
                    Icons.place,
                    color: Colors.white,
                    size: 18,
                  ),
                ),
              ),
          ],
        ),
          ],
        ),
        // North indicator (the map is locked north-up).
        const Positioned(
          top: 8,
          left: 8,
          child: CompassIndicator(size: 36),
        ),
      ],
    );
  }
}
