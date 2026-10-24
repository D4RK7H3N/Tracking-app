import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'screens/home_screen.dart';
import 'services/data_service.dart';
import 'services/distance_service.dart';
import 'services/location_service.dart';
import 'state/app_state.dart';

void main() {
  runApp(const SagayTouristNavigatorApp());
}

/// Root widget: wires up the services + central [AppState] via `provider`
/// and hosts the home screen.
class SagayTouristNavigatorApp extends StatelessWidget {
  const SagayTouristNavigatorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<DataService>(create: (_) => DataService()),
        Provider<LocationService>(create: (_) => LocationService()),
        Provider<DistanceService>(create: (_) => DistanceService()),
        ChangeNotifierProvider<AppState>(
          create: (context) => AppState(
            dataService: context.read<DataService>(),
            locationService: context.read<LocationService>(),
            distanceService: context.read<DistanceService>(),
          )
            // Load the bundled city data immediately. Location is requested
            // by the home screen once it is visible.
            ..loadData(),
        ),
      ],
      child: MaterialApp(
        title: 'Sagay Tourist Navigator',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF00796B), // sea / island teal
          ),
        ),
        home: const HomeScreen(),
      ),
    );
  }
}
