package by.picpac.studio.auth

/** Приведение email и телефона к одному виду, чтобы «+375 (29) 123-45-67» и «80291234567» были одним номером. */
object Contacts {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(raw: String): String? =
        raw.trim().lowercase().takeIf { it.length <= 254 && EMAIL.matches(it) }

    /** Белорусские номера: +375XXXXXXXXX или 80XXXXXXXXX. Результат всегда +375XXXXXXXXX. */
    fun normalizePhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val national = when {
            digits.length == 12 && digits.startsWith("375") -> digits.drop(3)
            digits.length == 11 && digits.startsWith("80") -> digits.drop(2)
            else -> return null
        }
        return "+375$national"
    }

    /** Для логов: личные данные целиком в логи не пишем. */
    fun mask(destination: String): String =
        if ('@' in destination) {
            val (name, domain) = destination.split('@', limit = 2)
            "${name.take(1)}***@$domain"
        } else {
            destination.take(4) + "*****" + destination.takeLast(2)
        }
}
