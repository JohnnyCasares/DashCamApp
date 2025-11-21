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
import com.kasahirotech.dashcamapp.interfaces.SpeedTrackingService

/**
 * Service for tracking vehicle speed using GPS location data.
 * Provides real-time speed updates with configurable units (mph or km/h).
 */
class SpeedTracker(
    private val context: Context,
    private val onSpeedUpdate: (speed: Float, unit: SpeedTrackingService.SpeedUnit) -> Unit
) : SpeedTrackingService {
    
    private val locationManager: LocationManager = 
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private var isTracking: Boolean = false
    private var currentUnit: SpeedTrackingService.SpeedUnit = SpeedTrackingService.SpeedUnit.MPH
    
    // For smoothing speed readings
    private val speedHistory = mutableListOf<Float>()
    private var lastSpeed: Float = 0f
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) {
                val speedMps = location.speed
                
                // Apply stationary threshold (< 0.447 m/s = 1 mph)
                if (speedMps < 0.447f) {
                    onSpeedUpdate(0f, currentUnit)
                    lastSpeed = 0f
                    speedHistory.clear()
                    return
                }
                
                // Filter out speed spikes (> 20 mph difference)
                val speedMph = speedMps * MPS_TO_MPH
                if (lastSpeed > 0 && Math.abs(speedMph - lastSpeed) > 20f) {
                    Log.w(TAG, "Speed spike detected, ignoring: $speedMph mph")
                    return
                }
                
                // Apply moving average smoothing
                speedHistory.add(speedMps)
                if (speedHistory.size > SMOOTHING_WINDOW) {
                    speedHistory.removeAt(0)
                }
                
                val smoothedSpeedMps = speedHistory.average().toFloat()
                val convertedSpeed = convertSpeed(smoothedSpeedMps, currentUnit)
                
                lastSpeed = smoothedSpeedMps * MPS_TO_MPH
                onSpeedUpdate(convertedSpeed, currentUnit)
            } else {
                // No speed data available
                onSpeedUpdate(-1f, currentUnit)
            }
        }
        
        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Deprecated but required for API compatibility
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "GPS provider enabled")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "GPS provider disabled")
            onSpeedUpdate(-1f, currentUnit)
        }
    }
    
    /**
     * Starts tracking speed using GPS location updates.
     * Requires ACCESS_FINE_LOCATION permission.
     */
    override fun startTracking() {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Cannot start tracking: location permission not granted")
            return
        }
        
        if (isTracking) {
            Log.w(TAG, "Speed tracking already started")
            return
        }
        
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MS,
                MIN_DISTANCE_METERS,
                locationListener
            )
            isTracking = true
            Log.d(TAG, "Speed tracking started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting speed tracking", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speed tracking", e)
        }
    }
    
    /**
     * Stops tracking speed and unregisters location listener.
     */
    override fun stopTracking() {
        if (!isTracking) {
            return
        }
        
        try {
            locationManager.removeUpdates(locationListener)
            isTracking = false
            speedHistory.clear()
            lastSpeed = 0f
            Log.d(TAG, "Speed tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speed tracking", e)
        }
    }
    
    /**
     * Checks if location permission is granted.
     * 
     * @return true if ACCESS_FINE_LOCATION permission is granted
     */
    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Sets the speed unit for display.
     * 
     * @param unit The desired speed unit (MPH or KMH)
     */
    override fun setSpeedUnit(unit: SpeedTrackingService.SpeedUnit) {
        currentUnit = unit
    }
    
    /**
     * Converts speed from meters per second to the specified unit.
     * 
     * @param metersPerSecond Speed in m/s from GPS
     * @param unit Target unit for conversion
     * @return Speed in the specified unit
     */
    private fun convertSpeed(metersPerSecond: Float, unit: SpeedTrackingService.SpeedUnit): Float {
        return when (unit) {
            SpeedTrackingService.SpeedUnit.MPH -> metersPerSecond * MPS_TO_MPH
            SpeedTrackingService.SpeedUnit.KMH -> metersPerSecond * MPS_TO_KMH
        }
    }
    
    companion object {
        private const val TAG = "SpeedTracker"
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second
        private const val MIN_DISTANCE_METERS = 0f // Update on any movement
        private const val MPS_TO_MPH = 2.237f // Conversion factor
        private const val MPS_TO_KMH = 3.6f // Conversion factor
        private const val SMOOTHING_WINDOW = 3 // Number of readings to average
    }
}
