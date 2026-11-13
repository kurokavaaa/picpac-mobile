package by.picpac.studio.common

import java.time.Clock
import java.time.Instant

/** Источник времени выносим отдельно, чтобы в тестах его можно было подменить. */
fun interface TimeSource {
    fun now(): Instant

    companion object {
        val SYSTEM = TimeSource { Clock.systemUTC().instant() }
    }
}
