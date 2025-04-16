package com.example.metroinfo.data.model

import com.google.gson.annotations.SerializedName

data class LineInfo(
    @SerializedName("line_id")
    val id: Int,
    @SerializedName("name_cn")
    val nameCn: String,
    @SerializedName("name_en")
    val nameEn: String
) {
    val color: String
        get() = when (id) {
            1 -> "#E3002B"  // 1号线
            2 -> "#8CC220"  // 2号线
            3 -> "#FCD600"  // 3号线
            4 -> "#461D84"  // 4号线
            5 -> "#944D9A"  // 5号线
            6 -> "#D40068"  // 6号线
            7 -> "#ED6F00"  // 7号线
            8 -> "#0094D8"  // 8号线
            9 -> "#87CAED"  // 9号线
            10 -> "#C6AFD4" // 10号线
            11 -> "#871C2B" // 11号线
            12 -> "#007A60" // 12号线
            13 -> "#E999C0" // 13号线
            14 -> "#626020" // 14号线
            15 -> "#BCA886" // 15号线
            16 -> "#98CBB5" // 16号线
            17 -> "#BC796F" // 17号线
            18 -> "#C4984F" // 18号线
            else -> "#000000"
        }
} 