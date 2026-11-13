package by.picpac.studio.data

/** Результат проверки email: либо нормализованный адрес, либо понятная ошибка с подсказкой. */
sealed interface EmailCheck {
    data class Ok(val email: String) : EmailCheck
    data class Error(val message: String) : EmailCheck
}

/** Популярные почты в Беларуси и вокруг — по ним ловим опечатки вроде gmial.com или mail.ry. */
private val KNOWN_DOMAINS = listOf(
    "gmail.com", "mail.ru", "yandex.by", "yandex.ru", "ya.ru", "tut.by", "inbox.ru", "list.ru", "bk.ru",
    "rambler.ru", "outlook.com", "hotmail.com", "live.com", "icloud.com", "yahoo.com", "proton.me", "protonmail.com",
)

/** Окончания доменов, которые реально встречаются. Всё остальное (например .cod) считаем опечаткой. */
private val KNOWN_TLDS = setOf(
    "by", "ru", "com", "net", "org", "me", "io", "dev", "app", "info", "biz", "eu", "ua", "kz", "pl", "lt", "lv", "de",
    "uk", "us", "co", "edu", "gov", "online", "site", "pro", "tech", "store", "xyz", "su", "рф", "бел",
)

private val EMAIL_SHAPE = Regex("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-zа-я]{2,}$")

fun checkEmail(raw: String): EmailCheck {
    val email = raw.trim().lowercase()
    if (email.isEmpty()) return EmailCheck.Error("Введите email")
    if (!EMAIL_SHAPE.matches(email) || ".." in email || email.startsWith(".")) {
        return EmailCheck.Error("Неверный формат email. Пример: name@gmail.com")
    }
    val (local, domain) = email.split("@", limit = 2)
    if (local.endsWith(".") || domain.startsWith(".") || domain.startsWith("-")) {
        return EmailCheck.Error("Неверный формат email. Пример: name@gmail.com")
    }
    if (domain in KNOWN_DOMAINS) return EmailCheck.Ok(email)

    val suggestion = KNOWN_DOMAINS
        .map { it to editDistance(domain, it) }
        .minByOrNull { it.second }
        ?.takeIf { it.second in 1..4 }
        ?.first
    val tld = domain.substringAfterLast('.')

    return when {
        // Почти как известная почта (gmial.com, mail.ry) — скорее всего опечатка.
        suggestion != null && editDistance(domain, suggestion) <= 2 ->
            EmailCheck.Error("Возможно, вы имели в виду $local@$suggestion?")
        tld !in KNOWN_TLDS ->
            EmailCheck.Error(
                "Проверьте окончание адреса: «.$tld» не похоже на домен" +
                    (suggestion?.let { ". Может быть, $local@$it?" } ?: ""),
            )
        else -> EmailCheck.Ok(email)
    }
}

/** Мягкая версия для входа: только приводим к виду, без подсказок. */
fun normalizeEmail(raw: String): String? = (checkEmail(raw) as? EmailCheck.Ok)?.email
    ?: raw.trim().lowercase().takeIf { EMAIL_SHAPE.matches(it) }

/** Расстояние Левенштейна: сколько букв нужно поменять, вставить или удалить, чтобы получить другую строку. */
private fun editDistance(a: String, b: String): Int {
    val prev = IntArray(b.length + 1) { it }
    val cur = IntArray(b.length + 1)
    for (i in 1..a.length) {
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
        }
        cur.copyInto(prev)
    }
    return prev[b.length]
}

/** Белорусский номер: принимаем +375…, 375… и 80…; храним в виде +375 29 123-45-67. */
fun normalizePhone(raw: String): String? {
    var digits = raw.filter { it.isDigit() }
    if (digits.startsWith("80") && digits.length == 11) digits = "375" + digits.drop(2)
    if (!digits.startsWith("375") || digits.length != 12) return null
    val d = digits.drop(3)
    return "+375 ${d.take(2)} ${d.substring(2, 5)}-${d.substring(5, 7)}-${d.substring(7, 9)}"
}
