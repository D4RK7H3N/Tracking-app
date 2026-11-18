import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';

import '../services/fare_service.dart';
import '../services/trip_store.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late final TripStore _store = context.read<TripStore>();
  late Future<List<TripRecord>> _records = _store.history();
  static const FareService _fareService = FareService();

  void _reload() => setState(() => _records = _store.history());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Trip history')),
      body: FutureBuilder<List<TripRecord>>(
        future: _records,
        builder: (context, snapshot) {
          final records = snapshot.data ?? const <TripRecord>[];
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (records.isEmpty) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Text(
                  'No saved trips yet. Open a destination and choose '
                  '"Save to trip history".',
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }
          final total = records.fold<double>(0, (sum, item) => sum + item.total);
          final mostUsed = <String, int>{};
          for (final record in records) {
            mostUsed[record.vehicleLabel] = (mostUsed[record.vehicleLabel] ?? 0) + 1;
          }
          final topService = mostUsed.entries.isEmpty
              ? ''
              : mostUsed.entries.reduce((a, b) => a.value >= b.value ? a : b).key;
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${records.length} trips · '
                      '${_fareService.formatFare(total)} total',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    if (topService.isNotEmpty)
                      Text(
                        'Most used service: $topService',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                  ],
                ),
              ),
              Expanded(
                child: ListView.builder(
                  itemCount: records.length,
                  itemBuilder: (context, index) {
                    final record = records[index];
                    return ListTile(
                      title: Text(record.destinationName),
                      subtitle: Text(
                        [
                          if (record.originZone.isNotEmpty) record.originZone,
                          if (record.vehicleLabel.isNotEmpty) record.vehicleLabel,
                          if (record.discounted) 'discounted',
                          '${record.passengers} passenger(s)',
                        ].join(' · '),
                      ),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(_fareService.formatFare(record.total)),
                          IconButton(
                            icon: const Icon(Icons.delete_outline),
                            tooltip: 'Delete trip',
                            onPressed: () async {
                              await _store.deleteTrip(record.id);
                              _reload();
                            },
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                child: OutlinedButton.icon(
                  onPressed: () {
                    final csv = <String>[
                      _fareService.csvHeader(),
                      for (final record in records)
                        _fareService.csvRow(
                          date: record.savedAt.toIso8601String(),
                          origin: record.originZone,
                          destination: record.destinationName,
                          vehicle: record.vehicleLabel,
                          fare: record.fare,
                          discounted: record.discounted,
                          passengers: record.passengers,
                          total: record.total,
                        ),
                    ].join('\n');
                    SharePlus.instance.share(
                      ShareParams(text: csv, title: 'Trip history CSV'),
                    );
                  },
                  icon: const Icon(Icons.file_download_outlined),
                  label: const Text('Export CSV'),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                child: TextButton.icon(
                  onPressed: () async {
                    await _store.clearHistory();
                    _reload();
                  },
                  icon: const Icon(Icons.delete_sweep_outlined),
                  label: const Text('Clear all history'),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
