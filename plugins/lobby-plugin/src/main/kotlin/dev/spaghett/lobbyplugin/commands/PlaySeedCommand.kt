package dev.spaghett.lobbyplugin.commands

import dev.spaghett.lobbyplugin.LobbyPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.IOException

class PlaySeedCommand(val plugin: LobbyPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be run by a player.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /playseed <seed>")
            return true
        }

        try {
            val passedSeed = args[0].toLongOrNull() ?: return false
            val seed = plugin.generateSeed(passedSeed, 8)
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