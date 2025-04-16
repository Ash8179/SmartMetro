package com.example.metroinfo.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.data.model.NearbyStation
import com.example.metroinfo.databinding.ItemNearbyStationBinding
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel

class NearbyStationsAdapter(
    private val onStationClick: (NearbyStation) -> Unit
) : ListAdapter<NearbyStation, NearbyStationsAdapter.ViewHolder>(StationDiffCallback()) {

    private var originalList: List<NearbyStation>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNearbyStationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onStationClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemNearbyStationBinding,
        private val onStationClick: (NearbyStation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(station: NearbyStation) {
            binding.apply {
                root.setOnClickListener { onStationClick(station) }
                
                stationNameText.text = station.name
                stationNameEnText.text = station.nameEn
                
                // 格式化距离显示
                val distance = station.distance * 1000 // 转换为米
                distanceText.text = when {
                    distance < 1000 -> "${distance.toInt()}m"
                    else -> String.format("%.1fkm", distance / 1000)
                }

                // 清除现有的线路标签
                lineTagsContainer.removeAllViews()

                // 添加线路标签
                station.lines.forEach { lineId ->
                    val chip = Chip(lineTagsContainer.context).apply {
                        text = "${lineId}号线"
                        setTextColor(android.graphics.Color.WHITE)
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(
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
                    }
                    lineTagsContainer.addView(chip)
                }
            }
        }
    }

    private class StationDiffCallback : DiffUtil.ItemCallback<NearbyStation>() {
        override fun areItemsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<NearbyStation>?) {
        originalList = list
        // 对换乘站进行去重处理
        val processedStations = list?.groupBy { "${it.name}_${it.lines.sorted()}" }
            ?.map { (_, stations) ->
                // 如果是同一个站点的不同线路，合并它们
                if (stations.size > 1 && stations.all { it.name == stations.first().name }) {
                    stations.first().copy(
                        lines = stations.flatMap { it.lines }
                            .distinct()
                            .sorted()
                    )
                } else {
                    stations.first()
                }
            }
            ?.sortedBy { it.distance }

        super.submitList(processedStations)
    }
} 