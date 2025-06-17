package com.example.metroinfo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * 不依赖Google Play服务的定位工具类
 * 适用于华为等不支持Google Play服务的设备
 */
class LocationHelper(private val context: Context) {
    
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float? = null
    )
    
    /**
     * 获取当前位置
     * @param timeoutMs 超时时间（毫秒），默认5秒
     * @return LocationResult 或 null（如果获取失败）
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 5000): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            try {
                // 检查权限
                if (!hasLocationPermission()) {
                    Timber.w("没有位置权限")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // 检查位置服务是否开启
                if (!isLocationEnabled()) {
                    Timber.w("位置服务未开启")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // 尝试获取最后已知位置
                val lastKnownLocation = getLastKnownLocation()
                if (lastKnownLocation != null && isLocationRecent(lastKnownLocation)) {
                    Timber.d("使用最后已知位置: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                    continuation.resume(
                        LocationResult(
                            latitude = lastKnownLocation.latitude,
                            longitude = lastKnownLocation.longitude,
                            accuracy = lastKnownLocation.accuracy
                        )
                    )
                    return@suspendCancellableCoroutine
                }
                
                // 请求新的位置更新
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Timber.d("获取到新位置: ${location.latitude}, ${location.longitude}")
                        locationManager.removeUpdates(this)
                        continuation.resume(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy
                            )
                        )
                    }
                    
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                // 设置超时
                val timeoutRunnable = Runnable {
                    locationManager.removeUpdates(locationListener)
                    if (continuation.isActive) {
                        Timber.w("定位超时，使用最后已知位置")
                        val fallbackLocation = getLastKnownLocation()
                        if (fallbackLocation != null) {
                            continuation.resume(
                                LocationResult(
                                    latitude = fallbackLocation.latitude,
                                    longitude = fallbackLocation.longitude,
                                    accuracy = fallbackLocation.accuracy
                                )
                            )
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
                
                // 取消时清理资源
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(locationListener)
                }
                
                // 开始定位
                val providers = getAvailableProviders()
                if (providers.isEmpty()) {
                    Timber.w("没有可用的位置提供者")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // 使用最佳提供者请求位置更新
                val bestProvider = providers.first()
                Timber.d("使用位置提供者: $bestProvider")
                
                locationManager.requestLocationUpdates(
                    bestProvider,
                    0L, // 最小时间间隔
                    0f,  // 最小距离
                    locationListener
                )
                
                // 设置超时
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    timeoutRunnable,
                    timeoutMs
                )
                
            } catch (e: Exception) {
                Timber.e(e, "获取位置时发生错误")
                continuation.resume(null)
            }
        }
    }
    
    /**
     * 检查是否有位置权限
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查位置服务是否开启
     */
    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * 获取最后已知位置
     */
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        val providers = getAvailableProviders()
        var bestLocation: Location? = null
        
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            } catch (e: SecurityException) {
                Timber.w("无法获取 $provider 的最后已知位置")
            }
        }
        
        return bestLocation
    }
    
    /**
     * 检查位置是否是最近的（5分钟内）
     */
    private fun isLocationRecent(location: Location): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000
        return location.time > fiveMinutesAgo
    }
    
    /**
     * 获取可用的位置提供者，按优先级排序
     */
    private fun getAvailableProviders(): List<String> {
        val providers = mutableListOf<String>()
        
        // GPS提供者（最精确）
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER)
        }
        
        // 网络提供者（较快）
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }
        
        // 被动提供者（最省电）
        if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            providers.add(LocationManager.PASSIVE_PROVIDER)
        }
        
        return providers
    }
    
    /**
     * 检查设备是否支持位置服务
     */
    fun isLocationSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)
    }
} 