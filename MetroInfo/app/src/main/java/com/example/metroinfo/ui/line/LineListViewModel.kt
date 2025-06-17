package com.example.metroinfo.ui.line

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.metroinfo.model.Line
import com.example.metroinfo.repository.LineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineListViewModel @Inject constructor(
    private val lineRepository: LineRepository
) : ViewModel() {
    
    private val _lines = MutableStateFlow<List<Line>>(emptyList())
    val lines: StateFlow<List<Line>> = _lines
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadLines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lines = lineRepository.getLines()
                _lines.value = lines
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
} 