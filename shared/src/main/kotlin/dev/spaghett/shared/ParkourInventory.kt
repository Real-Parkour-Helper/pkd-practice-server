package dev.spaghett.shared

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class ParkourInventory(
    private val player: Player,
    private val plugin: JavaPlugin,
    private val onInteract: (ItemStack) -> Unit
) : Listener {
    private var lastInteract: Long = 0L

    fun setupParkourInventory() {
        val inv = player.inventory
        inv.clear()

        fun customItem(type: Material, name: String, vararg lore: String): ItemStack {
            return ItemStack(type).apply {
                itemMeta = itemMeta?.apply {
                    displayName = name
                    if (lore.isNotEmpty()) {
                        setLore(lore.toList())
                    }
                }
            }
        }

        val boostItem = customItem(Material.FEATHER, "§b§lBoost")
        val checkpointItem = customItem(Material.GOLD_PLATE, "§e§lLast Checkpoint", "§7Reset to the last checkpoint.")
        val resetItem = customItem(Material.REDSTONE_BLOCK, "§c§lReset", "§7Reset to the start.")

        inv.setItem(0, boostItem)
        inv.setItem(4, checkpointItem)
        inv.setItem(6, resetItem)

        player.updateInventory() // Force the client to update
        registerPacketListeners(plugin)
    }

    fun toggleBoostItem(ready: Boolean) {
        val inv = player.inventory
        val currentItem = inv.getItem(0)

        if (ready) {
            if (currentItem == null || currentItem.type != Material.FEATHER) {
                val customItem = ItemStack(Material.FEATHER).apply {
                    itemMeta = itemMeta?.apply {
                        displayName = "§b§lBoost"
                    }
                }
                inv.setItem(0, customItem)
            }
        } else {
            if (currentItem == null || currentItem.type != Material.GHAST_TEAR) {
                val customItem = ItemStack(Material.GHAST_TEAR).apply {
                    itemMeta = itemMeta?.apply {
                        displayName = "§c§lBoost (Cooldown)"
                    }
                }
                inv.setItem(0, customItem)
            }
        }
        player.updateInventory() // Force the client to update
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        if (isParkourItem(item)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem
        if (item != null && isParkourItem(item)) {
            event.isCancelled = true
        }
    }

    private fun registerPacketListeners(plugin: JavaPlugin) {
        val manager = ProtocolLibrary.getProtocolManager()

        // Listen to both left and right-click related packets
        manager.addPacketListener(object : PacketAdapter(plugin, ListenerPriority.HIGHEST,
            PacketType.Play.Client.BLOCK_PLACE,
            PacketType.Play.Client.USE_ITEM,
            PacketType.Play.Client.ARM_ANIMATION
        ) {
            override fun onPacketReceiving(event: PacketEvent) {
                val player = event.player
                val packet = event.packet

                val item = player.inventory.itemInHand ?: return

                if (!isParkourItem(item)) return

                val now = System.currentTimeMillis()

                // Debounce to avoid firing both swing and use within a tick
                if (now - lastInteract < 100) return
                lastInteract = now

                when (event.packetType) {
                    PacketType.Play.Client.ARM_ANIMATION -> {
                        // Left click swing
                        onInteract(item)
                    }

                    PacketType.Play.Client.BLOCK_PLACE,
                    PacketType.Play.Client.USE_ITEM -> {
                        // Right click with item or placing block
                        onInteract(item)
                        println("Right click with item: ${item.type} and packet type: ${event.packetType}")
                        event.isCancelled = true // optional: block the default action
                    }
                }
            }
        })
    }

    private fun isParkourItem(item: ItemStack?): Boolean {
        if (item == null || !item.hasItemMeta()) return false
        val name = item.itemMeta?.displayName ?: return false
        return name.contains("Boost") || name.contains("Checkpoint") || name.contains("Reset")
    }
}