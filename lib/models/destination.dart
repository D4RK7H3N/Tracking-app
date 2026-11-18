class Destination {
  const Destination({
    required this.id,
    required this.name,
    required this.category,
    required this.lat,
    required this.lng,
    required this.terminalId,
    required this.zone,
    required this.routeId,
    this.boat = false,
  });

  factory Destination.fromJson(Map<String, dynamic> json) => Destination(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        category: json['category'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        terminalId: json['terminalId'] as String? ?? '',
        zone: json['zone'] as String? ?? '',
        routeId: json['routeId'] as String? ?? '',
        boat: json['boat'] == true,
      );

  final String id;
  final String name;
  final String category;
  final double lat;
  final double lng;
  final String terminalId;
  final String zone;
  final String routeId;
  final bool boat;
}
