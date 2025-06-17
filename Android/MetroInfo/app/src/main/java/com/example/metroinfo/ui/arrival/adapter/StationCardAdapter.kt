package com.example.metroinfo.ui.arrival.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.databinding.ItemStationCardBinding
import com.example.metroinfo.model.ArrivalTimeInfo
import com.example.metroinfo.utils.MetroLineColors

class StationCardAdapter(
    private val onStationClick: (ArrivalTimeInfo) -> Unit
) : ListAdapter<ArrivalTimeInfo, StationCardAdapter.ViewHolder>(StationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationCardBinding.inflate(
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
        private val binding: ItemStationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStationClick(getItem(position))
                }
            }
        }

        fun bind(station: ArrivalTimeInfo) {
            val context = binding.root.context
            
            // 添加调试日志
            timber.log.Timber.d("StationCard bind: ${station.stationName}, isOperating=${station.isOperating}, nextArrival=${station.nextArrival}, serviceStatus=${station.serviceStatus}")
            
            binding.apply {
                // 站点名称
                stationName.text = station.stationName
                
                // 英文站名（暂时隐藏，因为数据中可能没有）
                stationNameEn.visibility = android.view.View.GONE
                
                // 线路信息和颜色
                lineInfo.text = station.lineName
                val lineColor = MetroLineColors.getLineColor(station.lineId)
                lineInfo.setBackgroundColor(lineColor)
                lineInfo.setTextColor(MetroLineColors.getTextColor(lineColor))
                
                // 设置线路颜色指示条
                lineColorIndicator.setBackgroundColor(lineColor)
                
                // 检查是否为停运状态
                val isOperating = station.isOperating ?: true
                
                if (!isOperating || station.nextArrival == "停运中") {
                    // 停运状态
                    timber.log.Timber.d("Station ${station.stationName} is NOT operating")
                    nextTrainTime.text = "首班车：${station.firstArrivalTime}"
                    
                    // 显示停运信息和到首班车的时间
                    val serviceStatusText = station.serviceStatus ?: "停运中"
                    minutesRemaining.text = serviceStatusText
                    minutesRemaining.setTextColor(
                        androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_red_dark)
                    )
                } else {
                    // 正常运营状态
                    timber.log.Timber.d("Station ${station.stationName} is operating normally")
                    nextTrainTime.text = "首班车：${if (station.firstArrivalTime.isNotEmpty()) station.firstArrivalTime else "暂无信息"}"
                    
                    // 剩余时间，如果小于5分钟，显示红色
                    val minutesText = if (station.minutesRemaining > 0) "${station.minutesRemaining}分钟" else "暂无信息"
                    minutesRemaining.text = "剩余：$minutesText"
                    
                    // 恢复正常颜色
                    minutesRemaining.setTextColor(
                        androidx.core.content.ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                    )
                }
                
                // 方向信息 - 显示最早首班车的方向
                val directionText = when {
                    !station.directionInfo.isNullOrEmpty() -> "方向：${station.directionInfo}"
                    !station.directionDesc.isNullOrEmpty() -> "方向：${station.directionDesc}"
                    else -> "方向：上行"
                }
                direction.text = directionText
                
                // 添加调试日志
                timber.log.Timber.d("Station ${station.stationName}: directionInfo=${station.directionInfo}, directionDesc=${station.directionDesc}")
                
                // 换乘指示器
                transferIndicator.visibility = if (station.isTransfer) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
        

    }

    private class StationDiffCallback : DiffUtil.ItemCallback<ArrivalTimeInfo>() {
        override fun areItemsTheSame(oldItem: ArrivalTimeInfo, newItem: ArrivalTimeInfo): Boolean {
            return oldItem.stationId == newItem.stationId
        }

        override fun areContentsTheSame(oldItem: ArrivalTimeInfo, newItem: ArrivalTimeInfo): Boolean {
            return oldItem == newItem
        }
    }
} 