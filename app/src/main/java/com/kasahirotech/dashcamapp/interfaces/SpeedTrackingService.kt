package com.kasahirotech.dashcamapp.interfaces

/**
 * Interface for tracking vehicle speed using GPS or other sources.
 * Provides abstraction for speed tracking operations, enabling
 * testability and flexibility in speed data sources.
 */
interface SpeedTrackingService {
    
    enum class SpeedUnit {
        MPH,
        KMH
    }
    
    /**
     * Starts tracking speed.
     * Implementation should handle permission checks internally.
     */
    fun startTracking()
    
    /**
     * Stops tracking speed and cleans up resources.
     */
    fun stopTracking()
    
    /**
     * Checks if location permission is granted.
     * 
     * @return true if permission is granted, false otherwise
     */
    fun hasLocationPermission(): Boolean
    
    /**
     * Sets the speed unit for display.
     * 
     * @param unit The desired speed unit (MPH or KMH)
     */
    fun setSpeedUnit(unit: SpeedUnit)
}
