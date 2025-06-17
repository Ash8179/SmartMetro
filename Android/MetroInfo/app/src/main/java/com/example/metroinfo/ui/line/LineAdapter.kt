package com.example.metroinfo.ui.line

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemLineBinding
import com.example.metroinfo.model.Line

class LineAdapter(
    private val onLineSelected: (Line) -> Unit
) : ListAdapter<Line, LineAdapter.LineViewHolder>(LineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LineViewHolder(
        private val binding: ItemLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLineSelected(getItem(position))
                }
            }
        }

        fun bind(line: Line) {
            binding.lineName.text = line.nameCn
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