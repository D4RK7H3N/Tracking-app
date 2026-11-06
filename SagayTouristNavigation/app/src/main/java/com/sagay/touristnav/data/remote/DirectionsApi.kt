package com.sagay.touristnav.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class DirectionsResponse(
    val code: String,
    val routes: List<RouteDto> = emptyList(),
)

@Serializable
data class RouteDto(
    val distance: Double,
    val duration: Double,
    val geometry: GeometryDto = GeometryDto(),
)

@Serializable
data class GeometryDto(
    val coordinates: List<List<Double>> = emptyList(),
)

class RouteException(message: String) : Exception(message)

interface DirectionsApi {

    /**
     * OSRM public routing server, driving profile.
     * [coordinates] must be "lng1,lat1;lng2,lat2".
     * Response shape matches the Mapbox Directions v5 schema this app's
     * DTOs parse (OSRM is the upstream of that format).
     */
    @GET("route/v1/driving/{coordinates}")
    suspend fun drivingRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
    ): DirectionsResponse
}
