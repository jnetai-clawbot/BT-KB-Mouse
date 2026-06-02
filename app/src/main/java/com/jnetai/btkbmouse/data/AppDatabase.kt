package com.jnetai.btkbmouse.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for BT-KB-Mouse application.
 * Contains Device and Profile entities with their respective DAOs.
 */
@Database(
    entities = [Device::class, Profile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get DeviceDao instance
     */
    abstract fun deviceDao(): DeviceDao

    /**
     * Get ProfileDao instance
     */
    abstract fun profileDao(): ProfileDao

    companion object {
        private const val DATABASE_NAME = "btkbmouse_database"

        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Get singleton instance of AppDatabase
         * @param context Application context
         * @return AppDatabase instance
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        /**
         * Close database instance (for testing)
         */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
