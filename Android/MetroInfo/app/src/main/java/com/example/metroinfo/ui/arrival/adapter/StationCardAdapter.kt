package com.example.metroinfo.ui.arrival.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemStationCardBinding
import com.example.metroinfo.model.ArrivalTimeInfo

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
            binding.apply {
                stationName.text = station.stationName
                stationNameEn.visibility = android.view.View.GONE
                lineInfo.text = station.lineName
                nextTrainTime.text = "首班车：${if (station.firstArrivalTime.isNotEmpty()) station.firstArrivalTime else "暂无信息"}"
                minutesRemaining.text = "剩余：${if (station.minutesRemaining > 0) "${station.minutesRemaining}分钟" else "暂无信息"}"
                direction.text = "方向：${station.directionDesc}"
                
                if (station.isTransfer) {
                    transferIndicator.visibility = android.view.View.VISIBLE
                } else {
                    transferIndicator.visibility = android.view.View.GONE
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