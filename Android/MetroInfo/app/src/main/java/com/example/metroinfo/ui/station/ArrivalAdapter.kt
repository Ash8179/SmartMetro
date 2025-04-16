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
                firstArrivalTimeTextView.text = arrival.firstArrivalTime
                nextArrivalTimeTextView.text = arrival.nextArrivalTime
                minutesRemainingTextView.text = "${arrival.minutesRemaining} min"
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