package dev.spaghett.generator

import java.io.File
import kotlin.random.Random

class RoomGenerator {

    private val roomList = listOf(
        "around_pillars",  "blocks",
        "castle_wall",     "early_3+1",
        "fences",          "fence_squeeze",
        "fortress",        "four_towers",
        "ice",             "ladder_slide",
        "ladder_tower",    "overhead_4b",
        "quartz_climb",    "quartz_temple",
        "rng_skip",        "sandpit",
        "scatter",         "slime_scatter",
        "slime_skip",      "tightrope",
        "tower_tightrope", "triple_platform",
        "triple_trapdoor", "underbridge"
    )

    class GeneratedSeed(
        val seed: String,
        val roomCount: Int,
        val rooms: List<String>
    )

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

}