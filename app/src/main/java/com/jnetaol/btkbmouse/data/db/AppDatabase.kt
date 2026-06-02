package com.jnetaol.btkbmouse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jnetaol.btkbmouse.data.model.*

@Database(
    entities = [
        Profile::class,
        AppSetting::class,
        PresetPhrase::class,
        ClipboardEntry::class,
        EmulatedDevice::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun settingsDao(): SettingsDao
    abstract fun presetPhraseDao(): PresetPhraseDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun emulatedDeviceDao(): EmulatedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "btkbmouse_pro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
