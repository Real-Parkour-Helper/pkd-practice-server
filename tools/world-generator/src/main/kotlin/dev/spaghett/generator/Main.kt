package dev.spaghett.generator

import com.google.gson.Gson
import io.javalin.Javalin

import dev.spaghett.shared.GeneratedSeed

data class BuildRequest(
    val seed: GeneratedSeed?,
    val worldDir: String?
)

fun main() {
    val gson = Gson()
    val roomGenerator = RoomGenerator()

    val app = Javalin.create(/*config*/)
        .get("/") { ctx -> ctx.result("Hello World") }
        .post("/generateSeed") { ctx ->
            val body = ctx.body() // json body
            val parsed = gson.fromJson(body, Map::class.java)

            val roomCount = (parsed["roomCount"] as? Number)?.toInt() ?: 8

            val seed = when (val rawSeed = parsed["seed"]) { // seeds need to be a string to avoid precision loss
                is String -> rawSeed.toLongOrNull() ?: 0L
                is Number -> rawSeed.toLong()
                else -> 0L
            }

            try {
                val generatedSeed = roomGenerator.pickRooms(roomCount, seed)
                val json = gson.toJson(generatedSeed)
                ctx.json(json)
            } catch (e: IllegalArgumentException) {
                ctx.status(400).result("Invalid room count: $roomCount")
            }
        }
        .post("/buildMap") { ctx ->
            val body = ctx.body()
            val parsed = gson.fromJson(body, BuildRequest::class.java)

            val seed = parsed.seed
            val worldDir = parsed.worldDir

            if (seed == null || worldDir == null) {
                ctx.status(400).result("Invalid request: seed and worldDir are required")
                return@post
            }

            try {
                roomGenerator.buildMap(seed, worldDir)
                ctx.result("Map built successfully")
            } catch (e: Exception) {
                ctx.status(500).result("Error building map: ${e.message}")
            }
        }
        .start(8080)
}
