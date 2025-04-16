package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class Route(
    val path: List<Station>,
    val totalTime: Int,
    val totalDistance: Double,
    val transferCount: Int
)

data class PathSegment(
    @SerializedName("station")
    val station: Station,
    
    @SerializedName("line")
    val line: Line,
    
    @SerializedName("time")
    val time: Int,
    
    @SerializedName("distance")
    val distance: Double
) 