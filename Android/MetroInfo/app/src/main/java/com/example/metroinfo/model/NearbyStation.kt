package com.example.metroinfo.model

data class NearbyStation(
    val stationId: String,
    val nameCn: String,
    val nameEn: String,
    val distance: Double,
    val lines: List<Int> = emptyList()
) 