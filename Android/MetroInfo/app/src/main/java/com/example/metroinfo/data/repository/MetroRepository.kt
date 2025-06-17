package com.example.metroinfo.data.repository

import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.data.model.StationDetail
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.model.Line
import com.example.metroinfo.model.NearestStationsResponse
import com.example.metroinfo.model.LineStationsResponse
import com.example.metroinfo.network.ApiService
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetroRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNearbyStations(): List<NearbyStation> {
        try {
            Timber.d("Fetching nearby stations from API...")
            val response = apiService.getNearbyStations()
            
            if (!response.isSuccessful) {
                Timber.e("API call failed with code: ${response.code()}")
                throw Exception("获取附近站点失败：${response.code()}")
            }
            
            val stations = response.body()?.stations ?: emptyList()
            Timber.d("Successfully fetched ${stations.size} nearby stations")
            return stations
        } catch (e: Exception) {
            Timber.e(e, "Error fetching nearby stations")
            throw e
        }
    }
    
    suspend fun getNearbyStationsWithLocation(latitude: Double, longitude: Double): List<NearbyStation> {
        try {
            Timber.d("=== 调用 metro/stations/nearby API (带位置信息) ===")
            Timber.d("用户位置: 纬度=$latitude, 经度=$longitude")
            val response = apiService.getNearbyStationsWithLocation(latitude, longitude)
            
            if (!response.isSuccessful) {
                Timber.e("API call failed with code: ${response.code()}")
                throw Exception("获取附近站点失败：${response.code()}")
            }
            
            val stations = response.body()?.stations ?: emptyList()
            Timber.d("Successfully fetched ${stations.size} nearby stations with location")
            return stations
        } catch (e: Exception) {
            Timber.e(e, "Error fetching nearby stations with location")
            throw e
        }
    }
    


    suspend fun getStationDetail(stationId: Int): StationDetail {
        try {
            Timber.d("Fetching station detail for station ID: $stationId")
            Timber.d("调用 apiService.getStationDetail($stationId)")
        val response = apiService.getStationDetail(stationId)
            
            Timber.d("API响应状态码: ${response.code()}")
            Timber.d("API响应是否成功: ${response.isSuccessful}")
            
            if (!response.isSuccessful) {
                Timber.e("API call failed with code: ${response.code()}")
                Timber.e("错误响应体: ${response.errorBody()?.string()}")
                throw Exception("获取站点详情失败：${response.code()}")
            }
            
            val responseBody = response.body()
            Timber.d("响应体是否为空: ${responseBody == null}")
            
            val stationDetail = responseBody?.station
            if (stationDetail == null) {
                Timber.e("Station detail is null for station ID: $stationId")
                throw Exception("站点详情为空")
            }
            
            Timber.d("Successfully fetched station detail for: ${stationDetail.stationName}")
            Timber.d("关联线路: ${stationDetail.associatedLines}")
            Timber.d("拥挤度信息: ${stationDetail.crowdingInfo}")
            Timber.d("到达时间信息: ${stationDetail.arrivalTimes}")
            
            return StationDetail(
                id = stationDetail.stationId,
                name = stationDetail.stationName,
                nameEn = stationDetail.stationNameEn,
                lines = stationDetail.associatedLines,
                latitude = stationDetail.latitude,
                longitude = stationDetail.longitude,
                firstTrain = stationDetail.firstTrain,
                lastTrain = stationDetail.lastTrain,
                exitCount = stationDetail.exitCount,
                facilities = stationDetail.facilities,
                crowdingInfo = stationDetail.crowdingInfo?.let { crowdingResponse ->
                    com.example.metroinfo.data.model.CrowdingInfo(
                        personCount = crowdingResponse.personCount,
                        crowdLevel = crowdingResponse.crowdLevel,
                        crowdStatus = crowdingResponse.crowdStatus,
                        timestamp = crowdingResponse.timestamp
            )
                },
                arrivalTimes = stationDetail.arrivalTimes?.map { arrivalResponse ->
                    com.example.metroinfo.data.model.ArrivalTime(
                        direction = arrivalResponse.direction,
                        nextArrival = arrivalResponse.nextArrival,
                        minutesRemaining = arrivalResponse.minutesRemaining,
                        secondArrival = arrivalResponse.secondArrival,
                        intervalMinutes = arrivalResponse.intervalMinutes,
                        isOperating = arrivalResponse.isOperating,
                        serviceStatus = arrivalResponse.serviceStatus,
                        nextServiceTime = arrivalResponse.nextServiceTime
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error fetching station detail for station ID: $stationId")
            Timber.e("异常类型: ${e.javaClass.simpleName}")
            Timber.e("异常消息: ${e.message}")
            if (e.cause != null) {
                Timber.e("异常原因: ${e.cause?.message}")
            }
            throw e
        }
    }
    
    suspend fun getArrivalInfo(stationId: Int): List<ArrivalInfo> {
        val response = apiService.getArrivalInfo(stationId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw Exception("获取到站信息失败: ${response.code()}")
        }
    }

    suspend fun getStationArrivalTime(stationId: String): Response<List<ArrivalTimeInfo>> {
        return apiService.getStationArrivalTime(stationId)
    }

    suspend fun getLineStationsArrivalTime(lineId: Int): Response<LineStationsResponse> {
        return apiService.getLineStationsArrivalTime(lineId)
    }

    suspend fun searchStations(query: String): Response<List<ArrivalTimeInfo>> {
        return apiService.searchStations(query)
    }

    suspend fun getLines(): Response<List<Line>> {
        return apiService.getLines()
    }

    suspend fun getStationArrivalTimeDetails(stationId: String): Response<List<ArrivalTimeInfo>> {
        return apiService.getStationArrivalTimeDetails(stationId)
    }
} 