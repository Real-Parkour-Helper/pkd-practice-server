package dev.spaghett.shared

class RunTimer {
    private var startTime: Long = 0

    fun start() {
        startTime = System.currentTimeMillis()
    }

    /**
     * Returns the elapsed time in mm:ss.SSS format
     */
    fun elapsedTime(): String {
        val elapsed = System.currentTimeMillis() - startTime
        return format(elapsed)
    }

    fun stop(endTime: Long): String {
        return format(endTime - startTime)
    }

    private fun format(num: Long): String {
        val minutes = (num / 1000) / 60
        val seconds = (num / 1000) % 60
        val milliseconds = num % 1000

        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds)
    }
}