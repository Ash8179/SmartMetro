package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class Line(
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("name_cn")
    val nameCn: String,
    
    @SerializedName("name_en")
    val nameEn: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("stations")
    val stations: List<Station> = emptyList()
) 