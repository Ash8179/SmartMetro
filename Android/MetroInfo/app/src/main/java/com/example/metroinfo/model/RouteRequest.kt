package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class RouteRequest(
    @SerializedName("start_station")
    val startStation: String,
    
    @SerializedName("end_station")
    val endStation: String
) 