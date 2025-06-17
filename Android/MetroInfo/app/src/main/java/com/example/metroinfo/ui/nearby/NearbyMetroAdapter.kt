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
            station.lines.forEach { lineId ->
                val lineBinding = ItemLineTagBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.lineTagsContainer,
                    false
                )
                lineBinding.lineNumber.text = lineId.toString()
                lineBinding.lineTextEn.text = "Line $lineId"
                lineBinding.root.setBackgroundColor(
                    when (lineId) {
                        1 -> android.graphics.Color.parseColor("#E4002B") // 1号线红色
                        2 -> android.graphics.Color.parseColor("#97C024") // 2号线绿色
                        3 -> android.graphics.Color.parseColor("#FFD100") // 3号线黄色
                        4 -> android.graphics.Color.parseColor("#5C2D91") // 4号线紫色
                        5 -> android.graphics.Color.parseColor("#A064A3") // 5号线紫色
                        6 -> android.graphics.Color.parseColor("#D9027D") // 6号线粉色
                        7 -> android.graphics.Color.parseColor("#F37B20") // 7号线橙色
                        8 -> android.graphics.Color.parseColor("#00A1E0") // 8号线蓝色
                        9 -> android.graphics.Color.parseColor("#71C5E8") // 9号线浅蓝
                        10 -> android.graphics.Color.parseColor("#C4D600") // 10号线浅绿
                        11 -> android.graphics.Color.parseColor("#800000") // 11号线棕色
                        12 -> android.graphics.Color.parseColor("#007B5F") // 12号线深绿
                        13 -> android.graphics.Color.parseColor("#EF95CF") // 13号线粉红
                        14 -> android.graphics.Color.parseColor("#827A04") // 14号线橄榄
                        15 -> android.graphics.Color.parseColor("#653279") // 15号线深紫
                        16 -> android.graphics.Color.parseColor("#45B97C") // 16号线青绿
                        17 -> android.graphics.Color.parseColor("#0066B3") // 17号线深蓝
                        18 -> android.graphics.Color.parseColor("#D6A461") // 18号线金色
                        else -> android.graphics.Color.GRAY
                    }
                )
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