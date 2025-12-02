package com.interview.player.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "device_info")
data class DeviceInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var ipAddress: String = "",
    
    @Column(nullable = false)
    var lastLoginDate: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var timeInGame: Long = 0L,
    
    @Column(nullable = false)
    var sessionCount: Int = 0,
    
    @Column(nullable = false)
    var registrationDate: LocalDateTime = LocalDateTime.now(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    var player: Player? = null
)

