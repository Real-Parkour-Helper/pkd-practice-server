package dev.spaghett.lobbyplugin

import com.google.gson.Gson
import dev.spaghett.lobbyplugin.commands.*
import dev.spaghett.shared.GeneratedSeed
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL


class LobbyPlugin : JavaPlugin(), Listener {
    private val gson = Gson()
    private var dynamicServer: Process? = null

    override fun onEnable() {
        saveDefaultConfig()

        Bukkit.getPluginManager().registerEvents(this, this)
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        // Register commands
        getCommand("play")?.executor = PlayCommand(this)
        getCommand("playseed")?.executor = PlaySeedCommand(this)
        getCommand("playrooms")?.executor = PlayRoomsCommand(this)
        getCommand("playroomsseed")?.executor = PlayRoomsSeedCommand(this)
        getCommand("playcustom")?.executor = PlayCustomCommand(this)

    }

    override fun onDisable() {
        // Shutdown dynamic server if running
        dynamicServer?.destroy()
        logger.info("Dynamic server shut down.")
        logger.info("LobbyPlugin disabled.")
    }

    @Throws(IOException::class)
    fun generateSeed(seed: Long = 0L, roomCount: Int = 8): GeneratedSeed {
        val url: URL = URI("http://localhost:8080/generateSeed").toURL()
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

        // Setup POST
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json; utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.setDoOutput(true)

        val postDataMap = mapOf(
            "seed" to seed.toString(),
            "roomCount" to roomCount
        )
        val postData = gson.toJson(postDataMap)

        connection.outputStream.use { os ->
            val input = postData.toByteArray(charset("utf-8"))
            os.write(input, 0, input.size)
        }

        // Read Response
        val response = StringBuilder()
        BufferedReader(
            InputStreamReader(connection.inputStream, "utf-8")
        ).use { br ->
            var responseLine: String
            while ((br.readLine().also { responseLine = it }) != null) {
                response.append(responseLine.trim { it <= ' ' })
            }
        }

        return gson.fromJson(response.toString(), GeneratedSeed::class.java)
    }

    @Throws(IOException::class)
    fun buildSeed(seed: GeneratedSeed) {
        val pluginPath = dataFolder.absolutePath
        val sep = File.separator

        val pluginsIndex = pluginPath.lastIndexOf(sep + "plugins" + sep)

        if (pluginsIndex == -1) {
            logger.severe("Could not find 'plugins' folder in path: $pluginPath")
        } else {
            val serverRootPath = pluginPath.substring(0, pluginsIndex)

            val dynamicServerPath = serverRootPath.replace("lobby", "dynamic")

            val seedFile = File(dynamicServerPath, "seed.json")
            seedFile.writeText(gson.toJson(seed))

            val worldPath = File(dynamicServerPath, "world").absolutePath

            if (!File(worldPath).exists()) {
                logger.severe("World directory does not exist: $worldPath")
                throw MapBuilderException("World directory does not exist: $worldPath")
            }

            // Remove region and level.dat
            val worldDir = File(worldPath)
            val regionDir = File(worldDir, "region")
            val levelDat = File(worldDir, "level.dat")

            if (regionDir.exists()) {
                regionDir.deleteRecursively()
            }
            if (levelDat.exists()) {
                levelDat.delete()
            }

            val url: URL = URI("http://localhost:8080/buildMap").toURL()
            val data = gson.toJson(
                mapOf(
                    "seed" to seed,
                    "worldDir" to worldPath,
                    "resetCheckpoints" to false,
                    "startRoom" to true,
                    "finishRoom" to true
                )
            )
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("POST")
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setDoOutput(true)
            connection.outputStream.use { os ->
                val input = data.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }
            val response = StringBuilder()
            BufferedReader(
                InputStreamReader(connection.inputStream, "utf-8")
            ).use { br ->
                var responseLine: String
                while ((br.readLine().also { responseLine = it }) != null) {
                    response.append(responseLine.trim { it <= ' ' })
                }
            }
            if (connection.responseCode != 200) {
                logger.severe("Error building map: ${connection.responseCode} ${connection.responseMessage}")
                throw MapBuilderException("Error building map: ${connection.responseCode} ${connection.responseMessage}")
            } else {
                logger.info("Map built successfully")
            }
        }
    }

    fun startDynamicServer(player: Player): Boolean {
        val java8Path = config.getString("java8_path")
        if (java8Path == null || java8Path.isEmpty() || !File(java8Path).exists()) {
            logger.severe("Java 8 path not set (or set incorrectly) in config.yml")
            return false
        }

        val pluginPath = dataFolder.absolutePath
        val sep = File.separator

        val pluginsIndex = pluginPath.lastIndexOf(sep + "plugins" + sep)

        if (pluginsIndex == -1) {
            logger.severe("Could not find 'plugins' folder in path: $pluginPath")
            return false
        } else {
            val serverRootPath = pluginPath.substring(0, pluginsIndex)

            val dynamicServerPath = serverRootPath.replace("lobby", "dynamic")
            val paperJar = File(dynamicServerPath, "paper-1.8.8-445.jar")
            if (!paperJar.exists()) {
                logger.severe("Paper jar not found: $paperJar")
                return false
            }

            val pb = ProcessBuilder(
                java8Path,
                "-Xmx512m",
                "-jar",
                "paper-1.8.8-445.jar"
            )

            pb.directory(File(dynamicServerPath))

            player.sendMessage("§aServer starting...")
            dynamicServer = pb.start()

            Thread {
                dynamicServer?.inputStream?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        println("[DynamicServer] $line")

                        if ("Timings Reset" in line) {
                            // Dynamic server is ready
                            Bukkit.getScheduler().runTask(this) {
                                player.sendMessage("§aDynamic server is ready! Connecting you...")
                                this.sendToDynamicServer(player)
                            }
                        }
                    }
                }
            }.start()

            return true
        }
    }

    private fun sendToDynamicServer(player: Player) {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        data.writeUTF("Connect")
        data.writeUTF("dynamic")

        player.sendMessage("§aConnecting to dynamic server...")
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray())
    }
}