import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart' show Position;
import 'package:latlong2/latlong.dart';
import 'package:provider/provider.dart';

import '../models/app_data.dart';
import '../models/destination.dart';
import '../services/location_service.dart' show LocationStatus;
import '../state/app_state.dart';
import '../widgets/compass_indicator.dart';
import 'result_screen.dart';

/// Home screen: a full-screen map with the user's position and destination
/// markers, plus a draggable searchable destination list at the bottom.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  AppState? _appState;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final appState = context.read<AppState>();
      _appState = appState;
      appState.addListener(_onStateChanged);
      // Ask for permission and get a first fix as soon as the screen shows.
      appState.locateMe();
    });
  }

  @override
  void dispose() {
    _appState?.removeListener(_onStateChanged);
    super.dispose();
  }

  /// Side effect that must not happen during build: surfacing data-load
  /// errors once. Map recentering is handled inside [_MapWithSheet].
  void _onStateChanged() {
    if (!mounted) {
      return;
    }
    final appState = context.read<AppState>();
    if (appState.dataError != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(appState.dataError!)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();
    final data = appState.data;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Sagay Tourist Navigator'),
        centerTitle: true,
      ),
      body: appState.dataLoading
          ? const Center(child: CircularProgressIndicator())
          : data == null
              ? _DataErrorView(appState: appState)
              : _MapWithSheet(appState: appState, data: data),
    );
  }
}

/// Full-screen error view when the bundled asset cannot be parsed.
class _DataErrorView extends StatelessWidget {
  const _DataErrorView({required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            const Text(
              'Failed to load city data',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              appState.dataError ?? 'Unknown error.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: appState.loadData,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }
}

/// The map layer + "locate me" button + draggable destination sheet.
class _MapWithSheet extends StatefulWidget {
  const _MapWithSheet({required this.appState, required this.data});

  final AppState appState;
  final AppData data;

  @override
  State<_MapWithSheet> createState() => _MapWithSheetState();
}

class _MapWithSheetState extends State<_MapWithSheet> {
  /// Fallback map centre near Sagay City when no GPS fix is available.
  static const LatLng _sagayCenter = LatLng(10.92, 123.45);

  final MapController _mapController = MapController();
  final TextEditingController _searchController = TextEditingController();

  String _query = '';
  bool _mapReady = false;
  Position? _lastCenteredPosition;

  @override
  void initState() {
    super.initState();
    _lastCenteredPosition = widget.appState.location.position;
  }

  @override
  void dispose() {
    _searchController.dispose();
    _mapController.dispose();
    super.dispose();
  }

  /// Recenters the map once whenever a new GPS fix arrives. Safe to call from
  /// build: it only acts when a genuinely new position is present. The parent
  /// watches AppState, so this rebuilds on every location change.
  void _maybeRecenter() {
    final position = widget.appState.location.position;
    if (position != null && position != _lastCenteredPosition && _mapReady) {
      _lastCenteredPosition = position;
      _mapController.move(
        LatLng(position.latitude, position.longitude),
        14.5,
      );
    }
  }

  void _openResult(Destination destination) {
    final appState = context.read<AppState>();
    appState.selectDestination(destination);
    appState.loadDistance();
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const ResultScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    _maybeRecenter();
    final appState = widget.appState;
    final position = appState.location.position;
    final filtered = _filteredDestinations();

    return Stack(
      children: [
        FlutterMap(
          mapController: _mapController,
          options: MapOptions(
            initialCenter: position == null
                ? _sagayCenter
                : LatLng(position.latitude, position.longitude),
            initialZoom: 13.5,
            onMapReady: () => _mapReady = true,
            interactionOptions: const InteractionOptions(
              flags: InteractiveFlag.all & ~InteractiveFlag.rotate,
            ),
          ),
          children: [
            TileLayer(
              urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'ph.sagay.sagay_tourist_navigator',
            ),
            MarkerLayer(markers: _buildMarkers(position)),
          ],
        ),

        // Compass indicator: the map is locked north-up.
        const Positioned(
          top: 12,
          left: 12,
          child: CompassIndicator(size: 42),
        ),

        // Locate-me button, top right, away from the sheet.
        Positioned(
          top: 12,
          right: 12,
          child: FloatingActionButton.small(
            heroTag: 'locate-me',
            tooltip: 'Locate me',
            onPressed: appState.location.status == LocationStatus.loading
                ? null
                : () => appState.locateMe(),
            child: appState.location.status == LocationStatus.loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.my_location),
          ),
        ),

        // Destination sheet last, so it covers the button when expanded.
        DraggableScrollableSheet(
          initialChildSize: 0.34,
          minChildSize: 0.14,
          maxChildSize: 0.78,
          builder: (context, scrollController) {
            return Container(
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surface,
                borderRadius:
                    const BorderRadius.vertical(top: Radius.circular(20)),
                boxShadow: const [
                  BoxShadow(
                    color: Colors.black26,
                    blurRadius: 10,
                    offset: Offset(0, -2),
                  ),
                ],
              ),
              child: Column(
                children: [
                  // Drag handle.
                  Container(
                    width: 40,
                    height: 4,
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade400,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  // Search field.
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                    child: TextField(
                      controller: _searchController,
                      onChanged: (value) => setState(() => _query = value),
                      decoration: InputDecoration(
                        hintText: 'Search a destination or category…',
                        prefixIcon: const Icon(Icons.search),
                        suffixIcon: _query.isEmpty
                            ? null
                            : IconButton(
                                icon: const Icon(Icons.clear),
                                onPressed: () {
                                  _searchController.clear();
                                  setState(() => _query = '');
                                },
                              ),
                        isDense: true,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                  // Location problem banner (hidden when all is well).
                  _LocationBanner(appState: appState),
                  // Destination list.
                  Expanded(
                    child: filtered.isEmpty
                        ? const Center(
                            child: Text('No destinations match your search.'),
                          )
                        : ListView.builder(
                            controller: scrollController,
                            padding: const EdgeInsets.only(bottom: 16),
                            itemCount: filtered.length,
                            itemBuilder: (context, index) {
                              return _DestinationTile(
                                destination: filtered[index],
                                data: widget.data,
                                onTap: () => _openResult(filtered[index]),
                              );
                            },
                          ),
                  ),
                ],
              ),
            );
          },
        ),
      ],
    );
  }

  /// Filters destinations by name or category, case-insensitively.
  List<Destination> _filteredDestinations() {
    final query = _query.trim().toLowerCase();
    if (query.isEmpty) {
      return widget.data.destinations;
    }
    return widget.data.destinations
        .where(
          (d) =>
              d.name.toLowerCase().contains(query) ||
              d.category.toLowerCase().contains(query),
        )
        .toList();
  }

  /// User marker (blue dot) + one marker per destination, coloured by the
  /// terminal that serves it. Tapping a marker opens the result screen.
  List<Marker> _buildMarkers(Position? position) {
    final data = widget.data;
    final markers = <Marker>[];

    if (position != null) {
      markers.add(
        Marker(
          point: LatLng(position.latitude, position.longitude),
          width: 44,
          height: 44,
          child: DecoratedBox(
            decoration: const BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(color: Colors.black26, blurRadius: 4),
              ],
            ),
            child: const Padding(
              padding: EdgeInsets.all(4),
              child: Icon(
                Icons.my_location,
                color: Color(0xFF1565C0),
                size: 30,
              ),
            ),
          ),
        ),
      );
    }

    for (final destination in data.destinations) {
      final terminal = data.terminalForDestination(destination);
      final color = terminal?.uiColor ?? Colors.grey;
      markers.add(
        Marker(
          point: LatLng(destination.lat, destination.lng),
          width: 40,
          height: 40,
          child: GestureDetector(
            onTap: () => _openResult(destination),
            child: Icon(
              Icons.location_on,
              color: color,
              size: 40,
              shadows: const [
                Shadow(color: Colors.black38, blurRadius: 4),
              ],
            ),
          ),
        ),
      );
    }
    return markers;
  }
}

/// One row in the destination list.
class _DestinationTile extends StatelessWidget {
  const _DestinationTile({
    required this.destination,
    required this.data,
    required this.onTap,
  });

  final Destination destination;
  final AppData data;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final terminal = data.terminalForDestination(destination);
    final color = terminal?.uiColor ?? Colors.grey;
    return ListTile(
      leading: CircleAvatar(
        backgroundColor: color,
        child: const Icon(Icons.location_on, color: Colors.white, size: 20),
      ),
      title: Text(
        destination.name,
        style: const TextStyle(fontWeight: FontWeight.w600),
      ),
      subtitle: Text(
        '${destination.category} • ${terminal?.colorName ?? "Unknown"} tricycle',
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}

/// Amber banner explaining the current location problem, with a contextual
/// action (retry / open app settings / open GPS settings). Tourists can keep
/// browsing destinations while the banner is visible; distance and fare
/// simply need GPS.
class _LocationBanner extends StatelessWidget {
  const _LocationBanner({required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final status = appState.location.status;
    if (status == LocationStatus.ready || status == LocationStatus.loading) {
      return const SizedBox.shrink();
    }

    String actionLabel;
    if (status == LocationStatus.permissionPermanentlyDenied) {
      actionLabel = 'Open settings';
    } else if (status == LocationStatus.serviceDisabled) {
      actionLabel = 'Turn on GPS';
    } else {
      actionLabel = 'Retry';
    }

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.amber.shade100,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.location_disabled, size: 22, color: Colors.brown),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              appState.location.message ?? 'Location unavailable.',
              style: const TextStyle(fontSize: 13),
            ),
          ),
          TextButton(
            onPressed: appState.fixLocation,
            child: Text(actionLabel),
          ),
        ],
      ),
    );
  }
}
