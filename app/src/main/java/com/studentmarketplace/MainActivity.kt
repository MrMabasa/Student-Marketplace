
package com.studentmarketplace

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.studentmarketplace.notifications.MarketplaceFirebaseMessagingService
import com.studentmarketplace.ui.settings.AppPreferences

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Permission result is handled by Android.
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Apply the saved theme before creating the activity UI.
        AppCompatDelegate.setDefaultNightMode(
            if (AppPreferences.isDarkModeEnabled(this)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()
        registerTokenForSignedInUser()
    }

    private fun registerTokenForSignedInUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && currentUser.isEmailVerified) {
            MarketplaceFirebaseMessagingService.refreshAndRegisterToken(
                applicationContext
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }
}
