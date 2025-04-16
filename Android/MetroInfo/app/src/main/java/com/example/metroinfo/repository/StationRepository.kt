package com.example.metroinfo.repository

import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.Station
import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.NearestStationsResponse
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getStation(stationId: Int): Station? {
        val response = apiService.getStation(stationId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getStationArrivalTime(stationId: String): List<ArrivalInfo> {
        val response = apiService.getStationArrivalTime(stationId)
        return if (response.isSuccessful) {
            response.body()?.map { arrivalTimeInfo ->
                ArrivalInfo(
                    lineId = arrivalTimeInfo.lineId,
                    stationId = arrivalTimeInfo.stationId,
                    stationName = arrivalTimeInfo.stationName,
                    directionDesc = arrivalTimeInfo.directionDesc,
                    firstArrivalTime = arrivalTimeInfo.firstArrivalTime,
                    nextArrivalTime = "",
                    minutesRemaining = arrivalTimeInfo.minutesRemaining,
                    lineName = arrivalTimeInfo.lineName,
                    arrivalTime = arrivalTimeInfo.firstArrivalTime
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    suspend fun getNearestStations(latitude: Double, longitude: Double): List<Station> {
        val response = apiService.getNearestStations(latitude, longitude)
        return if (response.isSuccessful) {
            response.body()?.nearestStations?.map { station ->
                Station(
                    stationId = station.stationId.toString(),
                    nameCn = station.nameCn,
                    nameEn = station.nameEn,
                    lineId = station.lineInfo.line,
                    distance = station.distanceM.toDouble() / 1000.0,
                    associatedLines = station.associatedLines
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getStations(): List<Station> {
        val response = apiService.getStations()
        return if (response.isSuccessful) {
            response.body()?.map { station ->
                Station(
                    stationId = station.stationId.toString(),
                    nameCn = station.nameCn,
                    nameEn = station.nameEn,
                    lineId = station.lineId,
                    distance = station.distance,
                    associatedLines = station.associatedLines
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
} 