package com.kasahirotech.dashcamapp.screens

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kasahirotech.dashcamapp.databinding.ActivityTripLogDetailBinding
import java.io.File

class TripLogDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTripLogDetailBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripLogDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get log file path from intent
        val logFilePath = intent.getStringExtra("LOG_FILE_PATH")
        
        if (logFilePath != null) {
            loadLogFile(File(logFilePath))
        } else {
            showError("No log file specified")
            finish()
        }
        
        // Back button
        binding.fabBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadLogFile(file: File) {
        try {
            if (!file.exists()) {
                showError("Log file not found")
                finish()
                return
            }
            
            val content = file.readText()
            binding.tvLogContent.text = content
        } catch (e: Exception) {
            showError("Failed to read log file: ${e.message}")
            finish()
        }
    }
    
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }
}
