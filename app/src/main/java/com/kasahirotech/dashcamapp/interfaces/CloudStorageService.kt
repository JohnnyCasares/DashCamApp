package com.kasahirotech.dashcamapp.interfaces

import android.content.Context
import com.kasahirotech.dashcamapp.models.CloudFileInfo
import com.kasahirotech.dashcamapp.models.StorageQuota

/**
 * Interface for cloud storage operations (e.g., Google Drive)
 * Provides methods for authentication, file upload/management, and storage info
 */
interface CloudStorageService {
    // Authentication
    suspend fun authenticate(context: Context): Result<Boolean>
    suspend fun disconnect(context: Context): Result<Boolean>
    fun isAuthenticated(context: Context): Boolean
    fun getAccountInfo(context: Context): String?
    
    // Upload Operations
    suspend fun uploadFile(
        context: Context,
        filePath: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> // Returns file ID
    
    suspend fun cancelUpload(uploadId: String): Result<Boolean>
    
    // File Management
    suspend fun deleteFile(context: Context, fileId: String): Result<Boolean>
    suspend fun getFileInfo(context: Context, fileId: String): Result<CloudFileInfo>
    suspend fun listFiles(context: Context): Result<List<CloudFileInfo>>
    
    // Storage Info
    suspend fun getStorageQuota(context: Context): Result<StorageQuota>
}
