package dev.spaghett.dynamicplugin.commands

import dev.spaghett.dynamicplugin.DynamicPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class PingCommand(private val plugin: DynamicPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return false
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /ping <ping>")
            return false
        }

        val ping = args[0].toIntOrNull()
        if (ping == null) {
            sender.sendMessage("Please provide a valid ping value.")
            return false
        }

        plugin.setPing(sender, ping)
        return true
    }
}