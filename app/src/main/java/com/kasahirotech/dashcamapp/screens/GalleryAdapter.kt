package com.kasahirotech.dashcamapp.screens

import android.graphics.Bitmap
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemVideoThumbnailBinding
import com.kasahirotech.dashcamapp.interfaces.VideoItem

class GalleryAdapter (
    private var videos: List<VideoItem>,
    private val onItemClicked: (VideoItem) -> Unit

)
    : RecyclerView.Adapter<GalleryAdapter.VideoGalleryViewHolder>()
{

        inner class VideoGalleryViewHolder(private val binding: ItemVideoThumbnailBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(videoItem: VideoItem) {
                // Load thumbnail of a specific media item.
                val thumbnail: Bitmap =
                    binding.root.context.contentResolver.loadThumbnail(
                        videoItem.uri, Size(640, 480), null)
               binding.ivThumbnail.setImageBitmap(thumbnail)
//                binding.ivIcon.setImageResource(setting.icon)
                binding.root.setOnClickListener {
                    onItemClicked(videoItem)
                }

            }
        }


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VideoGalleryViewHolder {
            val binding = ItemVideoThumbnailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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