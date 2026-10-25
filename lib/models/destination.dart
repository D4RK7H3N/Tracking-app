/// A place a tourist can travel to, parsed from the bundled JSON asset.
class Destination {
  const Destination({
    required this.id,
    required this.name,
    required this.category,
    required this.lat,
    required this.lng,
    required this.terminalId,
    this.boat = false,
  });

  factory Destination.fromJson(Map<String, dynamic> json) => Destination(
        id: json['id'] as String? ?? '',
        name: json['name'] as String? ?? '',
        category: json['category'] as String? ?? '',
        lat: (json['lat'] as num?)?.toDouble() ?? 0,
        lng: (json['lng'] as num?)?.toDouble() ?? 0,
        terminalId: json['terminal_id'] as String? ?? '',
        // Sea destinations (reefs, islands) are reached by boat from their
        // terminal port; the tricycle only covers the road leg to the port.
        boat: json['boat'] == true,
      );

  final String id;
  final String name;

  /// Free-form grouping used by search, e.g. "Beach / Island".
  final String category;

  final double lat;
  final double lng;

  /// Id of the [Terminal] whose tricycles serve this destination.
  final String terminalId;

  /// true when the final leg requires a boat ride from the terminal port.
  final bool boat;
}
