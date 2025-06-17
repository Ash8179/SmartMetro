package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class StationDetailResponse(
    @SerializedName("station")
    val station: StationInfo
)

data class StationInfo(
    @SerializedName("station_id")
    val stationId: Int,
    @SerializedName("name_cn")
    val stationName: String,
    @SerializedName("name_en")
    val stationNameEn: String,
    @SerializedName("associated_lines")
    val associatedLines: List<Int>,
    @SerializedName("main_line")
    val mainLine: Int,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
    @SerializedName("first_train")
    val firstTrain: String,
    @SerializedName("last_train")
    val lastTrain: String,
    @SerializedName("exit_count")
    val exitCount: Int,
    @SerializedName("facilities")
    val facilities: List<String>?,
    @SerializedName("crowding_info")
    val crowdingInfo: CrowdingInfoResponse? = null,
    @SerializedName("arrival_times")
    val arrivalTimes: List<ArrivalTimeResponse>? = null
)

data class CrowdingInfoResponse(
    @SerializedName("person_count")
    val personCount: Int,
    @SerializedName("crowd_level")
    val crowdLevel: Int,
    @SerializedName("crowd_status")
    val crowdStatus: String,
    @SerializedName("timestamp")
    val timestamp: String
)

data class ArrivalTimeResponse(
    @SerializedName("direction")
    val direction: String,
    @SerializedName("next_arrival")
    val nextArrival: String,
    @SerializedName("minutes_remaining")
    val minutesRemaining: Int,
    @SerializedName("second_arrival")
    val secondArrival: String,
    @SerializedName("interval_minutes")
    val intervalMinutes: Int,
    @SerializedName("is_operating")
    val isOperating: Boolean? = true,
    @SerializedName("service_status")
    val serviceStatus: String? = null,
    @SerializedName("next_service_time")
    val nextServiceTime: String? = null
) 