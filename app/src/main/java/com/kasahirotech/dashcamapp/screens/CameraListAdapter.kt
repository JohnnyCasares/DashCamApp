package com.kasahirotech.dashcamapp.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kasahirotech.dashcamapp.databinding.ItemCameraBinding
import com.kasahirotech.dashcamapp.models.CameraInfo
import com.kasahirotech.dashcamapp.models.FieldOfViewType

/**
 * RecyclerView adapter for displaying a list of available cameras.
 */
class CameraListAdapter(
    private val cameras: List<CameraInfo>,
    private var selectedCameraId: String,
    private val onCameraSelected: (CameraInfo) -> Unit
) : RecyclerView.Adapter<CameraListAdapter.CameraViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        val binding = ItemCameraBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CameraViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        holder.bind(cameras[position])
    }
    
    override fun getItemCount(): Int = cameras.size
    
    /**
     * Updates the selected camera ID and refreshes the list.
     */
    fun updateSelectedCamera(cameraId: String) {
        val oldSelectedIndex = cameras.indexOfFirst { it.id == selectedCameraId }
        val newSelectedIndex = cameras.indexOfFirst { it.id == cameraId }
        
        selectedCameraId = cameraId
        
        if (oldSelectedIndex >= 0) {
            notifyItemChanged(oldSelectedIndex)
        }
        if (newSelectedIndex >= 0) {
            notifyItemChanged(newSelectedIndex)
        }
    }
    
    inner class CameraViewHolder(
        private val binding: ItemCameraBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(camera: CameraInfo) {
            val context = binding.root.context
            
            // Set camera name
            binding.tvCameraName.text = camera.displayName
            
            // Set camera details
            val fovText = when (camera.fieldOfView) {
                FieldOfViewType.ULTRA_WIDE -> context.getString(com.kasahirotech.dashcamapp.R.string.camera_type_ultra_wide)
                FieldOfViewType.WIDE -> context.getString(com.kasahirotech.dashcamapp.R.string.camera_type_wide)
                FieldOfViewType.STANDARD -> context.getString(com.kasahirotech.dashcamapp.R.string.camera_type_standard)
                FieldOfViewType.TELEPHOTO -> context.getString(com.kasahirotech.dashcamapp.R.string.camera_type_telephoto)
            }
            
            val zoomText = if (camera.supportsZoom) {
                context.getString(com.kasahirotech.dashcamapp.R.string.camera_zoom_range, camera.minZoomRatio, camera.maxZoomRatio)
            } else {
                context.getString(com.kasahirotech.dashcamapp.R.string.camera_no_zoom)
            }
            
            binding.tvCameraDetails.text = "$fovText • $zoomText"
            
            // Set content description for accessibility
            binding.root.contentDescription = context.getString(
                com.kasahirotech.dashcamapp.R.string.camera_item_description,
                camera.displayName
            )
            
            // Show/hide selected indicator
            binding.tvSelectedIndicator.visibility = if (camera.id == selectedCameraId) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Set click listener
            binding.cameraItemContainer.setOnClickListener {
                onCameraSelected(camera)
            }
        }
    }
}
