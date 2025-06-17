package com.example.metroinfo.ui.route

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.model.RouteSegment

class RouteStepAdapter : ListAdapter<RouteSegment, RouteStepAdapter.RouteStepViewHolder>(RouteStepDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteStepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_step, parent, false)
        return RouteStepViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteStepViewHolder, position: Int) {
        val step = getItem(position)
        holder.bind(step)
    }

    class RouteStepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFromStation: TextView = itemView.findViewById(R.id.tvFromStation)
        private val tvToStation: TextView = itemView.findViewById(R.id.tvToStation)
        private val tvLineInfo: TextView = itemView.findViewById(R.id.tvLineInfo)
        private val tvTimeInfo: TextView = itemView.findViewById(R.id.tvTimeInfo)

        fun bind(step: RouteSegment) {
            if (step.isTransfer) {
                tvFromStation.text = "换乘"
                tvToStation.text = "${step.transferFromStation} → ${step.transferToStation}"
                tvLineInfo.text = "换乘线路"
                tvTimeInfo.text = "换乘时间：${step.transferTime}分钟"
            } else {
                tvFromStation.text = step.segmentFromStation
                tvToStation.text = step.segmentToStation
                tvLineInfo.text = "线路：${step.lineName}"
                tvTimeInfo.text = "乘车时间：${step.segmentTime}分钟"
            }
        }
    }

    private class RouteStepDiffCallback : DiffUtil.ItemCallback<RouteSegment>() {
        override fun areItemsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }
    }
} 