package com.example.metroinfo.ui.arrival.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.metroinfo.databinding.ItemLineBinding
import com.example.metroinfo.model.Line

class LineAdapter(private val onLineClick: (Line) -> Unit) : ListAdapter<Line, LineAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemLineBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLineClick(getItem(position))
                }
            }
        }

        fun bind(line: Line) {
            binding.lineName.text = line.nameCn
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Line>() {
        override fun areItemsTheSame(oldItem: Line, newItem: Line): Boolean {
            return oldItem.lineId == newItem.lineId
        }

        override fun areContentsTheSame(oldItem: Line, newItem: Line): Boolean {
            return oldItem == newItem
        }
    }
} 