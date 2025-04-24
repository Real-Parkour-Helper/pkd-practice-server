package dev.spaghett.generator

import com.google.gson.Gson
import dev.spaghett.generator.world.WorldBuffer
import dev.spaghett.generator.world.blockIDs
import dev.spaghett.shared.*
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.tag.CompoundTag
import java.io.File
import kotlin.random.Random

class RoomGenerator {

    private val gson = Gson()

    /**
     * Picks a set amount of rooms, optionally using a set seed.
     */
    fun pickRooms(roomCount: Int = 8, seed: Long = 0L): GeneratedSeed {
        if (roomList.size < roomCount) {
            throw IllegalArgumentException("Not enough rooms available to pick $roomCount (found ${roomList.size})")
        }

        val seedToUse = if (seed != 0L) seed else Random.nextLong()

        val rng = Random(seedToUse)
        val selectedRooms = roomList.shuffled(rng).take(roomCount)
        return GeneratedSeed(seedToUse.toString(), roomCount, selectedRooms)
    }

    /**
     * Builds the map from a given generated seed.
     */
    fun buildMap(seed: GeneratedSeed, worldDir: String, resetCheckpoints: Boolean) {
        val rooms = seed.rooms.map { RoomUtil.loadRoom(it) }

        val worldBuffer = WorldBuffer()

        var currentRoomZ = 0
        var currentCheckpoint = 1

        for ((roomMeta, blocks) in rooms) {
            val palette = blocks.palette
            val reversePalette: Map<Int, String> = palette.entries.associate { (k, v) -> v to k }

            for (block in blocks.blocks) {
                val x = block.x
                val y = block.y
                val z = block.z + currentRoomZ

                val blockName = reversePalette[block.id] ?: continue
                val blockId = blockIDs[blockName] ?: continue

                val blockMeta = block.meta.toByte()
                val blockIdByte = blockId.toByte()
                worldBuffer.setBlock(x, y, z, blockIdByte, blockMeta)
            }

            for (checkpoint in roomMeta.checkpoints) {
                val x = checkpoint.x
                val baseY = checkpoint.y
                val z = checkpoint.z + currentRoomZ

                // First line: CHECKPOINT (green + bold)
                val nameTop = "§a§lCHECKPOINT"
                // Second line: #X (yellow + bold)
                val nameBottom = "§e§l#${currentCheckpoint++}"

                // Offset the Y to stack them nicely above the checkpoint block
                worldBuffer.addNametag(x.toDouble(), baseY + 0.5, z.toDouble(), nameTop)
                worldBuffer.addNametag(x.toDouble(), baseY + 0.2, z.toDouble(), nameBottom)
            }

            currentRoomZ += roomMeta.depth

            if (resetCheckpoints) {
                currentCheckpoint = 1
            }
        }

        val regionFolder = File(worldDir, "region")
        regionFolder.mkdirs()
        worldBuffer.writeAllRegions(regionFolder)

        writeLevelDat(worldDir)
    }

    /**
     * Writes the level.dat file.
     */
    private fun writeLevelDat(worldDir: String) {
        val data = CompoundTag().apply {
            putLong("RandomSeed", 123456789L)
            putString("generatorName", "flat")
            putInt("generatorVersion", 0)
            putString("generatorOptions", "")
            putInt("GameType", 1) // 0: Survival, 1: Creative
            putInt("SpawnX", 0)
            putInt("SpawnY", 65)
            putInt("SpawnZ", 0)
            putString("LevelName", "dynamic rooms")
            putInt("version", 19133) // For 1.12.2 style level.dat
            putLong("LastPlayed", System.currentTimeMillis())
            putInt("SizeOnDisk", 0)
            putInt("DayTime", 0)
            putInt("Time", 0)
            putInt("allowCommands", 1)
            putInt("MapFeatures", 1)
            putInt("hardcore", 0)
            putInt("raining", 0)
            putInt("thundering", 0)
        }

        val root = CompoundTag().apply {
            put("Data", data)
        }

        NBTUtil.write(root, File(worldDir, "level.dat"))
    }

}