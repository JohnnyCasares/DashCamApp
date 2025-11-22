package com.kasahirotech.dashcamapp.screens

import android.graphics.Bitmap
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemVideoThumbnailBinding
import com.kasahirotech.dashcamapp.models.AppVideo

class GalleryAdapter(
    private var videos: List<AppVideo>,
    private val onItemClicked: (AppVideo) -> Unit,
    private val onItemLongClicked: (AppVideo) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.VideoGalleryViewHolder>() {

    private val selectedVideos = mutableSetOf<AppVideo>()
    var selectionMode = false
        private set

    inner class VideoGalleryViewHolder(private val binding: ItemVideoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(videoItem: AppVideo, isSelected: Boolean) {
            // Load thumbnail of a specific media item.
            val thumbnail: Bitmap =
                binding.root.context.contentResolver.loadThumbnail(
                    videoItem.uri, Size(640, 480), null
                )

            // 1. Get the duration in total seconds
            val durationInSeconds = videoItem.duration / 1000
            // 2. Calculate minutes and seconds from total seconds
            val minutes = durationInSeconds / 60
            val seconds = durationInSeconds % 60 // The '%' (modulo) operator gives the remainder

            // 3. Set the text on your TextView
            binding.tvDuration.text = String.format("%02d:%02d", minutes, seconds)

            binding.ivThumbnail.setImageBitmap(thumbnail)
            
            // Show/hide selection indicators
            if (isSelected) {
                binding.vSelectionOverlay.visibility = android.view.View.VISIBLE
                binding.ivCheckmark.visibility = android.view.View.VISIBLE
                binding.root.cardElevation = 8f
            } else {
                binding.vSelectionOverlay.visibility = android.view.View.GONE
                binding.ivCheckmark.visibility = android.view.View.GONE
                binding.root.cardElevation = 4f
            }
            
            binding.root.setOnClickListener {
                onItemClicked(videoItem)
            }
            
            binding.root.setOnLongClickListener {
                onItemLongClicked(videoItem)
                true
            }
        }
    }
    
    fun toggleSelection(video: AppVideo) {
        if (selectedVideos.contains(video)) {
            selectedVideos.remove(video)
        } else {
            selectedVideos.add(video)
        }
        onSelectionChanged(selectedVideos.size)
        notifyDataSetChanged()
    }
    
    fun clearSelection() {
        selectedVideos.clear()
        onSelectionChanged(0)
        notifyDataSetChanged()
    }
    
    fun getSelectedVideos(): List<AppVideo> {
        return selectedVideos.toList()
    }
    
    fun enterSelectionMode(video: AppVideo) {
        selectionMode = true
        selectedVideos.add(video)
        onSelectionChanged(selectedVideos.size)
        notifyDataSetChanged()
    }
    
    fun exitSelectionMode() {
        selectionMode = false
        clearSelection()
    }
    
    fun updateVideos(newVideos: List<AppVideo>) {
        val diffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        videos = newVideos
        diffResult.dispatchUpdatesTo(this)
    }
    
    private class VideoDiffCallback(
        private val oldList: List<AppVideo>,
        private val newList: List<AppVideo>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].uri == newList[newItemPosition].uri
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoGalleryViewHolder {
        val binding =
            ItemVideoThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoGalleryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: VideoGalleryViewHolder,
        position: Int
    ) {
        val video = videos[position]
        val isSelected = selectedVideos.contains(video)
        holder.bind(video, isSelected)
    }


    override fun getItemCount(): Int {
        return videos.size
    }


}