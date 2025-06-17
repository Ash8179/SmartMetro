package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class NearestStationsResponse(
    @SerializedName("user_location")
    val userLocation: UserLocation,
    @SerializedName("nearest_stations")
    val nearestStations: List<StationInfo>
)

data class UserLocation(
    val lat: Double,
    val lng: Double
)

data class StationInfo(
    @SerializedName("stat_id")
    val stationId: Int,
    @SerializedName("name_cn")
    val nameCn: String,
    @SerializedName("name_en")
    val nameEn: String,
    @SerializedName("travel_group")
    val travelGroup: String,
    @SerializedName("distance_m")
    val distanceM: Int,
    @SerializedName("line_info")
    val lineInfo: LineInfo,
    @SerializedName("associated_lines")
    val associatedLines: List<Int>
)

data class LineInfo(
    val line: Int,
    @SerializedName("all_stations")
    val allStations: List<String>
) 