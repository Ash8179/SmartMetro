package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class NearbyStationsResponse(
    @SerializedName("stations")
    val stations: List<NearbyStation>
) 