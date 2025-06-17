package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class LineStationsResponse(
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("line_schedule")
    val lineSchedule: LineSchedule,
    
    @SerializedName("stations")
    val stations: List<LineStationInfo>
)

data class LineSchedule(
    @SerializedName("first")
    val first: String,
    
    @SerializedName("last")
    val last: String
)

data class LineStationInfo(
    @SerializedName("station_id")
    val stationId: Int,
    
    @SerializedName("station_name")
    val stationName: String,
    
    @SerializedName("station_name_en")
    val stationNameEn: String,
    
    @SerializedName("line_number")
    val lineNumber: Int,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("first_train")
    val firstTrain: String,
    
    @SerializedName("last_train")
    val lastTrain: String,
    
    @SerializedName("upbound_first_train")
    val upboundFirstTrain: String? = null,
    
    @SerializedName("downbound_first_train")
    val downboundFirstTrain: String? = null,
    
    @SerializedName("direction_info")
    val directionInfo: String? = null,
    
    @SerializedName("next_arrival")
    val nextArrival: String,
    
    @SerializedName("minutes_remaining")
    val minutesRemaining: Int,
    
    @SerializedName("second_arrival")
    val secondArrival: String,
    
    @SerializedName("interval_minutes")
    val intervalMinutes: Int,
    
    @SerializedName("person_count")
    val personCount: Int? = 0,
    
    @SerializedName("crowd_level")
    val crowdLevel: Int? = 0,
    
    @SerializedName("is_operating")
    val isOperating: Boolean? = true,
    
    @SerializedName("service_status")
    val serviceStatus: String? = null,
    
    @SerializedName("next_service_time")
    val nextServiceTime: String? = null
) 