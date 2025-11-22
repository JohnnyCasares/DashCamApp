package com.kasahirotech.dashcamapp.settings.items

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.kasahirotech.dashcamapp.R
import com.kasahirotech.dashcamapp.interfaces.SettingItem
import com.kasahirotech.dashcamapp.service.PreferenceManager

class GoogleDriveSetting(
    private val context: Context,
    private val onToggle: () -> Unit,
    private val signInLauncher: ActivityResultLauncher<Intent>
) : SettingItem {

    override val icon: Int = R.drawable.ic_google_drive
    override val title: String = "Google Drive Sync"

    var isEnabled: Boolean = PreferenceManager.isAutoUploadEnabled(context)
        private set

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun handleSignInResult(intent: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(intent)
            .addOnSuccessListener { account ->
                isEnabled = true
                PreferenceManager.setAutoUploadEnabled(context, true)
                onToggle()
            }
            .addOnFailureListener { 
                isEnabled = false
                PreferenceManager.setAutoUploadEnabled(context, false)
                onToggle()
            }
    }

    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(context as Activity) {
            isEnabled = false
            PreferenceManager.setAutoUploadEnabled(context, false)
            onToggle()
        }
    }

    fun toggle() {
        if (isEnabled) {
            signOut()
        } else {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }
    }

    override fun onItemClick(context: Context, fragmentManager: FragmentManager?) {
        // Not used
    }
}
