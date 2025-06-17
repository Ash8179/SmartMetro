package com.example.metroinfo.api

import com.example.metroinfo.model.BestRouteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MetroApiService {
    @GET("Dijkstra")
    suspend fun getRoute(
        @Query("from") fromStation: String,
        @Query("to") toStation: String
    ): BestRouteResponse
} 