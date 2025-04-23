package dev.spaghett.scanner

import net.minecraft.init.Items
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Mod(
    modid = Scanner.MOD_ID,
    name = Scanner.MOD_NAME,
    version = Scanner.VERSION
)
class Scanner {

    companion object {
        const val MOD_ID = "pkd-room-scanner"
        const val MOD_NAME = "Pkd Room Scanner"
        const val VERSION = "1.0"

        var pos1: BlockPos? = null
        var pos2: BlockPos? = null
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        // Initialization logic here
        println("Pkd Room Scanner mod initialized.")

        MinecraftForge.EVENT_BUS.register(this)

        ExportRoom()
        ClearSnow()
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.entityPlayer
        val heldItem = player.heldItem ?: return

        if (heldItem.item != Items.iron_axe) return

        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            pos1 = event.pos
            player.addChatMessage(ChatComponentText("§aSet second position to $pos1"))
            event.setCanceled(true)
        }

        if (event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            pos2 = event.pos
            player.addChatMessage(ChatComponentText("§aSet first position to $pos2"))
            event.setCanceled(true)
        }
    }

}