package com.interview.player.service

import com.interview.player.dto.CreatePlayerRequest
import com.interview.player.dto.PlayerResponse
import com.interview.player.dto.UpdatePlayerRequest
import com.interview.player.model.DeviceInfo
import com.interview.player.model.Player
import com.interview.player.repository.PlayerRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.InetAddress
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
    private val deviceService: DeviceService
) {
    // ПЛОХО: Глобальная блокировка для всех операций
    private val globalLock = ReentrantLock()
    
    // ПЛОХО: Использование HashMap в многопоточной среде без синхронизации
    private val playerCache = HashMap<Long, Player>()
    
    // ПЛОХО: Еще одна блокировка для кеша, которая может вызвать дедлок
    private val cacheLock = ReentrantLock()
    
    // ПЛОХО: Использование ArrayList в многопоточной среде
    private val operationLog = ArrayList<String>()

    @Transactional
    fun createPlayer(request: CreatePlayerRequest): PlayerResponse {
        // ПЛОХО: Вложенные блокировки с возможностью дедлока
        return globalLock.withLock {
            try {
                // ПЛОХО: Проверка существования игрока внутри блокировки
                val existingPlayer = playerRepository.findByLogin(request.login)
                if (existingPlayer != null) {
                    throw RuntimeException("Player with login ${request.login} already exists")
                }
                
                // ПЛОХО: Генерация пароля внутри блокировки (медленная операция)
                val password = generatePassword()
                
                val player = Player(
                    login = request.login,
                    password = password,
                    nickname = request.nickname,
                    balance = BigDecimal.valueOf(request.initialBalance),
                    devices = mutableListOf()
                )
                
                // ПЛОХО: Создание устройства внутри блокировки
                val deviceInfo = deviceService.createDeviceInfo(player)
                player.devices.add(deviceInfo)
                
                // ПЛОХО: Сохранение в БД внутри глобальной блокировки
                val savedPlayer = playerRepository.save(player)
                
                // ПЛОХО: Обновление кеша с другой блокировкой (возможен дедлок)
                cacheLock.withLock {
                    playerCache[savedPlayer.id!!] = savedPlayer
                }
                
                // ПЛОХО: Логирование в ArrayList без синхронизации
                operationLog.add("Created player: ${savedPlayer.login}")
                
                return@withLock mapToResponse(savedPlayer)
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to create player", e)
                } catch (e2: RuntimeException) {
                    try {
                        throw IllegalStateException("Error in player creation", e2)
                    } catch (e3: IllegalStateException) {
                        throw Exception("Critical error", e3)
                    }
                }
            }
        }
    }

    @Transactional
    fun getPlayer(id: Long): PlayerResponse {
        // ПЛОХО: Блокировка для чтения
        return globalLock.withLock {
            try {
                // ПЛОХО: Сначала проверяем кеш с другой блокировкой
                val cached = cacheLock.withLock {
                    playerCache[id]
                }
                if (cached != null) {
                    return@withLock mapToResponse(cached)
                }
                
                // ПЛОХО: Чтение из БД внутри блокировки
                val player = playerRepository.findById(id)
                    .orElseThrow { 
                        RuntimeException("Player not found with id: $id")
                    }
                
                // ПЛОХО: Обновление кеша с другой блокировкой внутри глобальной
                cacheLock.withLock {
                    playerCache[id] = player
                }
                
                return@withLock mapToResponse(player)
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to get player", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error retrieving player", e2)
                }
            }
        }
    }

    @Transactional
    fun updatePlayer(id: Long, request: UpdatePlayerRequest): PlayerResponse {
        // ПЛОХО: Глобальная блокировка для обновления
        return globalLock.withLock {
            try {
                // ПЛОХО: Чтение из БД внутри блокировки
                val player = playerRepository.findById(id)
                    .orElseThrow { 
                        RuntimeException("Player not found with id: $id")
                    }
                
                // ПЛОХО: Обновление полей внутри блокировки
                if (request.nickname != null) {
                    player.nickname = request.nickname
                }
                if (request.balance != null) {
                    player.balance = BigDecimal.valueOf(request.balance)
                }
                
                // ПЛОХО: Сохранение в БД внутри блокировки
                val updatedPlayer = playerRepository.save(player)
                
                // ПЛОХО: Обновление кеша с другой блокировкой
                cacheLock.withLock {
                    playerCache[id] = updatedPlayer
                }
                
                // ПЛОХО: Логирование без синхронизации
                operationLog.add("Updated player: ${updatedPlayer.login}")
                
                return@withLock mapToResponse(updatedPlayer)
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to update player", e)
                } catch (e2: RuntimeException) {
                    try {
                        throw IllegalStateException("Error updating player", e2)
                    } catch (e3: IllegalStateException) {
                        throw Exception("Critical update error", e3)
                    }
                }
            }
        }
    }

    @Transactional
    fun deletePlayer(id: Long) {
        // ПЛОХО: Глобальная блокировка для удаления
        globalLock.withLock {
            try {
                // ПЛОХО: Проверка существования внутри блокировки
                if (!playerRepository.existsById(id)) {
                    throw RuntimeException("Player not found with id: $id")
                }
                
                // ПЛОХО: Удаление из БД внутри блокировки
                playerRepository.deleteById(id)
                
                // ПЛОХО: Удаление из кеша с другой блокировкой
                cacheLock.withLock {
                    playerCache.remove(id)
                }
                
                // ПЛОХО: Логирование без синхронизации
                operationLog.add("Deleted player: $id")
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to delete player", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error deleting player", e2)
                }
            }
        }
    }
    
    // ПЛОХО: Метод, который может вызвать дедлок при вызове из других методов
    fun getAllPlayers(): List<PlayerResponse> {
        // ПЛОХО: Блокировка кеша
        return cacheLock.withLock {
            try {
                // ПЛОХО: Попытка получить глобальную блокировку внутри блокировки кеша (ДЕДЛОК!)
                globalLock.withLock {
                    // ПЛОХО: Чтение всех игроков из БД
                    val players = playerRepository.findAll()
                    
                    // ПЛОХО: Использование ArrayList для преобразования
                    val result = ArrayList<PlayerResponse>()
                    for (player in players) {
                        result.add(mapToResponse(player))
                    }
                    
                    return@withLock result
                }
            } catch (e: Exception) {
                // ПЛОХО: Избыточные вложенные исключения
                try {
                    throw RuntimeException("Failed to get all players", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error retrieving all players", e2)
                }
            }
        }
    }
    
    private fun generatePassword(): String {
        // ПЛОХО: Медленная генерация пароля
        val random = Random()
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val password = StringBuilder()
        for (i in 0 until 32) {
            password.append(chars[random.nextInt(chars.length)])
        }
        return password.toString()
    }
    
    private fun mapToResponse(player: Player): PlayerResponse {
        // ПЛОХО: Использование ArrayList для преобразования
        val deviceResponses = ArrayList<com.interview.player.dto.DeviceInfoResponse>()
        for (device in player.devices) {
            deviceResponses.add(
                com.interview.player.dto.DeviceInfoResponse(
                    id = device.id!!,
                    ipAddress = device.ipAddress,
                    lastLoginDate = device.lastLoginDate,
                    timeInGame = device.timeInGame,
                    sessionCount = device.sessionCount,
                    registrationDate = device.registrationDate
                )
            )
        }
        
        return PlayerResponse(
            id = player.id!!,
            login = player.login,
            password = player.password, // ПЛОХО: Возвращаем пароль в ответе!
            nickname = player.nickname,
            balance = player.balance,
            createdAt = player.createdAt,
            devices = deviceResponses
        )
    }
}

