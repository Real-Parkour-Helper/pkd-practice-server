package dev.spaghett.scanner

import com.google.gson.Gson
import dev.spaghett.shared.BlockEntry
import dev.spaghett.shared.BlockStructure
import dev.spaghett.shared.RoomMeta
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import java.io.File
import kotlin.math.abs

class ExportRoom : Command("export") {

    private val mc = Minecraft.getMinecraft()
    private val exportDir = File("./pkd-rooms/")
    private val gson = Gson()

    init {
        register()
    }

    @DefaultHandler
    fun handle(roomName: String) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            println("Player or world is null")
            return
        }

        val pos1 = Scanner.pos1
        val pos2 = Scanner.pos2

        if (pos1 == null || pos2 == null) {
            mc.thePlayer.addChatMessage(ChatComponentText("§cPlease set both positions first."))
            return
        }

        val roomMeta = RoomMeta(
            name = roomName,
            width = abs(pos1.x - pos2.x) + 1,
            height = abs(pos1.y - pos2.y) + 1,
            depth = abs(pos1.z - pos2.z) + 1
        )

        val blockStructure = BlockStructure(
            palette = mutableMapOf(),
            blocks = mutableListOf()
        )
        var paletteId = 0

        val minX = minOf(pos1.x, pos2.x)
        val maxX = maxOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y)
        val maxY = maxOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z)
        val maxZ = maxOf(pos1.z, pos2.z)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val pos = BlockPos(x, y, z)
                    val state = mc.theWorld.getBlockState(pos)
                    val block = state.block
                    val meta = block.getMetaFromState(state)

                    if (block == Blocks.air) continue // don't bloat the json with this

                    val blockName = block.registryName.toString()
                    val id = blockStructure.palette.getOrPut(blockName) {
                        paletteId++
                    }
                    blockStructure.blocks.add(BlockEntry(x - minX, y - minY, z - minZ, id, meta))
                }
            }
        }

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val roomDir = File(exportDir, roomName)
        if (!roomDir.exists()) {
            roomDir.mkdirs()
        }

        val metaFile = File(roomDir, "meta.json")
        val blockFile = File(roomDir, "blocks.json")

        try {
            metaFile.writeText(gson.toJson(roomMeta))
            blockFile.writeText(gson.toJson(blockStructure))
            mc.thePlayer.addChatMessage(ChatComponentText("§aRoom $roomName exported successfully."))
        } catch (e: Exception) {
            mc.thePlayer.addChatMessage(ChatComponentText("§cFailed to export room data: ${e.message}"))
        } finally {
            Scanner.pos1 = null
            Scanner.pos2 = null
            mc.thePlayer.addChatMessage(ChatComponentText("§aResetting positions."))
        }
    }

}