package com.example.metroinfo.data.model

data class StationDetail(
    val id: Int,
    val name: String,
    val nameEn: String,
    val lines: List<Int>,
    val latitude: Double?,
    val longitude: Double?,
    val firstTrain: String,
    val lastTrain: String,
    val exitCount: Int,
    val facilities: List<String>?,
    val crowdingInfo: CrowdingInfo? = null,
    val arrivalTimes: List<ArrivalTime>? = null
)

data class CrowdingInfo(
    val personCount: Int,
    val crowdLevel: Int,
    val crowdStatus: String,
    val timestamp: String
)

data class ArrivalTime(
    val direction: String,
    val nextArrival: String,
    val minutesRemaining: Int,
    val secondArrival: String,
    val intervalMinutes: Int,
    val isOperating: Boolean? = true,
    val serviceStatus: String? = null,
    val nextServiceTime: String? = null
) 