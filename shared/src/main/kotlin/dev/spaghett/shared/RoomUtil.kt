package dev.spaghett.shared

import com.google.gson.Gson
import org.bukkit.Location
import org.bukkit.entity.Player
import java.io.InputStreamReader

object RoomUtil {
    private val gson = Gson()

    /**
     * Loads a room from the :shared resources folder.
     */
    fun loadRoom(roomName: String): Pair<RoomMeta, BlockStructure> {
        val loader = javaClass.classLoader
        val stream = loader.getResourceAsStream("pkd-rooms/${roomName}/meta.json")

        val metaReader = InputStreamReader(stream!!, Charsets.UTF_8)
        val meta = gson.fromJson(metaReader, RoomMeta::class.java)
        metaReader.close()

        val blocksReader =
            InputStreamReader(loader.getResourceAsStream("pkd-rooms/${roomName}/blocks.json")!!, Charsets.UTF_8)
        val blocks = gson.fromJson(blocksReader, BlockStructure::class.java)
        blocksReader.close()

        return Pair(meta, blocks)
    }

    fun roomsToCheckpointLocations(player: Player, rooms: List<RoomMeta>): MutableList<Location> {
        val list = mutableListOf<Location>()
        rooms.forEachIndexed { index, roomMeta ->
            val zOffset = index * 57

            for (checkpoint in roomMeta.checkpoints) {
                val location = Location(player.world, checkpoint.x.toDouble(), checkpoint.y.toDouble(), checkpoint.z.toDouble() + zOffset)
                list.add(location)
            }
        }
        return list
    }
}