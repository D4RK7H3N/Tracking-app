import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/route.dart' as route_model;
import '../services/settings_store.dart';
import '../state/app_state.dart';
import '../theme/motion.dart';
import '../theme/palette.dart';
import '../widgets/brand_banner.dart';
import 'home_screen.dart';

/// Three-page first-run introduction. The "seen" flag is persisted so the
/// introduction is shown only once; all page transitions respect reduce
/// motion through [Motion.duration].
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _controller = PageController();
  int _page = 0;

  static const _pages = <_OnboardingPage>[
    _OnboardingPage(
      icon: Icons.search,
      title: 'Find your way around Sagay',
      subtitle: 'Search by name or category, then tap a destination to see '
          'the road distance and the terminal that serves it.',
    ),
    _OnboardingPage(
      icon: Icons.directions_bus,
      title: 'Ask for the colour-coded vehicle',
      subtitle: 'Each route has its own colour and vehicle. Match the badge '
          'in the app with the tricycle, Reporm 2, bus or boat at the terminal.',
    ),
    _OnboardingPage(
      icon: Icons.payments_outlined,
      title: 'Official matrix fares',
      subtitle: 'Published tricycle fares are shown when the origin and '
          'destination are in the city matrix, with the 20% student / senior / '
          'PWD discount offered only for those fares.',
    ),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _goTo(int page) {
    if (page < 0 || page >= _pages.length) return;
    if (Motion.reduced(context)) {
      // PageController asserts on a zero-duration animation: jump instead.
      _controller.jumpToPage(page);
      return;
    }
    _controller.animateToPage(
      page,
      duration: Motion.duration(context, Motion.page),
      curve: Motion.standard,
    );
  }

  Future<void> _finish() async {
    await const SettingsStore().markOnboardingSeen();
    if (!mounted) return;
    final navigator = Navigator.of(context);
    navigator.pushReplacement(
      SagayPageRoute<void>(context: context, child: const HomeScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final routes = context.watch<AppState>().data?.routes ??
        const <route_model.Route>[];

    return Scaffold(
      backgroundColor: Palette.mist,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 0),
              child: Row(
                children: [
                  Text(
                    'Sagay Tourist Navigator',
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: Palette.deepOcean,
                    ),
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: _finish,
                    child: const Text('Skip'),
                  ),
                ],
              ),
            ),
            Expanded(
              child: PageView.builder(
                controller: _controller,
                itemCount: _pages.length,
                onPageChanged: (index) => setState(() => _page = index),
                itemBuilder: (context, index) =>
                    _buildPage(context, index, routes),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 20),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      for (var i = 0; i < _pages.length; i++)
                        AnimatedContainer(
                          duration: Motion.duration(context, Motion.layout),
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          width: i == _page ? 24 : 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: i == _page ? Palette.ocean : Palette.teal.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      if (_page > 0)
                        OutlinedButton(
                          onPressed: () => _goTo(_page - 1),
                          child: const Text('Back'),
                        )
                      else
                        const SizedBox(width: 100),
                      const Spacer(),
                      FilledButton(
                        onPressed: () => _page == _pages.length - 1
                            ? _finish()
                            : _goTo(_page + 1),
                        child: Text(_page == _pages.length - 1
                            ? 'Get started'
                            : 'Next'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPage(
    BuildContext context,
    int index,
    List<route_model.Route> routes,
  ) {
    final page = _pages[index];
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BrandBanner(
            icon: page.icon,
            title: page.title,
            subtitle: page.subtitle,
          ),
          const SizedBox(height: 20),
          if (index == 0) _searchDemo(),
          if (index == 1) _vehicleDemo(routes),
          if (index == 2) _fareDemo(),
        ],
      ),
    );
  }

  Widget _searchDemo() => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                decoration: BoxDecoration(
                  color: Palette.mist,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: Palette.teal.withValues(alpha: 0.3)),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.search, color: Palette.ocean),
                    SizedBox(width: 10),
                    Text('Try “Carbin Reef”…'),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              const _Tip('Name or category search'),
              const _Tip('Road distance and offline estimate'),
              const _Tip('Terminal and colour badge for each destination'),
            ],
          ),
        ),
      );

  Widget _vehicleDemo(List<route_model.Route> routes) => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Colours below come from the bundled route data',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              if (routes.isEmpty)
                const Text('Route data is still loading…')
              else
                for (final route in routes)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 9,
                          backgroundColor: route.uiColor,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            '${route.serviceLabel} — ${route.name}',
                            style: const TextStyle(fontSize: 14),
                          ),
                        ),
                      ],
                    ),
                  ),
            ],
          ),
        ),
      );

  Widget _fareDemo() => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const _Tip('Official matrix fare when the zones are published'),
              const _Tip('20% student / senior / PWD discount'),
              const _Tip('Group total for your whole party'),
              const SizedBox(height: 12),
              Text(
                'Boat and bus fares are not published. They are shown as '
                '"Not verified" instead of a made-up amount.',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Palette.deepOcean,
                    ),
              ),
            ],
          ),
        ),
      );
}

class _OnboardingPage {
  const _OnboardingPage({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  final IconData icon;
  final String title;
  final String subtitle;
}

class _Tip extends StatelessWidget {
  const _Tip(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.check_circle_outline,
                size: 18, color: Palette.teal),
            const SizedBox(width: 10),
            Expanded(child: Text(text)),
          ],
        ),
      );
}
