package by.picpac.studio.orders

import by.picpac.studio.db.Database
import by.picpac.studio.db.insert
import by.picpac.studio.db.query
import by.picpac.studio.db.queryOne
import by.picpac.studio.db.update
import java.sql.Connection
import java.sql.ResultSet

/** Этапы заказа — как трекинг на WB и Ozon. Названия показываются в приложении. */
enum class OrderStatus(val title: String, val description: String) {
    AWAITING_PAYMENT("Ожидает оплаты", "Заказ создан. В производство он уйдёт только после оплаты"),
    PAID("Оплачен", "Оплата получена, готовим файлы"),
    ON_REVIEW("На проверке", "Сотрудник проверяет фото перед печатью"),
    PACKED("Файлы готовы", "Пакет для печати собран, на каждом листе свой штрихкод"),
    PRINTING("Печатается", "Печатаем и ламинируем листы"),
    QUALITY_CHECK("Контроль качества", "Проверяем каждый лист: человек и печатная машина"),
    SHIPPED("Передан в доставку", "Посылка у службы доставки, трек-номер в заказе"),
    DELIVERED("Доставлен", "Заказ получен. Спасибо, что выбрали PicPac!"),
}

data class OrderRecord(
    val id: Long,
    val userId: Long,
    val projectId: Long,
    val number: String,
    val status: OrderStatus,
    val deliveryMethod: String,
    val address: String,
    val itemsTotal: Long,
    val deliveryCost: Long,
    val total: Long,
    val paymentId: String?,
    val lastPaymentError: String?,
    val trackNumber: String?,
    val packagePath: String?,
    val createdAt: String,
)

data class StatusRecord(val status: OrderStatus, val note: String?, val createdAt: String)

class OrderRepository(private val db: Database) {

    fun find(id: Long): OrderRecord? = db.connection { conn ->
        conn.queryOne("SELECT * FROM orders WHERE id = ?", id, map = ::toOrder)
    }

    fun findByProject(projectId: Long): OrderRecord? = db.connection { conn ->
        conn.queryOne("SELECT * FROM orders WHERE project_id = ?", projectId, map = ::toOrder)
    }

    fun listByUser(userId: Long): List<OrderRecord> = db.connection { conn ->
        conn.query("SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC", userId, map = ::toOrder)
    }

    fun history(orderId: Long): List<StatusRecord> = db.connection { conn ->
        conn.query("SELECT * FROM order_status_history WHERE order_id = ? ORDER BY id", orderId) { rs ->
            StatusRecord(OrderStatus.valueOf(rs.getString("status")), rs.getString("note"), rs.getString("created_at"))
        }
    }

    fun findByIdempotencyKey(userId: Long, scope: String, key: String): Long? = db.connection { conn ->
        conn.queryOne(
            "SELECT result_id FROM idempotency_keys WHERE user_id = ? AND scope = ? AND idem_key = ?",
            userId, scope, key,
        ) { it.getLong("result_id") }
    }

    /** Заказ, первая запись истории и ключ повтора создаются одной транзакцией — всё или ничего. */
    fun create(
        userId: Long, projectId: Long, deliveryMethod: String, address: String,
        itemsTotal: Long, deliveryCost: Long, total: Long, idempotencyKey: String, now: String,
    ): Long = db.transaction { conn ->
        val id = conn.insert(
            """INSERT INTO orders (user_id, project_id, status, delivery_method, address, items_total,
               delivery_cost, total, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            userId, projectId, OrderStatus.AWAITING_PAYMENT.name, deliveryMethod, address,
            itemsTotal, deliveryCost, total, now, now,
        )
        conn.update("UPDATE orders SET number = ? WHERE id = ?", orderNumber(id), id)
        conn.addHistory(id, OrderStatus.AWAITING_PAYMENT, null, now)
        conn.insert(
            "INSERT INTO idempotency_keys (user_id, scope, idem_key, result_id, created_at) VALUES (?, ?, ?, ?, ?)",
            userId, SCOPE_CREATE_ORDER, idempotencyKey, id, now,
        )
        id
    }

    fun markPaid(id: Long, paymentId: String, now: String) = db.transaction { conn ->
        conn.update(
            "UPDATE orders SET status = ?, payment_id = ?, last_payment_error = NULL, updated_at = ? WHERE id = ?",
            OrderStatus.PAID.name, paymentId, now, id,
        )
        conn.addHistory(id, OrderStatus.PAID, "Платёж $paymentId", now)
    }

    fun setPaymentError(id: Long, error: String, now: String) = db.connection { conn ->
        conn.update("UPDATE orders SET last_payment_error = ?, updated_at = ? WHERE id = ?", error, now, id)
    }

    fun setStatus(id: Long, status: OrderStatus, note: String?, now: String) = db.transaction { conn ->
        conn.update("UPDATE orders SET status = ?, updated_at = ? WHERE id = ?", status.name, now, id)
        conn.addHistory(id, status, note, now)
    }

    fun setPackage(id: Long, path: String, now: String) = db.connection { conn ->
        conn.update("UPDATE orders SET package_path = ?, updated_at = ? WHERE id = ?", path, now, id)
    }

    fun setTrackNumber(id: Long, track: String, now: String) = db.connection { conn ->
        conn.update("UPDATE orders SET track_number = ?, updated_at = ? WHERE id = ?", track, now, id)
    }

    private fun Connection.addHistory(orderId: Long, status: OrderStatus, note: String?, now: String) {
        insert(
            "INSERT INTO order_status_history (order_id, status, note, created_at) VALUES (?, ?, ?, ?)",
            orderId, status.name, note, now,
        )
    }

    private fun toOrder(rs: ResultSet) = OrderRecord(
        id = rs.getLong("id"),
        userId = rs.getLong("user_id"),
        projectId = rs.getLong("project_id"),
        number = rs.getString("number"),
        status = OrderStatus.valueOf(rs.getString("status")),
        deliveryMethod = rs.getString("delivery_method"),
        address = rs.getString("address"),
        itemsTotal = rs.getLong("items_total"),
        deliveryCost = rs.getLong("delivery_cost"),
        total = rs.getLong("total"),
        paymentId = rs.getString("payment_id"),
        lastPaymentError = rs.getString("last_payment_error"),
        trackNumber = rs.getString("track_number"),
        packagePath = rs.getString("package_path"),
        createdAt = rs.getString("created_at"),
    )

    companion object {
        const val SCOPE_CREATE_ORDER = "create_order"
        fun orderNumber(id: Long) = "PP-" + id.toString().padStart(6, '0')
    }
}
