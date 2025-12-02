package com.interview.player.dto

data class CreatePlayerRequest(
    val login: String,
    val nickname: String,
    val initialBalance: Double = 0.0
)

