import 'dart:convert';
import 'dart:io';

void main(List<String> args) {
  final script = File(Platform.script.toFilePath()).resolveSymbolicLinksSync();
  final root = Directory(script).parent.parent.path;
  final source = File('$root${Platform.pathSeparator}SagayTouristNavigation'
      '${Platform.pathSeparator}app${Platform.pathSeparator}src'
      '${Platform.pathSeparator}main${Platform.pathSeparator}assets'
      '${Platform.pathSeparator}seed.json');
  final target = File('$root${Platform.pathSeparator}assets'
      '${Platform.pathSeparator}data${Platform.pathSeparator}sagay_data.json');

  if (!source.existsSync()) {
    stderr.writeln('Missing source: ${source.path}');
    exit(1);
  }

  final decoded = jsonDecode(source.readAsStringSync());
  if (decoded is! Map<String, dynamic>) {
    stderr.writeln('seed.json must contain an object');
    exit(1);
  }
  final requiredKeys = <String>[
    'note',
    'terminals',
    'destinations',
    'fare_rule',
    'fare_origins',
    'fare_matrix',
    'route_access',
    'routes',
  ];
  for (final key in requiredKeys) {
    if (!decoded.containsKey(key)) {
      stderr.writeln('seed.json is missing "$key"');
      exit(1);
    }
  }

  target.parent.createSync(recursive: true);
  target.writeAsStringSync(jsonEncode(decoded));

  stdout.writeln('Synced ${source.path} -> ${target.path}');
  for (final key in requiredKeys) {
    final value = decoded[key];
    final count = value is List ? value.length.toString() : '1';
    stdout.writeln('  $key: $count');
  }
}
