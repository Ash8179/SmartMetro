package com.example.metroinfo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.RouteData
import com.example.metroinfo.model.RouteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _routeData = MutableLiveData<RouteData>()
    val routeData: LiveData<RouteData> = _routeData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun findRoute(fromStation: String, toStation: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (fromStation.isBlank() || toStation.isBlank()) {
                    _errorMessage.value = "请输入起始站和终点站"
                    return@launch
                }

                val request = RouteRequest(startStation = fromStation, endStation = toStation)
                val response = apiService.findRoute(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        body.data?.let { routeData ->
                            _routeData.value = routeData
                        } ?: run {
                            _errorMessage.value = "未找到路线数据"
                        }
                    } else {
                        _errorMessage.value = body?.message ?: "获取路线失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "发生错误: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 