package com.example.metroinfo.data.repository

import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.RouteRequest
import retrofit2.Response
import javax.inject.Inject

class RouteRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getRoute(startStationId: String, endStationId: String): Response<BestRouteResponse> {
        val request = RouteRequest(
            startStation = startStationId,
            endStation = endStationId
        )
        return apiService.findRoute(request)
    }
} 