package dev.spaghett.roomsplugin.commands

import dev.spaghett.roomsplugin.RoomsPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PrevRoomCommand(val plugin: RoomsPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            plugin.previousRoom(sender)
        }
        return true
    }
}