package com.example.metroinfo.ui.arrival.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemLineBinding
import com.example.metroinfo.model.Line

class LineAdapter(
    private val onLineClick: (Line) -> Unit
) : ListAdapter<Line, LineAdapter.ViewHolder>(LineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineBinding.inflate(
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
        private val binding: ItemLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLineClick(getItem(position))
                }
            }
        }

        fun bind(line: Line) {
            binding.lineName.text = "${line.lineId}号线"
            binding.lineNameEn.text = "Line ${line.lineId}"
            binding.root.setBackgroundColor(
                when (line.lineId) {
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
            binding.lineName.setTextColor(android.graphics.Color.WHITE)
            binding.lineNameEn.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private class LineDiffCallback : DiffUtil.ItemCallback<Line>() {
        override fun areItemsTheSame(oldItem: Line, newItem: Line): Boolean {
            return oldItem.lineId == newItem.lineId
        }

        override fun areContentsTheSame(oldItem: Line, newItem: Line): Boolean {
            return oldItem == newItem
        }
    }
} 