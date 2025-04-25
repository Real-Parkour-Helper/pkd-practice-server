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
    private val onFinish: (Long) -> Unit
) {

    private var currentCheckpointIndex = 0
    private var taskId: Int? = null
    private var tickCounter = 0
    private var enteredCheckpointAt = 0L

    fun start() {
        reset()
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::tick, 1L, 1L)
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
            if (enteredCheckpointAt == 0L) {
                enteredCheckpointAt = System.currentTimeMillis()
            }

            if (tickCounter % 2 == 0) {
                currentCheckpointIndex++
                player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)

                if (currentCheckpointIndex >= checkpoints.size - 1) {
                    onFinish(enteredCheckpointAt)
                } else {
                    onCheckpoint(currentCheckpointIndex)
                }
            }
        } else {
            enteredCheckpointAt = 0L
        }

        if (current.y < 0) {
            tpToLastCheckpoint()
        }

        tickCounter++
        if (tickCounter % 20 == 0) {
            tickCounter = 0
        }
    }

    private fun teleportToCheckpoint(checkpoint: Location) {
        player.teleport(checkpoint)
    }

}