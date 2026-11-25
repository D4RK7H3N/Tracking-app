import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../services/guidance.dart';
import '../state/app_state.dart';
import '../widgets/map_tile_layer.dart';

/// Live turn-by-turn guidance to a destination.
///
/// Subscribes to the geolocator position stream, recomputing the remaining
/// distance, progress and next prompt on every fix. When no road route was
/// recorded (offline fallback) guidance is disabled and the reason is shown
/// explicitly instead of presenting a straight-line "route".
class GuidanceScreen extends StatefulWidget {
  const GuidanceScreen({
    super.key,
    required this.destinationName,
    required this.points,
    this.changeAtName,
    this.changeAtLat,
    this.changeAtLng,
  });

  final String destinationName;
  final List<LatLng> points;
  final String? changeAtName;
  final double? changeAtLat;
  final double? changeAtLng;

  @override
  State<GuidanceScreen> createState() => _GuidanceScreenState();
}

class _GuidanceScreenState extends State<GuidanceScreen> {
  StreamSubscription<Position>? _subscription;
  Position? _position;
  String? _streamError;
  bool _checking = true;
  late final List<({double lat, double lng})> _guidePoints;

  @override
  void initState() {
    super.initState();
    _guidePoints = widget.points
        .map((point) => (lat: point.latitude, lng: point.longitude))
        .toList();
    // Show the last known fix immediately; the stream takes over from here.
    _position = context.read<AppState>().location.position;
    if (_guidePoints.isEmpty) {
      // No road route: guidance stays disabled, but the map still shows GPS.
      _checking = false;
    } else {
      _startStream();
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _startStream() async {
    await _subscription?.cancel();
    _subscription = null;
    if (!mounted) return;
    setState(() {
      _checking = true;
      _streamError = null;
    });
    try {
      final enabled = await Geolocator.isLocationServiceEnabled();
      if (!mounted) return;
      if (!enabled) {
        setState(() {
          _checking = false;
          _streamError =
              'Location services are off. Turn on GPS for live guidance.';
        });
        return;
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (!mounted) return;
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        setState(() {
          _checking = false;
          _streamError =
              'Location permission denied. Allow location access in settings '
              'to get live guidance.';
        });
        return;
      }
      final stream = Geolocator.getPositionStream(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          distanceFilter: 5,
        ),
      );
      _subscription = stream.listen(
        (position) {
          if (!mounted) return;
          setState(() {
            _position = position;
            _streamError = null;
            _checking = false;
          });
        },
        onError: (Object error) {
          if (!mounted) return;
          setState(() {
            _checking = false;
            _streamError = 'Live location failed: $error';
          });
        },
      );
      if (mounted) setState(() => _checking = false);
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _checking = false;
        _streamError = 'Live location unavailable: $error';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tiles = context.read<AppState>().mapTiles.source;
    final hasRoute = _guidePoints.isNotEmpty;
    final lat = _position?.latitude ??
        (hasRoute ? _guidePoints.first.lat : 10.92);
    final lng = _position?.longitude ??
        (hasRoute ? _guidePoints.first.lng : 123.45);
    final userPoint = LatLng(lat, lng);
    final snapshot = hasRoute
        ? computeGuidance(
            points: _guidePoints,
            currentLat: lat,
            currentLng: lng,
            destinationName: widget.destinationName,
            changeAtName: widget.changeAtName,
            changeAtLat: widget.changeAtLat,
            changeAtLng: widget.changeAtLng,
          )
        : null;

    final fitPoints = <LatLng>[...widget.points, userPoint];
    final options = fitPoints.length >= 2
        ? MapOptions(
            initialCameraFit: CameraFit.bounds(
              bounds: LatLngBounds.fromPoints(fitPoints),
              padding: const EdgeInsets.all(48),
            ),
          )
        : MapOptions(initialCenter: userPoint, initialZoom: 14);

    return Scaffold(
      appBar: AppBar(title: Text('Guidance to ${widget.destinationName}')),
      body: Stack(
        children: [
          FlutterMap(
            options: options,
            children: [
              MapTileLayer(tiles: tiles),
              if (hasRoute)
                PolylineLayer(
                  polylines: [
                    Polyline(
                      points: widget.points,
                      strokeWidth: 5,
                      color: const Color(0xFF00796B),
                    ),
                  ],
                ),
              MarkerLayer(
                markers: [
                  Marker(
                    point: userPoint,
                    width: 32,
                    height: 32,
                    child: const Icon(
                      Icons.navigation,
                      color: Color(0xFF1565C0),
                      size: 32,
                    ),
                  ),
                  if (hasRoute)
                    Marker(
                      point: widget.points.last,
                      width: 34,
                      height: 34,
                      child: const Icon(
                        Icons.location_on,
                        color: Color(0xFFD32F2F),
                        size: 34,
                      ),
                    ),
                ],
              ),
            ],
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Card(
              margin: const EdgeInsets.all(16),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            widget.destinationName,
                            style: theme.textTheme.headlineSmall,
                          ),
                        ),
                        if (hasRoute && _streamError == null && !_checking)
                          const Chip(
                            avatar: Icon(Icons.my_location, size: 14),
                            label: Text('Live'),
                            visualDensity: VisualDensity.compact,
                          ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    if (!hasRoute)
                      _Notice(
                        icon: Icons.info_outline,
                        color: Colors.amber.shade100,
                        text: 'Guidance is unavailable offline: no road route '
                            'was recorded. Only your GPS position is shown.',
                      )
                    else ...[
                      Text(
                        '${snapshot!.remainingKm.toStringAsFixed(1)} km remaining',
                        style: theme.textTheme.displaySmall
                            ?.copyWith(fontWeight: FontWeight.w800),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${snapshot.travelledKm.toStringAsFixed(1)} km travelled',
                        style: theme.textTheme.bodySmall,
                      ),
                      const SizedBox(height: 12),
                      LinearProgressIndicator(value: snapshot.progress),
                      const SizedBox(height: 14),
                      Text(snapshot.nextPrompt,
                          style: theme.textTheme.titleMedium),
                    ],
                    if (_checking)
                      const Padding(
                        padding: EdgeInsets.only(top: 12),
                        child: Row(
                          children: [
                            SizedBox(
                              width: 16,
                              height: 16,
                              child:
                                  CircularProgressIndicator(strokeWidth: 2),
                            ),
                            SizedBox(width: 10),
                            Text('Starting live guidance…'),
                          ],
                        ),
                      ),
                    if (_streamError != null) ...[
                      const SizedBox(height: 12),
                      _Notice(
                        icon: Icons.location_disabled,
                        color: theme.colorScheme.errorContainer,
                        text: _streamError!,
                      ),
                      const SizedBox(height: 8),
                      FilledButton.tonalIcon(
                        onPressed: _startStream,
                        icon: const Icon(Icons.refresh),
                        label: const Text('Try again'),
                      ),
                    ],
                    const SizedBox(height: 14),
                    FilledButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('End guidance'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Visible permission / stream / offline notice inside the guidance card.
class _Notice extends StatelessWidget {
  const _Notice({
    required this.icon,
    required this.color,
    required this.text,
  });

  final IconData icon;
  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Icon(icon, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(text, style: const TextStyle(fontSize: 13)),
            ),
          ],
        ),
      );
}
