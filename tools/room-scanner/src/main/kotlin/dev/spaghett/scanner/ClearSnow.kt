package dev.spaghett.scanner

import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText

/**
 * Because I messed up and the command blocks to prevent the weather going bad weren't there anymore
 * and then suddenly it snowed everywhere ._.
 */
class ClearSnow : Command("clearsnow") {

    private val mc = Minecraft.getMinecraft()

    init {
        register()
    }

    @DefaultHandler
    fun handle() {
        val world = mc.theWorld
        val player = mc.thePlayer ?: return

        val radius = 64 // adjust this if you want a larger area
        val center = player.position

        var removed = 0

        for (x in (center.x - radius)..(center.x + radius)) {
            for (y in 0..255) {
                for (z in (center.z - radius)..(center.z + radius)) {
                    val pos = BlockPos(x, y, z)
                    val block = world.getBlockState(pos).block

                    if (block == Blocks.snow_layer) {
                        world.setBlockToAir(pos)
                        removed++
                    }
                }
            }
        }

        player.addChatMessage(ChatComponentText("§aRemoved $removed snow layer blocks."))
    }
}
