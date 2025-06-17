package com.example.metroinfo.ui.station.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.R
import com.example.metroinfo.data.model.StationEntrance
import com.example.metroinfo.databinding.ItemStationEntranceBinding
import com.google.android.material.chip.Chip

class StationEntrancesAdapter : ListAdapter<StationEntrance, StationEntrancesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStationEntranceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemStationEntranceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entrance: StationEntrance) {
            binding.apply {
                // 设置出口名称
                entranceNameText.text = entrance.entranceName
                
                // 设置别名（如果有）
                if (entrance.alias.isNotEmpty()) {
                    aliasText.text = entrance.alias
                    aliasText.visibility = android.view.View.VISIBLE
                } else {
                    aliasText.visibility = android.view.View.GONE
                }
                
                // 设置描述
                if (entrance.description.isNotEmpty()) {
                    descriptionText.text = entrance.description
                    descriptionText.visibility = android.view.View.VISIBLE
                } else {
                    descriptionText.visibility = android.view.View.GONE
                }
                
                // 设置状态
                statusText.text = entrance.status
                val statusColor = if (entrance.isOpen) {
                    ContextCompat.getColor(root.context, R.color.status_open)
                } else {
                    ContextCompat.getColor(root.context, R.color.status_closed)
                }
                statusText.setTextColor(statusColor)
                
                // 设置方向信息
                directionsContainer.removeAllViews()
                if (entrance.directions.isNotEmpty()) {
                    entrance.directions.forEach { direction ->
                        val chip = Chip(root.context).apply {
                            text = direction
                            isCheckable = false
                            isClickable = false
                            setChipBackgroundColorResource(R.color.chip_background)
                            setTextColor(ContextCompat.getColor(root.context, R.color.text_primary))
                        }
                        directionsContainer.addView(chip)
                    }
                    directionsLabel.visibility = android.view.View.VISIBLE
                    directionsContainer.visibility = android.view.View.VISIBLE
                } else {
                    directionsLabel.visibility = android.view.View.GONE
                    directionsContainer.visibility = android.view.View.GONE
                }
                
                // 设置备注（如果有）
                if (entrance.memo.isNotEmpty()) {
                    memoText.text = entrance.memo
                    memoText.visibility = android.view.View.VISIBLE
                } else {
                    memoText.visibility = android.view.View.GONE
                }
                
                // 设置卡片背景色（根据状态）
                val cardBackgroundColor = if (entrance.isOpen) {
                    ContextCompat.getColor(root.context, android.R.color.white)
                } else {
                    ContextCompat.getColor(root.context, R.color.card_disabled)
                }
                root.setCardBackgroundColor(cardBackgroundColor)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StationEntrance>() {
        override fun areItemsTheSame(oldItem: StationEntrance, newItem: StationEntrance): Boolean {
            return oldItem.entranceId == newItem.entranceId
        }

        override fun areContentsTheSame(oldItem: StationEntrance, newItem: StationEntrance): Boolean {
            return oldItem == newItem
        }
    }
} 