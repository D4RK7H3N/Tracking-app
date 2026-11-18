import 'dart:math' as math;

import '../services/haversine.dart';

class GuidanceSnapshot {
  const GuidanceSnapshot({
    required this.travelledKm,
    required this.remainingKm,
    required this.progress,
    required this.nextPrompt,
    required this.arrived,
  });

  final double travelledKm;
  final double remainingKm;
  final double progress;
  final String nextPrompt;
  final bool arrived;
}

class _Segment {
  const _Segment(
    this.startKm,
    this.endKm,
    this.aLat,
    this.aLng,
    this.bLat,
    this.bLng,
  );

  final double startKm;
  final double endKm;
  final double aLat;
  final double aLng;
  final double bLat;
  final double bLng;
}

GuidanceSnapshot computeGuidance({
  required List<({double lat, double lng})> points,
  required double currentLat,
  required double currentLng,
  required String destinationName,
  String? changeAtName,
  double? changeAtLat,
  double? changeAtLng,
}) {
  if (points.length < 2) {
    return const GuidanceSnapshot(
      travelledKm: 0,
      remainingKm: 0,
      progress: 0,
      nextPrompt: 'Route unavailable for guidance',
      arrived: false,
    );
  }
  final segments = _segments(points);
  final total = segments.last.endKm;
  final position = _project(currentLat, currentLng, segments);
  final remaining = (total - position).clamp(0.0, total).toDouble();
  final arrived = remaining <= 0.15;
  final changeKm = changeAtName != null && changeAtLat != null && changeAtLng != null
      ? _project(changeAtLat, changeAtLng, segments)
      : null;
  final prompt = arrived
      ? 'You have arrived at $destinationName'
      : changeKm != null && position < changeKm - 0.05
          ? 'Change at $changeAtName'
          : 'Continue to $destinationName';
  return GuidanceSnapshot(
    travelledKm: position,
    remainingKm: remaining,
    progress: (position / total).clamp(0.0, 1.0),
    nextPrompt: prompt,
    arrived: arrived,
  );
}

List<_Segment> _segments(List<({double lat, double lng})> points) {
  final result = <_Segment>[];
  var travelled = 0.0;
  for (var i = 1; i < points.length; i++) {
    final a = points[i - 1];
    final b = points[i];
    final length = haversineKm(a.lat, a.lng, b.lat, b.lng);
    result.add(_Segment(travelled, travelled + length, a.lat, a.lng, b.lat, b.lng));
    travelled += length;
  }
  return result;
}

double _project(double lat, double lng, List<_Segment> segments) {
  var bestKm = 0.0;
  var bestDistance = double.maxFinite;
  for (final segment in segments) {
    final cosLat = math.cos(_toRadians((segment.aLat + segment.bLat) / 2));
    final ax = segment.aLng * cosLat * 111.32;
    final ay = segment.aLat * 111.32;
    final bx = segment.bLng * cosLat * 111.32;
    final by = segment.bLat * 111.32;
    final px = lng * cosLat * 111.32;
    final py = lat * 111.32;
    final dx = bx - ax;
    final dy = by - ay;
    final lengthSq = dx * dx + dy * dy;
    var t = lengthSq == 0 ? 0.0 : ((px - ax) * dx + (py - ay) * dy) / lengthSq;
    if (t < 0) t = 0;
    if (t > 1) t = 1;
    final cx = ax + t * dx;
    final cy = ay + t * dy;
    final distance = math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
    if (distance < bestDistance) {
      bestDistance = distance;
      bestKm = segment.startKm + t * (segment.endKm - segment.startKm);
    }
  }
  return bestKm;
}

double _toRadians(double degrees) => degrees * math.pi / 180;
