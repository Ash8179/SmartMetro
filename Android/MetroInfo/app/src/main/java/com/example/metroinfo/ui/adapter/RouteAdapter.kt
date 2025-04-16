package com.example.metroinfo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemRouteSegmentBinding
import com.example.metroinfo.model.RouteSegment

class RouteAdapter : ListAdapter<RouteSegment, RouteAdapter.RouteViewHolder>(RouteSegmentDiffCallback()) {

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
                if (segment.isTransfer) {
                    tvTransferInfo.text = segment.message
                    tvTransferFromStation.text = segment.transferFromStation
                    tvTransferToStation.text = segment.transferToStation
                    tvTransferTime.text = "${segment.transferTime}分钟"
                    transferInfoLayout.visibility = android.view.View.VISIBLE
                    segmentInfoLayout.visibility = android.view.View.GONE
                } else {
                    tvLineName.text = segment.lineName
                    tvFromStation.text = segment.segmentFromStation
                    tvToStation.text = segment.segmentToStation
                    tvSegmentTime.text = "${segment.segmentTime}分钟"
                    transferInfoLayout.visibility = android.view.View.GONE
                    segmentInfoLayout.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    private class RouteSegmentDiffCallback : DiffUtil.ItemCallback<RouteSegment>() {
        override fun areItemsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: RouteSegment, newItem: RouteSegment): Boolean {
            return oldItem == newItem
        }
    }
} 