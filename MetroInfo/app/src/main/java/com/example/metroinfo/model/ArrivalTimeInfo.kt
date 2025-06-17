package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class ArrivalTimeInfo(
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("station_id")
    val stationId: String,
    
    @SerializedName("station_name")
    val stationName: String,
    
    @SerializedName("line_name")
    val lineName: String,
    
    @SerializedName("direction_desc")
    val directionDesc: String,
    
    @SerializedName("first_arrival_time")
    val firstArrivalTime: String,
    
    @SerializedName("minutes_remaining")
    val minutesRemaining: Int,
    
    @SerializedName("is_transfer")
    val isTransfer: Boolean = false,
    
    @SerializedName("is_operating")
    val isOperating: Boolean? = true,
    
    @SerializedName("service_status")
    val serviceStatus: String? = null,
    
    @SerializedName("next_service_time")
    val nextServiceTime: String? = null,
    
    @SerializedName("next_arrival")
    val nextArrival: String? = null,
    
    @SerializedName("upbound_first_train")
    val upboundFirstTrain: String? = null,
    
    @SerializedName("downbound_first_train")
    val downboundFirstTrain: String? = null,
    
    @SerializedName("direction_info")
    val directionInfo: String? = null
) 