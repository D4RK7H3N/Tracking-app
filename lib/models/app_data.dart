import 'destination.dart';
import 'fare_rule.dart';
import 'terminal.dart';

/// Root model for the bundled JSON asset `assets/data/sagay_data.json`.
///
/// Schema (see README for how to replace the values):
/// ```json
/// {
///   "version": 1,
///   "fare_rule": { ... },
///   "terminals": [ ... ],
///   "destinations": [ ... ]
/// }
/// ```
class AppData {
  const AppData({
    required this.version,
    required this.fareRule,
    required this.terminals,
    required this.destinations,
  });

  factory AppData.fromJson(Map<String, dynamic> json) => AppData(
        version: (json['version'] as num?)?.toInt() ?? 1,
        fareRule:
            FareRule.fromJson(json['fare_rule'] as Map<String, dynamic>? ?? const {}),
        terminals: (json['terminals'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Terminal.fromJson)
            .toList(),
        destinations: (json['destinations'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Destination.fromJson)
            .toList(),
      );

  final int version;
  final FareRule fareRule;
  final List<Terminal> terminals;
  final List<Destination> destinations;

  /// Looks up a terminal by id; null when the id is unknown.
  Terminal? terminalById(String id) {
    for (final terminal in terminals) {
      if (terminal.id == id) {
        return terminal;
      }
    }
    return null;
  }

  /// The terminal assigned to [destination], or null when the destination
  /// references an unknown terminal id.
  Terminal? terminalForDestination(Destination destination) =>
      terminalById(destination.terminalId);
}
