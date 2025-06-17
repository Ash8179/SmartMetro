package com.example.metroinfo.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.example.metroinfo.R

/**
 * 动画工具类，提供各种动画效果
 */
object AnimationUtils {

    /**
     * 应用Activity过渡动画
     *
     * @param activity 当前Activity
     * @param animType 动画类型
     */
    fun applyActivityTransition(activity: Activity, animType: AnimationType) {
        // 使用新的API实现转场动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上使用新API
            when (animType) {
                AnimationType.SLIDE_RIGHT -> activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                AnimationType.SLIDE_LEFT -> activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                AnimationType.FADE -> activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                AnimationType.ZOOM -> activity.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.zoom_in,
                    R.anim.zoom_out
                )
            }
        } else {
            // 较老版本Android使用兼容方法
            @Suppress("DEPRECATION")
            when (animType) {
                AnimationType.SLIDE_RIGHT -> activity.overridePendingTransition(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                AnimationType.SLIDE_LEFT -> activity.overridePendingTransition(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                AnimationType.FADE -> activity.overridePendingTransition(
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                AnimationType.ZOOM -> activity.overridePendingTransition(
                    R.anim.zoom_in,
                    R.anim.zoom_out
                )
            }
        }
    }

    /**
     * 为视图应用动画
     *
     * @param context 上下文
     * @param view 要应用动画的视图
     * @param animResId 动画资源ID
     * @param duration 动画持续时间（毫秒）
     * @param listener 动画监听器
     */
    fun applyAnimation(
        context: Context,
        view: View,
        animResId: Int,
        duration: Long = 300,
        listener: Animation.AnimationListener? = null
    ) {
        val animation = AnimationUtils.loadAnimation(context, animResId)
        animation.duration = duration
        if (listener != null) {
            animation.setAnimationListener(listener)
        }
        view.startAnimation(animation)
    }

    /**
     * 为视图应用淡入动画
     *
     * @param context 上下文
     * @param view 要应用动画的视图
     * @param duration 动画持续时间（毫秒）
     * @param listener 动画监听器
     */
    fun fadeIn(
        context: Context,
        view: View,
        duration: Long = 300,
        listener: Animation.AnimationListener? = null
    ) {
        applyAnimation(context, view, R.anim.fade_in, duration, listener)
        view.visibility = View.VISIBLE
    }

    /**
     * 为视图应用淡出动画
     *
     * @param context 上下文
     * @param view 要应用动画的视图
     * @param duration 动画持续时间（毫秒）
     * @param listener 动画监听器
     */
    fun fadeOut(
        context: Context,
        view: View,
        duration: Long = 300,
        listener: Animation.AnimationListener? = null
    ) {
        applyAnimation(context, view, R.anim.fade_out, duration, object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
                listener?.onAnimationEnd(animation)
            }

            override fun onAnimationStart(animation: Animation?) {
                listener?.onAnimationStart(animation)
            }

            override fun onAnimationRepeat(animation: Animation?) {
                listener?.onAnimationRepeat(animation)
            }
        })
    }

    /**
     * 为视图应用缩放进入动画
     *
     * @param context 上下文
     * @param view 要应用动画的视图
     * @param duration 动画持续时间（毫秒）
     * @param listener 动画监听器
     */
    fun zoomIn(
        context: Context,
        view: View,
        duration: Long = 300,
        listener: Animation.AnimationListener? = null
    ) {
        applyAnimation(context, view, R.anim.zoom_in, duration, listener)
        view.visibility = View.VISIBLE
    }

    /**
     * 为视图应用缩放退出动画
     *
     * @param context 上下文
     * @param view 要应用动画的视图
     * @param duration 动画持续时间（毫秒）
     * @param listener 动画监听器
     */
    fun zoomOut(
        context: Context,
        view: View,
        duration: Long = 300,
        listener: Animation.AnimationListener? = null
    ) {
        applyAnimation(context, view, R.anim.zoom_out, duration, object : AnimationListenerAdapter() {
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
                listener?.onAnimationEnd(animation)
            }

            override fun onAnimationStart(animation: Animation?) {
                listener?.onAnimationStart(animation)
            }

            override fun onAnimationRepeat(animation: Animation?) {
                listener?.onAnimationRepeat(animation)
            }
        })
    }

    /**
     * 动画类型枚举
     */
    enum class AnimationType {
        SLIDE_RIGHT, // 从右侧滑入
        SLIDE_LEFT,  // 从左侧滑入
        FADE,        // 淡入淡出
        ZOOM         // 缩放
    }

    /**
     * 动画监听器适配器
     */
    open class AnimationListenerAdapter : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
    }
} 