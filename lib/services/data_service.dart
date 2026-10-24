import 'dart:convert';

import 'package:flutter/services.dart' show rootBundle;

import '../models/app_data.dart';

/// Loads the bundled city data (fare rule, terminals, destinations).
///
/// There is NO backend: everything ships inside the app as a JSON asset.
/// To update the app's data, replace `assets/data/sagay_data.json` and
/// rebuild — see README "Replacing the data".
class DataService {
  /// Path of the bundled data file, declared in pubspec.yaml.
  static const String assetPath = 'assets/data/sagay_data.json';

  /// Reads and parses the asset into a typed [AppData] object.
  Future<AppData> load() async {
    final raw = await rootBundle.loadString(assetPath);
    final dynamic decoded = jsonDecode(raw);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException(
        'sagay_data.json must contain a JSON object at the root.',
      );
    }
    return AppData.fromJson(decoded);
  }
}
