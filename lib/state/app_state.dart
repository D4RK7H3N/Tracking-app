import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart' show Position;
import 'package:latlong2/latlong.dart';

import '../models/app_data.dart';
import '../models/destination.dart';
import '../services/data_service.dart';
import '../services/distance_service.dart';
import '../services/location_service.dart';

/// Lifecycle of the distance lookup shown on the result screen.
enum DistanceLoadState { idle, loading, done, error }

/// Immutable snapshot of the GPS pipeline for the UI.
@immutable
class LocationState {
  const LocationState({
    this.status = LocationStatus.loading,
    this.position,
    this.message,
  });

  final LocationStatus status;
  final Position? position;
  final String? message;
}

/// Central app state, exposed through `provider` + [ChangeNotifier].
///
/// This class only ORCHESTRATES: all heavy lifting (JSON parsing, GPS,
/// OSRM routing, fare math) lives in the services so it stays unit-testable.
class AppState extends ChangeNotifier {
  AppState({
    required this._dataService,
    required this._locationService,
    required this._distanceService,
  });

  final DataService _dataService;
  final LocationService _locationService;
  final DistanceService _distanceService;


  // ---- bundled city data ----
  AppData? _data;
  AppData? get data => _data;
  bool dataLoading = false;
  String? dataError;

  // ---- location ----
  LocationState location = const LocationState();

  // ---- selected destination + its distance ----
  Destination? selectedDestination;
  DistanceLoadState distanceState = DistanceLoadState.idle;
  DistanceResult? distanceResult;
  String? distanceError;

  /// Loads the bundled JSON asset.
  Future<void> loadData() async {
    dataLoading = true;
    dataError = null;
    notifyListeners();
    try {
      _data = await _dataService.load();
    } catch (e) {
      dataError = 'Could not load the city data file.\n($e)';
    }
    dataLoading = false;
    notifyListeners();
  }

  /// Requests permission (if needed) and reads one position fix.
  Future<void> locateMe() async {
    location = const LocationState(status: LocationStatus.loading);
    notifyListeners();
    final outcome = await _locationService.getCurrentLocation();
    location = LocationState(
      status: outcome.status,
      position: outcome.position,
      message: outcome.message,
    );
    notifyListeners();
  }

  /// One button that offers the right fix for the current location problem:
  /// open app settings when permission is permanently denied, open GPS
  /// settings when services are off, otherwise simply retry.
  Future<void> fixLocation() async {
    if (location.status == LocationStatus.permissionPermanentlyDenied) {
      await _locationService.openAppSettings();
    } else if (location.status == LocationStatus.serviceDisabled) {
      await _locationService.openLocationSettings();
    } else {
      await locateMe();
    }
  }

  /// Selects a destination and resets any previous distance lookup.
  void selectDestination(Destination destination) {
    selectedDestination = destination;
    distanceState = DistanceLoadState.idle;
    distanceResult = null;
    distanceError = null;
    notifyListeners();
  }

  /// Fetches the distance (road route or offline estimate) for the selected
  /// destination from the user's current position.
  Future<void> loadDistance() async {
    final destination = selectedDestination;
    if (destination == null) {
      return;
    }
    final position = location.position;
    if (position == null) {
      // No GPS: browsing still works, but distance and fare cannot be shown.
      distanceState = DistanceLoadState.error;
      distanceResult = null;
      distanceError =
          'Distance and fare need GPS. Enable location and allow permission, '
          'then try again.';
      notifyListeners();
      return;
    }
    // Tag this request so a slow response for an OLD selection can never
    // overwrite the route of a NEWLY selected destination.
    final requestId = ++_distanceRequestId;
    distanceState = DistanceLoadState.loading;
    distanceError = null;
    notifyListeners();

    // Sea destinations: OSRM's road graph has no roads on water, so routing
    // straight to a reef/island snaps the endpoint to a random coastal road
    // (the "line pointing to the wrong barangay" bug). The tricycle leg must
    // instead end at the boat terminal; the sea leg is drawn separately.
    final terminal = _data?.terminalForDestination(destination);
    final roadTarget = (destination.boat && terminal != null)
        ? LatLng(terminal.lat, terminal.lng)
        : LatLng(destination.lat, destination.lng);

    final roadFactor = _data?.fareRule.roadFactor ?? 1.25;
    final result = await _distanceService.getDistanceKm(
      from: LatLng(position.latitude, position.longitude),
      to: roadTarget,
      roadFactor: roadFactor,
    );

    // A newer selection happened while we were waiting -> ignore this result.
    if (requestId != _distanceRequestId ||
        selectedDestination?.id != destination.id) {
      return;
    }
    distanceResult = result;
    distanceState = DistanceLoadState.done;
    notifyListeners();
  }

  /// Monotonic counter used to discard stale distance responses.
  int _distanceRequestId = 0;

  /// Retry entry point for the result screen's retry button.
  void retryDistance() => loadDistance();
}
