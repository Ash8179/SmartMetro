package com.example.metroinfo.network

import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.model.ArrivalTimeResponse
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.NearestStationsResponse
import com.example.metroinfo.model.Station
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.Route
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.RouteRequest
import com.example.metroinfo.model.LineInfo
import com.example.metroinfo.model.StationInfo
import com.example.metroinfo.model.RouteInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.Path

interface ApiService {

    @GET("api/metro/nearest-stations")
    suspend fun getNearestStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 1000
    ): Response<List<Station>>

    @GET("api/stations")
    suspend fun getStations(): Response<List<Station>>

    @GET("api/lines")
    suspend fun getLines(): Response<List<Line>>

    @GET("api/lines/{lineId}")
    suspend fun getLine(@Path("lineId") lineId: Int): Response<Line>

    @GET("api/lines/{lineId}/stations")
    suspend fun getLineStations(@Path("lineId") lineId: Int): Response<List<Station>>

    @POST("api/route")
    suspend fun findRoute(
        @Body request: RouteRequest
    ): Response<BestRouteResponse>

    @GET("api/arrival-time")
    suspend fun getArrivalTime(
        @Query("station_id") stationId: Int,
        @Query("line_id") lineId: Int
    ): Response<ArrivalTimeResponse>

    @GET("api/metro/route")
    suspend fun getRoute(
        @Query("start_station_id") startStationId: Int,
        @Query("end_station_id") endStationId: Int
    ): Response<Route>

    @GET("api/arrival-time/station/{stationId}")
    suspend fun getStationArrivalTime(@Path("stationId") stationId: String): Response<List<ArrivalTimeInfo>>
    
    @GET("api/arrival-time/line/{lineId}")
    suspend fun getLineStationsArrivalTime(@Path("lineId") lineId: Int): Response<List<ArrivalTimeInfo>>
    
    @GET("api/arrival-time/search")
    suspend fun searchStations(@Query("q") query: String): Response<List<ArrivalTimeInfo>>

    @GET("api/metro/best-route")
    suspend fun getBestRoute(
        @Query("start_station_id") startStationId: Int,
        @Query("end_station_id") endStationId: Int
    ): Response<RouteInfo>

    @GET("api/stations/{stationId}")
    suspend fun getStation(@Path("stationId") stationId: Int): Response<Station>

    @GET("api/arrival-time/station/{stationId}/details")
    suspend fun getStationArrivalTimeDetails(@Path("stationId") stationId: String): Response<List<ArrivalTimeInfo>>

    @GET("api/arrival-time/station/{stationId}/info")
    suspend fun getArrivalInfo(@Path("stationId") stationId: Int): Response<List<ArrivalInfo>>
}
