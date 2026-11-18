import 'package:flutter/material.dart';

class Terminal {
  const Terminal({
    required this.id,
    required this.name,
    required this.lat,
    required this.lng,
    required this.color,
    required this.colorName,
    required this.areas,
    required this.vehicles,
  });

  factory Terminal.fromJson(Map<String, dynamic> json) => Terminal(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        color: json['colorHex'] as String? ?? '#9E9E9E',
        colorName: json['colorName'] as String? ?? '',
        areas: ((json['serviceArea'] as String?) ?? '')
            .split(',')
            .map((area) => area.trim())
            .where((area) => area.isNotEmpty)
            .toList(),
        vehicles: (json['vehicles'] as List<dynamic>? ?? const [])
            .map((vehicle) => vehicle.toString())
            .toList(),
      );

  final String id;
  final String name;
  final double lat;
  final double lng;
  final String color;
  final String colorName;
  final List<String> areas;
  final List<String> vehicles;

  Color get uiColor => parseHexColor(color);

  static Color parseHexColor(String hex) {
    var value = hex.trim().replaceFirst('#', '');
    if (value.length == 6) {
      value = 'FF$value';
    }
    final parsed = int.tryParse(value, radix: 16);
    return parsed == null ? const Color(0xFF9E9E9E) : Color(parsed);
  }
}
