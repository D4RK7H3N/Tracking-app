import 'destination.dart';
import 'fare_matrix.dart';
import 'fare_origin.dart';
import 'fare_rule.dart';
import 'journey.dart';
import 'route.dart';
import 'route_access.dart';
import 'terminal.dart';

class AppData {
  const AppData({
    required this.version,
    required this.note,
    required this.fareRule,
    required this.terminals,
    required this.destinations,
    required this.fareOrigins,
    required this.fareMatrix,
    required this.routeAccess,
    required this.routes,
  });

  factory AppData.fromJson(Map<String, dynamic> json) => AppData(
        version: (json['version'] as num?)?.toInt() ?? 3,
        note: json['note'] as String? ?? '',
        fareRule: FareRule.fromJson(json['fare_rule'] as Map<String, dynamic>? ?? const {}),
        terminals: (json['terminals'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Terminal.fromJson)
            .toList(),
        destinations: (json['destinations'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Destination.fromJson)
            .toList(),
        fareOrigins: (json['fare_origins'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(FareOrigin.fromJson)
            .toList(),
        fareMatrix: (json['fare_matrix'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(FareMatrixEntry.fromJson)
            .toList(),
        routeAccess: (json['route_access'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(RouteAccess.fromJson)
            .toList(),
        routes: (json['routes'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(Route.fromJson)
            .toList(),
      );

  final int version;
  final String note;
  final FareRule fareRule;
  final List<Terminal> terminals;
  final List<Destination> destinations;
  final List<FareOrigin> fareOrigins;
  final List<FareMatrixEntry> fareMatrix;
  final List<RouteAccess> routeAccess;
  final List<Route> routes;

  FareMatrixLookup get matrixLookup => FareMatrixLookup(fareMatrix);

  Terminal? terminalById(String id) {
    for (final terminal in terminals) {
      if (terminal.id == id) return terminal;
    }
    return null;
  }

  Terminal? terminalForDestination(Destination destination) =>
      terminalById(destination.terminalId);

  Route? routeById(String id) {
    for (final route in routes) {
      if (route.id == id) return route;
    }
    return null;
  }

  Route? routeForDestination(Destination destination) => routeById(destination.routeId);

  JourneyPlan journeyFor(String originZone, Destination destination) => planJourney(
        originZone: originZone,
        destinationRoute: routeForDestination(destination),
        rules: routeAccess,
        routes: routes,
        destinationZone: destination.zone,
      );
}
