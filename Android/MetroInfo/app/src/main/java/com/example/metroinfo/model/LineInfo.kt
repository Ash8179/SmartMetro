package com.example.metroinfo.model

import com.google.gson.annotations.SerializedName

data class LineInfo(
    @SerializedName("line_id")
    val lineId: Int,
    
    @SerializedName("line_name")
    val lineName: String,
    
    @SerializedName("line_color")
    val lineColor: String,
    
    @SerializedName("line_number")
    val lineNumber: String,
    
    @SerializedName("direction")
    val direction: String? = null,
    
    @SerializedName("is_operational")
    val isOperational: Boolean = true
) 