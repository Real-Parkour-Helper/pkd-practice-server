package dev.spaghett.sharedplugin.commands

import dev.spaghett.sharedplugin.SharedPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class LobbyCommand(val plugin: SharedPlugin): CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        data.writeUTF("Connect")
        data.writeUTF("lobby")

        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be run by a player.")
            return true
        }

        sender.sendMessage("§aConnecting to lobby...")
        sender.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        return true
    }
}