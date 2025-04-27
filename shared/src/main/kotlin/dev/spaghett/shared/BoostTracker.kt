package dev.spaghett.shared

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BoostTracker(
    private val player: Player,
    private var boostCooldown: Long,
    private val plugin: JavaPlugin,
    private val onBoost: () -> Unit,
    private val onCooldownEnd: () -> Unit
) {
    private var lastBoostTime: Long = 0L
    private var cooldownTask: BukkitRunnable? = null

    fun tryBoost(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoostTime >= boostCooldown) {
            boostPlayer(player)
            lastBoostTime = currentTime
            onBoost()
            startCooldownUpdater()
            return true
        }
        return false
    }

    fun setCooldown(cooldown: Long) {
        boostCooldown = cooldown
    }


    /**
     * Sends a precise velocity packet to a player using ProtocolLib
     */
    private fun boostPlayer(player: Player, magnitude: Int = 12000) {
        val yaw = player.location.yaw.toDouble()
        val pitch = player.location.pitch.toDouble()
        val yawRad = Math.toRadians(yaw)
        val pitchRad = Math.toRadians(pitch)

        var x = -cos(pitchRad) * sin(yawRad)
        var y = -sin(pitchRad)
        var z = cos(pitchRad) * cos(yawRad)

        // Normalize the vector to ensure the magnitude is exactly what we want
        val length = sqrt(x*x + y*y + z*z)
        x /= length
        y /= length
        z /= length

        // Scale to desired magnitude
        val velocityX = (x * magnitude).toInt()
        val velocityY = (y * magnitude).toInt()
        val velocityZ = (z * magnitude).toInt()

        val packet = PacketContainer(PacketType.Play.Server.ENTITY_VELOCITY)
        packet.integers
            .write(0, player.entityId)  // Entity ID
            .write(1, velocityX)        // X velocity
            .write(2, velocityY)        // Y velocity
            .write(3, velocityZ)        // Z velocity

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCooldownUpdater() {
        cooldownTask?.cancel()

        cooldownTask = object : BukkitRunnable() {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val timePassed = currentTime - lastBoostTime
                val timeLeftMillis = boostCooldown - timePassed

                if (timeLeftMillis <= 0) {
                    // Cooldown over
                    player.exp = 0f
                    player.level = 0
                    this.cancel()
                    onCooldownEnd()
                    return
                }

                // Update XP Bar
                val timeLeftSeconds = ceil(timeLeftMillis / 1000.0).toInt()
                val progress = (timeLeftMillis.toDouble() / boostCooldown.toDouble()).coerceIn(0.0, 1.0)

                player.level = timeLeftSeconds
                player.exp = progress.toFloat()
            }
        }

        cooldownTask!!.runTaskTimer(plugin, 0L, 2L) // update every 2 ticks (~0.1s)
    }

    fun reset() {
        lastBoostTime = 0L
    }
}