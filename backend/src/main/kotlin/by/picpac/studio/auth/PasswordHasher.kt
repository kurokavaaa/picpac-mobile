package by.picpac.studio.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хеширование паролей через PBKDF2 из стандартной библиотеки Java — без лишних зависимостей.
 * Формат строки: pbkdf2_sha256$<итерации>$<соль>$<хеш>, чтобы потом можно было поменять параметры.
 */
class PasswordHasher(private val iterations: Int = 210_000) {
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = derive(password, salt, iterations)
        val b64 = Base64.getEncoder()
        return "pbkdf2_sha256$$iterations$${b64.encodeToString(salt)}$${b64.encodeToString(hash)}"
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
        val iter = parts[1].toIntOrNull() ?: return false
        val b64 = Base64.getDecoder()
        val salt = b64.decode(parts[2])
        val expected = b64.decode(parts[3])
        // Сравнение за постоянное время: по скорости ответа нельзя угадать, сколько байт совпало.
        return MessageDigest.isEqual(derive(password, salt, iter), expected)
    }

    private fun derive(password: String, salt: ByteArray, iter: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iter, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
