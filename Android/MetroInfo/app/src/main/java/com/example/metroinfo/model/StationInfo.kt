package com.example.metroinfo.model

data class StationInfo(
    val id: Int,
    val name: String,
    val lineId: Int,
    val distance: Double,
    val isTransfer: Boolean = false
) 