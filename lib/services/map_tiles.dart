import 'package:flutter/foundation.dart';

import 'offline_map_pack.dart';

/// The tile source every map view shares.
@immutable
class TileSource {
  const TileSource._({required this.urlTemplate, required this.local});

  static const online = TileSource._(
    urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    local: false,
  );

  static TileSource localPack(String pattern) =>
      TileSource._(urlTemplate: pattern, local: true);

  final String urlTemplate;
  final bool local;
}

/// Holds the active tile source and refreshes it when the offline pack
/// changes. Lives on [AppState] so a pack downloaded during the session is
/// picked up by every map without a restart.
class MapTileController {
  MapTileController({OfflineMapPack? pack}) : _pack = pack ?? OfflineMapPack();

  final OfflineMapPack _pack;

  final ValueNotifier<TileSource> source = ValueNotifier<TileSource>(TileSource.online);

  Future<void> refresh() async {
    final pattern = await _pack.localTilePattern();
    source.value = pattern == null ? TileSource.online : TileSource.localPack(pattern);
  }

  void dispose() => source.dispose();
}
