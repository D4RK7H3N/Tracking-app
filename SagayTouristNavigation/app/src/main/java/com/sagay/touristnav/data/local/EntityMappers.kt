package com.sagay.touristnav.data.local

import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RouteAccess
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.RouteStop
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.VehicleType

fun TerminalEntity.toDomain(): Terminal = Terminal(
    id = id,
    name = name,
    lat = lat,
    lng = lng,
    colorHex = colorHex,
    colorName = colorName,
    serviceArea = serviceArea,
    // Unknown ids are dropped rather than failing the whole terminal.
    vehicles = vehicles.split(',')
        .mapNotNull { VehicleType.fromId(it.trim()) }
        .distinct(),
)

fun DestinationEntity.toDomain(): Destination = Destination(
    id = id,
    name = name,
    category = category,
    lat = lat,
    lng = lng,
    terminalId = terminalId,
    requiresBoat = requiresBoat,
    fareZone = fareZone,
    routeId = routeId,
)

fun FareRuleEntity.toDomain(): FareRule = FareRule(
    id = id,
    baseFare = baseFare,
    baseKm = baseKm,
    extraPerKm = extraPerKm,
    source = source,
    maxFare = maxFare,
)

fun FareMatrixEntity.toDomain(): FareMatrixEntry = FareMatrixEntry(
    originZone = originZone,
    destinationZone = destinationZone,
    regularFare = regularFare,
    discountedFare = discountedFare,
)

fun FareOriginEntity.toDomain(): FareOrigin = FareOrigin(
    zone = zone,
    name = name,
    lat = lat,
    lng = lng,
    radiusKm = radiusKm,
)

fun RouteEntity.toDomain(stops: List<RouteStopEntity>): Route = Route(
    id = id,
    name = name,
    terminalId = terminalId,
    colorHex = colorHex,
    colorName = colorName,
    vehicle = VehicleType.fromId(vehicle) ?: VehicleType.TRICYCLE,
    stops = stops
        .filter { it.routeId == id }
        .sortedBy { it.stopOrder }
        .map { RouteStop(order = it.stopOrder, name = it.name, lat = it.lat, lng = it.lng) },
)

fun RouteAccessEntity.toDomain(): RouteAccess = RouteAccess(
    originZone = originZone,
    destinationRouteId = destinationRouteId,
    viaRouteId = viaRouteId,
    changeAt = changeAt,
    fareZone = fareZone,
)
