package com.example.metroinfo.ui.crowding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.CrowdingData
import com.example.metroinfo.network.CrowdingApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrowdingViewModel @Inject constructor(
    private val crowdingApi: CrowdingApi
) : ViewModel() {
    private val _crowdingData = MutableLiveData<CrowdingData>()
    val crowdingData: LiveData<CrowdingData> = _crowdingData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun queryCrowding(lineId: String, lineNumber: String, carriage: String) {
        viewModelScope.launch {
            try {
                val response = crowdingApi.getCrowding(lineId, lineNumber, carriage)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        _crowdingData.value = CrowdingData(
                            line_id = data.line_id.toString(),
                            line_number = data.line_number.toString(),
                            line_carriage = data.line_carriage.toString(),
                            person_num = data.person_num,
                            crowd_level = data.crowd_level
                        )
                    } else {
                        _error.value = "未找到数据"
                    }
                } else {
                    _error.value = "请求失败: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "请求失败: ${e.message}"
            }
        }
    }
} 