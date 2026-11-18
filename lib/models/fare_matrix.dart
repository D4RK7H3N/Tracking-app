class FareMatrixEntry {
  const FareMatrixEntry({
    required this.originZone,
    required this.destinationZone,
    required this.regular,
    required this.discounted,
  });

  factory FareMatrixEntry.fromJson(Map<String, dynamic> json) => FareMatrixEntry(
        originZone: json['origin'] as String? ?? '',
        destinationZone: json['destination'] as String? ?? '',
        regular: (json['regular'] as num?)?.toDouble() ?? 0,
        discounted: (json['discounted'] as num?)?.toDouble() ?? 0,
      );

  final String originZone;
  final String destinationZone;
  final double regular;
  final double discounted;
}

class FareMatrixLookup {
  FareMatrixLookup(this.entries);

  final List<FareMatrixEntry> entries;

  FareMatrixEntry? find(String originZone, String destinationZone) {
    if (destinationZone.trim().isEmpty) return null;
    final keys = _keys(destinationZone);
    for (final entry in entries) {
      if (entry.originZone.toUpperCase() != originZone.toUpperCase()) {
        continue;
      }
      if (_keys(entry.destinationZone).any(keys.contains)) return entry;
    }
    return null;
  }

  Set<String> _keys(String zone) {
    final trimmed = zone.trim();
    final base = trimmed.split(' (').first.split('[').first.trim();
    final last = base.split('/').last.trim();
    final values = <String>{trimmed, base, last};
    if (base.toUpperCase().startsWith('BRGY.')) {
      values.add(base.substring(5).trim());
    }
    return values.where((key) => key.isNotEmpty).map((key) => key.toUpperCase()).toSet();
  }
}
