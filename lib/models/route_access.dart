class RouteAccess {
  const RouteAccess({
    required this.originZone,
    required this.destinationRouteId,
    required this.viaRouteId,
    required this.changeAt,
    required this.fareZone,
  });

  factory RouteAccess.fromJson(Map<String, dynamic> json) => RouteAccess(
        originZone: json['originZone'] as String? ?? '',
        destinationRouteId: json['destinationRouteId'] as String? ?? '',
        viaRouteId: json['viaRouteId'] as String? ?? '',
        changeAt: json['changeAt'] as String? ?? '',
        fareZone: json['fareZone'] as String? ?? '',
      );

  final String originZone;
  final String destinationRouteId;
  final String viaRouteId;
  final String changeAt;
  final String fareZone;
}
