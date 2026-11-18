import 'route.dart';
import 'route_access.dart';

class JourneyPlan {
  const JourneyPlan({
    required this.services,
    required this.changeAt,
    required this.fareZone,
  });

  final List<Route> services;
  final String? changeAt;
  final String? fareZone;
}

JourneyPlan planJourney({
  required String originZone,
  required Route? destinationRoute,
  required List<RouteAccess> rules,
  required List<Route> routes,
  String? destinationZone,
}) {
  RouteAccess? access;
  if (destinationRoute != null) {
    for (final rule in rules) {
      if (rule.originZone.toUpperCase() == originZone.toUpperCase() &&
          rule.destinationRouteId.toUpperCase() == destinationRoute.id.toUpperCase() &&
          routes.any((route) => route.id.toUpperCase() == rule.viaRouteId.toUpperCase())) {
        access = rule;
        break;
      }
    }
  }
  final viaRoute = access == null
      ? null
      : routes.firstWhere(
          (route) => route.id.toUpperCase() == access!.viaRouteId.toUpperCase(),
          orElse: () => const Route(
            id: '',
            name: '',
            terminalId: '',
            colorHex: '#9E9E9E',
            colorName: '',
            vehicle: '',
            stops: [],
          ),
        );
  final services = <Route>[
    ?destinationRoute,
  ];
  if (viaRoute != null && destinationRoute != null && viaRoute.id != destinationRoute.id) {
    services.insert(0, viaRoute);
  }
  return JourneyPlan(
    services: services,
    changeAt: services.length > 1 ? access?.changeAt : null,
    fareZone: services.length > 1 ? access?.fareZone : destinationZone,
  );
}
