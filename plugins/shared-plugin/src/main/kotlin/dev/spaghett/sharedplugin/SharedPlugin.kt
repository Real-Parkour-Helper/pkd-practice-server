package dev.spaghett.sharedplugin

import dev.spaghett.sharedplugin.commands.LobbyCommand
import dev.spaghett.sharedplugin.commands.RoomsCommand
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin

class SharedPlugin : JavaPlugin(), Listener {
    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        // Register commands
        getCommand("lobby")?.executor = LobbyCommand(this)
        getCommand("rooms")?.executor = RoomsCommand(this)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage = null // Suppress the default join message
    }

    @EventHandler
    fun onBlockSpread(event: BlockSpreadEvent) {
        val source = event.source
        val block = event.block

        if (source.type != Material.VINE) return

        event.isCancelled = true
    }
}