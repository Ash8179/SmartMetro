package com.example.metroinfo.repository

import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.RouteRequest
import retrofit2.Response
import javax.inject.Inject

class RouteRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getRoute(startStationId: Int, endStationId: Int): Response<BestRouteResponse> {
        val request = RouteRequest(
            startStation = startStationId.toString(),
            endStation = endStationId.toString()
        )
        return apiService.findRoute(request)
    }
} 