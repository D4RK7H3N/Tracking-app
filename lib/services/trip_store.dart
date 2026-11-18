import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

class TripRecord {
  const TripRecord({
    required this.id,
    required this.destinationId,
    required this.destinationName,
    required this.originZone,
    required this.vehicleLabel,
    required this.fare,
    required this.discounted,
    required this.passengers,
    required this.total,
    required this.savedAt,
  });

  factory TripRecord.fromJson(Map<String, dynamic> json) => TripRecord(
        id: json['id'] as String? ?? '',
        destinationId: json['destinationId'] as String? ?? '',
        destinationName: json['destinationName'] as String? ?? '',
        originZone: json['originZone'] as String? ?? '',
        vehicleLabel: json['vehicleLabel'] as String? ?? '',
        fare: (json['fare'] as num?)?.toDouble() ?? 0,
        discounted: json['discounted'] == true,
        passengers: (json['passengers'] as num?)?.toInt() ?? 1,
        total: (json['total'] as num?)?.toDouble() ?? 0,
        savedAt: DateTime.tryParse(json['savedAt'] as String? ?? '') ??
            DateTime.fromMillisecondsSinceEpoch(0),
      );

  final String id;
  final String destinationId;
  final String destinationName;
  final String originZone;
  final String vehicleLabel;
  final double fare;
  final bool discounted;
  final int passengers;
  final double total;
  final DateTime savedAt;

  Map<String, dynamic> toJson() => {
        'id': id,
        'destinationId': destinationId,
        'destinationName': destinationName,
        'originZone': originZone,
        'vehicleLabel': vehicleLabel,
        'fare': fare,
        'discounted': discounted,
        'passengers': passengers,
        'total': total,
        'savedAt': savedAt.toIso8601String(),
      };
}

class TripStore {
  static const _historyKey = 'sagay.trip_history';
  static const _favoritesKey = 'sagay.favorites';

  Future<SharedPreferences> get _prefs => SharedPreferences.getInstance();

  Future<List<TripRecord>> history() async {
    final prefs = await _prefs;
    final raw = prefs.getStringList(_historyKey) ?? const <String>[];
    return raw
        .map((entry) => TripRecord.fromJson(jsonDecode(entry) as Map<String, dynamic>))
        .toList()
      ..sort((a, b) => b.savedAt.compareTo(a.savedAt));
  }

  Future<void> addTrip(TripRecord record) async {
    final records = await history();
    records.insert(0, record);
    await _writeHistory(records);
  }

  Future<void> deleteTrip(String id) async {
    final records = await history();
    records.removeWhere((record) => record.id == id);
    await _writeHistory(records);
  }

  Future<void> clearHistory() => _writeHistory(const <TripRecord>[]);

  Future<Set<String>> favorites() async {
    final prefs = await _prefs;
    return (prefs.getStringList(_favoritesKey) ?? const <String>[]).toSet();
  }

  Future<void> toggleFavorite(String destinationId) async {
    final values = await favorites();
    if (values.contains(destinationId)) {
      values.remove(destinationId);
    } else {
      values.add(destinationId);
    }
    final prefs = await _prefs;
    await prefs.setStringList(_favoritesKey, values.toList()..sort());
  }

  Future<void> _writeHistory(List<TripRecord> records) async {
    final prefs = await _prefs;
    await prefs.setStringList(
      _historyKey,
      records.map((record) => jsonEncode(record.toJson())).toList(),
    );
  }
}
