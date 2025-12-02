package com.interview.player.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "players")
data class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true)
    var login: String = "",
    
    @Column(nullable = false)
    var password: String = "",
    
    @Column(nullable = false)
    var nickname: String = "",
    
    @Column(nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO,
    
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "player", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    var devices: MutableList<DeviceInfo> = mutableListOf()
)

