package dev.spaghett.shared

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class ParkourInventory(
    private val player: Player,
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

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return
        val action = event.action

        // We only care about left/right click actions
        if (action != Action.RIGHT_CLICK_AIR &&
            action != Action.RIGHT_CLICK_BLOCK &&
            action != Action.LEFT_CLICK_AIR &&
            action != Action.LEFT_CLICK_BLOCK
        ) return

        lastInteract = System.currentTimeMillis()

        if (isParkourItem(item)) {
            event.isCancelled = true
            onInteract(item)
        }
    }

    @EventHandler
    fun onSwing(event: PlayerAnimationEvent) {
        val player = event.player
        val item = player.itemInHand ?: return

        if (!isParkourItem(item)) return

        if (System.currentTimeMillis() - lastInteract < 100) {
            return
        }

        onInteract(item)
    }

    private fun isParkourItem(item: ItemStack?): Boolean {
        if (item == null || !item.hasItemMeta()) return false
        val name = item.itemMeta?.displayName ?: return false
        return name.contains("Boost") || name.contains("Checkpoint") || name.contains("Reset")
    }
}