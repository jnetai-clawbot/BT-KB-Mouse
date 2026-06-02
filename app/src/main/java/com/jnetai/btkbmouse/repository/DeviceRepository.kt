package com.jnetai.btkbmouse.repository

import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceDao
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {

    val allDevices: Flow<List<Device>> = deviceDao.getAllDevices()

    val trustedDevices: Flow<List<Device>> = deviceDao.getTrustedDevices()

    suspend fun getDeviceByAddress(address: String): Device? {
        return deviceDao.getDeviceByAddress(address)
    }

    suspend fun insertDevice(device: Device): Long {
        return deviceDao.insertDevice(device)
    }

    suspend fun updateDevice(device: Device) {
        deviceDao.updateDevice(device)
    }

    suspend fun deleteDevice(device: Device) {
        deviceDao.deleteDevice(device)
    }

    suspend fun updateLastConnected(address: String) {
        val device = deviceDao.getDeviceByAddress(address)
        device?.let {
            deviceDao.updateDevice(it.copy(lastConnected = System.currentTimeMillis()))
        }
    }

    suspend fun setDeviceTrusted(address: String, trusted: Boolean) {
        val device = deviceDao.getDeviceByAddress(address)
        device?.let {
            deviceDao.updateDevice(it.copy(isTrusted = trusted))
        }
    }

    suspend fun updateBatteryLevel(address: String, level: Int) {
        val device = deviceDao.getDeviceByAddress(address)
        device?.let {
            deviceDao.updateDevice(it.copy(batteryLevel = level))
        }
    }
}
