package com.example.metroinfo.ui.route.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.model.PathSegment
import com.google.android.material.card.MaterialCardView

class RouteStepAdapter : ListAdapter<PathSegment, RecyclerView.ViewHolder>(RouteStepDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_TRANSFER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).transfer) VIEW_TYPE_TRANSFER else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_step, parent, false)
        return when (viewType) {
            VIEW_TYPE_TRANSFER -> TransferViewHolder(view)
            else -> RouteStepViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is RouteStepViewHolder -> holder.bind(item)
            is TransferViewHolder -> holder.bind(item)
        }
        
        // 处理时间线连接
        if (holder.itemView.findViewById<View>(R.id.timeLineTop) != null) {
            holder.itemView.findViewById<View>(R.id.timeLineTop).visibility = 
                if (position == 0) View.INVISIBLE else View.VISIBLE
        }
        
        if (holder.itemView.findViewById<View>(R.id.timeLineBottom) != null) {
            holder.itemView.findViewById<View>(R.id.timeLineBottom).visibility = 
                if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE
        }
    }

    class RouteStepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvFromStation: TextView = itemView.findViewById(R.id.tvFromStation)
        private val tvToStation: TextView = itemView.findViewById(R.id.tvToStation)
        private val tvLineInfo: TextView = itemView.findViewById(R.id.tvLineInfo)
        private val tvTimeInfo: TextView = itemView.findViewById(R.id.tvTimeInfo)
        private val directionalInfo: View = itemView.findViewById(R.id.directionalInfo)

        fun bind(segment: PathSegment) {
            cardView.strokeWidth = 0
            
            val fromStation = segment.from?.nameCn ?: "未知"
            val toStation = segment.to?.nameCn ?: "未知"
            val lineId = segment.lineId ?: 0
            
            tvFromStation.text = fromStation
            tvToStation.text = toStation
            tvLineInfo.text = "${lineId}号线"
            tvTimeInfo.text = "${segment.time ?: 0}分钟"
            
            // 设置线路颜色
            val lineColorResId = getLineColorResourceId(lineId)
            val lineColor = ContextCompat.getColor(itemView.context, lineColorResId)
            tvLineInfo.setBackgroundColor(lineColor)
            
            // 显示方向指示
            directionalInfo.visibility = View.VISIBLE
        }
        
        private fun getLineColorResourceId(lineId: Int): Int {
            return when (lineId) {
                1 -> R.color.line_1
                2 -> R.color.line_2
                3 -> R.color.line_3
                4 -> R.color.line_4
                5 -> R.color.line_5
                6 -> R.color.line_6
                7 -> R.color.line_7
                8 -> R.color.line_8
                9 -> R.color.line_9
                10 -> R.color.line_10
                11 -> R.color.line_11
                12 -> R.color.line_12
                13 -> R.color.line_13
                14 -> R.color.line_14
                15 -> R.color.line_15
                16 -> R.color.line_16
                17 -> R.color.line_17
                18 -> R.color.line_18
                else -> R.color.primary_blue
            }
        }
    }
    
    class TransferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvFromStation: TextView = itemView.findViewById(R.id.tvFromStation)
        private val tvToStation: TextView = itemView.findViewById(R.id.tvToStation)
        private val tvLineInfo: TextView = itemView.findViewById(R.id.tvLineInfo)
        private val tvTimeInfo: TextView = itemView.findViewById(R.id.tvTimeInfo)
        private val directionalInfo: View = itemView.findViewById(R.id.directionalInfo)

        fun bind(segment: PathSegment) {
            // 设置换乘卡片的特殊样式
            cardView.strokeWidth = 1
            cardView.strokeColor = ContextCompat.getColor(itemView.context, R.color.line_7)
            cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.card_background))
            
            ivIcon.setImageResource(R.drawable.ic_transfer)
            ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.line_7))
            
            tvFromStation.text = "换乘站点"
            tvFromStation.setTextColor(ContextCompat.getColor(itemView.context, R.color.line_7))
            
            tvToStation.text = segment.message ?: "换乘到${segment.toLine}号线"
            
            // 显示从哪条线换乘到哪条线
            val fromLine = segment.fromLine ?: 0
            val toLine = segment.toLine ?: 0
            tvLineInfo.text = "${fromLine}→${toLine}号线"
            tvLineInfo.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.line_7))
            
            // 显示换乘时间
            tvTimeInfo.text = "${segment.transferTime ?: 5}分钟"
            tvTimeInfo.setTextColor(ContextCompat.getColor(itemView.context, R.color.line_7))
            
            // 隐藏方向指示
            directionalInfo.visibility = View.GONE
        }
    }

    private class RouteStepDiffCallback : DiffUtil.ItemCallback<PathSegment>() {
        override fun areItemsTheSame(oldItem: PathSegment, newItem: PathSegment): Boolean {
            return if (oldItem.transfer && newItem.transfer) {
                oldItem.fromLine == newItem.fromLine && oldItem.toLine == newItem.toLine
            } else if (!oldItem.transfer && !newItem.transfer) {
                oldItem.from?.stationId == newItem.from?.stationId && 
                oldItem.to?.stationId == newItem.to?.stationId
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItem: PathSegment, newItem: PathSegment): Boolean {
            return oldItem == newItem
        }
    }
} 