
package com.studentmarketplace.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.studentmarketplace.MainActivity
import com.studentmarketplace.R
import com.studentmarketplace.data.remote.DeviceTokenRequest
import com.studentmarketplace.data.remote.ListingApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MarketplaceFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MarketplaceFCM"
        private const val CHANNEL_ID = "marketplace_notifications"
        private const val CHANNEL_NAME = "Marketplace Notifications"
        private const val PREFS_NAME = "fcm_preferences"
        private const val TOKEN_KEY = "pending_fcm_token"

        /**
         * Retrieves the current FCM token and saves it locally.
         * Call this after a verified user signs in.
         */
        fun refreshAndRegisterToken(context: Context) {
            val appContext = context.applicationContext

            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    saveToken(appContext, token)
                    registerSavedToken(appContext)
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Could not retrieve FCM token", error)
                }
        }

        /**
         * Saves a token so it can be registered with the backend.
         */
        private fun saveToken(context: Context, token: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(TOKEN_KEY, token)
                .apply()
        }

        /**
         * Sends the saved token to the backend when a verified
         * Firebase user is signed in.
         */
        fun registerSavedToken(context: Context) {
            val appContext = context.applicationContext
            val preferences = appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            val token = preferences.getString(TOKEN_KEY, null) ?: return
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return

            if (!currentUser.isEmailVerified) {
                Log.d(TAG, "Waiting for email verification before registering token.")
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ListingApiClient.service.registerDeviceToken(
                        DeviceTokenRequest(token)
                    )

                    // Only remove the saved token if it has not changed
                    // while the backend request was running.
                    if (preferences.getString(TOKEN_KEY, null) == token) {
                        preferences.edit().remove(TOKEN_KEY).apply()
                    }

                    Log.d(TAG, "FCM device token registered successfully.")
                } catch (error: Exception) {
                    Log.e(TAG, "Could not register FCM device token", error)
                }
            }
        }
    }

    /**
     * Called by Firebase when the device token is created or refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "FCM token refreshed.")
        saveToken(applicationContext, token)
        registerSavedToken(applicationContext)
    }

    /**
     * Handles incoming push notifications.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Student Marketplace"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new notification."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (error: SecurityException) {
            Log.e(TAG, "Notification permission is not available", error)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about marketplace activity"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
