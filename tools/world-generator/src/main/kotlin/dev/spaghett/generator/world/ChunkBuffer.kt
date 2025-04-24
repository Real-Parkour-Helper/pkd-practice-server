package dev.spaghett.generator.world

import net.querz.mca.Chunk
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag

/**
 * Wrapper class for a chunk buffer.
 * Holds the sub-chunks and their blocks and can convert them to a Chunk.
 */
class ChunkBuffer(private val chunkX: Int, private val chunkZ: Int) {

    private val sections = mutableMapOf<Int, SectionBuffer>()

    fun setBlock(x: Int, y: Int, z: Int, id: Byte, meta: Byte) {
        val sectionY = y shr 4
        val section = sections.getOrPut(sectionY) { SectionBuffer(sectionY) }
        section.setBlock(x, y and 15, z, id, meta)
    }

    fun toChunk(): Chunk {
        val sectionTags = ListTag(CompoundTag::class.java)

        for (section in sections.values.sortedBy { it.y }) {
            sectionTags.add(section.toTag())
        }

        val level = CompoundTag().apply {
            put("Sections", sectionTags)
            putInt("xPos", chunkX)
            putInt("zPos", chunkZ)
            put("Entities", ListTag(CompoundTag::class.java))
            put("TileEntities", ListTag(CompoundTag::class.java))
            putByte("TerrainPopulated", 1)
        }

        val root = CompoundTag().apply {
            put("Level", level)
        }

        return Chunk(root)
    }

}