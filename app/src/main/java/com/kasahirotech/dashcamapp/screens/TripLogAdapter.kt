package com.kasahirotech.dashcamapp.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemTripLogBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TripLogAdapter(
    private val onItemClick: (File) -> Unit,
    private val onShareClick: (File) -> Unit,
    private val onDeleteClick: (File) -> Unit
) : ListAdapter<File, TripLogAdapter.TripLogViewHolder>(TripLogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripLogViewHolder {
        val binding = ItemTripLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripLogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TripLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TripLogViewHolder(
        private val binding: ItemTripLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(file: File) {
            binding.tvFileName.text = file.name
            binding.tvTimestamp.text = formatTimestamp(file.lastModified())
            
            binding.root.setOnClickListener {
                onItemClick(file)
            }
            
            binding.btnShare.setOnClickListener {
                onShareClick(file)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(file)
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US)
            return format.format(date)
        }
    }
    
    private class TripLogDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.lastModified() == newItem.lastModified()
        }
    }
}
