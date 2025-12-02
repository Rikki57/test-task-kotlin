package com.interview.player.service

import com.interview.player.model.DeviceInfo
import com.interview.player.model.Player
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class DeviceService {
    // ПЛОХО: Еще одна глобальная блокировка
    private val deviceLock = ReentrantLock()
    
    // ПЛОХО: HashMap без синхронизации
    private val deviceCache = HashMap<Long, DeviceInfo>()
    
    // ПЛОХО: ArrayList в многопоточной среде
    private val deviceHistory = ArrayList<DeviceInfo>()

    fun createDeviceInfo(player: Player): DeviceInfo {
        // ПЛОХО: Блокировка для создания устройства
        return deviceLock.withLock {
            try {
                // ПЛОХО: Медленная операция получения IP внутри блокировки
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
                
                // ПЛОХО: Добавление в историю без синхронизации
                deviceHistory.add(deviceInfo)
                
                return@withLock deviceInfo
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to create device info", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error creating device", e2)
                }
            }
        }
    }
    
    // ПЛОХО: Метод, который может вызвать дедлок
    fun updateDeviceInfo(deviceId: Long, player: Player) {
        // ПЛОХО: Блокировка устройства
        deviceLock.withLock {
            try {
                // ПЛОХО: Если этот метод вызывается из PlayerService с глобальной блокировкой - ДЕДЛОК!
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

