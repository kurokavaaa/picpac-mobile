package by.picpac.studio.db

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * Тонкая обёртка над SQLite через обычный JDBC.
 * Каждое действие открывает своё соединение: для прототипа это проще, чем пул.
 */
class Database(file: Path) {
    private val url: String

    init {
        file.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        url = "jdbc:sqlite:${file.toAbsolutePath()}"
        migrate()
    }

    fun <T> connection(block: (Connection) -> T): T =
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { st ->
                st.execute("PRAGMA foreign_keys = ON")
                // Ждём до 5 секунд, если база занята другой записью, вместо мгновенной ошибки.
                st.execute("PRAGMA busy_timeout = 5000")
            }
            block(conn)
        }

    /** Всё внутри block либо сохранится целиком, либо откатится целиком. */
    fun <T> transaction(block: (Connection) -> T): T = connection { conn ->
        conn.autoCommit = false
        try {
            val result = block(conn)
            conn.commit()
            result
        } catch (e: Throwable) {
            conn.rollback()
            throw e
        }
    }

    private fun migrate() = connection { conn ->
        conn.createStatement().use { st ->
            st.execute("PRAGMA journal_mode = WAL")
            SCHEMA.forEach { st.execute(it) }
        }
    }

    private companion object {
        val SCHEMA = listOf(
            """CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE,
                phone TEXT UNIQUE,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id),
                product TEXT NOT NULL,
                format TEXT NOT NULL,
                pages INTEGER NOT NULL,
                title TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL REFERENCES projects(id),
                stored_name TEXT NOT NULL,
                original_name TEXT NOT NULL,
                content_type TEXT NOT NULL,
                size_bytes INTEGER NOT NULL,
                verdict TEXT NOT NULL,
                nsfw_score REAL,
                moderation_note TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id),
                project_id INTEGER NOT NULL REFERENCES projects(id),
                number TEXT UNIQUE,
                status TEXT NOT NULL,
                delivery_method TEXT NOT NULL,
                address TEXT NOT NULL,
                items_total INTEGER NOT NULL,
                delivery_cost INTEGER NOT NULL,
                total INTEGER NOT NULL,
                payment_id TEXT,
                last_payment_error TEXT,
                track_number TEXT,
                package_path TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS order_status_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL REFERENCES orders(id),
                status TEXT NOT NULL,
                note TEXT,
                created_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS idempotency_keys (
                user_id INTEGER NOT NULL,
                scope TEXT NOT NULL,
                idem_key TEXT NOT NULL,
                result_id INTEGER NOT NULL,
                created_at TEXT NOT NULL,
                PRIMARY KEY (user_id, scope, idem_key)
            )""",
            """CREATE TABLE IF NOT EXISTS notifications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id),
                channel TEXT NOT NULL,
                destination TEXT NOT NULL,
                text TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
            """CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT NOT NULL,
                object_type TEXT NOT NULL,
                object_id TEXT,
                result TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
        )
    }
}

// Небольшие помощники, чтобы не писать одно и то же в каждом репозитории.

fun Connection.update(sql: String, vararg args: Any?): Int =
    prepareStatement(sql).use { it.bind(args); it.executeUpdate() }

fun Connection.insert(sql: String, vararg args: Any?): Long =
    prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { st ->
        st.bind(args)
        st.executeUpdate()
        st.generatedKeys.use { keys -> keys.next(); keys.getLong(1) }
    }

fun <T> Connection.query(sql: String, vararg args: Any?, map: (ResultSet) -> T): List<T> =
    prepareStatement(sql).use { st ->
        st.bind(args)
        st.executeQuery().use { rs ->
            val out = mutableListOf<T>()
            while (rs.next()) out += map(rs)
            out
        }
    }

fun <T> Connection.queryOne(sql: String, vararg args: Any?, map: (ResultSet) -> T): T? =
    query(sql, *args, map = map).firstOrNull()

private fun PreparedStatement.bind(args: Array<out Any?>) {
    args.forEachIndexed { i, value -> setObject(i + 1, value) }
}

fun ResultSet.getLongOrNull(column: String): Long? = getLong(column).takeUnless { wasNull() }
fun ResultSet.getDoubleOrNull(column: String): Double? = getDouble(column).takeUnless { wasNull() }
