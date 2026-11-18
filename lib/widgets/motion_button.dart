import 'package:flutter/material.dart';

import '../theme/motion.dart';

class MotionIconButton extends StatefulWidget {
  const MotionIconButton({
    super.key,
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback onPressed;

  @override
  State<MotionIconButton> createState() => _MotionIconButtonState();
}

class _MotionIconButtonState extends State<MotionIconButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _scale = AnimationController(
    vsync: this,
    duration: Motion.micro,
    lowerBound: 0.86,
    upperBound: 1,
  )..value = 1;

  @override
  void dispose() {
    _scale.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Listener(
      onPointerDown: (_) => _scale.reverse(),
      onPointerUp: (_) => _scale.forward(),
      onPointerCancel: (_) => _scale.forward(),
      child: ScaleTransition(
        scale: _scale,
        child: IconButton(
          icon: Icon(widget.icon),
          tooltip: widget.tooltip,
          onPressed: widget.onPressed,
        ),
      ),
    );
  }
}
