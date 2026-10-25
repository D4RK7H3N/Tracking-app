import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_compass/flutter_compass.dart';

/// A live compass badge drawn over the maps.
///
/// The red needle keeps pointing at the top edge of the map (true map
/// north) as the tourist turns the phone, because the map is locked
/// north-up. The needle is low-pass filtered and animated so it glides
/// smoothly instead of jumping with every noisy sensor reading.
///
/// A small status dot shows the sensor state:
///   green  -> heading is reliable
///   amber  -> magnetometer needs calibration (wave the phone in a figure-8)
///   grey   -> no heading available on this device
class CompassIndicator extends StatefulWidget {
  const CompassIndicator({super.key, this.size = 42});

  final double size;

  @override
  State<CompassIndicator> createState() => _CompassIndicatorState();
}

class _CompassIndicatorState extends State<CompassIndicator> {
  StreamSubscription<CompassEvent>? _subscription;

  /// Smoothed needle angle in radians (0 = pointing up = map north).
  double _needleAngle = 0;
  bool _hasHeading = false;
  bool _calibrated = false;

  @override
  void initState() {
    super.initState();
    _subscription = FlutterCompass.events?.listen(_onEvent);
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  void _onEvent(CompassEvent event) {
    final heading = event.heading;
    if (heading == null) {
      if (mounted && _hasHeading) {
        setState(() => _hasHeading = false);
      }
      return;
    }
    // The needle must keep pointing at map-north, so it rotates opposite
    // to the device heading.
    final target = -heading * math.pi / 180.0;
    // Shortest-path low-pass filter: glides towards the new angle instead
    // of jumping, and always takes the short way around the circle.
    var diff = (target - _needleAngle) % (2 * math.pi);
    if (diff > math.pi) diff -= 2 * math.pi;
    if (diff < -math.pi) diff += 2 * math.pi;
    final accuracy = event.accuracy;
    if (!mounted) return;
    setState(() {
      _hasHeading = true;
      _calibrated = accuracy != null && accuracy < 15;
      _needleAngle += diff * 0.25;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: widget.size,
      height: widget.size,
      decoration: BoxDecoration(
        color: Colors.white,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.black12),
        boxShadow: const [
          BoxShadow(color: Colors.black26, blurRadius: 4),
        ],
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          AnimatedRotation(
            turns: _needleAngle / (2 * math.pi),
            duration: const Duration(milliseconds: 250),
            child: Icon(
              Icons.navigation,
              size: widget.size * 0.55,
              // Faded until the sensor actually delivers a heading.
              color: _hasHeading
                  ? const Color(0xFFD32F2F)
                  : Colors.blueGrey.shade300,
            ),
          ),
          // Sensor status dot.
          Positioned(
            bottom: 3,
            child: Container(
              width: 5,
              height: 5,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: !_hasHeading
                    ? Colors.grey
                    : _calibrated
                        ? const Color(0xFF2E7D32)
                        : Colors.amber,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
