package dev.spaghett.shared

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class ParkourRun(
    private val plugin: JavaPlugin,
    private val player: Player,
    private val checkpoints: List<Location>,
    private val boostCooldown: Long,
    private val onCheckpoint: (Int) -> Unit,
    private val onFinish: () -> Unit,
) {

    private var currentCheckpointIndex = 0
    private var startTime: Long = System.currentTimeMillis()
    private var lastBoostTime: Long = 0L
    private var taskId: Int? = null

    fun start() {
        reset()
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::tick, 2L, 2L)
        println("Parkour run started for ${player.name} with ${checkpoints.size} checkpoints.")
    }

    fun stop() {
        taskId?.let { Bukkit.getScheduler().cancelTask(it) }
    }

    fun reset() {
        currentCheckpointIndex = 0
        startTime = System.currentTimeMillis()
        lastBoostTime = 0L

        teleportToCheckpoint(checkpoints[0])
    }

    fun tryBoost(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoostTime >= boostCooldown) {
            lastBoostTime = currentTime

            val direction = player.location.direction.normalize() // Unit vector of where they're facing
            val boost = direction.multiply(1.5).setY(0.6) // 1.5 blocks/sec horizontal, 0.6 upward
            player.velocity = boost
            return true
        }
        return false
    }

    fun teleportToLastCheckpoint() {
        teleportToCheckpoint(checkpoints[currentCheckpointIndex])
    }

    private fun tick() {
        val current = player.location
        val nextCheckpoint = checkpoints.getOrNull(currentCheckpointIndex + 1) ?: return

        if (current.distance(nextCheckpoint) < 1.41) {
            currentCheckpointIndex++
            onCheckpoint(currentCheckpointIndex)

            if (currentCheckpointIndex >= checkpoints.size - 1) {
                stop()
                onFinish()
            }
        }

        if (current.y < 0) {
            println("Player fell below Y=0, resetting to checkpoint $currentCheckpointIndex.")
            if (currentCheckpointIndex == 0) {
                reset()
            } else {
                teleportToLastCheckpoint()
            }
        }
    }

    private fun teleportToCheckpoint(checkpoint: Location) {
        val chunk = checkpoint.chunk
        if (!chunk.isLoaded) {
            chunk.load(true)
        }

        Bukkit.getScheduler().runTask(plugin) {
            player.teleport(checkpoint)
            player.fallDistance = 0f
            player.velocity = Vector(0, 0, 0)
        }
    }

}