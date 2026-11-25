import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:sagay_tourist_navigator/screens/onboarding_screen.dart';
import 'package:sagay_tourist_navigator/services/data_service.dart';
import 'package:sagay_tourist_navigator/services/distance_service.dart';
import 'package:sagay_tourist_navigator/services/location_service.dart';
import 'package:sagay_tourist_navigator/state/app_state.dart';
import 'package:sagay_tourist_navigator/theme/motion.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('onboarding walks through all three pages', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: ChangeNotifierProvider<AppState>(
          create: (_) => AppState(
            dataService: DataService(),
            locationService: LocationService(),
            distanceService: DistanceService(),
          ),
          child: const OnboardingScreen(),
        ),
      ),
    );

    expect(find.text('Find your way around Sagay'), findsOneWidget);
    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    expect(find.text('Ask for the colour-coded vehicle'), findsOneWidget);
    await tester.tap(find.text('Next'));
    await tester.pumpAndSettle();
    expect(find.text('Official matrix fares'), findsOneWidget);
    expect(find.text('Get started'), findsOneWidget);
  });

  testWidgets('reduce motion makes page changes instantaneous', (tester) async {
    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(disableAnimations: true),
        child: MaterialApp(
          home: ChangeNotifierProvider<AppState>(
            create: (_) => AppState(
              dataService: DataService(),
              locationService: LocationService(),
              distanceService: DistanceService(),
            ),
            child: const OnboardingScreen(),
          ),
        ),
      ),
    );

    final context = tester.element(find.byType(OnboardingScreen));
    expect(Motion.reduced(context), isTrue);
    await tester.tap(find.text('Next'));
    await tester.pump();
    expect(find.text('Ask for the colour-coded vehicle'), findsOneWidget);
  });
}
