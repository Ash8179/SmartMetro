package com.example.metroinfo.ui.route

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.network.ApiService
import com.example.metroinfo.model.BestRouteResponse
import com.example.metroinfo.model.PathSegment
import com.example.metroinfo.model.RouteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _routeResponse = MutableStateFlow<Response<BestRouteResponse>?>(null)
    val routeResponse: StateFlow<Response<BestRouteResponse>?> = _routeResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun findRoute(startStationId: Int, endStationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getBestRoute(startStationId, endStationId)
                _routeResponse.value = response
                if (!response.isSuccessful) {
                    _error.value = "Failed to get route: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 