package com.example.metroinfo.ui.crowding

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.metroinfo.databinding.ActivityCrowdingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrowdingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCrowdingBinding
    private val viewModel: CrowdingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrowdingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnQuery.setOnClickListener {
            val lineId = binding.etLineId.text.toString()
            val lineNumber = binding.etLineNumber.text.toString()
            val carriage = binding.etCarriage.text.toString()
            viewModel.queryCrowding(lineId, lineNumber, carriage)
        }
    }

    private fun observeViewModel() {
        viewModel.crowdingData.observe(this) { data ->
            val result = """
                线路ID: ${data.line_id}
                车次: ${data.line_number}
                车厢: ${data.line_carriage}
                人数: ${data.person_num}
                拥挤度: ${getCrowdLevel(data.crowd_level)}
            """.trimIndent()
            binding.tvResult.text = result
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
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
} 