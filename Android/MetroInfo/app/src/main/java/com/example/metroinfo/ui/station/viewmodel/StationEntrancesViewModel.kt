package com.example.metroinfo.ui.station.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.StationEntrancesData
import com.example.metroinfo.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StationEntrancesViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _entrancesData = MutableStateFlow<StationEntrancesData?>(null)
    val entrancesData: StateFlow<StationEntrancesData?> = _entrancesData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadStationEntrances(stationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Timber.d("开始加载站点出口信息: stationId=$stationId")
                val response = apiService.getStationEntrances(stationId)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        body.data?.let { data ->
                            Timber.d("成功获取出口信息: ${data.entrances.size}个出口")
                            _entrancesData.value = data
                        } ?: run {
                            Timber.w("响应成功但数据为空")
                            _error.value = "未找到出口信息"
                        }
                    } else {
                        val errorMsg = body?.message ?: "获取出口信息失败"
                        Timber.e("API返回错误: $errorMsg")
                        _error.value = errorMsg
                    }
                } else {
                    val errorMsg = "网络请求失败: ${response.code()}"
                    Timber.e(errorMsg)
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = "发生错误: ${e.message}"
                Timber.e(e, errorMsg)
                _error.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }
} 