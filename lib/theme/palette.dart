import 'package:flutter/material.dart';

/// The project's visual identity. These are chrome colours only: the
/// red/pink/yellow/white route colours come from the bundled city data and
/// must never be derived from this palette.
class Palette {
  static const ocean = Color(0xFF0B3C5D);
  static const deepOcean = Color(0xFF072A42);
  static const coral = Color(0xFFFF6B52);
  static const gold = Color(0xFFFFB347);
  static const mist = Color(0xFFF7F9F8);
  static const ink = Color(0xFF12212B);
  static const teal = Color(0xFF1B7A78);

  /// Warm sunrise gradient used for hero banners.
  static const List<Color> warmGradient = <Color>[gold, coral];
}
