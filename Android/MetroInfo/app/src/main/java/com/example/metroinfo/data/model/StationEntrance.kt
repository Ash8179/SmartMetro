package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

/**
 * 站点出口信息数据模型
 */
data class StationEntrance(
    @SerializedName("entranceId")
    val entranceId: String,
    @SerializedName("entranceName")
    val entranceName: String,
    @SerializedName("alias")
    val alias: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("directions")
    val directions: List<String>,
    @SerializedName("status")
    val status: String,
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("memo")
    val memo: String,
    @SerializedName("isOpen")
    val isOpen: Boolean
)

/**
 * 站点出口详情响应数据模型
 */
data class StationEntrancesResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: StationEntrancesData?,
    @SerializedName("message")
    val message: String?
)

/**
 * 站点出口详情数据
 */
data class StationEntrancesData(
    @SerializedName("stationId")
    val stationId: Int,
    @SerializedName("stationName")
    val stationName: String,
    @SerializedName("stationNameEn")
    val stationNameEn: String,
    @SerializedName("totalEntrances")
    val totalEntrances: Int,
    @SerializedName("entrances")
    val entrances: List<StationEntrance>
) 