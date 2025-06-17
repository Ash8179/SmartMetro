package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class ArrivalInfo(
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("station_id")
    val stationId: String? = null,
    
    @SerializedName("station_name")
    val stationName: String,
    
    @SerializedName("direction_desc")
    val directionDesc: String,
    
    @SerializedName("first_arrival_time")
    val firstArrivalTime: String,
    
    @SerializedName("next_arrival_time")
    val nextArrivalTime: String,
    
    @SerializedName("minutes_remaining")
    val minutesRemaining: Int,
    
    @SerializedName("line_name")
    val lineName: String,
    
    @SerializedName("arrival_time")
    val arrivalTime: String,
    
    @SerializedName("first_train")
    val firstTrain: String? = null,
    
    @SerializedName("last_train")
    val lastTrain: String? = null
) 