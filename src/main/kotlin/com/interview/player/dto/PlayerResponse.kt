package com.interview.player.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class DeviceInfoResponse(
    val id: Long,
    val ipAddress: String,
    val lastLoginDate: LocalDateTime,
    val timeInGame: Long,
    val sessionCount: Int,
    val registrationDate: LocalDateTime
)

data class PlayerResponse(
    val id: Long,
    val login: String,
    val password: String,
    val nickname: String,
    val balance: BigDecimal,
    val createdAt: LocalDateTime,
    val devices: List<DeviceInfoResponse>
)

