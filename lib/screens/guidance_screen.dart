import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../services/guidance.dart';
import '../state/app_state.dart';

class GuidanceScreen extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final position = appState.location.position;
    final currentLat = position?.latitude ?? points.first.latitude;
    final currentLng = position?.longitude ?? points.first.longitude;
    final snapshot = computeGuidance(
      points: points.map((point) => (lat: point.latitude, lng: point.longitude)).toList(),
      currentLat: currentLat,
      currentLng: currentLng,
      destinationName: destinationName,
      changeAtName: changeAtName,
      changeAtLat: changeAtLat,
      changeAtLng: changeAtLng,
    );
    final userPoint = LatLng(currentLat, currentLng);
    final bounds = LatLngBounds.fromPoints(<LatLng>[...points, userPoint]);
    final options = MapOptions(
      initialCameraFit: CameraFit.bounds(
        bounds: bounds,
        padding: const EdgeInsets.all(48),
      ),
    );

    return Scaffold(
      appBar: AppBar(title: Text('Guidance to $destinationName')),
      body: Stack(
        children: [
          FlutterMap(
            options: options,
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'ph.sagay.sagay_tourist_navigator',
              ),
              if (points.length > 1)
                PolylineLayer(
                  polylines: [
                    Polyline(
                      points: points,
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
                  Marker(
                    point: points.last,
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
                borderRadius: BorderRadius.circular(20),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      destinationName,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${snapshot.remainingKm.toStringAsFixed(1)} km remaining',
                      style: Theme.of(context)
                          .textTheme
                          .displaySmall
                          ?.copyWith(fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 12),
                    LinearProgressIndicator(value: snapshot.progress),
                    const SizedBox(height: 14),
                    Text(
                      snapshot.nextPrompt,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
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
