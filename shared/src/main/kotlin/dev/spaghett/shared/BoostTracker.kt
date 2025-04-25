package dev.spaghett.shared

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BoostTracker(
    private val player: Player,
    private val boostCooldown: Long,
    private val onBoost: () -> Unit
) {
    private var lastBoostTime: Long = 0L

    fun tryBoost(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoostTime >= boostCooldown) {
            boostPlayer(player)
            lastBoostTime = currentTime
            onBoost()
            return true
        }
        return false
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

    fun reset() {
        lastBoostTime = 0L
    }
}