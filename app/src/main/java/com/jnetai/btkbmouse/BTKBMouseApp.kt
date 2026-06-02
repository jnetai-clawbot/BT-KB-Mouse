package com.jnetai.btkbmouse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.jnetai.btkbmouse.data.AppDatabase

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "btkbmouse_settings")

class BTKBMouseApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var dataStore: DataStore<Preferences>
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room database singleton
        database = AppDatabase.getInstance(this)

        // Initialize DataStore
        dataStore = dataStore

        // Create notification channel for foreground service
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "BT-KB-Mouse Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for the Bluetooth keyboard and mouse service"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val FOREGROUND_CHANNEL_ID = "btkbmouse_foreground_service"

        @Volatile
        private var instance: BTKBMouseApp? = null

        fun getInstance(): BTKBMouseApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}
