/// A place a tourist can travel to, parsed from the bundled JSON asset.
class Destination {
  const Destination({
    required this.id,
    required this.name,
    required this.category,
    required this.lat,
    required this.lng,
    required this.terminalId,
  });

  factory Destination.fromJson(Map<String, dynamic> json) => Destination(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        category: json['category'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        terminalId: json['terminal_id'] as String? ?? '',
      );

  final String id;
  final String name;

  /// Free-form grouping used by search, e.g. "Beach / Island".
  final String category;

  final double lat;
  final double lng;

  /// Id of the [Terminal] whose tricycles serve this destination.
  final String terminalId;
}
