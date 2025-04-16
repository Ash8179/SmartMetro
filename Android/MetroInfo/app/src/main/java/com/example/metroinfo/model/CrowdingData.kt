package com.example.metroinfo.model

data class CrowdingData(
    val line_id: String,
    val line_number: String,
    val line_carriage: String,
    val person_num: Int,
    val crowd_level: Int
) 