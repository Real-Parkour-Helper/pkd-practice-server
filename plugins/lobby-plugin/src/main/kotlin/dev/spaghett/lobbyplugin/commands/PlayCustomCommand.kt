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

        plugin.startListeningForRooms(sender)
        return true
    }
}