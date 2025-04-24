package dev.spaghett.generator.world

import net.querz.nbt.tag.CompoundTag

/**
 * Wrapper class for a buffer of a 16x16x16 section of a chunk.
 */
class SectionBuffer(val y: Int) {
    private val blocks = ByteArray(4096)
    private val data = ByteArray(2048)

    fun setBlock(x: Int, y: Int, z: Int, id: Byte, meta: Byte) {
        val index = (y shl 8) or (z shl 4) or x
        blocks[index] = id

        val dataIndex = index / 2
        val nibble = meta.toInt() and 0xF
        data[dataIndex] = if (index % 2 == 0) {
            ((data[dataIndex].toInt() and 0xF0) or nibble).toByte()
        } else {
            ((data[dataIndex].toInt() and 0x0F) or (nibble shl 4)).toByte()
        }
    }

    fun toTag(): CompoundTag = CompoundTag().apply {
        putByte("Y", y.toByte())
        putByteArray("Blocks", blocks)
        putByteArray("Data", data)
        putByteArray("BlockLight", ByteArray(2048))
        putByteArray("SkyLight", ByteArray(2048))
    }
}