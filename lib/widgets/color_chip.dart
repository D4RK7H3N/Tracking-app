import 'package:flutter/material.dart';

/// Large, high-contrast swatch showing a terminal's tricycle colour.
/// Tourists identify the correct tricycle queue by this colour, so the chip
/// is intentionally big and the label is easy to read from arm's length.
class ColorChip extends StatelessWidget {
  const ColorChip({
    super.key,
    required this.color,
    required this.label,
    this.height = 72,
  });

  final Color color;
  final String label;
  final double height;

  @override
  Widget build(BuildContext context) {
    final textColor =
        color.computeLuminance() > 0.5 ? Colors.black87 : Colors.white;
    return Container(
      height: height,
      width: double.infinity,
      decoration: const BoxDecoration(
        boxShadow: [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 6,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Center(
          child: Text(
            label.toUpperCase(),
            textAlign: TextAlign.center,
            style: TextStyle(
              color: textColor,
              fontSize: 22,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.2,
            ),
          ),
        ),
      ),
    );
  }
}
