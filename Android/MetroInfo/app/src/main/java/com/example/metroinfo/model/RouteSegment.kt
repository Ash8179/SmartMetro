package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class RouteSegment(
    @SerializedName("from_station")
    val fromStation: Station,
    
    @SerializedName("to_station")
    val toStation: Station,
    
    @SerializedName("segment_time")
    val segmentTime: Int,
    
    @SerializedName("line_id")
    val lineId: String,
    
    @SerializedName("is_transfer")
    val isTransfer: Boolean
) 