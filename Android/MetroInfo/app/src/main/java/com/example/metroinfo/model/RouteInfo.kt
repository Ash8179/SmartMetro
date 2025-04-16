package com.example.metroinfo.model

data class RouteInfo(
    val segments: List<RouteSegment>,
    val totalTime: Int,
    val transferCount: Int
) 