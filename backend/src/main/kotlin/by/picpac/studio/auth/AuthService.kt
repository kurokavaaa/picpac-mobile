package by.picpac.studio.auth

import by.picpac.studio.audit.AuditLog
import by.picpac.studio.common.ApiException
import by.picpac.studio.common.TimeSource
import by.picpac.studio.common.badRequest
import by.picpac.studio.common.conflict
import by.picpac.studio.notifications.Notifier
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String? = null,
    val phone: String? = null,
    val password: String,
    val captchaId: String,
    val captchaAnswer: String,
)

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class UserDto(
    val id: Long,
    val email: String?,
    val phone: String?,
    /** Куда придут уведомления: EMAIL или SMS. */
    val notificationChannel: String,
    /** true — пользователь без email: приложение предлагает его привязать. */
    val suggestLinkEmail: Boolean,
)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

class AuthService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val captcha: CaptchaService,
    private val jwt: JwtService,
    private val notifier: Notifier,
    private val audit: AuditLog,
    private val time: TimeSource,
) {
    fun register(req: RegisterRequest): AuthResponse {
        // Капчу проверяем первой: без неё не тратим время на хеширование пароля.
        if (!captcha.verify(req.captchaId, req.captchaAnswer)) {
            throw badRequest("CAPTCHA_INVALID", "Неверный ответ на проверку. Получите новую и попробуйте ещё раз")
        }
        val email = req.email?.takeIf { it.isNotBlank() }?.let {
            Contacts.normalizeEmail(it) ?: throw badRequest("EMAIL_INVALID", "Проверьте адрес почты")
        }
        val phone = req.phone?.takeIf { it.isNotBlank() }?.let {
            Contacts.normalizePhone(it) ?: throw badRequest("PHONE_INVALID", "Номер в формате +375 XX XXX-XX-XX")
        }
        if (email == null && phone == null) {
            throw badRequest("CONTACT_REQUIRED", "Укажите email или номер телефона")
        }
        if (req.password.length < 8) {
            throw badRequest("PASSWORD_WEAK", "Пароль должен быть не короче 8 символов")
        }
        if ((email != null && users.findByEmail(email) != null) || (phone != null && users.findByPhone(phone) != null)) {
            throw conflict("USER_EXISTS", "Такой аккаунт уже есть. Попробуйте войти")
        }

        val id = users.create(email, phone, hasher.hash(req.password), time.now().toString())
        val user = users.findById(id)!!
        audit.record(id, "register", "user", id, "ok")
        notifier.notify(user, "Добро пожаловать в PicPac Studio! Аккаунт создан.")
        return AuthResponse(jwt.issue(id), toDto(user))
    }

    fun login(req: LoginRequest): AuthResponse {
        val user = if ('@' in req.login) {
            Contacts.normalizeEmail(req.login)?.let(users::findByEmail)
        } else {
            Contacts.normalizePhone(req.login)?.let(users::findByPhone)
        }
        // Одна и та же ошибка для «нет такого» и «неверный пароль»: так нельзя перебором узнать, кто зарегистрирован.
        if (user == null || !hasher.verify(req.password, user.passwordHash)) {
            audit.record(user?.id, "login", "user", user?.id, "denied")
            throw ApiException(HttpStatusCode.Unauthorized, "INVALID_CREDENTIALS", "Неверный логин или пароль")
        }
        audit.record(user.id, "login", "user", user.id, "ok")
        return AuthResponse(jwt.issue(user.id), toDto(user))
    }

    fun toDto(user: User) = UserDto(
        id = user.id,
        email = user.email,
        phone = user.phone,
        notificationChannel = notifier.channelFor(user).name,
        suggestLinkEmail = user.email == null,
    )
}
