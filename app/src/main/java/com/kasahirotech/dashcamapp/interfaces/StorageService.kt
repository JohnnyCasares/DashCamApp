package com.kasahirotech.dashcamapp.interfaces

import java.io.File

/**
 * Interface for managing file storage operations.
 * Provides abstraction for storage operations, enabling
 * testability and flexibility in storage implementation.
 */
interface StorageService {
    
    /**
     * Gets the private directory for storing recordings.
     * 
     * @return File object representing the recordings directory, or null if unavailable
     */
    fun getPrivateRecordingsDirectory(): File?
    
    /**
     * Gets the directory for storing trip log files.
     * Creates the directory if it doesn't exist.
     * 
     * @return File object representing the logs directory, or null if unavailable
     */
    fun getTripLogsDirectory(): File?
}
