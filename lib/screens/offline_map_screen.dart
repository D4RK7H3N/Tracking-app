import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/offline_map_pack.dart';
import '../state/app_state.dart';

class OfflineMapScreen extends StatefulWidget {
  const OfflineMapScreen({super.key});

  @override
  State<OfflineMapScreen> createState() => _OfflineMapScreenState();
}

class _OfflineMapScreenState extends State<OfflineMapScreen> {
  final OfflineMapPack _pack = OfflineMapPack();
  late Future<bool> _downloaded = _pack.isDownloaded();
  late Future<int> _size = _pack.sizeBytes();
  final Future<int> _total = OfflineMapPack().tileCount();
  int _progress = 0;
  bool _downloading = false;

  void _reload() {
    if (!mounted) return;
    setState(() {
      _downloaded = _pack.isDownloaded();
      _size = _pack.sizeBytes();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Offline map pack')),
      body: FutureBuilder<bool>(
        future: _downloaded,
        builder: (context, snapshot) {
          final downloaded = snapshot.data ?? false;
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Sagay City offline map',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 12),
                FutureBuilder<int>(
                  future: _total,
                  builder: (context, totalSnapshot) => FutureBuilder<int>(
                    future: _size,
                    builder: (context, sizeSnapshot) => Text(
                      'Bounded city area, zoom 12\u201314 · '
                      '${totalSnapshot.data ?? "?"} tiles · '
                      '${((sizeSnapshot.data ?? 0) / (1024 * 1024)).toStringAsFixed(1)} MB',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                if (downloaded)
                  const Text(
                    'The pack is on this device. The map uses it automatically '
                    'when there is no internet.',
                  )
                else if (_downloading)
                  LinearProgressIndicator(
                    value: _progress / 1000,
                  )
                else
                  const Text(
                    'Downloads the city area for use without internet. '
                    'Allow about 10\u201320 MB of storage.',
                  ),
                const SizedBox(height: 20),
                if (downloaded)
                  OutlinedButton.icon(
                    onPressed: () async {
                      final appState = context.read<AppState>();
                      await _pack.delete();
                      await appState.refreshMapTiles();
                      _reload();
                    },
                    icon: const Icon(Icons.delete_outline),
                    label: const Text('Delete offline map'),
                  )
                else
                  FilledButton.icon(
                    onPressed: _downloading
                        ? null
                        : () async {
                            final appState = context.read<AppState>();
                            setState(() => _downloading = true);
                            await _pack.download((done, total) {
                              if (!mounted) return;
                              setState(() => _progress = total == 0 ? 0 : (done * 1000) ~/ total);
                            });
                            if (mounted) {
                              setState(() => _downloading = false);
                            }
                            // Activate the pack for every map immediately,
                            // without waiting for an app restart.
                            await appState.refreshMapTiles();
                            _reload();
                          },
                    icon: const Icon(Icons.download),
                    label: Text(_downloading ? 'Downloading…' : 'Download offline map'),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}
