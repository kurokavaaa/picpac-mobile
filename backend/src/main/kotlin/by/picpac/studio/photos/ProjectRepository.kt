package by.picpac.studio.photos

import by.picpac.studio.db.Database
import by.picpac.studio.db.getDoubleOrNull
import by.picpac.studio.db.insert
import by.picpac.studio.db.query
import by.picpac.studio.db.queryOne
import by.picpac.studio.db.update
import java.sql.ResultSet

data class ProjectRecord(
    val id: Long,
    val userId: Long,
    val product: String,
    val format: String,
    val pages: Int,
    val title: String,
    val createdAt: String,
)

data class PhotoRecord(
    val id: Long,
    val projectId: Long,
    val storedName: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val verdict: String,
    val nsfwScore: Double?,
    val note: String,
    val createdAt: String,
)

class ProjectRepository(private val db: Database) {
    fun create(userId: Long, product: String, format: String, pages: Int, title: String, createdAt: String): Long =
        db.connection { conn ->
            conn.insert(
                "INSERT INTO projects (user_id, product, format, pages, title, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                userId, product, format, pages, title, createdAt,
            )
        }

    fun find(id: Long): ProjectRecord? = db.connection { conn ->
        conn.queryOne("SELECT * FROM projects WHERE id = ?", id, map = ::toProject)
    }

    fun listByUser(userId: Long): List<ProjectRecord> = db.connection { conn ->
        conn.query("SELECT * FROM projects WHERE user_id = ? ORDER BY id DESC", userId, map = ::toProject)
    }

    fun addPhoto(
        projectId: Long, storedName: String, originalName: String, contentType: String,
        sizeBytes: Long, verdict: String, nsfwScore: Double?, note: String, createdAt: String,
    ): Long = db.connection { conn ->
        conn.insert(
            """INSERT INTO photos (project_id, stored_name, original_name, content_type, size_bytes,
               verdict, nsfw_score, moderation_note, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            projectId, storedName, originalName, contentType, sizeBytes, verdict, nsfwScore, note, createdAt,
        )
    }

    fun photos(projectId: Long): List<PhotoRecord> = db.connection { conn ->
        conn.query("SELECT * FROM photos WHERE project_id = ? ORDER BY id", projectId, map = ::toPhoto)
    }

    fun approvePendingPhotos(projectId: Long, note: String): Int = db.connection { conn ->
        conn.update(
            "UPDATE photos SET verdict = 'APPROVED', moderation_note = ? WHERE project_id = ? AND verdict = 'NEEDS_REVIEW'",
            note, projectId,
        )
    }

    private fun toProject(rs: ResultSet) = ProjectRecord(
        id = rs.getLong("id"),
        userId = rs.getLong("user_id"),
        product = rs.getString("product"),
        format = rs.getString("format"),
        pages = rs.getInt("pages"),
        title = rs.getString("title"),
        createdAt = rs.getString("created_at"),
    )

    private fun toPhoto(rs: ResultSet) = PhotoRecord(
        id = rs.getLong("id"),
        projectId = rs.getLong("project_id"),
        storedName = rs.getString("stored_name"),
        originalName = rs.getString("original_name"),
        contentType = rs.getString("content_type"),
        sizeBytes = rs.getLong("size_bytes"),
        verdict = rs.getString("verdict"),
        nsfwScore = rs.getDoubleOrNull("nsfw_score"),
        note = rs.getString("moderation_note"),
        createdAt = rs.getString("created_at"),
    )
}
