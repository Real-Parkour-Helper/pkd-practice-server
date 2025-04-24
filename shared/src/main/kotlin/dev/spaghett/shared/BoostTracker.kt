package dev.spaghett.shared

import org.bukkit.entity.Player

class BoostTracker(
    private val player: Player,
    private val boostCooldown: Long,
    private val onBoost: () -> Unit
) {
    private var lastBoostTime: Long = 0L
    private val boostVelocity = 1.5

    fun tryBoost(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoostTime >= boostCooldown) {
            val direction = player.location.direction.normalize() // Unit vector of where they're facing
            val boost = direction.multiply(boostVelocity)
            player.velocity = boost

            lastBoostTime = currentTime
            onBoost()
            return true
        }
        return false
    }

    fun reset() {
        lastBoostTime = 0L
    }
}