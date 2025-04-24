package dev.spaghett.shared

import com.google.gson.Gson
import java.io.InputStreamReader

object RoomLoader {
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
}