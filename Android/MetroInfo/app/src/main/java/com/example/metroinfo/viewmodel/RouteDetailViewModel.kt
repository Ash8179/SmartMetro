package com.example.metroinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.PathSegment
import com.example.metroinfo.model.RouteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _routeSteps = MutableLiveData<List<PathSegment>>()
    val routeSteps: LiveData<List<PathSegment>> = _routeSteps

    private val _routeInfo = MutableLiveData<String>()
    val routeInfo: LiveData<String> = _routeInfo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchRouteDetails(fromStationId: String, toStationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = RouteRequest(
                    startStation = fromStationId,
                    endStation = toStationId
                )
                val response = apiService.findRoute(request)
                if (response.isSuccessful) {
                    response.body()?.let { bestRouteResponse ->
                        if (bestRouteResponse.success) {
                            bestRouteResponse.data?.let { routeData ->
                                _routeSteps.value = routeData.path
                                _routeInfo.value = buildRouteInfoText(routeData.totalTime, routeData.path.size - 1)
                            }
                        } else {
                            _routeInfo.value = bestRouteResponse.message ?: "获取路线失败"
                        }
                    }
                } else {
                    _routeInfo.value = "错误: ${response.message()}"
                }
            } catch (e: Exception) {
                _routeInfo.value = "错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildRouteInfoText(totalTime: Int, transferCount: Int): String {
        return "总时间: ${totalTime}分钟 | 换乘次数: $transferCount"
    }
} 