package com.example.metroinfo.model

data class NearestStationsResponse(
    val userLocation: UserLocation,
    val nearestStations: List<StationInfo>
)

data class UserLocation(
    val lat: Double,
    val lng: Double
) 