package dev.spaghett.lobbyplugin.commands

import dev.spaghett.lobbyplugin.LobbyPlugin
import dev.spaghett.shared.GeneratedSeed
import dev.spaghett.shared.roomList
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.IOException

class PlayCustomCommand(val plugin: LobbyPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be run by a player.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /playcustom <room1> <room2> ... <roomN>")
            return true
        }

        for (room in args) {
            if (room !in roomList) {
                sender.sendMessage("§cRoom $room does not exist.")
                return false
            }
        }

        try {
            val seed = GeneratedSeed(
                seed = "custom seed",
                rooms = args.toList(),
                roomCount = args.size
            )
            plugin.buildSeed(seed)
            val started = plugin.startDynamicServer(sender)
            if (!started) {
                sender.sendMessage("§cError starting server.")
                return false
            }
        } catch (e: IOException) {
            sender.sendMessage("§cError generating seed: ${e.message}.")
            sender.sendMessage("§cIs the world generator running?")
            return false
        }
        return true
    }
}