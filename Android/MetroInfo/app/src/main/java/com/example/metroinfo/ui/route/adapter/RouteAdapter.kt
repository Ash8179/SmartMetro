package com.example.metroinfo.ui.route.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.model.RouteSegment
import com.example.metroinfo.databinding.ItemRouteBinding

class RouteAdapter : ListAdapter<RouteSegment, RouteAdapter.RouteViewHolder>(RouteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(
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
        private val binding: ItemRouteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routeSegment: RouteSegment) {
            binding.routeTitleTextView.text = "路线详情"
            binding.routeDetailTextView.text = "从 ${routeSegment.fromStation.nameCn} 到 ${routeSegment.toStation.nameCn}"
            binding.timeTextView.text = "预计时间：${routeSegment.segmentTime}分钟"
        }
    }

    private class RouteDiffCallback : DiffUtil.ItemCallback<RouteSegment>() {
        override fun areItemsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem.segmentTime == newItem.segmentTime
        }

        override fun areContentsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }
    }
} 