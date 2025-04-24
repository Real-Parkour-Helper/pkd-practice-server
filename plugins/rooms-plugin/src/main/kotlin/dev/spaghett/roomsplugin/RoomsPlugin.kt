package dev.spaghett.roomsplugin

import dev.spaghett.roomsplugin.commands.NextRoomCommand
import dev.spaghett.roomsplugin.commands.PrevRoomCommand
import dev.spaghett.roomsplugin.commands.RoomCommand
import dev.spaghett.shared.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.Vector

class RoomsPlugin : JavaPlugin(), Listener {
    private val startPosition = Triple(18.5, 8.0, 2.5)
    private var currentRoom = mutableMapOf<String, String>()

    private var checkpointTrackers: MutableMap<String, CheckpointTracker> = mutableMapOf() // player.name -> CheckpointTracker
    private var boostTrackers = mutableMapOf<String, BoostTracker>() // player.name -> ParkourBoost

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        // Register commands
        getCommand("room")?.executor = RoomCommand(this)
        getCommand("nextroom")?.executor = NextRoomCommand(this)
        getCommand("prevroom")?.executor = PrevRoomCommand(this)

        Bukkit.getScheduler().runTaskTimer(this, {
            // Permanently nice weather
            val world = Bukkit.getWorld("world")
            world?.setStorm(false)
            world?.isThundering = false
            world?.time = 1000
        }, 0L, 5L)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        moveToRoom(event.player, roomList[0])

        boostTrackers[event.player.name] = BoostTracker(
            event.player,
            1000L
        ) {
            println("Player ${event.player.name} boosted!")
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity is Player && event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    /**
     * Move this player to the next room.
     */
    fun nextRoom(player: Player) {
        val currentRoom = currentRoom[player.name]
        val currentIndex = roomList.indexOf(currentRoom)
        val nextIndex = (currentIndex + 1) % roomList.size
        val nextRoom = roomList[nextIndex]
        moveToRoom(player, nextRoom)
    }

    /**
     * Move this player to the previous room.
     */
    fun previousRoom(player: Player) {
        val currentRoom = currentRoom[player.name]
        val currentIndex = roomList.indexOf(currentRoom)
        val previousIndex = (currentIndex - 1 + roomList.size) % roomList.size
        val previousRoom = roomList[previousIndex]
        moveToRoom(player, previousRoom)
    }

    /**
     * Teleport the player to the specified room.
     */
    fun moveToRoom(player: Player, roomName: String) {
        if (!roomList.contains(roomName)) {
            player.sendMessage("§cRoom §l$roomName§r§c not found.")
            return
        }

        if (currentRoom[player.name] == roomName) {
            player.sendMessage("§cYou are already in this room.")
            return
        }

        // Cancel any existing checkpoint tracker for this player
        checkpointTrackers[player.name]?.stop()
        checkpointTrackers.remove(player.name)

        currentRoom[player.name] = roomName
        val zOffset = roomList.indexOf(roomName) * 57
        val newLocation = Location(player.world, startPosition.first, startPosition.second, startPosition.third + zOffset)

        val chunk = newLocation.chunk
        if (!chunk.isLoaded) {
            chunk.load(true) // or asynchronously if needed
        }

        Bukkit.getScheduler().runTask(this) {
            player.teleport(newLocation)
            player.fallDistance = 0f
            player.velocity = Vector(0, 0, 0)
        }

        val formattedName = roomName.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        player.sendMessage("§aYou have been teleported to §l$formattedName§r§a.")

        val checkpoints = RoomUtil.roomsToCheckpointLocations(player, listOf(RoomUtil.loadRoom(roomName).first))
        checkpoints.forEach {
            it.add(Location(player.world, 0.0, 0.0, zOffset.toDouble()))
        }

        val nList = mutableListOf(newLocation)
        nList.addAll(checkpoints)

        checkpointTrackers[player.name] = CheckpointTracker(
            this,
            player,
            nList,
            { checkpoint ->
                println("Player ${player.name} reached checkpoint $checkpoint")
            },
            { ->
                println("Player ${player.name} finished the parkour run!")
                checkpointTrackers[player.name]?.reset()
            }
        )

        checkpointTrackers[player.name]?.start()
    }
}