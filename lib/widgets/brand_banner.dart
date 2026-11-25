import 'package:flutter/material.dart';

import '../theme/palette.dart';

/// Warm gradient hero banner used on onboarding and information screens.
/// Only chrome — never used to recolour routes, vehicles or terminals.
class BrandBanner extends StatelessWidget {
  const BrandBanner({
    super.key,
    required this.title,
    this.subtitle,
    this.icon = Icons.explore_outlined,
    this.child,
  });

  final String title;
  final String? subtitle;
  final IconData icon;
  final Widget? child;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: Palette.warmGradient,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x1A072A42),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.28),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: Palette.deepOcean, size: 26),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    color: Palette.deepOcean,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.5,
                  ),
                ),
              ),
            ],
          ),
          if (subtitle != null) ...[
            const SizedBox(height: 10),
            Text(
              subtitle!,
              style: const TextStyle(
                color: Palette.ink,
                fontSize: 14,
                height: 1.4,
              ),
            ),
          ],
          if (child != null) ...[
            const SizedBox(height: 14),
            child!,
          ],
        ],
      ),
    );
  }
}
