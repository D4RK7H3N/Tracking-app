import 'fare_rule.dart';

/// What the displayed fare means, scoped to the vehicle carrying the trip.
///
/// Mirrors the native `FareScope` contract: the city publishes fares for
/// motorized tricycles only. The matrix may be applied to tricycle and
/// Reporm 2 rides; a bus ride has no published fare; an island trip is a
/// tricycle ride to the port plus a boat leg whose fare is unpublished.
/// The two legs are never added together into one claimed total.
sealed class FareScope {
  const FareScope();

  /// True only for an official matrix fare: the published 20% discount
  /// applies to no other scope (not even to a distance estimate).
  bool get canDiscount;

  /// Whether the amount comes from the official fare matrix.
  bool get isOfficial;

  /// The amount to show, or 0 when no fare can be claimed.
  double get amount;

  /// Full explanation shown under the headline.
  String get basis;

  /// Short label next to the headline.
  String get label;

  /// Headline text: a peso amount for a priced ride, otherwise a warning.
  String headline(String Function(double amount) formatFare);
}

/// A tricycle or Reporm 2 ride priced by the matrix or by distance.
class MotorizedTricycleFare extends FareScope {
  const MotorizedTricycleFare({
    required this.amount,
    required this.isOfficial,
    required this.basis,
    this.qualifier = '',
  });

  @override
  final double amount;

  @override
  final bool isOfficial;

  @override
  final String basis;

  /// Extra wording when a zone fare is applied to a non-tricycle body.
  final String qualifier;

  @override
  bool get canDiscount => isOfficial;

  @override
  String get label => isOfficial ? 'official matrix fare' : 'estimated fare';

  @override
  String headline(String Function(double amount) formatFare) => formatFare(amount);

  /// Basis including the vehicle qualifier, when one applies.
  String get displayBasis => '$basis$qualifier';
}

/// The city publishes no bus fare.
class BusFare extends FareScope {
  const BusFare();

  @override
  bool get canDiscount => false;

  @override
  bool get isOfficial => false;

  @override
  double get amount => 0;

  @override
  String get basis =>
      'Bus fare not verified: the published matrix prices tricycle rides only.';

  @override
  String get label => 'fare not verified';

  @override
  String headline(String Function(double amount) formatFare) => 'Not verified';
}

/// An island trip: the land leg to the port is priced; the boat is not.
class IslandFare extends FareScope {
  const IslandFare({required this.tricycleToPort});

  final MotorizedTricycleFare tricycleToPort;

  @override
  bool get canDiscount => tricycleToPort.canDiscount;

  @override
  bool get isOfficial => tricycleToPort.isOfficial;

  @override
  double get amount => tricycleToPort.amount;

  @override
  String get label => isOfficial ? 'tricycle fare to the port' : 'estimated fare to the port';

  @override
  String headline(String Function(double amount) formatFare) =>
      tricycleToPort.headline(formatFare);

  @override
  String get basis =>
      'Tricycle fare to the port: ${tricycleToPort.displayBasis}\n'
      'Boat fare not verified';
}

/// No known route, so no fare can be claimed.
class UnverifiedFare extends FareScope {
  const UnverifiedFare();

  @override
  bool get canDiscount => false;

  @override
  bool get isOfficial => false;

  @override
  double get amount => 0;

  @override
  String get basis =>
      'Fare not verified: local public-transport details need confirmation.';

  @override
  String get label => 'fare not verified';

  @override
  String headline(String Function(double amount) formatFare) => 'Not verified';
}

/// The fare math plus the meaning of that fare for this vehicle.
class FareQuote {
  const FareQuote({required this.estimate, required this.scope});

  /// The underlying estimate (kept intact for history and parity tests).
  final FareEstimate estimate;

  final FareScope scope;
}

/// Builds the [FareScope] for a route from the already computed [estimate].
///
/// An island trip always prices its land leg as a tricycle ride. A bus or an
/// unknown vehicle keeps its distance but carries no claimable fare.
FareScope scopeForFare({
  required FareEstimate estimate,
  required String? vehicle,
  required bool requiresBoat,
}) {
  final tricycle = MotorizedTricycleFare(
    amount: estimate.amount,
    isOfficial: estimate.official,
    basis: estimate.basis,
  );
  if (requiresBoat) return IslandFare(tricycleToPort: tricycle);

  switch (vehicle?.trim().toLowerCase()) {
    case 'tricycle':
      return tricycle;
    case 'reporm2':
      // The zone matrix covers the motorized tricycle body; the Reporm 2
      // fare itself is not published, so say so in the basis.
      return MotorizedTricycleFare(
        amount: estimate.amount,
        isOfficial: estimate.official,
        basis: estimate.basis,
        qualifier: estimate.official
            ? ' (zone fare; reporm 2 fare not published)'
            : '',
      );
    case 'bus':
      return const BusFare();
    default:
      return const UnverifiedFare();
  }
}
