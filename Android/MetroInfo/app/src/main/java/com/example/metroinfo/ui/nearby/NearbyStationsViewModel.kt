package com.example.metroinfo.ui.nearby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.data.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyStationsViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stations = repository.getNearbyStations(latitude, longitude)
                _nearbyStations.value = stations
            } catch (e: Exception) {
                _error.value = e.message ?: "获取附近站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 