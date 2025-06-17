package com.example.metroinfo.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.data.repository.MetroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NearbyStationsViewModel @Inject constructor(
    private val repository: MetroRepository
) : ViewModel() {
    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadNearbyStations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Timber.d("Loading nearby stations...")
                val stations = repository.getNearbyStations()
                Timber.d("Loaded ${stations.size} nearby stations")
                
                _nearbyStations.value = stations
            } catch (e: Exception) {
                Timber.e(e, "Error loading nearby stations")
                _error.value = "加载附近站点失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        loadNearbyStationsWithLocation(latitude, longitude)
    }
    
    fun loadNearbyStationsWithLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Timber.d("Loading nearby stations with location: $latitude, $longitude")
                val stations = repository.getNearbyStationsWithLocation(latitude, longitude)
                Timber.d("Loaded ${stations.size} nearby stations with location")
                
                _nearbyStations.value = stations
            } catch (e: Exception) {
                Timber.e(e, "Error loading nearby stations with location")
                _error.value = "加载附近站点失败：${e.message}"
                // 如果定位加载失败，尝试使用默认方式
                loadNearbyStations()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 