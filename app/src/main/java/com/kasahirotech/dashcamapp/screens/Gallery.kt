package com.kasahirotech.dashcamapp.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.kasahirotech.dashcamapp.databinding.ActivityGalleryBinding
import com.kasahirotech.dashcamapp.service.GalleryService

class Gallery : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding

    private val videosOpener = GalleryService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val listOfVideos = videosOpener.getMyAppVideos(this)

        // Show empty state if no videos, otherwise show the gallery
        if (listOfVideos.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvVideoGallery.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvVideoGallery.visibility = android.view.View.VISIBLE
        }

        val adapter = GalleryAdapter(listOfVideos) { clickedVideo ->

            // Create an Intent to view the video
            val intent = Intent(Intent.ACTION_VIEW).apply {
                // Set the data (the video's URI) and the type (video/mp4)
                setDataAndType(clickedVideo.uri, "video/mp4")
                // Grant permission for the video player app to read the file
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Start the activity to play the video
            startActivity(intent)

        }
        binding.rvVideoGallery.adapter = adapter
        binding.rvVideoGallery.layoutManager = GridLayoutManager(this, 3)

        binding.fabBack.setOnClickListener {
            finish()
        }
    }
}