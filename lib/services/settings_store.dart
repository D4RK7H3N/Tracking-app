import 'package:shared_preferences/shared_preferences.dart';

/// Persisted user preferences that are not part of trip data.
class SettingsStore {
  const SettingsStore();

  static const reduceMotionKey = 'sagay.reduce_motion';
  static const onboardingKey = 'sagay.onboarding_seen';

  Future<bool?> reduceMotion() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(reduceMotionKey);
  }

  Future<void> setReduceMotion(bool? value) async {
    final prefs = await SharedPreferences.getInstance();
    if (value == null) {
      await prefs.remove(reduceMotionKey);
    } else {
      await prefs.setBool(reduceMotionKey, value);
    }
  }

  Future<bool> onboardingSeen() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(onboardingKey) ?? false;
  }

  Future<void> markOnboardingSeen() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(onboardingKey, true);
  }
}
