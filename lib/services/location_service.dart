import 'dart:async';

import 'package:geolocator/geolocator.dart';

/// Where the GPS pipeline currently stands.
///
/// [loading] is set by the app state while a fix is in progress; the other
/// values are terminal outcomes produced by [LocationService.getCurrentLocation].
enum LocationStatus {
  loading,
  ready,
  permissionDenied,
  permissionPermanentlyDenied,
  serviceDisabled,
  unavailable,
}

/// Outcome of one attempt to read the device position.
class LocationOutcome {
  const LocationOutcome({required this.status, this.position, this.message});

  final LocationStatus status;
  final Position? position;

  /// Human-readable explanation for the UI (null when ready).
  final String? message;
}

/// Thin wrapper around geolocator that normalises every failure mode
/// (permission denied, permission denied forever, GPS switched off,
/// timeout) into a [LocationOutcome] the UI can render without knowing
/// anything about geolocator specifics.
class LocationService {
  /// Attempts to read one high-accuracy position fix.
  Future<LocationOutcome> getCurrentLocation() async {
    // 1. Are location services (GPS) switched on at all?
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return const LocationOutcome(
        status: LocationStatus.serviceDisabled,
        message:
            'Location services are turned off. Enable GPS so we can measure '
            'your distance and fare.',
      );
    }

    // 2. Do we have permission? Request it if it was never answered.
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return const LocationOutcome(
          status: LocationStatus.permissionDenied,
          message:
              'Location permission was denied. Allow it so we can measure '
              'your distance and fare.',
        );
      }
    }
    if (permission == LocationPermission.deniedForever) {
      return const LocationOutcome(
        status: LocationStatus.permissionPermanentlyDenied,
        message:
            'Location permission is permanently denied. Enable it in '
            'Settings to see distances and fares.',
      );
    }

    // 3. Read a fix with a time limit so the UI never hangs forever.
    try {
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 12),
        ),
      );
      return LocationOutcome(status: LocationStatus.ready, position: position);
    } on TimeoutException {
      return const LocationOutcome(
        status: LocationStatus.unavailable,
        message: 'It took too long to find your location. Please try again.',
      );
    } catch (_) {
      return const LocationOutcome(
        status: LocationStatus.unavailable,
        message: 'Your location could not be found. Please try again.',
      );
    }
  }

  /// Opens this app's page in Android Settings (permission toggle).
  Future<void> openAppSettings() => Geolocator.openAppSettings();

  /// Opens the system location settings page (GPS toggle).
  Future<void> openLocationSettings() => Geolocator.openLocationSettings();
}
