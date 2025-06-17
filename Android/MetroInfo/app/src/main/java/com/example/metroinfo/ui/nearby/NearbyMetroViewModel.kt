package com.example.metroinfo.ui.nearby

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.data.repository.MetroRepository
import com.example.metroinfo.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyMetroViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService
) : AndroidViewModel(application) {
    private val repository = MetroRepository(apiService)

    private val _nearbyStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val nearbyStations: StateFlow<List<NearbyStation>> = _nearbyStations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadNearbyStations(location: Location) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val stations = repository.getNearbyStations(
                    location.latitude,
                    location.longitude
                )
                _nearbyStations.value = stations
            } catch (e: Exception) {
                _error.value = e.message ?: "加载附近站点失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 