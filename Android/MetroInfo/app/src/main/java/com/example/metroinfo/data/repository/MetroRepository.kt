package com.example.metroinfo.data.repository

import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.data.model.LineInfo
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
            response.body()?.map { station ->
                val associatedLines = station.associatedLines?.split(",")?.map { it.trim().toInt() } ?: emptyList()
                val lines = if (associatedLines.isNotEmpty()) {
                    associatedLines.map { lineId ->
                        LineInfo(
                            id = lineId,
                            nameCn = "${lineId}号线",
                            nameEn = "Line $lineId"
                        )
                    }
                } else {
                    listOf(LineInfo(
                        id = station.lineId,
                        nameCn = "${station.lineId}号线",
                        nameEn = "Line ${station.lineId}"
                    ))
                }
                
                NearbyStation(
                    id = station.stationId,
                    name = station.nameCn,
                    nameEn = station.nameEn,
                    distance = station.distance,
                    lines = lines
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // ... existing code ...
} 