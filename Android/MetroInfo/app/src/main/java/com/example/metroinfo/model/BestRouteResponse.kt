package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class BestRouteResponse(
    val success: Boolean,
    val data: RouteData?,
    val message: String?
)

data class RouteData(
    @SerializedName("from_station")
    val fromStation: Station,
    
    @SerializedName("to_station")
    val toStation: Station,
    
    @SerializedName("total_time")
    val totalTime: Int,
    
    @SerializedName("transfer_count")
    val transferCount: Int,
    
    val path: List<RouteSegment>
) 