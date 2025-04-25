package dev.spaghett.shared

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class CheckpointTracker(
    private val plugin: JavaPlugin,
    private val player: Player,
    private val checkpoints: List<Location>,
    private val onCheckpoint: (Int) -> Unit,
    private val onFinish: () -> Unit,
    private val onVoid: () -> Unit
) {

    private var currentCheckpointIndex = 0
    private var taskId: Int? = null

    fun start() {
        reset()
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::tick, 2L, 2L)
        println("Checkpoint tracking started for ${player.name} with ${checkpoints.size} checkpoints.")
    }

    fun stop() {
        taskId?.let { Bukkit.getScheduler().cancelTask(it) }
    }

    fun reset() {
        currentCheckpointIndex = 0
    }

    fun isAtFirstCheckpoint(): Boolean {
        return currentCheckpointIndex == 0
    }

    fun tpToLastCheckpoint() {
        var location = checkpoints[currentCheckpointIndex]
        if (currentCheckpointIndex != 0) {
           location = Location(player.world, location.x + 0.5, location.y, location.z + 0.5)
        }
        teleportToCheckpoint(location)
        player.playSound(player.location, Sound.ENDERMAN_TELEPORT, 0.6f, 2.0f)
    }

    private fun tick() {
        val current = player.location
        val nextCheckpoint = checkpoints.getOrNull(currentCheckpointIndex + 1) ?: return

        if (current.distance(nextCheckpoint) < 2.5) {
            currentCheckpointIndex++
            player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)

            if (currentCheckpointIndex >= checkpoints.size - 1) {
                onFinish()
            } else {
                onCheckpoint(currentCheckpointIndex)
            }
        }

        if (current.y < 0) {
            tpToLastCheckpoint()
            onVoid()
        }
    }

    private fun teleportToCheckpoint(checkpoint: Location) {
        player.teleport(checkpoint)
    }

    fun currentCP(): Int {
        return currentCheckpointIndex
    }
}