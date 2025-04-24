package dev.spaghett.shared

data class Checkpoint(
    val x: Int,
    val y: Int,
    val z: Int
)

data class RoomMeta(
    val name: String,
    val width: Int, // X
    val height: Int,
    val depth: Int, // Z
    val checkpoints: List<Checkpoint>
)
