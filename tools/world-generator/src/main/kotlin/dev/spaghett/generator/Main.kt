package dev.spaghett.generator

import com.google.gson.Gson
import io.javalin.Javalin

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
        .start(8080)
}
