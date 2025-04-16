package com.example.metroinfo.ui.arrival.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemStationCardBinding
import com.example.metroinfo.model.ArrivalTimeInfo

class StationCardAdapter(
    private val onStationSelected: (ArrivalTimeInfo) -> Unit
) : ListAdapter<ArrivalTimeInfo, StationCardAdapter.StationCardViewHolder>(StationCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationCardViewHolder {
        val binding = ItemStationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StationCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StationCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StationCardViewHolder(
        private val binding: ItemStationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStationSelected(getItem(position))
                }
            }
        }

        fun bind(station: ArrivalTimeInfo) {
            binding.apply {
                stationName.text = station.stationName
                lineInfo.text = "${station.lineName}号线"
                nextTrainTime.text = "首班车时间：${station.firstArrivalTime}"
                minutesRemaining.text = "剩余时间：${station.minutesRemaining}分钟"
                direction.text = "方向：${station.directionDesc}"
                
                if (station.isTransfer) {
                    transferIndicator.visibility = android.view.View.VISIBLE
                } else {
                    transferIndicator.visibility = android.view.View.GONE
                }
            }
        }
    }

    private class StationCardDiffCallback : DiffUtil.ItemCallback<ArrivalTimeInfo>() {
        override fun areItemsTheSame(oldItem: ArrivalTimeInfo, newItem: ArrivalTimeInfo): Boolean {
            return oldItem.stationId == newItem.stationId
        }

        override fun areContentsTheSame(oldItem: ArrivalTimeInfo, newItem: ArrivalTimeInfo): Boolean {
            return oldItem == newItem
        }
    }
} 