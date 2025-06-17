package com.example.metroinfo.ui.arrival.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.model.ArrivalInfo
import com.example.metroinfo.databinding.ItemArrivalTimeBinding

class ArrivalTimeAdapter : ListAdapter<ArrivalInfo, ArrivalTimeAdapter.ArrivalTimeViewHolder>(ArrivalTimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArrivalTimeViewHolder {
        val binding = ItemArrivalTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArrivalTimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArrivalTimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ArrivalTimeViewHolder(
        private val binding: ItemArrivalTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(arrivalInfo: ArrivalInfo) {
            binding.directionTextView.text = arrivalInfo.directionDesc
            binding.timeTextView.text = arrivalInfo.firstArrivalTime
        }
    }

    private class ArrivalTimeDiffCallback : DiffUtil.ItemCallback<ArrivalInfo>() {
        override fun areItemsTheSame(oldItem: ArrivalInfo, newItem: ArrivalInfo): Boolean {
            return oldItem.stationId == newItem.stationId
        }

        override fun areContentsTheSame(oldItem: ArrivalInfo, newItem: ArrivalInfo): Boolean {
            return oldItem == newItem
        }
    }
} 