package com.example.metroinfo.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CrowdingApi {
    @GET("api/crowding")
    suspend fun getCrowding(
        @Query("line_id") lineId: String,
        @Query("line_number") lineNumber: String,
        @Query("line_carriage") carriage: String
    ): Response<CrowdingResponse>
}

data class CrowdingResponse(
    val line_id: Int,
    val line_number: Int,
    val line_carriage: Int,
    val person_num: Int,
    val crowd_level: Int,
    val timestamp: String
) 