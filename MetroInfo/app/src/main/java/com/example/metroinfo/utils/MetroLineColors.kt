package com.example.metroinfo.utils

import android.graphics.Color

/**
 * 上海地铁线路颜色工具类
 * 包含各条地铁线路的官方颜色定义
 */
object MetroLineColors {
    
    /**
     * 获取地铁线路对应的颜色
     * @param lineNumber 线路号码
     * @return 对应的颜色值
     */
    fun getLineColor(lineNumber: Int): Int {
        return when (lineNumber) {
            1 -> Color.parseColor("#E4002B")    // 1号线 - 红色
            2 -> Color.parseColor("#00AD56")    // 2号线 - 绿色
            3 -> Color.parseColor("#FFD100")    // 3号线 - 黄色
            4 -> Color.parseColor("#5F259F")    // 4号线 - 紫色
            5 -> Color.parseColor("#9A48A0")    // 5号线 - 紫红色
            6 -> Color.parseColor("#D9027D")    // 6号线 - 品红色
            7 -> Color.parseColor("#F3965E")    // 7号线 - 橙色
            8 -> Color.parseColor("#009FDB")    // 8号线 - 蓝色
            9 -> Color.parseColor("#71C5E8")    // 9号线 - 浅蓝色
            10 -> Color.parseColor("#C8ACD6")   // 10号线 - 淡紫色
            11 -> Color.parseColor("#8B1538")   // 11号线 - 深红色
            12 -> Color.parseColor("#007B5F")   // 12号线 - 深绿色
            13 -> Color.parseColor("#EC91C4")   // 13号线 - 粉色
            14 -> Color.parseColor("#82C0C0")   // 14号线 - 青色
            15 -> Color.parseColor("#68217A")   // 15号线 - 深紫色
            16 -> Color.parseColor("#32D0C6")   // 16号线 - 青绿色
            17 -> Color.parseColor("#C4A484")   // 17号线 - 棕色
            18 -> Color.parseColor("#D4A574")   // 18号线 - 金色
            else -> Color.parseColor("#666666") // 默认灰色
        }
    }
    
    /**
     * 获取线路颜色的字符串表示
     * @param lineNumber 线路号码
     * @return 颜色的十六进制字符串
     */
    fun getLineColorHex(lineNumber: Int): String {
        return when (lineNumber) {
            1 -> "#E4002B"
            2 -> "#00AD56"
            3 -> "#FFD100"
            4 -> "#5F259F"
            5 -> "#9A48A0"
            6 -> "#D9027D"
            7 -> "#F3965E"
            8 -> "#009FDB"
            9 -> "#71C5E8"
            10 -> "#C8ACD6"
            11 -> "#8B1538"
            12 -> "#007B5F"
            13 -> "#EC91C4"
            14 -> "#82C0C0"
            15 -> "#68217A"
            16 -> "#32D0C6"
            17 -> "#C4A484"
            18 -> "#D4A574"
            else -> "#666666"
        }
    }
    
    /**
     * 获取线路名称
     * @param lineNumber 线路号码
     * @return 线路名称
     */
    fun getLineName(lineNumber: Int): String {
        return "${lineNumber}号线"
    }
    
    /**
     * 判断颜色是否为浅色（用于确定文字颜色）
     * @param color 颜色值
     * @return true表示浅色，应使用深色文字；false表示深色，应使用浅色文字
     */
    fun isLightColor(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        
        // 使用亮度公式计算
        val brightness = (red * 0.299 + green * 0.587 + blue * 0.114)
        return brightness > 128
    }
    
    /**
     * 获取适合的文字颜色
     * @param backgroundColor 背景颜色
     * @return 适合的文字颜色（黑色或白色）
     */
    fun getTextColor(backgroundColor: Int): Int {
        return if (isLightColor(backgroundColor)) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }
    
    /**
     * 获取所有支持的线路号码
     */
    fun getSupportedLines(): List<Int> {
        return (1..18).toList()
    }
} 