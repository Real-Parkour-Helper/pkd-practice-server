package dev.spaghett.shared

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

fun JavaPlugin.runLaterMs(delayMs: Long, block: () -> Unit) {
    Thread {
        try {
            Thread.sleep(delayMs)
            Bukkit.getScheduler().runTask(this, {
                block()
            })
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }.start()
}