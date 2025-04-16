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
                station.lines.sortedBy { it.id }.forEach { line ->
                    val chip = Chip(itemView.context).apply {
                        text = line.id.toString()
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        
                        // 设置线路颜色
                        val backgroundColor = getLineColor(line.id)
                        chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
                        
                        // 设置为矩形
                        shapeAppearanceModel = ShapeAppearanceModel.builder()
                            .setAllCornerSizes(TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                4f,
                                context.resources.displayMetrics
                            ))
                            .build()
                        
                        // 调整大小和内边距
                        minHeight = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            24f,
                            context.resources.displayMetrics
                        ).toInt()
                        
                        isCheckable = false
                        isClickable = false
                        
                        // 调整内边距
                        val horizontalPadding = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            8f,
                            context.resources.displayMetrics
                        ).toInt()
                        val verticalPadding = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            0f,
                            context.resources.displayMetrics
                        ).toInt()
                        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                        
                        // 设置文字垂直居中
                        gravity = Gravity.CENTER
                        
                        // 设置固定宽度
                        width = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            32f,
                            context.resources.displayMetrics
                        ).toInt()
                    }
                    
                    lineTagsContainer.addView(chip)
                }
            }
        }

        private fun getLineColor(lineId: Int): Int {
            return when (lineId) {
                1 -> Color.parseColor("#E1002A")  // 红色
                2 -> Color.parseColor("#97D700")  // 绿色
                3 -> Color.parseColor("#FFD100")  // 黄色
                4 -> Color.parseColor("#4F1C74")  // 紫色
                5 -> Color.parseColor("#8F4199")  // 紫色
                6 -> Color.parseColor("#D9027D")  // 粉红色
                7 -> Color.parseColor("#FF7F27")  // 橙色
                8 -> Color.parseColor("#0082C8")  // 蓝色
                9 -> Color.parseColor("#95D3F2")  // 浅蓝色
                10 -> Color.parseColor("#C9A7D5") // 紫色
                11 -> Color.parseColor("#800000") // 棕色
                12 -> Color.parseColor("#007B5F") // 深绿色
                13 -> Color.parseColor("#EF82A0") // 粉色
                14 -> Color.parseColor("#827A04") // 褐色
                15 -> Color.parseColor("#673278") // 紫色
                16 -> Color.parseColor("#77C8C7") // 青色
                17 -> Color.parseColor("#D61F85") // 玫红色
                18 -> Color.parseColor("#BC2F19") // 红色
                else -> Color.GRAY
            }
        }
    }

    class StationDiffCallback : DiffUtil.ItemCallback<NearbyStation>() {
        override fun areItemsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem.id == newItem.id && oldItem.lines.map { it.id }.sorted() == newItem.lines.map { it.id }.sorted()
        }

        override fun areContentsTheSame(oldItem: NearbyStation, newItem: NearbyStation): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<NearbyStation>?) {
        originalList = list
        // 对换乘站进行去重处理
        val processedStations = list?.groupBy { "${it.name}_${it.lines.map { line -> line.id }.sorted()}" }
            ?.map { (_, stations) ->
                // 如果是同一个站点的不同线路，合并它们
                if (stations.size > 1 && stations.all { it.name == stations.first().name }) {
                    stations.first().copy(
                        lines = stations.flatMap { it.lines }
                            .distinctBy { it.id }
                            .sortedBy { it.id }
                    )
                } else {
                    stations.first()
                }
            }
            ?.sortedBy { it.distance }

        super.submitList(processedStations)
    }
} 