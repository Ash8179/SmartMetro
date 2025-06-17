package com.example.metroinfo.ui.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.databinding.ItemNearbyStationBinding
import com.example.metroinfo.utils.MetroLineColors
import com.google.android.material.chip.Chip
import timber.log.Timber
import kotlin.math.roundToInt

class NearbyStationsAdapter(
    private val onStationClick: (NearbyStation) -> Unit,
    private val onRouteClick: (NearbyStation) -> Unit,
    private val onNavigateClick: (NearbyStation) -> Unit
) : ListAdapter<NearbyStation, NearbyStationsAdapter.ViewHolder>(DiffCallback()) {

    private var originalList: List<NearbyStation>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNearbyStationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDetails.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStationClick(getItem(position))
                }
            }

            binding.btnRoute.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRouteClick(getItem(position))
                }
            }

            binding.btnNavigate.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNavigateClick(getItem(position))
                }
            }
        }

        fun bind(station: NearbyStation) {
            try {
                binding.apply {
                    // 设置站点名称
                    stationNameText.text = station.stationName
                    stationNameEnText.text = station.stationNameEn
                    
                    // 设置距离 - 后端返回的是米
                    val distanceText = if (station.distance >= 1000) {
                        String.format("%.1fkm", station.distance / 1000.0)
                    } else {
                        "${station.distance}m"
                    }
                    this.distanceText.text = distanceText
                    
                    // 设置首末班车时间
                    firstTrainText.text = "首班车: ${station.firstTrain}"
                    lastTrainText.text = "末班车: ${station.lastTrain}"
                    
                    // 设置出口数量
                    exitCountText.text = "出口: ${station.exitCount}个"
                    
                    // 设置线路标签 - 为每条线路创建单独的Chip
                    lineTagsContainer.removeAllViews()
                    if (station.lines.isNotEmpty()) {
                        try {
                            // 为每条线路创建单独的Chip
                            station.lines.sorted().forEach { lineNumber ->
                                val chip = Chip(lineTagsContainer.context).apply {
                                    text = MetroLineColors.getLineName(lineNumber)
                                    isCheckable = false
                                    isClickable = false
                                    
                                    // 使用线路对应的颜色
                                    val lineColor = MetroLineColors.getLineColor(lineNumber)
                                    chipBackgroundColor = ColorStateList.valueOf(lineColor)
                                    
                                    // 根据背景颜色自动选择文字颜色
                                    val textColor = MetroLineColors.getTextColor(lineColor)
                                    setTextColor(textColor)
                                    
                                    // 设置Chip样式
                                    val heightInPx = (32 * lineTagsContainer.context.resources.displayMetrics.density).toInt()
                                    minimumHeight = heightInPx
                                    textSize = 12f
                                    
                                    Timber.d("创建线路标签: ${lineNumber}号线, 颜色: ${MetroLineColors.getLineColorHex(lineNumber)}")
                                }
                                lineTagsContainer.addView(chip)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error creating line chips for station: ${station.stationName}")
                        }
                    }
                    
                    // 设置颜色指示条 - 使用主要线路的颜色
                    try {
                        val indicatorColor = if (station.lines.isNotEmpty()) {
                            // 使用第一条线路的颜色作为指示条颜色
                            MetroLineColors.getLineColor(station.lines.sorted().first())
                        } else {
                            // 默认颜色
                            root.context.resources.getColor(R.color.primary_blue, null)
                        }
                        colorIndicator.setBackgroundColor(indicatorColor)
                    } catch (e: Exception) {
                        Timber.e(e, "Error setting color indicator")
                        colorIndicator.setBackgroundColor(Color.BLUE)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error binding station data: ${station.stationName}")
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<NearbyStation>() {
        override fun areItemsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem.stationId == newItem.stationId
        }

        override fun areContentsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<NearbyStation>?) {
        originalList = list
        try {
            // 对换乘站进行去重处理
            val processedStations = list?.groupBy { "${it.stationName}_${it.lines.sorted()}" }
                ?.map { (_, stations) ->
                    // 如果是同一个站点的不同线路，合并它们
                    if (stations.size > 1 && stations.all { it.stationName == stations.first().stationName }) {
                        val firstStation = stations.first()
                        val allLines = stations.flatMap { it.lines }
                            .distinct()
                            .sorted()
                        
                        // 创建新的站点对象而不是使用 copy
                        NearbyStation(
                            stationId = firstStation.stationId,
                            stationName = firstStation.stationName,
                            stationNameEn = firstStation.stationNameEn,
                            distance = firstStation.distance,
                            lines = allLines,
                            mainLine = firstStation.mainLine,
                            firstTrain = firstStation.firstTrain,
                            lastTrain = firstStation.lastTrain,
                            exitCount = firstStation.exitCount,
                            latitude = firstStation.latitude,
                            longitude = firstStation.longitude
                        )
                    } else {
                        stations.first()
                    }
                }
                ?.sortedBy { it.distance }

            super.submitList(processedStations)
        } catch (e: Exception) {
            Timber.e(e, "Error processing nearby stations list")
            // 如果处理失败，直接提交原始列表
            super.submitList(list)
        }
    }
} 