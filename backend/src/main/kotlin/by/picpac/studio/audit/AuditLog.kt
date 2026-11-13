package by.picpac.studio.audit

import by.picpac.studio.common.TimeSource
import by.picpac.studio.db.Database
import by.picpac.studio.db.insert

/**
 * Журнал действий: кто, когда, что сделал, с каким объектом и чем закончилось.
 * Этого требует ТЗ (раздел «Аудит»).
 */
class AuditLog(private val db: Database, private val time: TimeSource) {
    fun record(userId: Long?, action: String, objectType: String, objectId: Any?, result: String): Long =
        db.connection { conn ->
            conn.insert(
                "INSERT INTO audit_log (user_id, action, object_type, object_id, result, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                userId, action, objectType, objectId?.toString(), result, time.now().toString(),
            )
        }
}
