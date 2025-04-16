package com.example.metroinfo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemRouteSegmentBinding
import com.example.metroinfo.model.RouteSegment

class RouteAdapter : ListAdapter<RouteSegment, RouteAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteSegmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RouteViewHolder(
        private val binding: ItemRouteSegmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(segment: RouteSegment) {
            binding.apply {
                lineText.text = "${segment.lineId}号线"
                timeText.text = "${segment.segmentTime}分钟"
                
                val stationText = if (segment.isTransfer) {
                    "换乘站：${segment.fromStation.nameCn}"
                } else {
                    "${segment.fromStation.nameCn} → ${segment.toStation.nameCn}"
                }
                binding.stationText.text = stationText
            }
        }
    }

    private class RouteDiffCallback : DiffUtil.ItemCallback<RouteSegment>() {
        override fun areItemsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem.fromStation.stationId == newItem.fromStation.stationId &&
                   oldItem.toStation.stationId == newItem.toStation.stationId
        }

        override fun areContentsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }
    }
} 