package com.kasahirotech.dashcamapp.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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


        val adapter = GalleryAdapter(videosOpener.getMyAppVideos(this)){

        }

    }
}