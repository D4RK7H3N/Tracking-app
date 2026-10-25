import 'package:flutter/material.dart';

/// A small compass rose badge drawn over the maps. The map is locked to
/// north-up (rotation disabled), so the red needle always points to the
/// top edge of the map.
class CompassIndicator extends StatelessWidget {
  const CompassIndicator({super.key, this.size = 42});

  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: Colors.white,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.black12),
        boxShadow: const [
          BoxShadow(color: Colors.black26, blurRadius: 4),
        ],
      ),
      padding: const EdgeInsets.all(4),
      child: const CustomPaint(painter: _CompassPainter()),
    );
  }
}

class _CompassPainter extends CustomPainter {
  const _CompassPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final centre = Offset(size.width / 2, size.height / 2);
    final half = size.width / 2;

    // North needle (red), pointing up.
    final north = Path()
      ..moveTo(centre.dx, 2)
      ..lineTo(centre.dx - half * 0.32, centre.dy + half * 0.30)
      ..lineTo(centre.dx + half * 0.32, centre.dy + half * 0.30)
      ..close();
    canvas.drawPath(north, Paint()..color = const Color(0xFFD32F2F));

    // South needle (grey), pointing down.
    final south = Path()
      ..moveTo(centre.dx, size.height - 2)
      ..lineTo(centre.dx - half * 0.32, centre.dy - half * 0.30)
      ..lineTo(centre.dx + half * 0.32, centre.dy - half * 0.30)
      ..close();
    canvas.drawPath(south, Paint()..color = const Color(0xFF90A4AE));

    // "N" label over the north needle.
    final label = TextPainter(
      text: const TextSpan(
        text: 'N',
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.bold,
          color: Color(0xFFD32F2F),
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    label.paint(canvas, centre - Offset(label.width / 2, half * 0.92));
  }

  @override
  bool shouldRepaint(_CompassPainter oldDelegate) => false;
}
