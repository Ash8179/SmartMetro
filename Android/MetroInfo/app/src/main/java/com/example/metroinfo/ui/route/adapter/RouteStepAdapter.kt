package com.example.metroinfo.ui.route.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.model.PathSegment

class RouteStepAdapter : ListAdapter<PathSegment, RouteStepAdapter.RouteStepViewHolder>(RouteStepDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteStepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_step, parent, false)
        return RouteStepViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteStepViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class RouteStepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvFromStation: TextView = itemView.findViewById(R.id.tvFromStation)
        private val tvToStation: TextView = itemView.findViewById(R.id.tvToStation)
        private val tvLineInfo: TextView = itemView.findViewById(R.id.tvLineInfo)
        private val tvTimeInfo: TextView = itemView.findViewById(R.id.tvTimeInfo)

        fun bind(segment: PathSegment) {
            if (segment.transfer) {
                // 设置换乘卡片的特殊样式
                cardView.setCardBackgroundColor(Color.parseColor("#FFF5F5F5"))
                cardView.cardElevation = 0f
                cardView.radius = 16f
                
                ivIcon.setImageResource(R.drawable.ic_transfer)
                ivIcon.setColorFilter(null) // 清除颜色过滤器，使用图标原本的红色
                
                tvFromStation.text = "换乘"
                tvFromStation.setTextColor(Color.parseColor("#FF0000"))
                tvToStation.text = segment.message
                tvToStation.setTextColor(Color.parseColor("#FF0000"))
                tvLineInfo.text = "${segment.fromLine}号线 → ${segment.toLine}号线"
                tvTimeInfo.text = "${segment.transferTime}分钟"
            } else {
                // 设置普通卡片的样式
                cardView.setCardBackgroundColor(Color.WHITE)
                cardView.cardElevation = 2f
                cardView.radius = 8f
                
                ivIcon.setImageResource(R.drawable.ic_subway)
                ivIcon.setColorFilter(Color.parseColor("#2196F3")) // 设置蓝色
                
                tvFromStation.text = segment.from?.nameCn ?: "未知"
                tvFromStation.setTextColor(Color.BLACK)
                tvToStation.text = segment.to?.nameCn ?: "未知"
                tvToStation.setTextColor(Color.BLACK)
                tvLineInfo.text = "${segment.lineId}号线"
                tvTimeInfo.text = "${segment.time}分钟"
            }
        }
    }

    private class RouteStepDiffCallback : DiffUtil.ItemCallback<PathSegment>() {
        override fun areItemsTheSame(oldItem: PathSegment, newItem: PathSegment): Boolean {
            return oldItem.from?.stationId == newItem.from?.stationId && 
                   oldItem.to?.stationId == newItem.to?.stationId
        }

        override fun areContentsTheSame(oldItem: PathSegment, newItem: PathSegment): Boolean {
            return oldItem == newItem
        }
    }
} 