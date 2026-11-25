import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/settings_store.dart';
import '../services/trip_store.dart';
import '../state/app_state.dart';
import '../theme/palette.dart';
import '../widgets/brand_banner.dart';

/// User preferences, local data management and the required data caveat.
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  /// Kept in sync with pubspec.yaml's version.
  static const appVersion = '1.1.0+2';

  bool? _reduceMotion;

  @override
  void initState() {
    super.initState();
    const SettingsStore().reduceMotion().then((value) {
      if (mounted) setState(() => _reduceMotion = value);
    });
  }

  Future<void> _clearHistory() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Clear trip history?'),
        content: const Text(
          'This deletes every saved trip on this device. Favourites are kept.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('Clear'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    await context.read<TripStore>().clearHistory();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Trip history cleared')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final appState = context.watch<AppState>();
    final deviceReduced =
        MediaQuery.maybeOf(context)?.disableAnimations ?? false;
    final effective = _reduceMotion ?? deviceReduced;

    return Scaffold(
      appBar: AppBar(title: const Text('Settings & data')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const BrandBanner(
            icon: Icons.tune,
            title: 'Make the app yours',
            subtitle: 'Preferences are stored on this device only.',
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Display', style: theme.textTheme.titleMedium),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    value: effective,
                    onChanged: (value) async {
                      await appState.setReduceMotion(value);
                      if (mounted) setState(() => _reduceMotion = value);
                    },
                    title: const Text('Reduce motion'),
                    subtitle: Text(
                      _reduceMotion == null
                          ? 'Following the device setting'
                          : 'Override saved on this device',
                    ),
                  ),
                  if (_reduceMotion != null)
                    Align(
                      alignment: Alignment.centerLeft,
                      child: TextButton(
                        onPressed: () async {
                          await appState.setReduceMotion(null);
                          if (mounted) setState(() => _reduceMotion = null);
                        },
                        child: const Text('Use device setting'),
                      ),
                    ),
                ],
              ),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Local data', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 8),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.delete_outline, color: Palette.coral),
                    title: const Text('Clear trip history'),
                    subtitle: const Text('Removes all saved trips (with confirmation)'),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: _clearHistory,
                  ),
                ],
              ),
            ),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('About', style: theme.textTheme.titleMedium),
                  const SizedBox(height: 10),
                  Text('Sagay Tourist Navigator · version $appVersion'),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: Palette.gold.withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: Palette.gold),
                    ),
                    child: Text(
                      'IMPORTANT: route colours, vehicle assignments, '
                      'coordinates and fares are researched placeholders from '
                      'public sources. They have NOT been verified with the '
                      'Sagay City Tourism Office or LTFRB-franchised operators. '
                      'Confirm the colour, boarding point and fare locally '
                      'before you travel.',
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: Palette.deepOcean,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
