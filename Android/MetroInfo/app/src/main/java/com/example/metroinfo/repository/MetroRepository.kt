package com.example.metroinfo.repository

import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.ArrivalTimeResponse
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.Route
import com.example.metroinfo.model.RouteInfo
import com.example.metroinfo.model.Station
import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.RouteRequest
import com.example.metroinfo.model.ArrivalTimeInfo
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import com.example.metroinfo.model.NearbyStation

@Singleton
class MetroRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getLines(): List<Line> {
        val response = apiService.getLines()
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getStations(): List<Station> {
        val response = apiService.getStations()
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getNearestStations(
        latitude: Double,
        longitude: Double,
        radius: Int = 1000
    ): List<Station> {
        val response = apiService.getNearestStations(latitude, longitude, radius)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getRoute(startStationId: Int, endStationId: Int): Route? {
        val response = apiService.getRoute(startStationId, endStationId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun findRoute(startStationId: Int, endStationId: Int): BestRouteResponse? {
        val startStation = getStation(startStationId)
        val endStation = getStation(endStationId)
        
        if (startStation == null || endStation == null) {
            return null
        }
        
        val request = RouteRequest(
            startStation = startStation.nameCn,
            endStation = endStation.nameCn
        )
        val response = apiService.findRoute(request)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getArrivalTime(stationId: Int, lineId: Int): ArrivalTimeResponse {
        val response = apiService.getArrivalTime(stationId, lineId)
        return if (response.isSuccessful) {
            response.body() ?: ArrivalTimeResponse(
                arrivalTime = "",
                success = false,
                message = "获取到达时间失败"
            )
        } else {
            ArrivalTimeResponse(
                arrivalTime = "",
                success = false,
                message = "获取到达时间失败"
            )
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

    suspend fun getLineStationsWithArrivalTime(lineId: Int): List<ArrivalTimeInfo> {
        val response = apiService.getLineStationsArrivalTime(lineId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun searchStationsWithArrivalTime(query: String): List<ArrivalTimeInfo> {
        val response = apiService.searchStations(query)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getLineStationsArrivalTime(lineId: Int): List<ArrivalInfo> {
        val response = apiService.getLineStationsArrivalTime(lineId)
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
    
    suspend fun searchStations(query: String): List<ArrivalInfo> {
        val response = apiService.searchStations(query)
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

    suspend fun getBestRoute(
        startStationId: Int,
        endStationId: Int
    ): RouteInfo? {
        val response = apiService.getBestRoute(startStationId, endStationId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getStation(stationId: Int): Station? {
        val response = apiService.getStation(stationId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }


    suspend fun getStationArrivalTimeDetails(stationId: String): List<ArrivalTimeInfo> {
        val response = apiService.getStationArrivalTimeDetails(stationId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun getArrivalInfo(stationId: Int): Response<List<ArrivalInfo>> {
        return apiService.getArrivalInfo(stationId)
    }

    suspend fun getNearbyStations(latitude: Double, longitude: Double): List<NearbyStation> {
        val response = apiService.getNearestStations(latitude, longitude)
        return response.body()?.map { station ->
            NearbyStation(
                stationId = station.stationId,
                nameCn = station.nameCn,
                nameEn = station.nameEn,
                distance = station.distance,
                lines = listOf(station.lineId)
            )
        } ?: emptyList()
    }
} 