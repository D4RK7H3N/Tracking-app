package com.sagay.touristnav.data.local

import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.Terminal

fun TerminalEntity.toDomain(): Terminal = Terminal(
    id = id,
    name = name,
    lat = lat,
    lng = lng,
    colorHex = colorHex,
    colorName = colorName,
    serviceArea = serviceArea,
)

fun DestinationEntity.toDomain(): Destination = Destination(
    id = id,
    name = name,
    category = category,
    lat = lat,
    lng = lng,
    terminalId = terminalId,
    requiresBoat = requiresBoat,
)

fun FareRuleEntity.toDomain(): FareRule = FareRule(
    id = id,
    baseFare = baseFare,
    baseKm = baseKm,
    extraPerKm = extraPerKm,
    source = source,
    maxFare = maxFare,
)
