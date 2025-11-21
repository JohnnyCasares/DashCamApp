package com.kasahirotech.dashcamapp.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.kasahirotech.dashcamapp.interfaces.TripLogService
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service for logging trip data (GPS coordinates and speed) to text files.
 * Records location data at regular intervals during recording sessions.
 */
class TripLogger(
    private val context: Context,
    private val storageService: Storage
) : TripLogService {
    
    companion object {
        private const val TAG = "TripLogger"
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second for location updates
        private const val MIN_DISTANCE_METERS = 0f
        private const val LOG_INTERVAL_MS = 5000L // 5 seconds for log entries
        private const val MPS_TO_MPH = 2.237f
        private const val MPS_TO_KMH = 3.6f
    }
    
    private val locationManager: LocationManager = 
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private var isLogging: Boolean = false
    private var currentLogFile: File? = null
    private var logWriter: BufferedWriter? = null
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logTimer: Job? = null
    
    private var lastLocation: Location? = null
    private var currentSpeedUnit: String = "mph"
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
        }
        
        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Deprecated but required for API compatibility
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "GPS provider enabled for trip logging")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "GPS provider disabled for trip logging")
        }
    }

    
    /**
     * Starts logging trip data to a new log file.
     */
    override fun startLogging(context: Context): Boolean {
        Log.d(TAG, "startLogging() called")
        
        if (isLogging) {
            Log.w(TAG, "Trip logging already started")
            return true
        }
        
        // Check location permission (optional - will log without GPS if not granted)
        val hasLocationPerm = hasLocationPermission()
        if (!hasLocationPerm) {
            Log.w(TAG, "Location permission not granted - will log without GPS data")
        }
        
        // Get trip logs directory
        val logsDir = storageService.getTripLogsDirectory()
        Log.d(TAG, "Logs directory: ${logsDir?.absolutePath}")
        if (logsDir == null) {
            Log.e(TAG, "Cannot start trip logging: storage unavailable")
            return false
        }
        
        // Get speed unit preference
        currentSpeedUnit = PreferenceManager.getSpeedUnit(context)
        Log.d(TAG, "Speed unit: $currentSpeedUnit")
        
        return try {
            // Create log file with timestamp-based name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            currentLogFile = File(logsDir, "trip_log_$timestamp.txt")
            
            // Open file for writing
            logWriter = BufferedWriter(FileWriter(currentLogFile!!))
            
            // Write header
            writeHeader(hasLocationPerm)
            
            // Start location updates only if permission granted
            if (hasLocationPerm) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL_MS,
                        MIN_DISTANCE_METERS,
                        locationListener
                    )
                    Log.d(TAG, "Location updates started")
                } catch (e: SecurityException) {
                    Log.w(TAG, "Failed to start location updates: ${e.message}")
                }
            }
            
            // Start periodic logging timer
            startLogTimer()
            
            isLogging = true
            Log.d(TAG, "Trip logging started successfully!")
            Log.d(TAG, "Log file: ${currentLogFile?.absolutePath}")
            Log.d(TAG, "GPS enabled: $hasLocationPerm")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting trip logging", e)
            cleanup()
            false
        }
    }
    
    /**
     * Stops logging and finalizes the current log file.
     */
    override fun stopLogging() {
        if (!isLogging) {
            return
        }
        
        try {
            // Stop timer
            logTimer?.cancel()
            logTimer = null
            
            // Stop location updates
            locationManager.removeUpdates(locationListener)
            
            // Close file
            logScope.launch {
                try {
                    logWriter?.flush()
                    logWriter?.close()
                    logWriter = null
                    Log.d(TAG, "Trip logging stopped: ${currentLogFile?.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing log file", e)
                }
            }
            
            isLogging = false
            lastLocation = null
            currentLogFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping trip logging", e)
        }
    }
    
    /**
     * Checks if trip logging is currently active.
     */
    override fun isLogging(): Boolean = isLogging
    
    /**
     * Gets list of all trip log files, sorted by date (newest first).
     */
    override fun getTripLogFiles(context: Context): List<File> {
        val logsDir = storageService.getTripLogsDirectory() ?: return emptyList()
        
        return try {
            logsDir.listFiles { file ->
                file.isFile && file.name.startsWith("trip_log_") && file.name.endsWith(".txt")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error listing trip log files", e)
            emptyList()
        }
    }

    
    /**
     * Writes the header section to the log file.
     */
    private fun writeHeader(hasLocationPermission: Boolean) {
        val startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val unitLabel = if (currentSpeedUnit == "kmh") "km/h" else "mph"
        
        logWriter?.apply {
            write("Trip Log\n")
            write("Start Time: $startTime\n")
            write("Format Version: 1.0\n")
            if (!hasLocationPermission) {
                write("GPS: Disabled (no location permission)\n")
            }
            write("---\n")
            write("Timestamp,Latitude,Longitude,Speed ($unitLabel)\n")
            flush()
        }
    }
    
    /**
     * Starts the periodic timer for logging entries.
     */
    private fun startLogTimer() {
        logTimer = logScope.launch {
            while (isActive && isLogging) {
                delay(LOG_INTERVAL_MS)
                logCurrentLocation()
            }
        }
    }
    
    /**
     * Logs the current location to the file.
     */
    private fun logCurrentLocation() {
        logScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val location = lastLocation
                
                val latitude: String
                val longitude: String
                val speedStr: String
                
                if (location != null) {
                    // GPS data available
                    latitude = String.format(Locale.US, "%.6f", location.latitude)
                    longitude = String.format(Locale.US, "%.6f", location.longitude)
                    
                    // Get speed and convert to appropriate unit
                    val speedMps = if (location.hasSpeed()) location.speed else 0f
                    val speed = if (currentSpeedUnit == "kmh") {
                        speedMps * MPS_TO_KMH
                    } else {
                        speedMps * MPS_TO_MPH
                    }
                    speedStr = String.format(Locale.US, "%.1f", speed)
                } else {
                    // No GPS data - write null values
                    latitude = "N/A"
                    longitude = "N/A"
                    speedStr = "N/A"
                }
                
                // Write entry
                logWriter?.apply {
                    write("$timestamp,$latitude,$longitude,$speedStr\n")
                    flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing log entry", e)
            }
        }
    }
    
    /**
     * Checks if location permission is granted.
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Cleans up resources on error.
     */
    private fun cleanup() {
        try {
            logWriter?.close()
            logWriter = null
            currentLogFile?.delete()
            currentLogFile = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
