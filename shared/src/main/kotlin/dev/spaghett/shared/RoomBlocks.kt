package dev.spaghett.shared

data class BlockStructure(
    val palette: MutableMap<String, Int>,  // "0" to "minecraft:stone"
    val blocks: MutableList<BlockEntry>
)

data class BlockEntry(
    val x: Int,
    val y: Int,
    val z: Int,
    val id: Int,
    val meta: Int = 0
)