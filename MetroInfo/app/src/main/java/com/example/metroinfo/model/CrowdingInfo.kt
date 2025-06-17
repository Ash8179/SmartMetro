package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class CrowdingInfo(
    @SerializedName("station_id")
    val stationId: String,
    
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("crowd_level")
    val crowdLevel: Int, // 0-100 的拥挤度百分比
    
    @SerializedName("crowd_status")
    val crowdStatus: String, // 文本描述：舒适、适中、拥挤、非常拥挤
    
    @SerializedName("timestamp")
    val timestamp: String // 时间戳
) 