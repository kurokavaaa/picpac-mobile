package by.picpac.studio.notifications

import by.picpac.studio.auth.Contacts
import by.picpac.studio.auth.User
import by.picpac.studio.common.TimeSource
import by.picpac.studio.db.Database
import by.picpac.studio.db.insert
import by.picpac.studio.db.query
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

enum class Channel { EMAIL, SMS }

@Serializable
data class NotificationDto(val id: Long, val channel: String, val destination: String, val text: String, val createdAt: String)

/**
 * Уведомления: есть email — пишем на email, нет — SMS на телефон.
 * Реальной отправки нет (заглушка): сообщение сохраняется в базе и видно в личном кабинете.
 */
class Notifier(private val db: Database, private val time: TimeSource) {
    private val log = LoggerFactory.getLogger(Notifier::class.java)

    fun channelFor(user: User): Channel = if (user.email != null) Channel.EMAIL else Channel.SMS

    fun notify(user: User, text: String) {
        val channel = channelFor(user)
        val destination = when (channel) {
            Channel.EMAIL -> user.email
            Channel.SMS -> user.phone
        } ?: return
        db.connection { conn ->
            conn.insert(
                "INSERT INTO notifications (user_id, channel, destination, text, created_at) VALUES (?, ?, ?, ?, ?)",
                user.id, channel.name, destination, text, time.now().toString(),
            )
        }
        log.info("[заглушка] {} -> {}: {}", channel, Contacts.mask(destination), text)
    }

    fun list(userId: Long): List<NotificationDto> = db.connection { conn ->
        conn.query("SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC", userId) { rs ->
            NotificationDto(
                id = rs.getLong("id"),
                channel = rs.getString("channel"),
                destination = rs.getString("destination"),
                text = rs.getString("text"),
                createdAt = rs.getString("created_at"),
            )
        }
    }
}
