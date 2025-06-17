package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class NearbyStation(
    @SerializedName("stationId")
    val stationId: Int,
    @SerializedName("stationName")
    val stationName: String,
    @SerializedName("stationNameEn")
    val stationNameEn: String,
    @SerializedName("distance")
    val distance: Int = 0, // 距离，单位：米
    @SerializedName("lines")
    val lines: List<Int> = emptyList(),
    @SerializedName("mainLine")
    val mainLine: Int = 1,
    @SerializedName("firstTrain")
    val firstTrain: String = "06:00",
    @SerializedName("lastTrain")
    val lastTrain: String = "23:00",
    @SerializedName("exitCount")
    val exitCount: Int = 4,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null
) 