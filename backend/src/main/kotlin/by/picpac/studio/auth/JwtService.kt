package by.picpac.studio.auth

import by.picpac.studio.common.TimeSource
import by.picpac.studio.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/** Выдаёт токен после входа. В токене только id пользователя — без пароля и контактов. */
class JwtService(private val config: AppConfig, private val time: TimeSource) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)

    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(config.jwtIssuer).build()

    fun issue(userId: Long): String {
        val now = time.now()
        return JWT.create()
            .withIssuer(config.jwtIssuer)
            .withClaim(USER_ID_CLAIM, userId)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(config.tokenTtl)))
            .sign(algorithm)
    }

    companion object {
        const val USER_ID_CLAIM = "uid"
    }
}
