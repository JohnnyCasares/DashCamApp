package com.kasahirotech.dashcamapp.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kasahirotech.dashcamapp.databinding.ActivityTripLogViewerBinding
import com.kasahirotech.dashcamapp.service.Storage
import com.kasahirotech.dashcamapp.service.TripLogger
import java.io.File

class TripLogViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTripLogViewerBinding
    private lateinit var tripLogger: TripLogger
    private lateinit var adapter: TripLogAdapter
    private var logFiles: List<File> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripLogViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize trip logger
        val storage = Storage(this)
        tripLogger = TripLogger(this, storage)
        
        // Setup RecyclerView
        adapter = TripLogAdapter(
            onItemClick = { file -> viewLogFile(file) },
            onShareClick = { file -> shareLogFile(file) },
            onDeleteClick = { file -> confirmDeleteLogFile(file) }
        )
        binding.rvTripLogs.adapter = adapter
        binding.rvTripLogs.layoutManager = LinearLayoutManager(this)
        
        // Load log files
        loadLogFiles()
        
        // Back button
        binding.fabBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadLogFiles() {
        logFiles = tripLogger.getTripLogFiles(this)
        
        if (logFiles.isEmpty()) {
            binding.rvTripLogs.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvTripLogs.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            adapter.submitList(logFiles)
        }
    }
    
    private fun viewLogFile(file: File) {
        val intent = Intent(this, TripLogDetailActivity::class.java)
        intent.putExtra("LOG_FILE_PATH", file.absolutePath)
        startActivity(intent)
    }
    
    private fun shareLogFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Trip Log: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Trip Log"))
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to share log file: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private fun confirmDeleteLogFile(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete Trip Log")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLogFile(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteLogFile(file: File) {
        try {
            if (file.delete()) {
                loadLogFiles() // Refresh the list
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Failed to delete log file")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to delete log file: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
