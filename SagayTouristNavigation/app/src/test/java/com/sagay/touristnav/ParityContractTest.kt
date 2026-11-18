package com.sagay.touristnav

import com.sagay.touristnav.data.local.SeedParser
import com.sagay.touristnav.domain.FareCalculator
import com.sagay.touristnav.domain.FareMatrixLookup
import com.sagay.touristnav.domain.FareZoneDetector
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ParityContractTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val seed = SeedParser.parse(File("src/main/assets/seed.json").readText())
    private val golden = json.parseToJsonElement(
        File("../../shared/sagay-contract/cases/golden.json").readText(),
    ).jsonObject

    private val origins = seed.fareOrigins.map {
        FareOrigin(it.zone, it.name, it.lat, it.lng, it.radiusKm)
    }
    private val matrix = seed.fareMatrix.map {
        FareMatrixEntry(it.origin, it.destination, it.regular, it.discounted)
    }
    private val rule = FareRule(
        id = seed.fareRule?.id ?: 1,
        baseFare = seed.fareRule?.baseFare ?: 15.0,
        baseKm = seed.fareRule?.baseKm ?: 3.0,
        extraPerKm = seed.fareRule?.extraPerKm ?: 2.0,
        source = seed.fareRule?.source.orEmpty(),
        maxFare = seed.fareRule?.maxFare,
    )

    private fun text(value: JsonElement?, key: String): String? {
        val element = value?.jsonObject?.get(key) ?: return null
        if (element is JsonNull) return null
        return element.jsonPrimitive.content
    }

    private fun number(value: kotlinx.serialization.json.JsonElement, key: String): Double =
        value.jsonObject.getValue(key).jsonPrimitive.content.toDouble()

    private fun bool(value: kotlinx.serialization.json.JsonElement, key: String): Boolean =
        value.jsonObject.getValue(key).jsonPrimitive.content.toBoolean()

    @Test
    fun `golden fare cases match the native fare rules`() {
        golden.getValue("fare").jsonArray.forEach { item ->
            val originZone = text(item, "originZone")!!
            val destinationZone = text(item, "destinationZone")!!
            val roadKm = number(item, "roadKm")
            val discounted = bool(item, "discounted")
            val origin = origins.firstOrNull { it.zone.equals(originZone, ignoreCase = true) }
            val entry = origin?.let { FareMatrixLookup.find(matrix, it.zone, destinationZone) }
            val amount = if (entry != null) {
                if (discounted) entry.discountedFare else entry.regularFare
            } else {
                FareCalculator.compute(rule, roadKm)
            }
            assertEquals(
                "case ${text(item, "id")} amount",
                number(item, "expectedAmount"),
                amount,
                0.001,
            )
            assertEquals(
                "case ${text(item, "id")} official",
                bool(item, "expectedOfficial"),
                entry != null,
            )
        }
    }

    @Test
    fun `golden zone cases match the native zone detector`() {
        golden.getValue("zones").jsonArray.forEach { item ->
            val origin = FareZoneDetector.detect(
                number(item, "lat"),
                number(item, "lng"),
                origins,
            )
            val expectedZone = text(item, "expectedZone")
            if (expectedZone == null) {
                assertNull("case ${text(item, "id")}", origin)
            } else {
                assertEquals("case ${text(item, "id")} zone", expectedZone, origin?.zone)
                assertEquals(
                    "case ${text(item, "id")} name",
                    text(item, "expectedName"),
                    origin?.name,
                )
            }
        }
    }

    @Test
    fun `golden journey cases match the native access rules`() {
        golden.getValue("journeys").jsonArray.forEach { item ->
            val originZone = text(item, "originZone")!!
            val destinationRouteId = text(item, "destinationRouteId")!!
            val destinationRoute = seed.routes.first { it.id == destinationRouteId }
            val access = seed.routeAccess.firstOrNull {
                it.originZone.equals(originZone, ignoreCase = true) &&
                    it.destinationRouteId.equals(destinationRouteId, ignoreCase = true) &&
                    seed.routes.any { route -> route.id == it.viaRouteId }
            }
            val viaRoute = access?.let { rule -> seed.routes.first { it.id == rule.viaRouteId } }
            val services = buildList {
                if (viaRoute != null && viaRoute.id != destinationRoute.id) add(viaRoute.id)
                add(destinationRoute.id)
            }
            val expectedServices = item.jsonObject.getValue("expectedServices").jsonArray
                .map { it.jsonPrimitive.content }
            assertEquals("case ${text(item, "id")} services", expectedServices, services)
            val expectedChange = text(item, "expectedChangeAt")
            assertEquals(
                "case ${text(item, "id")} change point",
                expectedChange,
                if (services.size > 1) access?.changeAt else null,
            )
            val fareZone = if (services.size > 1) access?.fareZone else text(item, "destinationZone")
            assertEquals("case ${text(item, "id")} fare zone", text(item, "expectedFareZone"), fareZone)

            val origin = origins.first { it.zone.equals(originZone, ignoreCase = true) }
            val entry = FareMatrixLookup.find(matrix, origin.zone, fareZone.orEmpty())!!
            assertEquals(
                "case ${text(item, "id")} regular",
                number(item, "expectedRegular"),
                entry.regularFare,
                0.001,
            )
            assertEquals(
                "case ${text(item, "id")} discounted",
                number(item, "expectedDiscounted"),
                entry.discountedFare,
                0.001,
            )
        }
    }

    @Test
    fun `golden group cases match the native totals`() {
        golden.getValue("groups").jsonArray.forEach { item ->
            val total = number(item, "unitAmount") *
                item.jsonObject.getValue("passengers").jsonPrimitive.content.toInt()
            assertEquals(
                "case ${text(item, "id")}",
                number(item, "expectedTotal"),
                total,
                0.001,
            )
        }
    }
}
