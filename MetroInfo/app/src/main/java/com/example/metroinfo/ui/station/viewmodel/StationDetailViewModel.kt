package com.example.metroinfo.ui.station.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.StationDetail
import com.example.metroinfo.data.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    
    private val _stationDetail = MutableStateFlow<StationDetail?>(null)
    val stationDetail: StateFlow<StationDetail?> = _stationDetail
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun loadStationDetail(stationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Timber.d("开始加载站点详情: $stationId")
                Timber.d("调用 repository.getStationDetail($stationId)")
                val station = repository.getStationDetail(stationId)
                Timber.d("站点详情加载成功: ${station.name}")
                Timber.d("站点线路: ${station.lines}")
                Timber.d("拥挤度信息: ${station.crowdingInfo}")
                Timber.d("到达时间信息: ${station.arrivalTimes}")
                _stationDetail.value = station
            } catch (e: Exception) {
                Timber.e(e, "获取站点详情失败")
                Timber.e("错误详情: ${e.message}")
                Timber.e("错误类型: ${e.javaClass.simpleName}")
                if (e.cause != null) {
                    Timber.e("错误原因: ${e.cause?.message}")
                }
                _error.value = e.message ?: "获取站点详情失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 