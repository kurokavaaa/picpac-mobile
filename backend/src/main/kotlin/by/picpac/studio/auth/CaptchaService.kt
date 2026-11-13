package by.picpac.studio.auth

import by.picpac.studio.common.TimeSource
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Serializable
data class CaptchaChallenge(val captchaId: String, val question: String)

/**
 * Простая своя капча: пример с числами. Ответ хранится только на сервере,
 * каждая капча одноразовая и живёт 5 минут.
 * Для продакшена её заменят на настоящий сервис — интерфейс останется тем же.
 */
class CaptchaService(
    private val time: TimeSource,
    private val ttl: Duration = Duration.ofMinutes(5),
    private val random: Random = Random.Default,
) {
    private data class Pending(val answer: Int, val expiresAt: Instant)

    private val pending = ConcurrentHashMap<String, Pending>()

    fun issue(): CaptchaChallenge {
        removeExpired()
        val a = random.nextInt(2, 10)
        val b = random.nextInt(1, 10)
        val id = UUID.randomUUID().toString()
        pending[id] = Pending(a + b, time.now().plus(ttl))
        return CaptchaChallenge(id, "$a + $b = ?")
    }

    /** remove() — капчу нельзя использовать повторно, даже если ответ неверный. */
    fun verify(id: String, answer: String): Boolean {
        val p = pending.remove(id) ?: return false
        if (time.now().isAfter(p.expiresAt)) return false
        return answer.trim().toIntOrNull() == p.answer
    }

    private fun removeExpired() {
        val now = time.now()
        pending.entries.removeIf { now.isAfter(it.value.expiresAt) }
    }
}
