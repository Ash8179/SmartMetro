package com.example.metroinfo.model

data class RouteSegment(
    val fromStation: String,
    val toStation: String,
    val lineInfo: String,
    val timeInfo: String,
    val isTransfer: Boolean = false,
    val message: String = "乘车",
    val transferFromStation: String? = null,
    val transferToStation: String? = null,
    val transferTime: Int = 0,
    val lineName: String? = null,
    val segmentFromStation: String? = null,
    val segmentToStation: String? = null,
    val segmentTime: Int = 0
) 