package com.example.metroinfo.ui.crowding

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.metroinfo.R
import com.example.metroinfo.databinding.ActivityCrowdingBinding
import com.example.metroinfo.util.AnimationUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class CrowdingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrowdingBinding
    private val viewModel: CrowdingViewModel by viewModels()

    // 线路列表
    private val lineOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "16", "17", "18")
    
    // 车厢列表
    private val carriageOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrowdingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinners()
        setupViews()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupSpinners() {
        // 设置线路下拉选择器
        val lineAdapter = ArrayAdapter(this, R.layout.item_dropdown, lineOptions.map { "${it}号线" })
        binding.spinnerLineId.setAdapter(lineAdapter)

        // 设置车厢下拉选择器
        val carriageAdapter = ArrayAdapter(this, R.layout.item_dropdown, carriageOptions.map { "${it}号车厢" })
        binding.spinnerCarriage.setAdapter(carriageAdapter)
    }

    private fun setupViews() {
        // 为查询按钮添加缩放动画效果
        binding.btnQuery.setOnClickListener { view ->
            // 添加按钮点击动画
            val scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f)
            val scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f)
            scaleX.duration = 200
            scaleY.duration = 200
            scaleX.start()
            scaleY.start()
            
            val lineIdText = binding.spinnerLineId.text.toString()
            val lineId = lineIdText.replace("号线", "")
            
            val lineNumber = binding.etLineNumber.text.toString()
            
            val carriageText = binding.spinnerCarriage.text.toString()
            val carriage = carriageText.replace("号车厢", "")
            
            if (lineId.isBlank() || lineNumber.isBlank() || carriage.isBlank()) {
                Toast.makeText(this, "请填写完整查询信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载指示器
            binding.progressLoading.visibility = View.VISIBLE
            
            // 如果结果卡片已显示，使用淡出动画隐藏
            if (binding.cardResult.visibility == View.VISIBLE) {
                AnimationUtils.fadeOut(this, binding.cardResult)
            } else {
                binding.cardResult.visibility = View.GONE
            }
            
            viewModel.queryCrowding(lineId, lineNumber, carriage)
        }
    }

    private fun observeViewModel() {
        viewModel.crowdingData.observe(this) { data ->
            binding.progressLoading.visibility = View.GONE
            binding.cardResult.visibility = View.VISIBLE
            
            // 使用淡入动画显示结果卡片
            AnimationUtils.fadeIn(this, binding.cardResult)
            
            // 更新结果视图
            binding.tvLineId.text = "${data.line_id}号线"
            binding.tvLineNumber.text = data.line_number
            binding.tvCarriage.text = "${data.line_carriage}号车厢"
            binding.tvPersonNum.text = "${data.person_num}人"
            
            // 更新拥挤度指示器
            updateCrowdingIndicator(data.crowd_level)
            
            // 更新时间戳
            binding.tvTimestamp.text = "更新时间: ${getCurrentTime()}"
        }

        viewModel.error.observe(this) { error ->
            binding.progressLoading.visibility = View.GONE
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateCrowdingIndicator(level: Int) {
        val crowdLevel = getCrowdLevel(level)
        binding.tvCrowdLevel.text = crowdLevel
        
        // 设置进度条和颜色
        when (level) {
            0 -> {
                binding.progressCrowding.progress = 30
                binding.tvCrowdLevel.setTextColor(ContextCompat.getColor(this, R.color.colorGreen))
            }
            1 -> {
                binding.progressCrowding.progress = 65
                binding.tvCrowdLevel.setTextColor(ContextCompat.getColor(this, R.color.colorYellow))
            }
            2 -> {
                binding.progressCrowding.progress = 90
                binding.tvCrowdLevel.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
            }
            else -> {
                binding.progressCrowding.progress = 0
                binding.tvCrowdLevel.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
        }
    }

    private fun getCrowdLevel(level: Int): String {
        return when (level) {
            0 -> "宽松"
            1 -> "适中"
            2 -> "拥挤"
            else -> "未知"
        }
    }
    
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            AnimationUtils.applyActivityTransition(this, AnimationUtils.AnimationType.SLIDE_LEFT)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        AnimationUtils.applyActivityTransition(this, AnimationUtils.AnimationType.SLIDE_LEFT)
    }
} 