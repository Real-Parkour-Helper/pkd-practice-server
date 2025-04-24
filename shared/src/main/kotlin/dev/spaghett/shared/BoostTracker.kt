package dev.spaghett.shared

import org.bukkit.entity.Player

class BoostTracker(
    private val player: Player,
    private val boostCooldown: Long,
    private val onBoost: () -> Unit
) {
    private var lastBoostTime: Long = 0L

    fun tryBoost(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoostTime >= boostCooldown) {
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