import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart' show Position;
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../models/destination.dart';
import '../models/fare_rule.dart' show Discount;
import '../models/terminal.dart';
import '../services/distance_service.dart' show DistanceService;
import '../services/fare_service.dart';
import '../state/app_state.dart';
import '../widgets/color_chip.dart';
import '../widgets/compass_indicator.dart';
import '../widgets/result_card.dart';

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

    // Fare (null until a distance is available).
    final distance = appState.distanceResult;
    final discount = data.fareRule.firstDiscount;
    final double? fare = distance == null
        ? null
        : _fareService.computeFare(
            rule: data.fareRule,
            distanceKm: distance.km,
            discount: _applyDiscount ? discount : null,
          );

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

          // Fare card with optional discount toggle.
          ResultCard(
            icon: Icons.payments_outlined,
            title: 'Estimated Tricycle Fare',
            child: _FareSection(
              fare: fare,
              discount: discount,
              applyDiscount: _applyDiscount,
              onToggleDiscount: (value) =>
                  setState(() => _applyDiscount = value),
              hasDistance: distance != null,
            ),
          ),

          // Terminal card with the big colour chip.
          ResultCard(
            icon: Icons.directions_bus,
            title: 'Tricycle Terminal',
            child: _TerminalSection(terminal: terminal),
          ),

          // Mini map: user -> destination, plus the terminal.
          Card(
            margin: const EdgeInsets.only(bottom: 12),
            elevation: 1.5,
            clipBehavior: Clip.antiAlias,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
            child: SizedBox(
              height: 230,
              child: _MiniMap(
                appState: appState,
                destination: destination,
                terminal: terminal,
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
    // boat terminal — say so explicitly.
    final selected = appState.selectedDestination;
    final boatTerminalName =
        selected == null ? null : appState.data?.terminalForDestination(selected)?.name;

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

/// Fare block: big whole-peso figure plus the optional discount toggle
/// (hidden entirely when the fare rule has no discounts or no distance yet).
class _FareSection extends StatelessWidget {
  const _FareSection({
    required this.fare,
    required this.discount,
    required this.applyDiscount,
    required this.onToggleDiscount,
    required this.hasDistance,
  });

  final double? fare;
  final Discount? discount;
  final bool applyDiscount;
  final ValueChanged<bool> onToggleDiscount;
  final bool hasDistance;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // Local copy so the compiler can promote the nullable field below.
    final discountEntry = discount;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (fare == null)
          Text(
            hasDistance
                ? 'Calculating…'
                : 'A GPS location is needed to estimate the fare.',
            style: theme.textTheme.bodyMedium,
          )
        else
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                _fareService.formatFare(fare!),
                style: theme.textTheme.displaySmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: theme.colorScheme.primary,
                ),
              ),
              const SizedBox(width: 8),
              Text('estimated fare', style: theme.textTheme.bodyMedium),
            ],
          ),
        // Toggle only when a discount exists AND a fare was computed.
        if (discountEntry != null && fare != null)
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: applyDiscount,
            onChanged: onToggleDiscount,
            title: Text(
              'Apply ${discountEntry.label} '
              '(${discountEntry.percent.round()}% off)',
            ),
          ),
      ],
    );
  }
}

/// Terminal block: name, big colour chip with the colour name, and the
/// list of areas the terminal serves.
class _TerminalSection extends StatelessWidget {
  const _TerminalSection({required this.terminal});

  final Terminal? terminal;

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
        ColorChip(
          color: t.uiColor,
          label: t.colorName.isEmpty ? t.color : t.colorName,
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
  });

  final AppState appState;
  final Destination destination;
  final Terminal? terminal;

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
        TileLayer(
          urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
          userAgentPackageName: 'ph.sagay.sagay_tourist_navigator',
        ),
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
                    Icons.directions_bus,
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
