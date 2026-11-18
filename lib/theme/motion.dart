import 'package:flutter/material.dart';

class Motion {
  static const Duration micro = Duration(milliseconds: 120);
  static const Duration layout = Duration(milliseconds: 220);
  static const Duration page = Duration(milliseconds: 320);
  static const Curve standard = Curves.easeOutCubic;

  static bool reduced(BuildContext context) =>
      MediaQuery.maybeOf(context)?.disableAnimations ?? false;

  static Duration duration(BuildContext context, Duration base) =>
      reduced(context) ? Duration.zero : base;
}

class SagayPageRoute<T> extends PageRouteBuilder<T> {
  SagayPageRoute({
    required this.child,
    required BuildContext context,
  }) : super(
          transitionDuration: Motion.duration(context, Motion.page),
          reverseTransitionDuration: Motion.duration(context, Motion.page),
          pageBuilder: (_, _, _) => child,
          transitionsBuilder: (_, animation, _, child) {
            final curved = CurvedAnimation(parent: animation, curve: Motion.standard);
            return SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0.08, 0),
                end: Offset.zero,
              ).animate(curved),
              child: FadeTransition(opacity: curved, child: child),
            );
          },
        );

  final Widget child;
}
