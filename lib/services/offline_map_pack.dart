import 'dart:io';
import 'dart:math';

import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

class OfflineMapPack {
  static const south = 10.78;
  static const north = 10.99;
  static const west = 123.34;
  static const east = 123.58;
  static const minZoom = 12;
  static const maxZoom = 14;

  Future<Directory> get _directory async {
    final root = await getApplicationDocumentsDirectory();
    return Directory('${root.path}${Platform.pathSeparator}sagay_offline');
  }

  Future<bool> isDownloaded() async {
    final dir = await _directory;
    if (!await dir.exists()) return false;
    final marker = File('${dir.path}${Platform.pathSeparator}complete');
    return marker.existsSync();
  }

  Future<int> sizeBytes() async {
    final dir = await _directory;
    if (!await dir.exists()) return 0;
    var total = 0;
    await for (final entity in dir.list(recursive: true)) {
      if (entity is File && entity.path.endsWith('.png')) {
        total += await entity.length();
      }
    }
    return total;
  }

  Future<int> tileCount() async {
    var count = 0;
    for (var zoom = minZoom; zoom <= maxZoom; zoom++) {
      final range = _tileRange(zoom);
      count += (range.$2 - range.$1 + 1) * (range.$4 - range.$3 + 1);
    }
    return count;
  }

  Future<void> delete() async {
    final dir = await _directory;
    if (await dir.exists()) await dir.delete(recursive: true);
  }

  Future<void> download(void Function(int done, int total) onProgress) async {
    final dir = await _directory;
    final total = await tileCount();
    var done = 0;
    final client = http.Client();
    try {
      for (var zoom = minZoom; zoom <= maxZoom; zoom++) {
        final range = _tileRange(zoom);
        for (var x = range.$1; x <= range.$2; x++) {
          for (var y = range.$3; y <= range.$4; y++) {
            final target = File(
              '${dir.path}${Platform.pathSeparator}$zoom'
              '${Platform.pathSeparator}$x'
              '${Platform.pathSeparator}$y.png',
            );
            if (!await target.exists()) {
              final response = await client.get(
                Uri.parse('https://tile.openstreetmap.org/$zoom/$x/$y.png'),
              );
              if (response.statusCode == 200) {
                await target.parent.create(recursive: true);
                await target.writeAsBytes(response.bodyBytes);
              }
            }
            done++;
            onProgress(done, total);
          }
        }
      }
      final marker = File('${dir.path}${Platform.pathSeparator}complete');
      await marker.writeAsString(DateTime.now().toIso8601String());
    } finally {
      client.close();
    }
  }

  Future<String?> localTilePattern() async {
    if (!await isDownloaded()) return null;
    final dir = await _directory;
    return '${dir.path}/{z}/{x}/{y}.png';
  }

  (int, int, int, int) _tileRange(int zoom) {
    final scale = pow(2, zoom).toDouble();
    int longitudeToTile(double longitude) =>
        ((longitude + 180.0) / 360.0 * scale).floor();
    int latitudeToTile(double latitude) {
      final radians = latitude * pi / 180;
      return ((1.0 - log(tan(radians) + 1 / cos(radians)) / pi) / 2.0 * scale).floor();
    }

    return (
      longitudeToTile(west),
      longitudeToTile(east),
      latitudeToTile(north),
      latitudeToTile(south),
    );
  }
}
