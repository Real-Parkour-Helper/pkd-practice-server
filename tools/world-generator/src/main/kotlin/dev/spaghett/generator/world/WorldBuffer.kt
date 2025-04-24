package dev.spaghett.generator.world

import net.querz.mca.MCAFile
import java.io.File
import java.io.RandomAccessFile

class WorldBuffer {
    private val chunks = mutableMapOf<Pair<Int, Int>, ChunkBuffer>()

    fun setBlock(x: Int, y: Int, z: Int, id: Byte, meta: Byte) {
        val chunkX = x shr 4
        val chunkZ = z shr 4
        val chunkKey = chunkX to chunkZ

        val chunk = chunks.getOrPut(chunkKey) { ChunkBuffer(chunkX, chunkZ) }
        chunk.setBlock(x and 15, y, z and 15, id, meta)
    }

    /**
     * Writes all regions to the specified folder.
     */
    fun writeAllRegions(regionFolder: File) {
        val regions = mutableMapOf<Pair<Int, Int>, MCAFile>()

        for ((coord, chunkBuf) in chunks) {
            val (chunkX, chunkZ) = coord
            val regionX = chunkX shr 5
            val regionZ = chunkZ shr 5
            val regionKey = regionX to regionZ

            val mca = regions.getOrPut(regionKey) { MCAFile(regionX, regionZ) }
            mca.setChunk(chunkX, chunkZ, chunkBuf.toChunk())
        }

        for ((regionKey, mca) in regions) {
            val (regionX, regionZ) = regionKey
            val file = File(regionFolder, "r.$regionX.$regionZ.mca")
            file.parentFile.mkdirs()
            RandomAccessFile(file, "rw").use { raf ->
                mca.serialize(raf)
            }
        }
    }
}