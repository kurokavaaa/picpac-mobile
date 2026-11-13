package by.picpac.studio.common

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/** Ошибка, которую можно показать пользователю: код для приложения + понятный текст. */
class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message)

@Serializable
data class ErrorBody(val error: ErrorInfo)

@Serializable
data class ErrorInfo(val code: String, val message: String)

fun badRequest(code: String, message: String) = ApiException(HttpStatusCode.BadRequest, code, message)
fun notFound(message: String) = ApiException(HttpStatusCode.NotFound, "NOT_FOUND", message)
fun conflict(code: String, message: String) = ApiException(HttpStatusCode.Conflict, code, message)
fun unauthorized(message: String = "Нужно войти в аккаунт") =
    ApiException(HttpStatusCode.Unauthorized, "UNAUTHORIZED", message)
