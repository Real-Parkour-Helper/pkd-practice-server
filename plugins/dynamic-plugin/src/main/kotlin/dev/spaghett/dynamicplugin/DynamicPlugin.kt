package dev.spaghett.dynamicplugin

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.google.gson.Gson
import dev.spaghett.dynamicplugin.commands.CooldownCommand
import dev.spaghett.dynamicplugin.commands.PingCommand
import dev.spaghett.shared.*
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class DynamicPlugin : JavaPlugin(), Listener {
    private val startPosition = Triple(18.5, 10.0, 4.5)
    private val firstDoor = Triple(18.0, 8.0, 12.0)

    private var checkpointTrackers: MutableMap<String, CheckpointTracker> =
        mutableMapOf() // player.name -> CheckpointTracker
    private var boostTrackers = mutableMapOf<String, BoostTracker>() // player.name -> ParkourBoost
    private var parkourInventories = mutableMapOf<String, ParkourInventory>() // player.name -> ParkourInventory

    private var timers = mutableMapOf<String, RunTimer>() // player.name -> RunTimer
    private var runFinished = mutableMapOf<String, Boolean>() // player.name -> finished

    private var seed: GeneratedSeed? = null
    private var currentRoom = 0
    private var nextRoomAtCheckpoint = 0

    private val doorBlocks = mutableMapOf<Location, Pair<Material, Byte>>()
    private val clearedDoors = mutableListOf<Triple<Int, Int, Int>>()

    override fun onEnable() {
        saveDefaultConfig()

        val seedFile = File("seed.json")

        if (seedFile.exists()) {
            val content = seedFile.readText(Charsets.UTF_8)
            val seed = Gson().fromJson(content, GeneratedSeed::class.java)
            println("Loaded seed: ${seed.seed}")
            this.seed = seed

            val room1 = seed.rooms[0]
            nextRoomAtCheckpoint = checkpointCounts[room1] ?: error("No checkpoint count found for room $room1!")
            println("Next room at checkpoint: $nextRoomAtCheckpoint")
        } else {
            println("No seed file found!")
        }

        Bukkit.getPluginManager().registerEvents(this, this)
        getCommand("cooldown")?.executor = CooldownCommand(this)
        getCommand("ping")?.executor = PingCommand(this)

        Bukkit.getScheduler().runTaskTimer(this, {
            // Permanently nice weather
            val world = Bukkit.getWorld("world")
            world?.setStorm(false)
            world?.isThundering = false
            world?.time = 1000

            for (player in Bukkit.getOnlinePlayers()) {
                val timer = timers[player.name]
                if (timer != null && !runFinished.getOrDefault(player.name, false)) {
                    timer.tick()
                    sendActionBar(player, "§b§l" + timer.getElapsed())
                }
            }
        }, 0L, 2L)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        boostTrackers[event.player.name] = BoostTracker(
            event.player,
            60000L,
            this,
            {
                parkourInventories[event.player.name]?.toggleBoostItem(false)
            },
            {
                parkourInventories[event.player.name]?.toggleBoostItem(true)
            }
        )

        boostTrackers[event.player.name]?.setPing(config.getInt("ping"))

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
                }

                Material.REDSTONE_BLOCK -> {
                    // Reset to the start
                    reset(event.player)
                }

                else -> {
                    // Do nothing
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(parkourInventories[event.player.name], this)
        parkourInventories[event.player.name]?.setupParkourInventory()

        val checkpoints = seed?.rooms?.let { RoomUtil.roomsToCheckpointLocations(event.player, it.map { RoomUtil.loadRoom(it).first }) }
        if (checkpoints == null) {
            event.player.sendMessage("Error making checkpoints!")
            error("Error making checkpoints!")
        }

        val startLocation = Location(event.player.world, startPosition.first, startPosition.second - 3.0, startPosition.third)
        val startRoomDepth = RoomUtil.loadRoom("start_room").first.depth
        val nList = mutableListOf(startLocation)
        checkpoints.forEach {
            val newLoc = it.clone()
            newLoc.add(0.0, 0.0, startRoomDepth.toDouble())
            nList.add(newLoc)
        }
        val finalCheckpoint = RoomUtil.loadRoom("finish_room").first.checkpoints[0]
        val finalLoc = Location(event.player.world, finalCheckpoint.x.toDouble(), finalCheckpoint.y.toDouble(), finalCheckpoint.z.toDouble() + startRoomDepth + ((seed?.rooms?.size ?: 8) * 57))
        nList.add(finalLoc)

        checkpointTrackers[event.player.name] = CheckpointTracker(
            this,
            event.player,
            nList,
            { checkpoint ->
                val elapsedTime = timers[event.player.name]?.getElapsed() ?: "00:00.000"
                event.player.sendMessage("§e§lCHECKPOINT!§r§a You reached checkpoint §6$checkpoint§a in §6$elapsedTime!")

                if (checkpointTrackers[event.player.name]?.currentCP() == nextRoomAtCheckpoint) {
                    currentRoom++
                    dropDoor(event.player, currentRoom)

                    if (currentRoom >= (seed?.rooms?.size ?: 0)) {
                        return@CheckpointTracker
                    }

                    val room = seed?.rooms?.get(currentRoom)
                    if (room != null) {
                        nextRoomAtCheckpoint += checkpointCounts[room] ?: error("No checkpoint count found for room $room!")
                        println("Next room at checkpoint: $nextRoomAtCheckpoint")
                    } else {
                        event.player.sendMessage("§cSomething went wrong! There are no more rooms!?")
                        error("Something went wrong! There are no more rooms!?")
                    }
                }
            },
            {
                checkpointTrackers[event.player.name]?.reset()
                runFinished[event.player.name] = true

                val time = timers[event.player.name]?.stop() ?: "00:00.000"
                event.player.sendMessage("§e§lCOMPLETED!§r§a You completed the parkour in §6§l$time§r§6!")
            }, {
                // nothing on void
            }
        )

        checkpointTrackers[event.player.name]?.start()
        runFinished[event.player.name] = false

        if (timers[event.player.name] == null) {
            timers[event.player.name] = RunTimer()
        }

        val textComponent = TextComponent("§aYou are playing on the seed ${seed?.seed}! Click here to copy it!")
        textComponent.clickEvent = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, seed?.seed.toString())
        event.player.sendMessage(textComponent)

        event.player.level = 0
        event.player.exp = 0f

        event.player.teleport(Location(event.player.world, startPosition.first, startPosition.second, startPosition.third))
        timers[event.player.name]?.start()

        dropDoor(event.player, 0)
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

        Bukkit.shutdown()
    }

    @EventHandler
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.entity is FallingBlock && event.entity.customName == "doorblock") {
            event.isCancelled = true // Prevent falling blocks from becoming actual blocks
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity is Player && event.cause == EntityDamageEvent.DamageCause.FALL) {
            event.isCancelled = true
        }
    }

    fun setBoostCooldown(player: Player, time: Long) {
        boostTrackers[player.name]?.setCooldown(time)
    }

    fun setPing(player: Player, ping: Int) {
        boostTrackers[player.name]?.setPing(ping)
        config.set("ping", ping)
        saveConfig()
    }

    private fun dropDoor(player: Player, room: Int) {
        // Center of first layer of door on the bottom
        val doorPos = Location(player.world, firstDoor.first, firstDoor.second, firstDoor.third + (room * 57))

        // Capture door block information
        for (x in -2..2) {
            for (y in 0..4) {
                for (z in 0..1) {
                    val block = doorPos.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    doorBlocks[block] = Pair(block.block.type, block.block.data)
                }
            }
        }

        // Clear space below door
        for (x in -2..2) {
            for (y in -1 downTo -6) {
                for (z in 0..1) {
                    val block = doorPos.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    block.block.type = Material.AIR
                }
            }
        }

        // Place log blocks at y=-1 immediately
        for (x in -2..2) {
            for (z in 0..1) {
                val logLoc = doorPos.clone().add(x.toDouble(), -1.0, z.toDouble())
                val block = logLoc.block
                block.type = Material.LOG
                block.data = 4.toByte()
            }
        }

        // Now set door blocks to air and spawn falling blocks
        for (x in -2..2) {
            for (y in 0..4) {
                for (z in 0..1) {
                    val block = doorPos.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    block.block.type = Material.AIR
                }
            }
        }

        // Create falling blocks with "no collision" property
        for ((location, blockData) in doorBlocks) {
            val fallingBlock = location.world.spawnFallingBlock(location, blockData.first, blockData.second)
            fallingBlock.dropItem = false
            fallingBlock.setHurtEntities(false)

            // Make falling blocks pass through other blocks
            fallingBlock.customName = "doorblock"
            fallingBlock.isCustomNameVisible = false
        }

        clearedDoors.add(Triple(firstDoor.first.toInt(), firstDoor.second.toInt(), firstDoor.third.toInt() + (room * 57)))
    }

    private fun reset(player: Player) {
        checkpointTrackers[player.name]?.reset()
        player.teleport(Location(player.world, startPosition.first, startPosition.second, startPosition.third))
        player.sendMessage("§aYou have been reset to the start.")
        timers[player.name]?.start()
        runFinished[player.name] = false

        boostTrackers[player.name]?.reset()
        player.level = 0
        player.exp = 0f

        resetDoors(player)

        val room1 = seed?.rooms?.get(0)
        nextRoomAtCheckpoint = checkpointCounts[room1] ?: error("No checkpoint count found for room $room1!")
        println("Next room at checkpoint: $nextRoomAtCheckpoint")
        currentRoom = 0


        dropDoor(player, 0)
    }

    private fun resetDoors(player: Player) {
        for (door in clearedDoors) {
            val doorPos = Location(player.world, door.first.toDouble(), door.second.toDouble(), door.third.toDouble())
            for (x in -2..2) {
                for (y in 0..4) {
                    for (z in 0..1) {
                        val block = doorPos.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                        block.block.type = doorBlocks[block]?.first ?: Material.AIR
                        block.block.data = doorBlocks[block]?.second ?: 0.toByte()
                    }
                }
            }
        }
        clearedDoors.clear()
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