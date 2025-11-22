package com.kasahirotech.dashcamapp.screens

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.databinding.ActivityGalleryBinding
import com.kasahirotech.dashcamapp.service.GalleryService
import com.kasahirotech.dashcamapp.service.VideoDeletion
import kotlinx.coroutines.launch

class Gallery : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private var actionMode: ActionMode? = null

    private val videosOpener = GalleryService()
    private val deletionService = VideoDeletion()

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

        adapter = GalleryAdapter(
            videos = listOfVideos,
            onItemClicked = { clickedVideo ->
                if (adapter.selectionMode) {
                    // In selection mode, toggle selection
                    adapter.toggleSelection(clickedVideo)
                } else {
                    // In normal mode, play the video
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(clickedVideo.uri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClicked = { clickedVideo ->
                if (!adapter.selectionMode) {
                    adapter.enterSelectionMode(clickedVideo)
                    startActionMode()
                }
            },
            onSelectionChanged = { count ->
                actionMode?.title = "$count selected"
            }
        )
        binding.rvVideoGallery.adapter = adapter
        binding.rvVideoGallery.layoutManager = GridLayoutManager(this, 3)

        binding.fabBack.setOnClickListener {
            finish()
        }
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_gallery_action_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    val selectedVideos = adapter.getSelectedVideos()
                    if (selectedVideos.isEmpty()) {
                        Toast.makeText(this@Gallery, "No videos selected", Toast.LENGTH_SHORT).show()
                    } else {
                        showDeleteConfirmationDialog(selectedVideos.size)
                    }
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            adapter.exitSelectionMode()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                if (adapter.selectionMode) {
                    val selectedVideos = adapter.getSelectedVideos()
                    if (selectedVideos.isEmpty()) {
                        Toast.makeText(this, "No videos selected", Toast.LENGTH_SHORT).show()
                    } else {
                        showDeleteConfirmationDialog(selectedVideos.size)
                    }
                } else {
                    Toast.makeText(this, "Long press a video to select", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (actionMode != null) {
            actionMode?.finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun showDeleteConfirmationDialog(count: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete videos?")
            .setMessage("Delete $count video(s)? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedVideos()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedVideos() {
        val selectedVideos = adapter.getSelectedVideos()
        
        lifecycleScope.launch {
            val result = deletionService.deleteVideos(this@Gallery, selectedVideos)
            
            if (result.success) {
                Toast.makeText(
                    this@Gallery,
                    "${result.deletedCount} video(s) deleted",
                    Toast.LENGTH_SHORT
                ).show()
                refreshGallery()
                actionMode?.finish()
            } else {
                Toast.makeText(
                    this@Gallery,
                    result.errorMessage ?: "Failed to delete videos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun refreshGallery() {
        val listOfVideos = videosOpener.getMyAppVideos(this)
        adapter.updateVideos(listOfVideos)
        
        // Show empty state if no videos
        if (listOfVideos.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvVideoGallery.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvVideoGallery.visibility = android.view.View.VISIBLE
        }
    }
}