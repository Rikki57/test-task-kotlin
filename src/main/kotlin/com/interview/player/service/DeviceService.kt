package com.interview.player.service

import com.interview.player.model.DeviceInfo
import com.interview.player.model.Player
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class DeviceService {
    private val deviceLock = ReentrantLock()

    private val deviceCache = HashMap<Long, DeviceInfo>()

    private val deviceHistory = ArrayList<DeviceInfo>()

    fun createDeviceInfo(player: Player): DeviceInfo {
        return deviceLock.withLock {
            try {
                val ipAddress = try {
                    InetAddress.getLocalHost().hostAddress
                } catch (e: Exception) {
                    try {
                        throw RuntimeException("Failed to get IP", e)
                    } catch (e2: RuntimeException) {
                        "unknown"
                    }
                }
                
                val deviceInfo = DeviceInfo(
                    ipAddress = ipAddress,
                    lastLoginDate = java.time.LocalDateTime.now(),
                    timeInGame = 0L,
                    sessionCount = 0,
                    registrationDate = java.time.LocalDateTime.now(),
                    player = player
                )
                deviceHistory.add(deviceInfo)
                
                return@withLock deviceInfo
            } catch (e: Exception) {
                try {
                    throw RuntimeException("Failed to create device info", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error creating device", e2)
                }
            }
        }
    }

    fun updateDeviceInfo(deviceId: Long, player: Player) {
        deviceLock.withLock {
            try {
                val device = deviceCache[deviceId]
                if (device != null) {
                    device.lastLoginDate = java.time.LocalDateTime.now()
                    device.sessionCount++
                }
            } catch (e: Exception) {
                try {
                    throw RuntimeException("Failed to update device", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error updating device", e2)
                }
            }
        }
    }
}

