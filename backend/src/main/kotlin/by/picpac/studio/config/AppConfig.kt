package by.picpac.studio.config

import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

data class AppConfig(
    val port: Int,
    val dataDir: Path,
    val jwtSecret: String,
    val jwtIssuer: String = "picpac-studio",
    val tokenTtl: Duration = Duration.ofDays(7),
    /** Папка с model.onnx, config.json и preprocessor_config.json. null — модель не подключена. */
    val nsfwModelDir: Path?,
    /** Порог вероятности NSFW, начиная с которого фото уходит на ручную проверку. */
    val nsfwReviewThreshold: Double = 0.5,
    /**
     * Демо-эндпоинты под /demo (продвижение статусов заказа, «одобрение» модерации).
     * Выключены по умолчанию: через них владелец заказа может сам обойти ручную проверку фото.
     * На показе включать явно: DEMO_MODE=true.
     */
    val demoMode: Boolean = false,
) {
    companion object {
        private val log = LoggerFactory.getLogger(AppConfig::class.java)

        fun fromEnv(env: Map<String, String> = System.getenv()): AppConfig {
            val secret = env["JWT_SECRET"]?.takeIf { it.isNotBlank() } ?: run {
                // Секрет в коде — дыра, поэтому без переменной генерируем случайный.
                // Цена: после перезапуска сервера все входят заново.
                log.warn("JWT_SECRET не задан: сгенерирован случайный, токены сбросятся после перезапуска")
                randomSecret()
            }
            return AppConfig(
                port = env["PORT"]?.toIntOrNull() ?: 8080,
                dataDir = Paths.get(env["DATA_DIR"] ?: "data"),
                jwtSecret = secret,
                nsfwModelDir = env["NSFW_MODEL_DIR"]?.takeIf { it.isNotBlank() }?.let { Paths.get(it) },
                nsfwReviewThreshold = env["NSFW_REVIEW_THRESHOLD"]?.toDoubleOrNull() ?: 0.5,
                demoMode = env["DEMO_MODE"]?.lowercase() == "true",
            )
        }

        fun randomSecret(): String {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            return Base64.getEncoder().encodeToString(bytes)
        }
    }
}
