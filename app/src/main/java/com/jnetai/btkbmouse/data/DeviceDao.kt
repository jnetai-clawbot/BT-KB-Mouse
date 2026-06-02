package com.jnetai.btkbmouse.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Device entity
 */
@Dao
interface DeviceDao {

    /**
     * Observe all devices ordered by last connected timestamp
     */
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<Device>>

    /**
     * Observe all trusted devices ordered by last connected timestamp
     */
    @Query("SELECT * FROM devices WHERE isTrusted = 1 ORDER BY lastConnected DESC")
    fun getTrustedDevices(): Flow<List<Device>>

    /**
     * Observe all connected devices
     */
    @Query("SELECT * FROM devices WHERE isConnected = 1 ORDER BY name ASC")
    fun getConnectedDevices(): Flow<List<Device>>

    /**
     * Get a single device by address
     */
    @Query("SELECT * FROM devices WHERE address = :address LIMIT 1")
    suspend fun getDeviceByAddress(address: String): Device?

    /**
     * Get all devices as a one-shot list (non-Flow)
     */
    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    suspend fun getAllDevicesList(): List<Device>

    /**
     * Get trusted devices as a one-shot list (non-Flow)
     */
    @Query("SELECT * FROM devices WHERE isTrusted = 1 ORDER BY lastConnected DESC")
    suspend fun getTrustedDevicesList(): List<Device>

    /**
     * Insert or replace a device
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    /**
     * Update an existing device
     */
    @Update
    suspend fun updateDevice(device: Device)

    /**
     * Delete a device
     */
    @Delete
    suspend fun deleteDevice(device: Device)

    /**
     * Delete a device by address
     */
    @Query("DELETE FROM devices WHERE address = :address")
    suspend fun deleteDeviceByAddress(address: String)

    /**
     * Update connection status for a device
     */
    @Query("UPDATE devices SET isConnected = :isConnected, lastConnected = :timestamp WHERE address = :address")
    suspend fun updateConnectionStatus(address: String, isConnected: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * Update battery level for a device
     */
    @Query("UPDATE devices SET batteryLevel = :batteryLevel WHERE address = :address")
    suspend fun updateBatteryLevel(address: String, batteryLevel: Int)

    /**
     * Update signal strength for a device
     */
    @Query("UPDATE devices SET signalStrength = :signalStrength WHERE address = :address")
    suspend fun updateSignalStrength(address: String, signalStrength: Int)

    /**
     * Set trusted status for a device
     */
    @Query("UPDATE devices SET isTrusted = :isTrusted WHERE address = :address")
    suspend fun setTrustedStatus(address: String, isTrusted: Boolean)

    /**
     * Count total devices
     */
    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCount(): Int

    /**
     * Count trusted devices
     */
    @Query("SELECT COUNT(*) FROM devices WHERE isTrusted = 1")
    suspend fun getTrustedDeviceCount(): Int
}
