package com.example.metroinfo.ui.station

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.databinding.ItemArrivalBinding

class ArrivalAdapter : ListAdapter<ArrivalInfo, ArrivalAdapter.ArrivalViewHolder>(ArrivalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArrivalViewHolder {
        val binding = ItemArrivalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArrivalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArrivalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ArrivalViewHolder(
        private val binding: ItemArrivalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(arrival: ArrivalInfo) {
            binding.apply {
                destinationTextView.text = arrival.directionDesc
                firstArrivalTimeTextView.text = formatTime(arrival.firstArrivalTime)
                nextArrivalTimeTextView.text = formatTime(arrival.nextArrivalTime)
                minutesRemainingTextView.text = "${arrival.minutesRemaining}分钟"
            }
        }
        
        private fun formatTime(timeString: String): String {
            // 如果时间已经是格式化的，直接返回
            if (timeString.contains(":")) {
                return timeString
            }
            
            // 尝试解析时间字符串并格式化
            return try {
                if (timeString.length >= 4) {
                    val hour = timeString.substring(0, 2).toInt()
                    val minute = timeString.substring(2, 4).toInt()
                    String.format("%02d:%02d", hour, minute)
                } else {
                    timeString
                }
            } catch (e: Exception) {
                timeString
            }
        }
    }

    private class ArrivalDiffCallback : DiffUtil.ItemCallback<ArrivalInfo>() {
        override fun areItemsTheSame(oldItem: ArrivalInfo, newItem: ArrivalInfo): Boolean {
            return oldItem.stationId == newItem.stationId && 
                   oldItem.lineId == newItem.lineId &&
                   oldItem.directionDesc == newItem.directionDesc
        }

        override fun areContentsTheSame(oldItem: ArrivalInfo, newItem: ArrivalInfo): Boolean {
            return oldItem == newItem
        }
    }
} 