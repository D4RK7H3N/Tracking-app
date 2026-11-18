import 'package:flutter/material.dart';

class RouteStop {
  const RouteStop({required this.name, required this.lat, required this.lng});

  factory RouteStop.fromJson(Map<String, dynamic> json) => RouteStop(
        name: json['name'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
      );

  final String name;
  final double lat;
  final double lng;
}

class Route {
  const Route({
    required this.id,
    required this.name,
    required this.terminalId,
    required this.colorHex,
    required this.colorName,
    required this.vehicle,
    required this.stops,
  });

  factory Route.fromJson(Map<String, dynamic> json) => Route(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        terminalId: json['terminalId'] as String? ?? '',
        colorHex: json['colorHex'] as String? ?? '#9E9E9E',
        colorName: json['colorName'] as String? ?? '',
        vehicle: json['vehicle'] as String? ?? '',
        stops: (json['stops'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(RouteStop.fromJson)
            .toList(),
      );

  final String id;
  final String name;
  final String terminalId;
  final String colorHex;
  final String colorName;
  final String vehicle;
  final List<RouteStop> stops;

  String get serviceLabel => '$colorName $vehicle';

  Color get uiColor {
    var value = colorHex.replaceFirst('#', '');
    if (value.length == 6) value = 'FF$value';
    final parsed = int.tryParse(value, radix: 16);
    return parsed == null ? const Color(0xFF9E9E9E) : Color(parsed);
  }
}
