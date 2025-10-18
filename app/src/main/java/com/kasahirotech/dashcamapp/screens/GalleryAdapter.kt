package com.kasahirotech.dashcamapp.screens

import android.graphics.Bitmap
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemVideoThumbnailBinding
import com.kasahirotech.dashcamapp.models.AppVideo

class GalleryAdapter(
    private var videos: List<AppVideo>,
    private val onItemClicked: (AppVideo) -> Unit

) : RecyclerView.Adapter<GalleryAdapter.VideoGalleryViewHolder>() {

    inner class VideoGalleryViewHolder(private val binding: ItemVideoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(videoItem: AppVideo) {
            // Load thumbnail of a specific media item.
            val thumbnail: Bitmap =
                binding.root.context.contentResolver.loadThumbnail(
                    videoItem.uri, Size(640, 480), null
                )
            val durationMs = videoItem.duration.toLong()

            // 1. Get the duration in total seconds
            val durationInSeconds = videoItem.duration / 1000
            // 2. Calculate minutes and seconds from total seconds
            val minutes = durationInSeconds / 60
            val seconds = durationInSeconds % 60 // The '%' (modulo) operator gives the remainder

            // 3. Set the text on your TextView
            binding.tvDuration.text = String.format("%02d:%02d", minutes, seconds)

            binding.ivThumbnail.setImageBitmap(thumbnail)
            binding.root.setOnClickListener {
                onItemClicked(videoItem)
            }

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
        holder.bind(videos[position])
    }


    override fun getItemCount(): Int {
        return videos.size
    }


}