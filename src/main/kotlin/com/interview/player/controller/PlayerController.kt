package com.interview.player.controller

import com.interview.player.dto.CreatePlayerRequest
import com.interview.player.dto.PlayerResponse
import com.interview.player.dto.UpdatePlayerRequest
import com.interview.player.service.PlayerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/players")
@Tag(name = "Player API", description = "API for managing player profiles")
class PlayerController(
    private val playerService: PlayerService
) {
    
    @PostMapping
    @Operation(summary = "Create a new player profile")
    fun createPlayer(@RequestBody request: CreatePlayerRequest): ResponseEntity<PlayerResponse> {
        val player = playerService.createPlayer(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(player)
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get player profile by ID")
    fun getPlayer(@PathVariable id: Long): ResponseEntity<PlayerResponse> {
        val player = playerService.getPlayer(id)
        return ResponseEntity.ok(player)
    }
    
    @GetMapping
    @Operation(summary = "Get all players")
    fun getAllPlayers(): ResponseEntity<List<PlayerResponse>> {
        val players = playerService.getAllPlayers()
        return ResponseEntity.ok(players)
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update player profile")
    fun updatePlayer(
        @PathVariable id: Long,
        @RequestBody request: UpdatePlayerRequest
    ): ResponseEntity<PlayerResponse> {
        val player = playerService.updatePlayer(id, request)
        return ResponseEntity.ok(player)
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete player profile")
    fun deletePlayer(@PathVariable id: Long): ResponseEntity<Void> {
        playerService.deletePlayer(id)
        return ResponseEntity.noContent().build()
    }
    
    // ПЛОХО: Эндпоинт для поиска по логину без защиты
    @GetMapping("/login/{login}")
    @Operation(summary = "Get player by login")
    fun getPlayerByLogin(@PathVariable login: String): ResponseEntity<PlayerResponse> {
        // TODO: Реализовать поиск по логину
        return ResponseEntity.notFound().build()
    }
}

