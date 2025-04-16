package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class Station(
    @SerializedName("station_id")
    val stationId: String,
    @SerializedName("name_cn")
    val nameCn: String,
    @SerializedName("name_en")
    val nameEn: String,
    @SerializedName("line_id")
    val lineId: Int = 0,
    @SerializedName("distance")
    val distance: Double = 0.0,
    @SerializedName("associated_lines")
    val associatedLines: String? = null
) 