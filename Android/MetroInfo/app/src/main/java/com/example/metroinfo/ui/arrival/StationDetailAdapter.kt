package com.example.metroinfo.ui.arrival

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemStationDetailBinding
import com.example.metroinfo.model.ArrivalInfo

class StationDetailAdapter : ListAdapter<ArrivalInfo, StationDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStationDetailBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(arrivalInfo: ArrivalInfo) {
            binding.apply {
                stationNameText.text = arrivalInfo.stationName
                lineNameText.text = "${arrivalInfo.lineName}号线"
                directionText.text = "方向：${arrivalInfo.directionDesc}"
                arrivalTimeText.text = "到站时间：${arrivalInfo.firstArrivalTime}"
                minutesRemainingText.text = "剩余时间：${arrivalInfo.minutesRemaining}分钟"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ArrivalInfo>() {
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