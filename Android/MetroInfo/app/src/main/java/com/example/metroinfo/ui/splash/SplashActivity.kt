package com.example.metroinfo.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.metroinfo.R
import com.example.metroinfo.ui.main.MainActivity
import com.example.metroinfo.util.AnimationUtils
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var mascotImageView: ImageView
    private lateinit var loadingTextView: TextView
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 隐藏系统状态栏，实现全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        
        setContentView(R.layout.activity_splash)

        // 初始化视图
        progressBar = findViewById(R.id.progressBar)
        mascotImageView = findViewById(R.id.ivMascot)
        loadingTextView = findViewById(R.id.tvLoading)
        
        // 设置初始位置（屏幕外）
        mascotImageView.translationX = -100f
        
        // 设置进度条和小火车动画
        setupAnimations()

        // 延迟2.5秒后跳转到主页面
        coroutineScope.launch {
            delay(2500)
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            
            // 使用自定义动画工具类实现转场动画
            startActivity(intent)
            // 确保在启动Activity后应用动画
            AnimationUtils.applyActivityTransition(this@SplashActivity, AnimationUtils.AnimationType.ZOOM)
            
            finish()
        }
    }
    
    private fun setupAnimations() {
        // 进度条动画
        val progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        progressAnimator.duration = 2000 // 2秒
        progressAnimator.interpolator = AccelerateDecelerateInterpolator()
        
        // 小火车动画
        val mascotAnimator = ObjectAnimator.ofFloat(
            mascotImageView, 
            "translationX", 
            -50f, 
            (progressBar.width * 0.9).toFloat()
        )
        mascotAnimator.duration = 2000 // 2秒
        mascotAnimator.startDelay = 200 // 延迟200毫秒开始
        mascotAnimator.interpolator = AccelerateDecelerateInterpolator()
        
        // 开始动画
        progressAnimator.start()
        mascotAnimator.start()
        
        // 加载文本动画效果
        animateLoadingText()
    }
    
    private fun animateLoadingText() {
        var dotCount = 0
        coroutineScope.launch {
            while (isActive) {
                val dots = when (dotCount % 4) {
                    0 -> ""
                    1 -> "."
                    2 -> ".."
                    3 -> "..."
                    else -> ""
                }
                loadingTextView.text = "正在努力加载中$dots"
                dotCount++
                delay(500) // 每0.5秒更新一次
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
} 