package by.picpac.studio.account

import by.picpac.studio.audit.AuditLog
import by.picpac.studio.auth.AuthService
import by.picpac.studio.auth.Contacts
import by.picpac.studio.auth.UserDto
import by.picpac.studio.auth.UserRepository
import by.picpac.studio.common.badRequest
import by.picpac.studio.common.conflict
import by.picpac.studio.common.unauthorized
import by.picpac.studio.notifications.Notifier
import kotlinx.serialization.Serializable

@Serializable
data class LinkEmailRequest(val email: String)

/** Личный кабинет: профиль, привязка email, история уведомлений. */
class AccountService(
    private val users: UserRepository,
    private val auth: AuthService,
    private val notifier: Notifier,
    private val audit: AuditLog,
) {
    fun profile(userId: Long): UserDto =
        auth.toDto(users.findById(userId) ?: throw unauthorized())

    fun linkEmail(userId: Long, req: LinkEmailRequest): UserDto {
        val email = Contacts.normalizeEmail(req.email) ?: throw badRequest("EMAIL_INVALID", "Проверьте адрес почты")
        val owner = users.findByEmail(email)
        if (owner != null && owner.id != userId) throw conflict("EMAIL_TAKEN", "Эта почта уже привязана к другому аккаунту")
        users.setEmail(userId, email)
        audit.record(userId, "link_email", "user", userId, "ok")
        val user = users.findById(userId) ?: throw unauthorized()
        notifier.notify(user, "Почта привязана. Теперь уведомления о заказах будут приходить сюда.")
        return auth.toDto(user)
    }
}
