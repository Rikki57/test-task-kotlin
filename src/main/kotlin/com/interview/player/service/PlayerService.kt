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
    private val globalLock = ReentrantLock()

    private val playerCache = HashMap<Long, Player>()

    private val cacheLock = ReentrantLock()

    private val operationLog = ArrayList<String>()

    @Transactional
    fun createPlayer(request: CreatePlayerRequest): PlayerResponse {
        return globalLock.withLock {
            try {
                val existingPlayer = playerRepository.findByLogin(request.login)
                if (existingPlayer != null) {
                    throw RuntimeException("Player with login ${request.login} already exists")
                }

                val password = generatePassword()
                
                val player = Player(
                    login = request.login,
                    password = password,
                    nickname = request.nickname,
                    balance = BigDecimal.valueOf(request.initialBalance),
                    devices = mutableListOf()
                )

                val deviceInfo = deviceService.createDeviceInfo(player)
                player.devices.add(deviceInfo)

                val savedPlayer = playerRepository.save(player)

                cacheLock.withLock {
                    playerCache[savedPlayer.id!!] = savedPlayer
                }

                operationLog.add("Created player: ${savedPlayer.login}")
                
                return@withLock mapToResponse(savedPlayer)
            } catch (e: Exception) {
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
        return globalLock.withLock {
            try {
                val cached = cacheLock.withLock {
                    playerCache[id]
                }
                if (cached != null) {
                    return@withLock mapToResponse(cached)
                }

                val player = playerRepository.findById(id)
                    .orElseThrow { 
                        RuntimeException("Player not found with id: $id")
                    }

                cacheLock.withLock {
                    playerCache[id] = player
                }
                
                return@withLock mapToResponse(player)
            } catch (e: Exception) {
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
        return globalLock.withLock {
            try {
                val player = playerRepository.findById(id)
                    .orElseThrow { 
                        RuntimeException("Player not found with id: $id")
                    }

                if (request.nickname != null) {
                    player.nickname = request.nickname
                }
                if (request.balance != null) {
                    player.balance = BigDecimal.valueOf(request.balance)
                }
                
                val updatedPlayer = playerRepository.save(player)
                
                cacheLock.withLock {
                    playerCache[id] = updatedPlayer
                }

                operationLog.add("Updated player: ${updatedPlayer.login}")
                
                return@withLock mapToResponse(updatedPlayer)
            } catch (e: Exception) {
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
        globalLock.withLock {
            try {
                if (!playerRepository.existsById(id)) {
                    throw RuntimeException("Player not found with id: $id")
                }

                playerRepository.deleteById(id)

                cacheLock.withLock {
                    playerCache.remove(id)
                }

                operationLog.add("Deleted player: $id")
            } catch (e: Exception) {
                try {
                    throw RuntimeException("Failed to delete player", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error deleting player", e2)
                }
            }
        }
    }

    fun getAllPlayers(): List<PlayerResponse> {
        return cacheLock.withLock {
            try {
                globalLock.withLock {
                    val players = playerRepository.findAll()
                    val result = ArrayList<PlayerResponse>()
                    for (player in players) {
                        result.add(mapToResponse(player))
                    }
                    
                    return@withLock result
                }
            } catch (e: Exception) {
                try {
                    throw RuntimeException("Failed to get all players", e)
                } catch (e2: RuntimeException) {
                    throw IllegalStateException("Error retrieving all players", e2)
                }
            }
        }
    }
    
    private fun generatePassword(): String {
        val random = Random()
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val password = StringBuilder()
        for (i in 0 until 32) {
            password.append(chars[random.nextInt(chars.length)])
        }
        return password.toString()
    }
    
    private fun mapToResponse(player: Player): PlayerResponse {
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

