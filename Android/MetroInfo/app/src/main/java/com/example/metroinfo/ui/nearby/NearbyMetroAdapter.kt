package com.example.metroinfo.ui.nearby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.databinding.ItemLineTagBinding
import com.example.metroinfo.databinding.ItemNearbyMetroBinding

class NearbyMetroAdapter : ListAdapter<NearbyStation, NearbyMetroAdapter.ViewHolder>(NearbyStationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyMetroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemNearbyMetroBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(station: NearbyStation) {
            binding.stationName.text = station.name
            binding.stationNameEn.text = station.nameEn
            binding.distance.text = String.format("%.1f米", station.distance * 1000)

            binding.lineTagsContainer.removeAllViews()
            station.lines.forEach { line ->
                val lineBinding = ItemLineTagBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.lineTagsContainer,
                    false
                )
                lineBinding.lineNumber.text = line.id.toString()
                lineBinding.lineTextEn.text = "Line ${line.id}"
                lineBinding.root.setBackgroundColor(android.graphics.Color.parseColor(line.color))
                binding.lineTagsContainer.addView(lineBinding.root)
            }
        }
    }

    private class NearbyStationDiffCallback : DiffUtil.ItemCallback<NearbyStation>() {
        override fun areItemsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem == newItem
        }
    }
} 