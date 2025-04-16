package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class NearbyStation(
    @SerializedName("station_id")
    val id: String,
    @SerializedName("name_cn")
    val name: String,
    @SerializedName("name_en")
    val nameEn: String,
    val distance: Double,
    var lines: List<LineInfo> = emptyList()
) 