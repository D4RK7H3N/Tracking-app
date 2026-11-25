import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/app_data.dart';
import '../models/destination.dart';
import '../models/fare_scope.dart';
import '../services/fare_service.dart';
import '../state/app_state.dart';
import '../theme/palette.dart';
import '../widgets/brand_banner.dart';

/// Offline fare calculator: pick an origin zone and a destination route, then
/// see the published matrix fare, its 20% discounted counterpart and a group
/// total. No network, GPS or distance lookup is involved.
class FareCalculatorScreen extends StatefulWidget {
  const FareCalculatorScreen({super.key});

  @override
  State<FareCalculatorScreen> createState() => _FareCalculatorScreenState();
}

class _FareCalculatorScreenState extends State<FareCalculatorScreen> {
  static const FareService _fareService = FareService();

  String? _originZone;
  String? _routeId;
  int _passengers = 1;
  bool _applyDiscount = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final data = context.read<AppState>().data;
      if (data == null || !mounted) return;
      setState(() {
        _originZone ??= data.fareOrigins.isEmpty ? null : data.fareOrigins.first.zone;
        _routeId ??= data.routes.isEmpty ? null : data.routes.first.id;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final data = context.watch<AppState>().data;

    return Scaffold(
      appBar: AppBar(title: const Text('Fare calculator')),
      body: data == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                const BrandBanner(
                  icon: Icons.calculate_outlined,
                  title: 'Plan the fare before you go',
                  subtitle: 'Uses the bundled matrix and fare rules. Works '
                      'without internet or GPS.',
                ),
                const SizedBox(height: 16),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Your trip', style: theme.textTheme.titleLarge),
                        const SizedBox(height: 14),
                        _originField(data),
                        const SizedBox(height: 14),
                        _routeField(data),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: _result(data),
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'Estimate only. Unpublished boat fares are never '
                  'turned into a number.',
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: Palette.deepOcean,
                  ),
                ),
              ],
            ),
    );
  }

  Widget _originField(AppData data) {
    final zones = data.fareOrigins.map((origin) => origin.zone).toSet().toList()
      ..sort();
    final value = zones.contains(_originZone) ? _originZone : null;
    return DropdownButtonFormField<String>(
      initialValue: value,
      decoration: const InputDecoration(
        labelText: 'Origin zone',
        prefixIcon: Icon(Icons.place_outlined),
      ),
      items: [
        for (final zone in zones)
          DropdownMenuItem<String>(
            value: zone,
            child: Text(zone),
          ),
      ],
      onChanged: (zone) => setState(() => _originZone = zone),
    );
  }

  Widget _routeField(AppData data) {
    final routes = data.routes;
    final value = routes.any((route) => route.id == _routeId) ? _routeId : null;
    return DropdownButtonFormField<String>(
      initialValue: value,
      decoration: const InputDecoration(
        labelText: 'Destination route',
        prefixIcon: Icon(Icons.route_outlined),
      ),
      items: [
        for (final route in routes)
          DropdownMenuItem<String>(
            value: route.id,
            child: Text(route.serviceLabel),
          ),
      ],
      onChanged: (routeId) => setState(() => _routeId = routeId),
    );
  }

  Widget _result(AppData data) {
    final theme = Theme.of(context);
    if (_originZone == null || _routeId == null) {
      return const Text('Choose an origin zone and a destination route.');
    }
    final destination = _representativeDestination(data, _routeId!);
    FareQuote quote(bool discounted) => _fareService.quote(
          data: data,
          destination: destination,
          roadKm: 0,
          discounted: discounted,
          originZone: _originZone,
        );
    final regular = quote(false);
    final discounted = quote(true);
    final scope = regular.scope;
    final canDiscount = scope.canDiscount;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Fare', style: theme.textTheme.titleLarge),
        const SizedBox(height: 12),
        if (canDiscount) ...[
          Row(
            children: [
              Expanded(
                child: _FareTile(
                  label: 'Regular',
                  amount: _fareService.formatFare(regular.scope.amount),
                  highlight: false,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _FareTile(
                  label: '20% discounted',
                  amount: _fareService.formatFare(discounted.scope.amount),
                  highlight: true,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _applyDiscount,
            onChanged: (value) => setState(() => _applyDiscount = value),
            title: const Text('Use discounted fare for the group total'),
          ),
        ] else ...[
          Text(
            scope.headline(_fareService.formatFare),
            style: theme.textTheme.displaySmall?.copyWith(
              fontWeight: FontWeight.w800,
              color: Palette.ocean,
            ),
          ),
        ],
        const SizedBox(height: 8),
        Text(
          scope.basis,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            const Text('Passengers'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.remove),
              tooltip: 'Fewer passengers',
              onPressed: () => setState(() => _passengers = (_passengers - 1).clamp(1, 20)),
            ),
            Text('$_passengers', style: theme.textTheme.titleMedium),
            IconButton(
              icon: const Icon(Icons.add),
              tooltip: 'More passengers',
              onPressed: () => setState(() => _passengers = (_passengers + 1).clamp(1, 20)),
            ),
          ],
        ),
        if (canDiscount) ...[
          const SizedBox(height: 8),
          Text(
            'Group total: ${_fareService.formatFare(
              _fareService.groupTotal(
                _applyDiscount ? discounted.scope.amount : regular.scope.amount,
                _passengers,
              ),
            )}',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
              color: Palette.ocean,
            ),
          ),
        ],
      ],
    );
  }

  /// The fares are keyed by zone, not by a precise pin. Pick a representative
  /// destination on the chosen route so the shared journey rules (including
  /// the Old Sagay corridor's two-service fare zone) still apply.
  Destination _representativeDestination(AppData data, String routeId) {
    for (final destination in data.destinations) {
      if (destination.routeId == routeId) return destination;
    }
    return Destination(
      id: 'calculator-$routeId',
      name: routeId,
      category: 'Route',
      lat: 0,
      lng: 0,
      terminalId: '',
      zone: '',
      routeId: routeId,
    );
  }
}

class _FareTile extends StatelessWidget {
  const _FareTile({
    required this.label,
    required this.amount,
    required this.highlight,
  });

  final String label;
  final String amount;
  final bool highlight;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: highlight ? Palette.gold.withValues(alpha: 0.22) : Palette.mist,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: highlight ? Palette.gold : Palette.teal.withValues(alpha: 0.2),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 6),
            Text(
              amount,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: Palette.deepOcean,
                  ),
            ),
          ],
        ),
      );
}
