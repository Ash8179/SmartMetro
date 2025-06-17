package com.example.metroinfo.repository

import com.example.metroinfo.model.Line
import com.example.metroinfo.model.Station
import com.example.metroinfo.network.ApiService
import retrofit2.Response
import javax.inject.Inject

class LineRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getLines(): List<Line> {
        val response = apiService.getLines()
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    suspend fun getLine(lineId: Int): Line? {
        val response = apiService.getLine(lineId)
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
    
    suspend fun getLineStations(lineId: Int): List<Station> {
        val response = apiService.getLineStations(lineId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }
} 