package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import java.io.File

/**
 * Interface for managing trip log operations.
 * Provides abstraction for trip logging functionality, enabling
 * testability and flexibility in logging implementation.
 */
interface TripLogService {
    
    /**
     * Starts logging trip data to a new log file.
     * Creates a new log file with timestamp-based naming and begins
     * recording GPS coordinates and speed at regular intervals.
     * 
     * @param context Application context
     * @return true if logging started successfully, false otherwise
     */
    fun startLogging(context: Context): Boolean
    
    /**
     * Stops logging and finalizes the current log file.
     * Closes the file handle and stops recording location data.
     */
    fun stopLogging()
    
    /**
     * Checks if trip logging is currently active.
     * 
     * @return true if logging is active, false otherwise
     */
    fun isLogging(): Boolean
    
    /**
     * Gets list of all trip log files.
     * 
     * @param context Application context
     * @return List of trip log files, sorted by date (newest first)
     */
    fun getTripLogFiles(context: Context): List<File>
}
