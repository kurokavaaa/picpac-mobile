package by.picpac.studio.auth

import by.picpac.studio.db.Database
import by.picpac.studio.db.insert
import by.picpac.studio.db.queryOne
import by.picpac.studio.db.update
import java.sql.ResultSet

data class User(
    val id: Long,
    val email: String?,
    val phone: String?,
    val passwordHash: String,
    val createdAt: String,
)

class UserRepository(private val db: Database) {
    fun create(email: String?, phone: String?, passwordHash: String, createdAt: String): Long =
        db.connection { conn ->
            conn.insert(
                "INSERT INTO users (email, phone, password_hash, created_at) VALUES (?, ?, ?, ?)",
                email, phone, passwordHash, createdAt,
            )
        }

    fun findById(id: Long): User? = findBy("id", id)
    fun findByEmail(email: String): User? = findBy("email", email)
    fun findByPhone(phone: String): User? = findBy("phone", phone)

    fun setEmail(id: Long, email: String) {
        db.connection { conn -> conn.update("UPDATE users SET email = ? WHERE id = ?", email, id) }
    }

    // column — только из кода выше, не из запроса пользователя, поэтому подставлять в SQL безопасно.
    private fun findBy(column: String, value: Any): User? = db.connection { conn ->
        conn.queryOne("SELECT * FROM users WHERE $column = ?", value, map = ::toUser)
    }

    private fun toUser(rs: ResultSet) = User(
        id = rs.getLong("id"),
        email = rs.getString("email"),
        phone = rs.getString("phone"),
        passwordHash = rs.getString("password_hash"),
        createdAt = rs.getString("created_at"),
    )
}
