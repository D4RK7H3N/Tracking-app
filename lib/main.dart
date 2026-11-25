import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'screens/home_screen.dart';
import 'screens/onboarding_screen.dart';
import 'services/data_service.dart';
import 'services/distance_service.dart';
import 'services/location_service.dart';
import 'services/settings_store.dart';
import 'services/trip_store.dart';
import 'state/app_state.dart';
import 'theme/palette.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final showOnboarding =
      !(await const SettingsStore().onboardingSeen());
  runApp(SagayTouristNavigatorApp(showOnboarding: showOnboarding));
}

/// Root widget: wires up the services + central [AppState] via `provider`
/// and hosts the first-run onboarding or the home screen.
class SagayTouristNavigatorApp extends StatelessWidget {
  const SagayTouristNavigatorApp({super.key, this.showOnboarding = false});

  /// Weakened by tests that only need the theme; main() resolves the real
  /// first-run state from SharedPreferences before calling runApp.
  final bool showOnboarding;

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<DataService>(create: (_) => DataService()),
        Provider<LocationService>(create: (_) => LocationService()),
        Provider<DistanceService>(create: (_) => DistanceService()),
        Provider<TripStore>(create: (_) => TripStore()),
        ChangeNotifierProvider<AppState>(
          create: (context) => AppState(
            dataService: context.read<DataService>(),
            locationService: context.read<LocationService>(),
            distanceService: context.read<DistanceService>(),
          )
            // Load the bundled city data and persisted settings immediately.
            // Location is requested by the home screen once it is visible.
            ..loadData()
            ..loadSettings()
            ..refreshMapTiles(),
        ),
      ],
      child: MaterialApp(
        title: 'Sagay Tourist Navigator',
        debugShowCheckedModeBanner: false,
        theme: _buildTheme(),
        home: showOnboarding ? const OnboardingScreen() : const HomeScreen(),
        builder: (context, child) {
          // The settings screen can persist a reduce-motion override. Apply
          // it through MediaQuery so every Motion.duration()/reduced() call
          // sees it without threading extra state through each widget.
          final appState = context.read<AppState>();
          return ValueListenableBuilder<bool?>(
            valueListenable: appState.motionOverride,
            builder: (context, override, child) {
              final media = MediaQuery.of(context);
              return MediaQuery(
                data: media.copyWith(
                  disableAnimations: override ?? media.disableAnimations,
                ),
                child: child ?? const SizedBox.shrink(),
              );
            },
            child: child,
          );
        },
      ),
    );
  }

  ThemeData _buildTheme() {
    final scheme = ColorScheme.fromSeed(
      seedColor: Palette.ocean,
      brightness: Brightness.light,
    ).copyWith(
      primary: Palette.ocean,
      onPrimary: Colors.white,
      primaryContainer: const Color(0xFFD9ECF5),
      onPrimaryContainer: Palette.deepOcean,
      secondary: Palette.teal,
      onSecondary: Colors.white,
      tertiary: Palette.coral,
      onTertiary: Palette.deepOcean,
      surface: Palette.mist,
      onSurface: Palette.ink,
      surfaceContainerHighest: const Color(0xFFE8F0F1),
      error: Palette.coral,
      onError: Colors.white,
    );
    final base = ThemeData(useMaterial3: true, colorScheme: scheme);
    final shape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(20));
    return base.copyWith(
      scaffoldBackgroundColor: Palette.mist,
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 0.6,
        shadowColor: const Color(0x1A072A42),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        margin: const EdgeInsets.only(bottom: 14),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Palette.mist,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: TextStyle(
          color: Palette.deepOcean,
          fontSize: 20,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.4,
        ),
        iconTheme: IconThemeData(color: Palette.ocean),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: Palette.ocean,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
          textStyle: const TextStyle(fontWeight: FontWeight.w700, letterSpacing: -0.2),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: Palette.ocean,
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: Palette.ocean,
          side: const BorderSide(color: Palette.ocean, width: 1.4),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(20)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: const OutlineInputBorder(
          borderRadius: BorderRadius.all(Radius.circular(20)),
          borderSide: BorderSide(color: Palette.ocean, width: 2),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Palette.mist,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: Palette.mist,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: shape,
      ),
      textTheme: base.textTheme.copyWith(
        displaySmall: base.textTheme.displaySmall
            ?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -1.5),
        headlineSmall: base.textTheme.headlineSmall
            ?.copyWith(fontWeight: FontWeight.w800, letterSpacing: -1.0),
        titleLarge: base.textTheme.titleLarge
            ?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.6),
        titleMedium: base.textTheme.titleMedium
            ?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.3),
        bodyLarge: base.textTheme.bodyLarge?.copyWith(height: 1.45),
        bodyMedium: base.textTheme.bodyMedium?.copyWith(height: 1.4),
      ),
    );
  }
}
