package dev.spaghett.scanner

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent

@Mod(
    modid = Scanner.MOD_ID,
    name = Scanner.MOD_NAME,
    version = Scanner.VERSION
)
class Scanner {

    companion object {
        const val MOD_ID = "pkd-room-scanner"
        const val MOD_NAME = "Pkd Room Scanner"
        const val VERSION = "1.0"
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        // Initialization logic here
        println("Pkd Room Scanner mod initialized.")
    }

}