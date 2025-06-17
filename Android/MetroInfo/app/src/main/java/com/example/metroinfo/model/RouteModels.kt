package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class Station(
    @SerializedName("station_id")
    val stationId: String? = null,
    
    @SerializedName("name_cn")
    val nameCn: String,
    
    @SerializedName("name_en")
    val nameEn: String? = null,
    
    @SerializedName("line_id")
    val lineId: Int? = null,
    
    val distance: Double? = null,
    
    @SerializedName("associated_lines")
    val associatedLines: List<Int>? = null
)

data class RouteStep(
    val from: Station,
    val to: Station,
    val line: String,
    val time: Int
)

data class BestRouteResponse(
    val success: Boolean,
    val data: RouteData?,
    val message: String?
)

data class RouteData(
    @SerializedName("total_time")
    val totalTime: Int,
    
    val path: List<PathSegment>,
    
    @SerializedName("transfer_count")
    val transferCount: Int
)

data class PathSegment(
    @SerializedName("from_station")
    val from: Station? = null,
    @SerializedName("to_station")
    val to: Station? = null,
    @SerializedName("line_id")
    val lineId: Int? = null,
    @SerializedName("time")
    val time: Int? = null,
    @SerializedName("transfer")
    val transfer: Boolean = false,
    @SerializedName("transfer_time")
    val transferTime: Int? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("from_line")
    val fromLine: Int? = null,
    @SerializedName("to_line")
    val toLine: Int? = null
) 