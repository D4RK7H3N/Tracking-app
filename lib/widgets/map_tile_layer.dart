import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';

import '../services/map_tiles.dart';

/// The single tile layer used by the home map, the result mini-map and live
/// guidance. Switches from network to the downloaded pack as soon as the
/// shared notifier changes, with no restart required.
class MapTileLayer extends StatelessWidget {
  const MapTileLayer({super.key, required this.tiles});

  final ValueNotifier<TileSource> tiles;

  @override
  Widget build(BuildContext context) => ValueListenableBuilder<TileSource>(
        valueListenable: tiles,
        builder: (context, source, _) => TileLayer(
          urlTemplate: source.urlTemplate,
          tileProvider: source.local ? FileTileProvider() : NetworkTileProvider(),
          userAgentPackageName: 'ph.sagay.sagay_tourist_navigator',
        ),
      );
}
