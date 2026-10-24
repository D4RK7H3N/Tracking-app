import 'package:flutter/material.dart';

/// A tricycle terminal. Every terminal has a signature COLOR so tourists can
/// identify the correct tricycle queue at a glance.
class Terminal {
  const Terminal({
    required this.id,
    required this.name,
    required this.lat,
    required this.lng,
    required this.color,
    required this.colorName,
    required this.areas,
  });

  factory Terminal.fromJson(Map<String, dynamic> json) => Terminal(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        color: json['color'] as String? ?? '#9E9E9E',
        colorName: json['color_name'] as String? ?? '',
        areas: (json['areas'] as List<dynamic>? ?? const [])
            .map((e) => e.toString())
            .toList(),
      );

  final String id;
  final String name;

  /// Terminal location (used on the result-screen mini map).
  final double lat;
  final double lng;

  /// Hex colour string, e.g. "#E53935", from the JSON data.
  final String color;

  /// Human-readable colour name, e.g. "Red", shown on the big colour chip.
  final String colorName;

  /// Neighbourhoods / barangays this terminal serves.
  final List<String> areas;

  /// The terminal colour parsed from [color] for UI use.
  Color get uiColor => parseHexColor(color);

  /// Parses "#RRGGBB" (or "#AARRGGBB") into a Flutter [Color].
  /// Falls back to grey for malformed values so bad data never crashes the UI.
  static Color parseHexColor(String hex) {
    var value = hex.trim().replaceFirst('#', '');
    if (value.length == 6) {
      value = 'FF$value';
    }
    final parsed = int.tryParse(value, radix: 16);
    return parsed == null ? const Color(0xFF9E9E9E) : Color(parsed);
  }
}
