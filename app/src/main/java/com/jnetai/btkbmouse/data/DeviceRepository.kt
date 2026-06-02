package com.jnetai.btkbmouse.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Device entities.
 * Provides Flow-based observation and CRUD operations for devices.
 */
class DeviceRepository(private val database: AppDatabase) {

    private val deviceDao: DeviceDao = database.deviceDao()

    /**
     * Observe all devices
     */
    fun observeAllDevices(): Flow<List<Device>> {
        return deviceDao.getAllDevices()
    }

    /**
     * Observe trusted devices
     */
    fun observeTrustedDevices(): Flow<List<Device>> {
        return deviceDao.getTrustedDevices()
    }

    /**
     * Observe connected devices
     */
    fun observeConnectedDevices(): Flow<List<Device>> {
        return deviceDao.getConnectedDevices()
    }

    /**
     * Get a device by address (one-shot)
     */
    suspend fun getDeviceByAddress(address: String): Device? {
        return deviceDao.getDeviceByAddress(address)
    }

    /**
     * Get all devices as a list (one-shot)
     */
    suspend fun getAllDevicesList(): List<Device> {
        return deviceDao.getAllDevicesList()
    }

    /**
     * Get trusted devices as a list (one-shot)
     */
    suspend fun getTrustedDevicesList(): List<Device> {
        return deviceDao.getTrustedDevicesList()
    }

    /**
     * Save a device (insert or update)
     */
    suspend fun saveDevice(device: Device): Long {
        return deviceDao.insertDevice(device)
    }

    /**
     * Create and save a new device from basic info
     */
    suspend fun createDevice(
        name: String,
        address: String,
        type: DeviceType = DeviceType.UNKNOWN,
        isTrusted: Boolean = false
    ): Long {
        val device = Device.create(
            name = name,
            address = address,
            type = type,
            isTrusted = isTrusted
        )
        return deviceDao.insertDevice(device)
    }

    /**
     * Update an existing device
     */
    suspend fun updateDevice(device: Device) {
        deviceDao.updateDevice(device)
    }

    /**
     * Delete a device
     */
    suspend fun deleteDevice(device: Device) {
        deviceDao.deleteDevice(device)
    }

    /**
     * Delete a device by address
     */
    suspend fun deleteDeviceByAddress(address: String) {
        deviceDao.deleteDeviceByAddress(address)
    }

    /**
     * Update connection status for a device
     */
    suspend fun updateConnectionStatus(address: String, connected: Boolean) {
        deviceDao.updateConnectionStatus(address, connected)
    }

    /**
     * Update battery level for a device
     */
    suspend fun updateBatteryLevel(address: String, batteryLevel: Int) {
        deviceDao.updateBatteryLevel(address, batteryLevel)
    }

    /**
     * Update signal strength for a device
     */
    suspend fun updateSignalStrength(address: String, signalStrength: Int) {
        deviceDao.updateSignalStrength(address, signalStrength)
    }

    /**
     * Set trusted status for a device
     */
    suspend fun setTrustedStatus(address: String, isTrusted: Boolean) {
        deviceDao.setTrustedStatus(address, isTrusted)
    }

    /**
     * Mark a device as trusted
     */
    suspend fun trustDevice(device: Device) {
        deviceDao.setTrustedStatus(device.address, true)
    }

    /**
     * Untrust a device
     */
    suspend fun untrustDevice(device: Device) {
        deviceDao.setTrustedStatus(device.address, false)
    }

    /**
     * Get device count
     */
    suspend fun getDeviceCount(): Int {
        return deviceDao.getDeviceCount()
    }

    /**
     * Get trusted device count
     */
    suspend fun getTrustedDeviceCount(): Int {
        return deviceDao.getTrustedDeviceCount()
    }

    /**
     * Find or create device by address
     */
    suspend fun findOrCreateDevice(address: String, name: String, type: DeviceType): Device {
        val existing = deviceDao.getDeviceByAddress(address)
        return if (existing != null) {
            existing
        } else {
            val newDevice = Device.create(name = name, address = address, type = type)
            val id = deviceDao.insertDevice(newDevice)
            newDevice.copy(id = id)
        }
    }

    /**
     * Update device type
     */
    suspend fun updateDeviceType(address: String, type: DeviceType) {
        val device = deviceDao.getDeviceByAddress(address)
        if (device != null) {
            deviceDao.updateDevice(device.copy(type = type))
        }
    }
}
