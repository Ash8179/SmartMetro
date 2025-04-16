package com.example.metroinfo.repository

import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.RouteRequest
import com.example.metroinfo.model.Station
import com.example.metroinfo.model.NearestStationsResponse
import com.example.metroinfo.model.ArrivalTimeResponse
import com.example.metroinfo.data.model.NearbyStation
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetroRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun getLines(): Response<List<Line>> = apiService.getLines()

    suspend fun getStations(): Response<List<Station>> = apiService.getStations()

    suspend fun getNearestStations(
        latitude: Double,
        longitude: Double
    ): List<NearbyStation> = apiService.getNearbyStations(latitude, longitude)

    suspend fun getRoute(startStationId: Int, endStationId: Int): Response<BestRouteResponse> =
        apiService.getRoute(startStationId, endStationId)

    suspend fun findRoute(request: RouteRequest): Response<BestRouteResponse> = apiService.findRoute(request)

    suspend fun getArrivalTime(stationId: Int, lineId: Int): Response<ArrivalTimeResponse> =
        apiService.getArrivalTime(stationId, lineId)

    suspend fun getStationArrivalTime(stationId: String): Response<List<ArrivalTimeInfo>> =
        apiService.getStationArrivalTime(stationId)

    suspend fun getLineStationsArrivalTime(lineId: Int): Response<List<ArrivalTimeInfo>> =
        apiService.getLineStationsArrivalTime(lineId)

    suspend fun searchStations(query: String): Response<List<ArrivalTimeInfo>> =
        apiService.searchStations(query)

    suspend fun getBestRoute(startStationId: Int, endStationId: Int): Response<BestRouteResponse> =
        apiService.getBestRoute(startStationId, endStationId)

    suspend fun getStation(stationId: Int): Response<Station> = apiService.getStation(stationId)

    suspend fun getStationArrivalTimeDetails(stationId: String): Response<List<ArrivalTimeInfo>> =
        apiService.getStationArrivalTimeDetails(stationId)

    suspend fun getArrivalInfo(stationId: Int): Response<List<ArrivalInfo>> =
        apiService.getArrivalInfo(stationId)
} 