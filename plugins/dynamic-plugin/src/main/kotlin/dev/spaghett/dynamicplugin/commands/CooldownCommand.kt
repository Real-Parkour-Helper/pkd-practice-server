package dev.spaghett.dynamicplugin.commands

import dev.spaghett.dynamicplugin.DynamicPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CooldownCommand(private val plugin: DynamicPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be run by a player.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /cooldown <seconds>")
            return true
        }

        val seconds: Long
        try {
            seconds = args[0].toLong()
        } catch (e: NumberFormatException) {
            sender.sendMessage("§cInvalid number format.")
            return true
        }

        if (seconds <= 0) {
            sender.sendMessage("§cCooldown must be greater than 0.")
            return true
        }

        plugin.setBoostCooldown(sender, seconds * 1000)
        return true
    }
}