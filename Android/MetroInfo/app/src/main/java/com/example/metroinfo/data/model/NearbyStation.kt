package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class NearbyStation(
    @SerializedName("stat_id")
    val id: Int,
    @SerializedName("name_cn")
    val name: String,
    @SerializedName("name_en")
    val nameEn: String,
    @SerializedName("distance_m")
    val distance: Double,
    @SerializedName("associated_lines")
    val lines: List<Int> = emptyList()
) 