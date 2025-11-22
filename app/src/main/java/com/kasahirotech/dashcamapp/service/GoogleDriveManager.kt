package com.kasahirotech.dashcamapp.service

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.kasahirotech.dashcamapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.kasahirotech.dashcamapp.interfaces.CloudStorageService
import com.kasahirotech.dashcamapp.models.CloudFileInfo
import com.kasahirotech.dashcamapp.models.StorageQuota
import kotlinx.coroutines.Job

/**
 * Implementation of CloudStorageService for Google Drive
 * Handles OAuth 2.0 authentication, file uploads, and Drive API operations
 */
object GoogleDriveManager : CloudStorageService {
    private const val FOLDER_NAME = "DashCamVideos"
    private const val PREFS_NAME = "google_drive_prefs"
    private const val KEY_ACCOUNT_NAME = "account_name"
    private const val KEY_FOLDER_ID = "folder_id"
    
    private var driveService: Drive? = null
    private val activeUploads = mutableMapOf<String, Job>()
    private var dashCamFolderId: String? = null
    
    /**
     * Get encrypted shared preferences for secure credential storage
     */
    private fun getEncryptedPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if encryption fails
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Authenticate with Google Drive using OAuth 2.0
     * This should be called from an Activity context to launch the sign-in flow
     */
    override suspend fun authenticate(context: Context): Result<Boolean> {
        return try {
            // Note: This method prepares authentication but requires Activity context
            // to actually launch the sign-in intent. The calling Activity should handle
            // the sign-in flow and call storeCredentials() after successful authentication.
            
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            
            // Check if already signed in
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))) {
                initializeDriveService(context, account)
                storeAccountInfo(context, account.email ?: "")
                Result.success(true)
            } else {
                // Return false to indicate sign-in flow needs to be initiated
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize Drive service with authenticated account
     */
    private fun initializeDriveService(context: Context, account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }
    
    /**
     * Store account information securely
     */
    private fun storeAccountInfo(context: Context, accountName: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_ACCOUNT_NAME, accountName)
            .apply()
    }
    
    /**
     * Disconnect from Google Drive and clear credentials
     */
    override suspend fun disconnect(context: Context): Result<Boolean> {
        return try {
            // Sign out from Google
            val signInClient = GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            )
            signInClient.signOut()
            
            // Clear stored credentials
            getEncryptedPrefs(context).edit().clear().apply()
            
            // Clear drive service
            driveService = null
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is authenticated with Google Drive
     */
    override fun isAuthenticated(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && 
               GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE)) &&
               driveService != null
    }
    
    /**
     * Get connected account information
     */
    override fun getAccountInfo(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_ACCOUNT_NAME, null)
    }
    
    /**
     * Get or create the DashCamVideos folder in Google Drive
     */
    private suspend fun getOrCreateFolder(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            // Check if we have cached folder ID
            val cachedFolderId = getEncryptedPrefs(context).getString(KEY_FOLDER_ID, null)
            if (cachedFolderId != null) {
                // Verify folder still exists
                try {
                    service.files().get(cachedFolderId).execute()
                    dashCamFolderId = cachedFolderId
                    return@withContext cachedFolderId
                } catch (e: Exception) {
                    // Folder doesn't exist, create new one
                }
            }
            
            // Search for existing folder
            val query = "mimeType='application/vnd.google-apps.folder' and name='$FOLDER_NAME' and trashed=false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
            
            val folderId = if (result.files.isNotEmpty()) {
                result.files[0].id
            } else {
                // Create new folder
                val folderMetadata = DriveFile().apply {
                    name = FOLDER_NAME
                    mimeType = "application/vnd.google-apps.folder"
                }
                val folder = service.files().create(folderMetadata)
                    .setFields("id")
                    .execute()
                folder.id
            }
            
            // Cache folder ID
            getEncryptedPrefs(context).edit()
                .putString(KEY_FOLDER_ID, folderId)
                .apply()
            
            dashCamFolderId = folderId
            folderId
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Upload a file to Google Drive
     */
    override suspend fun uploadFile(
        context: Context,
        filePath: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Not authenticated with Google Drive")
            )
            
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("File not found: $filePath")
                )
            }
            
            // Get or create folder
            val folderId = getOrCreateFolder(context) ?: return@withContext Result.failure(
                IllegalStateException("Failed to create/access DashCamVideos folder")
            )
            
            // Create file metadata
            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
            }
            
            // Create media content
            val mediaContent = FileContent("video/mp4", file)
            
            // Report initial progress
            onProgress(0f)
            
            // Upload file
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, createdTime, modifiedTime, webViewLink")
                .execute()
            
            // Report completion
            onProgress(100f)
            
            Result.success(uploadedFile.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel an active upload
     */
    override suspend fun cancelUpload(uploadId: String): Result<Boolean> {
        return try {
            val job = activeUploads[uploadId]
            if (job != null) {
                job.cancel()
                activeUploads.remove(uploadId)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a file from Google Drive
     */
    override suspend fun deleteFile(context: Context, fileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Not authenticated with Google Drive")
            )
            
            service.files().delete(fileId).execute()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get file information from Google Drive
     */
    override suspend fun getFileInfo(context: Context, fileId: String): Result<CloudFileInfo> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Not authenticated with Google Drive")
            )
            
            val file = service.files().get(fileId)
                .setFields("id, name, size, mimeType, createdTime, modifiedTime, webViewLink")
                .execute()
            
            val fileInfo = CloudFileInfo(
                id = file.id,
                name = file.name,
                size = file.getSize() ?: 0L,
                mimeType = file.mimeType,
                createdTime = file.createdTime?.value ?: 0L,
                modifiedTime = file.modifiedTime?.value ?: 0L,
                webViewLink = file.webViewLink
            )
            
            Result.success(fileInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * List all files in the DashCamVideos folder
     */
    override suspend fun listFiles(context: Context): Result<List<CloudFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Not authenticated with Google Drive")
            )
            
            val folderId = getOrCreateFolder(context) ?: return@withContext Result.failure(
                IllegalStateException("Failed to access DashCamVideos folder")
            )
            
            val query = "'$folderId' in parents and trashed=false"
            val result = service.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, size, mimeType, createdTime, modifiedTime, webViewLink)")
                .setOrderBy("createdTime desc")
                .execute()
            
            val fileList = result.files.map { file ->
                CloudFileInfo(
                    id = file.id,
                    name = file.name,
                    size = file.getSize() ?: 0L,
                    mimeType = file.mimeType,
                    createdTime = file.createdTime?.value ?: 0L,
                    modifiedTime = file.modifiedTime?.value ?: 0L,
                    webViewLink = file.webViewLink
                )
            }
            
            Result.success(fileList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get storage quota information from Google Drive
     */
    override suspend fun getStorageQuota(context: Context): Result<StorageQuota> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext Result.failure(
                IllegalStateException("Not authenticated with Google Drive")
            )
            
            val about = service.about().get()
                .setFields("storageQuota")
                .execute()
            
            val quota = about.storageQuota
            val storageQuota = StorageQuota(
                limit = quota.limit ?: 0L,
                usage = quota.usage ?: 0L,
                usageInDrive = quota.usageInDrive ?: 0L
            )
            
            Result.success(storageQuota)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
