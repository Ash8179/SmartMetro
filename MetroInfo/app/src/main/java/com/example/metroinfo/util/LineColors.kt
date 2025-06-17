package com.example.metroinfo.util

import android.graphics.Color

object LineColors {
    private val colorMap = mapOf(
        1 to Color.rgb(227, 0, 43),
        2 to Color.rgb(140, 194, 32),
        3 to Color.rgb(252, 214, 0),
        4 to Color.rgb(70, 29, 132),
        5 to Color.rgb(148, 77, 154),
        6 to Color.rgb(212, 0, 104),
        7 to Color.rgb(237, 111, 0),
        8 to Color.rgb(0, 148, 216),
        9 to Color.rgb(135, 202, 237),
        10 to Color.rgb(198, 175, 212),
        11 to Color.rgb(135, 28, 43),
        12 to Color.rgb(0, 122, 96),
        13 to Color.rgb(233, 153, 192),
        14 to Color.rgb(98, 96, 32),
        15 to Color.rgb(188, 168, 134),
        16 to Color.rgb(152, 203, 181),
        17 to Color.rgb(188, 121, 111),
        18 to Color.rgb(196, 152, 79)
    )

    fun getLineColor(lineNumber: Int): Int {
        return colorMap[lineNumber] ?: Color.GRAY
    }
} 