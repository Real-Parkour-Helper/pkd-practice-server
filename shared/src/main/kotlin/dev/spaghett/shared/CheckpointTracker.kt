package dev.spaghett.shared

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class CheckpointTracker(
    private val plugin: JavaPlugin,
    private val player: Player,
    private val checkpoints: List<Location>,
    private val onCheckpoint: (Int) -> Unit,
    private val onFinish: () -> Unit
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

    fun tpToLastCheckpoint() {
        teleportToCheckpoint(checkpoints[currentCheckpointIndex])
    }

    private fun tick() {
        val current = player.location
        val nextCheckpoint = checkpoints.getOrNull(currentCheckpointIndex + 1) ?: return

        if (current.distance(nextCheckpoint) < 2.0) {
            currentCheckpointIndex++
            onCheckpoint(currentCheckpointIndex)

            if (currentCheckpointIndex >= checkpoints.size - 1) {
                onFinish()
            }
        }

        if (current.y < 0) {
            tpToLastCheckpoint()
        }
    }

    private fun teleportToCheckpoint(checkpoint: Location) {
        player.teleport(checkpoint)
    }

}