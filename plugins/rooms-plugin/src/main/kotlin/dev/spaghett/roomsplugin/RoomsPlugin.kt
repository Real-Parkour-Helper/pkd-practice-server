package dev.spaghett.roomsplugin

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.WrappedChatComponent
import dev.spaghett.roomsplugin.commands.NextRoomCommand
import dev.spaghett.roomsplugin.commands.PrevRoomCommand
import dev.spaghett.roomsplugin.commands.RoomCommand
import dev.spaghett.shared.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector


class RoomsPlugin : JavaPlugin(), Listener {
    private val startPosition = Triple(18.5, 8.0, 2.5)
    private var currentRoom = mutableMapOf<String, String>()

    private var checkpointTrackers: MutableMap<String, CheckpointTracker> =
        mutableMapOf() // player.name -> CheckpointTracker
    private var boostTrackers = mutableMapOf<String, BoostTracker>() // player.name -> ParkourBoost
    private var parkourInventories = mutableMapOf<String, ParkourInventory>() // player.name -> ParkourInventory

    private var timers = mutableMapOf<String, RunTimer>() // player.name -> RunTimer
    private var runFinished = mutableMapOf<String, Boolean>() // player.name -> finished

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

            for (player in Bukkit.getOnlinePlayers()) {
                val timer = timers[player.name]
                if (timer != null && !runFinished[player.name]!!) {
                    val elapsedTime = timer.elapsedTime()
                    sendActionBar(player, "§b§l$elapsedTime")
                }
            }
        }, 0L, 2L)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("Player ${event.player.name} has joined the game.")
        moveToRoom(event.player, roomList[0], true)

        boostTrackers[event.player.name] = BoostTracker(
            event.player,
            1000L
        ) {
            println("Player ${event.player.name} boosted!")
        }

        parkourInventories[event.player.name] = ParkourInventory(event.player) { item ->
            when (item.type) {
                Material.FEATHER -> {
                    // Boost the player
                    boostTrackers[event.player.name]?.tryBoost()
                }

                Material.GOLD_PLATE -> {
                    // Reset to the last checkpoint
                    checkpointTrackers[event.player.name]?.tpToLastCheckpoint()
                    event.player.sendMessage("§aReset you to your last checkpoint.")
                    if (checkpointTrackers[event.player.name]?.isAtFirstCheckpoint() == true) {
                        timers[event.player.name]?.start()
                        runFinished[event.player.name] = false
                    }
                }

                Material.REDSTONE_BLOCK -> {
                    // Reset to the start
                    checkpointTrackers[event.player.name]?.reset()
                    checkpointTrackers[event.player.name]?.tpToLastCheckpoint()
                    event.player.sendMessage("§aYou have been reset to the start.")
                    if (checkpointTrackers[event.player.name]?.isAtFirstCheckpoint() == true) {
                        timers[event.player.name]?.start()
                        runFinished[event.player.name] = false
                    }
                }

                else -> {
                    // Do nothing
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(parkourInventories[event.player.name], this)
        parkourInventories[event.player.name]?.setupParkourInventory()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        println("Player ${event.player.name} has left the game.")
        // Stop the checkpoint tracker if it exists
        checkpointTrackers[event.player.name]?.stop()
        checkpointTrackers.remove(event.player.name)

        // Stop the boost tracker if it exists
        boostTrackers.remove(event.player.name)

        // Remove the parkour inventory if it exists
        parkourInventories[event.player.name]?.let {
            HandlerList.unregisterAll(it)
            parkourInventories.remove(event.player.name)
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
    fun moveToRoom(player: Player, roomName: String, force: Boolean = false) {
        if (!roomList.contains(roomName)) {
            player.sendMessage("§cRoom §l$roomName§r§c not found.")
            return
        }

        if (currentRoom[player.name] == roomName && !force) {
            player.sendMessage("§cYou are already in this room.")
            return
        }

        // Cancel any existing checkpoint tracker for this player
        checkpointTrackers[player.name]?.stop()
        checkpointTrackers.remove(player.name)

        currentRoom[player.name] = roomName
        val zOffset = roomList.indexOf(roomName) * 57
        val newLocation =
            Location(player.world, startPosition.first, startPosition.second, startPosition.third + zOffset)

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
                val elapsedTime = timers[player.name]?.elapsedTime() ?: "00:00.000"
                player.sendMessage("§e§lCHECKPOINT!§r§a You reached checkpoint §6$checkpoint§a in §6$elapsedTime!")
            },
            { endTime ->
                checkpointTrackers[player.name]?.reset()
                runFinished[player.name] = true

                val time = timers[player.name]?.stop(endTime) ?: "00:00.000"
                player.sendMessage("§e§lCOMPLETED!§r§a You completed the room in §6§l$time§r§6!")
            }
        )

        checkpointTrackers[player.name]?.start()
        runFinished[player.name] = false

        if (timers[player.name] == null) {
            timers[player.name] = RunTimer()
        }

        timers[player.name]?.start()
    }

    private fun sendActionBar(player: Player, message: String) {
        val protocolManager = ProtocolLibrary.getProtocolManager()

        val packet = protocolManager.createPacket(PacketType.Play.Server.CHAT)
        packet.chatComponents.write(0, WrappedChatComponent.fromText(message))
        packet.bytes.write(0, 2.toByte())

        try {
            protocolManager.sendServerPacket(player, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}