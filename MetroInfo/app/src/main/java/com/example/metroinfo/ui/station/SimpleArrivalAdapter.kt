package com.example.metroinfo.ui.station

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.databinding.ItemSimpleArrivalBinding

class SimpleArrivalAdapter : ListAdapter<SimpleArrivalInfo, SimpleArrivalAdapter.SimpleArrivalViewHolder>(SimpleArrivalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleArrivalViewHolder {
        val binding = ItemSimpleArrivalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimpleArrivalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimpleArrivalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SimpleArrivalViewHolder(
        private val binding: ItemSimpleArrivalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(arrival: SimpleArrivalInfo) {
            try {
                binding.apply {
                    directionTextView.text = if (arrival.direction.isNotEmpty()) arrival.direction else "未知方向"
                    
                    // 检查是否为停运状态
                    if (arrival.nextArrival == "停运中") {
                        nextArrivalTextView.text = "停运中"
                        minutesRemainingTextView.text = "已停运"
                        secondArrivalTextView.text = arrival.serviceStatus ?: "停运中"
                        intervalTextView.text = "等待首班车"
                        
                        // 设置停运状态的文字颜色
                        nextArrivalTextView.setTextColor(
                            ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                        )
                        minutesRemainingTextView.setTextColor(
                            ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                        )
                    } else {
                        // 正常运营状态
                        nextArrivalTextView.text = if (arrival.nextArrival.isNotEmpty()) arrival.nextArrival else "--:--:--"
                        minutesRemainingTextView.text = "${arrival.minutesRemaining}分钟后"
                        secondArrivalTextView.text = "下一班: ${if (arrival.secondArrival.isNotEmpty()) arrival.secondArrival else "--:--:--"}"
                        intervalTextView.text = "间隔: ${arrival.intervalMinutes}分钟"
                        
                        // 恢复正常颜色
                        nextArrivalTextView.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.text_primary)
                        )
                        minutesRemainingTextView.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.primary_blue)
                        )
                    }
                }
            } catch (e: Exception) {
                // 如果绑定数据失败，显示默认信息
                binding.apply {
                    directionTextView.text = "未知方向"
                    nextArrivalTextView.text = "--:--:--"
                    minutesRemainingTextView.text = "--分钟后"
                    secondArrivalTextView.text = "下一班: --:--:--"
                    intervalTextView.text = "间隔: --分钟"
                }
            }
        }
    }

    private class SimpleArrivalDiffCallback : DiffUtil.ItemCallback<SimpleArrivalInfo>() {
        override fun areItemsTheSame(oldItem: SimpleArrivalInfo, newItem: SimpleArrivalInfo): Boolean {
            return oldItem.direction == newItem.direction
        }

        override fun areContentsTheSame(oldItem: SimpleArrivalInfo, newItem: SimpleArrivalInfo): Boolean {
            return oldItem == newItem
        }
    }
} 