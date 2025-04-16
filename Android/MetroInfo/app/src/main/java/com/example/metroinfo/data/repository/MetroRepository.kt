package com.example.metroinfo.data.repository

import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.model.NearestStationsResponse
import com.example.metroinfo.network.ApiService
import javax.inject.Inject

class MetroRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNearbyStations(
        latitude: Double,
        longitude: Double
    ): List<NearbyStation> {
        val response = apiService.getNearestStations(
            latitude = latitude,
            longitude = longitude
        )
        return if (response.isSuccessful) {
            response.body()?.nearestStations?.map { station ->
                NearbyStation(
                    id = station.stationId,
                    name = station.nameCn,
                    nameEn = station.nameEn,
                    distance = station.distanceM.toDouble() / 1000.0, // 转换为公里
                    lines = station.associatedLines
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // ... existing code ...
} 