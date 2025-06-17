package com.example.metroinfo.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import timber.log.Timber

/**
 * 导航工具类
 * 支持多种导航应用，优先使用高德地图
 */
class NavigationHelper(private val context: Context) {
    
    companion object {
        // 高德地图包名（多个可能的包名）
        private val AMAP_PACKAGES = listOf(
            "com.autonavi.minimap",      // 标准版高德地图
            "com.autonavi.amap",         // 另一个可能的包名
            "com.amap.android.ams",      // 高德地图车机版
            "com.autonavi.xmgd.navigator" // 高德导航
        )
        // 百度地图包名
        private const val BAIDU_PACKAGE = "com.baidu.BaiduMap"
        // 腾讯地图包名
        private const val TENCENT_PACKAGE = "com.tencent.map"
        // Google地图包名
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
    
    /**
     * 导航到指定位置
     * @param latitude 纬度
     * @param longitude 经度
     * @param destinationName 目的地名称
     * @param currentLatitude 当前位置纬度（可选）
     * @param currentLongitude 当前位置经度（可选）
     */
    fun navigateTo(
        latitude: Double,
        longitude: Double,
        destinationName: String,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null
    ) {
        Timber.d("开始导航到: $destinationName ($latitude, $longitude)")
        
        // 调试：检查所有已安装的应用
        debugInstalledApps()
        
        // 优先级顺序：高德地图 > 百度地图 > 腾讯地图 > Google地图 > 系统默认
        val amapPackage = getInstalledAmapPackage()
        when {
            amapPackage != null -> {
                Timber.d("找到高德地图: $amapPackage")
                openAmapNavigation(amapPackage, latitude, longitude, destinationName, currentLatitude, currentLongitude)
            }
            isAppInstalled(BAIDU_PACKAGE) -> {
                Timber.d("找到百度地图")
                openBaiduNavigation(latitude, longitude, destinationName, currentLatitude, currentLongitude)
            }
            isAppInstalled(TENCENT_PACKAGE) -> {
                Timber.d("找到腾讯地图")
                openTencentNavigation(latitude, longitude, destinationName, currentLatitude, currentLongitude)
            }
            isAppInstalled(GOOGLE_MAPS_PACKAGE) -> {
                Timber.d("找到Google地图")
                openGoogleMapsNavigation(latitude, longitude, destinationName, currentLatitude, currentLongitude)
            }
            else -> {
                Timber.w("未找到任何地图应用，使用系统默认")
                openSystemNavigation(latitude, longitude, destinationName)
            }
        }
    }
    
    /**
     * 检查应用是否已安装
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            Timber.d("检测到应用: $packageName")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.d("未找到应用: $packageName")
            false
        }
    }
    
    /**
     * 获取已安装的高德地图包名
     */
    private fun getInstalledAmapPackage(): String? {
        for (packageName in AMAP_PACKAGES) {
            if (isAppInstalled(packageName)) {
                return packageName
            }
        }
        return null
    }
    
    /**
     * 调试：输出所有已安装的地图应用信息
     */
    private fun debugInstalledApps() {
        Timber.d("=== 开始检测地图应用 ===")
        
        // 检查高德地图
        val amapPackage = getInstalledAmapPackage()
        if (amapPackage != null) {
            Timber.d("✅ 高德地图已安装: $amapPackage")
        } else {
            Timber.d("❌ 高德地图未安装")
            // 详细检查每个包名
            AMAP_PACKAGES.forEach { packageName ->
                Timber.d("   检查包名: $packageName - ${if (isAppInstalled(packageName)) "已安装" else "未安装"}")
            }
        }
        
        // 检查其他地图应用
        if (isAppInstalled(BAIDU_PACKAGE)) {
            Timber.d("✅ 百度地图已安装")
        } else {
            Timber.d("❌ 百度地图未安装")
        }
        
        if (isAppInstalled(TENCENT_PACKAGE)) {
            Timber.d("✅ 腾讯地图已安装")
        } else {
            Timber.d("❌ 腾讯地图未安装")
        }
        
        if (isAppInstalled(GOOGLE_MAPS_PACKAGE)) {
            Timber.d("✅ Google地图已安装")
        } else {
            Timber.d("❌ Google地图未安装")
        }
        
        Timber.d("=== 地图应用检测完成 ===")
    }
    
    /**
     * 打开高德地图导航
     */
    private fun openAmapNavigation(
        packageName: String,
        latitude: Double,
        longitude: Double,
        destinationName: String,
        currentLatitude: Double?,
        currentLongitude: Double?
    ) {
        try {
            val uri = if (currentLatitude != null && currentLongitude != null) {
                // 有起点和终点的路线规划
                "amapuri://route/plan/?sid=&slat=$currentLatitude&slon=$currentLongitude&sname=我的位置&did=&dlat=$latitude&dlon=$longitude&dname=$destinationName&dev=0&t=0"
            } else {
                // 只有终点的导航
                "amapuri://navi?sourceApplication=SmartMetro&poiname=$destinationName&lat=$latitude&lon=$longitude&dev=0&style=2"
            }
            
            Timber.d("高德地图URI: $uri")
            Timber.d("使用包名: $packageName")
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Toast.makeText(context, "正在打开高德地图导航...", Toast.LENGTH_SHORT).show()
            Timber.d("成功打开高德地图导航")
            
        } catch (e: Exception) {
            Timber.e(e, "打开高德地图失败")
            // 降级到百度地图
            if (isAppInstalled(BAIDU_PACKAGE)) {
                openBaiduNavigation(latitude, longitude, destinationName, currentLatitude, currentLongitude)
            } else {
                openSystemNavigation(latitude, longitude, destinationName)
            }
        }
    }
    
    /**
     * 打开百度地图导航
     */
    private fun openBaiduNavigation(
        latitude: Double,
        longitude: Double,
        destinationName: String,
        currentLatitude: Double?,
        currentLongitude: Double?
    ) {
        try {
            val uri = if (currentLatitude != null && currentLongitude != null) {
                // 百度地图路线规划
                "baidumap://map/direction?origin=$currentLatitude,$currentLongitude&destination=$latitude,$longitude&mode=transit&src=SmartMetro"
            } else {
                // 百度地图导航
                "baidumap://map/navi?location=$latitude,$longitude&query=$destinationName&src=SmartMetro"
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(BAIDU_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Toast.makeText(context, "正在打开百度地图导航...", Toast.LENGTH_SHORT).show()
            Timber.d("成功打开百度地图导航")
            
        } catch (e: Exception) {
            Timber.e(e, "打开百度地图失败")
            openSystemNavigation(latitude, longitude, destinationName)
        }
    }
    
    /**
     * 打开腾讯地图导航
     */
    private fun openTencentNavigation(
        latitude: Double,
        longitude: Double,
        destinationName: String,
        currentLatitude: Double?,
        currentLongitude: Double?
    ) {
        try {
            val uri = if (currentLatitude != null && currentLongitude != null) {
                // 腾讯地图路线规划
                "qqmap://map/routeplan?type=drive&from=我的位置&fromcoord=$currentLatitude,$currentLongitude&to=$destinationName&tocoord=$latitude,$longitude&referer=SmartMetro"
            } else {
                // 腾讯地图导航
                "qqmap://map/geocoder?geocode=$latitude,$longitude&referer=SmartMetro"
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(TENCENT_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Toast.makeText(context, "正在打开腾讯地图导航...", Toast.LENGTH_SHORT).show()
            Timber.d("成功打开腾讯地图导航")
            
        } catch (e: Exception) {
            Timber.e(e, "打开腾讯地图失败")
            openSystemNavigation(latitude, longitude, destinationName)
        }
    }
    
    /**
     * 打开Google地图导航
     */
    private fun openGoogleMapsNavigation(
        latitude: Double,
        longitude: Double,
        destinationName: String,
        currentLatitude: Double?,
        currentLongitude: Double?
    ) {
        try {
            val uri = if (currentLatitude != null && currentLongitude != null) {
                // Google地图路线规划
                "https://maps.google.com/maps?saddr=$currentLatitude,$currentLongitude&daddr=$latitude,$longitude"
            } else {
                // Google地图导航
                "geo:$latitude,$longitude?q=$latitude,$longitude($destinationName)"
            }
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                setPackage(GOOGLE_MAPS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            Toast.makeText(context, "正在打开Google地图导航...", Toast.LENGTH_SHORT).show()
            Timber.d("成功打开Google地图导航")
            
        } catch (e: Exception) {
            Timber.e(e, "打开Google地图失败")
            openSystemNavigation(latitude, longitude, destinationName)
        }
    }
    
    /**
     * 打开系统默认地图应用
     */
    private fun openSystemNavigation(
        latitude: Double,
        longitude: Double,
        destinationName: String
    ) {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($destinationName)")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Toast.makeText(context, "正在打开地图应用...", Toast.LENGTH_SHORT).show()
                Timber.d("成功打开系统默认地图应用")
            } else {
                Toast.makeText(context, "未找到可用的地图应用", Toast.LENGTH_SHORT).show()
                Timber.w("没有找到可用的地图应用")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "打开系统地图应用失败")
            Toast.makeText(context, "打开地图应用失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 获取已安装的地图应用列表
     */
    fun getInstalledMapApps(): List<String> {
        val installedApps = mutableListOf<String>()
        
        if (getInstalledAmapPackage() != null) {
            installedApps.add("高德地图")
        }
        if (isAppInstalled(BAIDU_PACKAGE)) {
            installedApps.add("百度地图")
        }
        if (isAppInstalled(TENCENT_PACKAGE)) {
            installedApps.add("腾讯地图")
        }
        if (isAppInstalled(GOOGLE_MAPS_PACKAGE)) {
            installedApps.add("Google地图")
        }
        
        return installedApps
    }
    
    /**
     * 检查是否有可用的地图应用
     */
    fun hasMapApp(): Boolean {
        return getInstalledAmapPackage() != null || 
               isAppInstalled(BAIDU_PACKAGE) || 
               isAppInstalled(TENCENT_PACKAGE) || 
               isAppInstalled(GOOGLE_MAPS_PACKAGE) ||
               hasSystemMapApp()
    }
    
    /**
     * 检查系统是否有默认地图应用
     */
    private fun hasSystemMapApp(): Boolean {
        val uri = Uri.parse("geo:0,0?q=test")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return intent.resolveActivity(context.packageManager) != null
    }
    
    /**
     * 公开的调试方法，用于测试地图应用检测
     */
    fun debugMapApps(): String {
        val result = StringBuilder()
        result.appendLine("=== 地图应用检测报告 ===")
        
        // 检查高德地图
        val amapPackage = getInstalledAmapPackage()
        if (amapPackage != null) {
            result.appendLine("✅ 高德地图: $amapPackage")
        } else {
            result.appendLine("❌ 高德地图: 未安装")
            AMAP_PACKAGES.forEach { packageName ->
                val installed = isAppInstalled(packageName)
                result.appendLine("   $packageName: ${if (installed) "已安装" else "未安装"}")
            }
        }
        
        // 检查其他地图
        result.appendLine("${if (isAppInstalled(BAIDU_PACKAGE)) "✅" else "❌"} 百度地图")
        result.appendLine("${if (isAppInstalled(TENCENT_PACKAGE)) "✅" else "❌"} 腾讯地图")
        result.appendLine("${if (isAppInstalled(GOOGLE_MAPS_PACKAGE)) "✅" else "❌"} Google地图")
        result.appendLine("${if (hasSystemMapApp()) "✅" else "❌"} 系统默认地图")
        
        result.appendLine("总计可用应用: ${getInstalledMapApps().size}")
        result.appendLine("已安装应用: ${getInstalledMapApps().joinToString(", ")}")
        
        return result.toString()
    }
} 