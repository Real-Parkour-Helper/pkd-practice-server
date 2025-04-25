package dev.spaghett.shared

class RunTimer {
    private var tickCount = 0

    fun start() {
        tickCount = 0
    }

    fun tick() {
        tickCount+=2
    }

    fun stop(): String {
        return format(tickCount)
    }

    fun getElapsed(): String {
        return format(tickCount)
    }

    private fun format(ticks: Int): String {
        val totalMs = ticks * 50L
        val minutes = (totalMs / 1000) / 60
        val seconds = (totalMs / 1000) % 60
        val milliseconds = totalMs % 1000
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}
