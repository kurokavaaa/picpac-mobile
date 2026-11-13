package by.picpac.studio.moderation

import java.awt.image.BufferedImage

enum class Verdict { APPROVED, NEEDS_REVIEW }

data class ModerationResult(
    val verdict: Verdict,
    /** Вероятность NSFW от 0 до 1. null — модель не запускалась. */
    val nsfwScore: Double?,
    val note: String,
)

/**
 * Проверка фото перед печатью. Подозрительное фото не блокируем, а отправляем человеку:
 * у модели бывают ложные срабатывания (пляж, детские фото в купальниках).
 * Интерфейс один — реализацию можно заменить (например, на Google SafeSearch), не трогая остальной код.
 */
fun interface ImageModerator {
    fun check(image: BufferedImage): ModerationResult
}

/** Модель не подключена: честно отправляем всё на ручную проверку, а не пропускаем молча. */
class ManualReviewModerator(private val reason: String) : ImageModerator {
    override fun check(image: BufferedImage) =
        ModerationResult(Verdict.NEEDS_REVIEW, null, "Автопроверка недоступна ($reason): фото проверит сотрудник")
}
