package by.picpac.studio.orders

import by.picpac.studio.audit.AuditLog
import by.picpac.studio.auth.UserRepository
import by.picpac.studio.common.ApiException
import by.picpac.studio.common.TimeSource
import by.picpac.studio.common.badRequest
import by.picpac.studio.common.conflict
import by.picpac.studio.common.notFound
import by.picpac.studio.notifications.Notifier
import by.picpac.studio.packaging.PackageBuilder
import by.picpac.studio.photos.ProjectRepository
import by.picpac.studio.photos.ProjectService
import by.picpac.studio.pricing.PriceCalculator
import by.picpac.studio.pricing.ProductKind
import by.picpac.studio.pricing.PriceCatalog
import by.picpac.studio.pricing.QuoteRequest
import by.picpac.studio.pricing.formatByn
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.random.Random

@Serializable
data class CreateOrderRequest(val projectId: Long, val deliveryMethod: String, val address: String)

/** outcome = "decline" имитирует отказ банка — чтобы показать обработку сбоя на демо. */
@Serializable
data class PayRequest(val outcome: String = "success")

@Serializable
data class StatusStep(val status: String, val title: String, val description: String, val note: String?, val at: String)

@Serializable
data class OrderDto(
    val id: Long,
    val number: String,
    val status: String,
    val statusTitle: String,
    val statusDescription: String,
    val projectId: Long,
    val product: String,
    val format: String,
    val pages: Int,
    val photoCount: Int,
    val deliveryMethod: String,
    val address: String,
    val itemsTotal: Long,
    val deliveryCost: Long,
    val total: Long,
    val totalDisplay: String,
    val paid: Boolean,
    val lastPaymentError: String?,
    val trackNumber: String?,
    val packageReady: Boolean,
    val history: List<StatusStep>,
    val createdAt: String,
)

@Serializable
data class PaymentResult(val orderId: Long, val paid: Boolean, val paymentId: String?, val status: String, val message: String)

class OrderService(
    private val orders: OrderRepository,
    private val projects: ProjectRepository,
    private val projectService: ProjectService,
    private val users: UserRepository,
    private val calculator: PriceCalculator,
    private val packages: PackageBuilder,
    private val notifier: Notifier,
    private val audit: AuditLog,
    private val time: TimeSource,
    private val random: Random = Random.Default,
) {
    // Один сервер — один замок: два одновременных «оформить» не создадут два заказа.
    private val lock = Any()

    /** Возвращает заказ и признак «создан сейчас» (false — это повтор того же запроса). */
    fun create(userId: Long, idempotencyKey: String, req: CreateOrderRequest): Pair<OrderDto, Boolean> = synchronized(lock) {
        orders.findByIdempotencyKey(userId, OrderRepository.SCOPE_CREATE_ORDER, idempotencyKey)?.let { existingId ->
            return get(userId, existingId) to false
        }

        val project = projectService.owned(userId, req.projectId)
        orders.findByProject(project.id)?.let {
            throw conflict("PROJECT_ALREADY_ORDERED", "По этому проекту уже есть заказ ${it.number}")
        }
        val address = req.address.trim()
        if (address.length < 5) throw badRequest("ADDRESS_REQUIRED", "Укажите адрес доставки")

        val photos = projects.photos(project.id)
        if (photos.isEmpty()) throw badRequest("PROJECT_EMPTY", "Добавьте хотя бы одно фото")

        val isPrint = PriceCatalog.catalog.products.find { it.code == project.product }?.kind == ProductKind.PRINT
        // Цену считает сервер: сумме из приложения не доверяем.
        val quote = calculator.quote(
            QuoteRequest(
                product = project.product,
                format = project.format,
                pages = project.pages.takeIf { !isPrint },
                quantity = photos.size.takeIf { isPrint },
                delivery = req.deliveryMethod,
            ),
        )

        val now = time.now().toString()
        val id = orders.create(
            userId, project.id, req.deliveryMethod.uppercase(), address.take(300),
            quote.itemsTotal, quote.deliveryCost, quote.total, idempotencyKey, now,
        )
        val order = orders.find(id)!!
        audit.record(userId, "create_order", "order", id, "ok")
        notify(userId, "Заказ ${order.number} создан на сумму ${formatByn(order.total)}. Оплатите его, и мы начнём печать.")
        return get(userId, id) to true
    }

    fun list(userId: Long): List<OrderDto> = orders.listByUser(userId).map(::toDto)

    fun get(userId: Long, orderId: Long): OrderDto = toDto(owned(userId, orderId))

    /**
     * Мок-оплата. Повторный вызов после успешной оплаты не списывает деньги второй раз,
     * а возвращает тот же результат. Отказ банка сохраняет заказ — можно попробовать ещё раз.
     */
    fun pay(userId: Long, orderId: Long, req: PayRequest): PaymentResult = synchronized(lock) {
        val order = owned(userId, orderId)
        if (order.paymentId != null) {
            return PaymentResult(order.id, true, order.paymentId, order.status.name, "Заказ уже оплачен")
        }
        val now = time.now().toString()
        if (req.outcome.lowercase() == "decline") {
            val error = "Банк отклонил платёж (тестовый режим). Деньги не списаны, попробуйте ещё раз"
            orders.setPaymentError(order.id, error, now)
            audit.record(userId, "pay_order", "order", order.id, "declined")
            throw ApiException(HttpStatusCode.PaymentRequired, "PAYMENT_DECLINED", error)
        }

        val paymentId = "MOCK-" + UUID.randomUUID().toString().take(8).uppercase()
        orders.markPaid(order.id, paymentId, now)
        audit.record(userId, "pay_order", "order", order.id, "paid")
        notify(userId, "Оплата заказа ${order.number} получена. Готовим его к печати.")
        val status = afterPaid(order.id)
        return PaymentResult(order.id, true, paymentId, status.name, "Оплата прошла")
    }

    /** Файл пакета для производства. В продакшене его забирает типография, тут — для демонстрации. */
    fun packageFile(userId: Long, orderId: Long): Path {
        val order = owned(userId, orderId)
        val path = order.packagePath?.let { Paths.get(it) }?.takeIf { Files.exists(it) }
            ?: throw conflict("PACKAGE_NOT_READY", "Пакет появится после оплаты и проверки фото")
        audit.record(userId, "download_package", "order", order.id, "ok")
        return path
    }

    /**
     * Только для демо: двигает заказ на следующий этап, как это делало бы производство.
     * Оплату пропустить нельзя даже здесь.
     */
    fun demoAdvance(userId: Long, orderId: Long): OrderDto = synchronized(lock) {
        val order = owned(userId, orderId)
        val now = time.now().toString()
        when (order.status) {
            OrderStatus.AWAITING_PAYMENT -> throw conflict("PAYMENT_REQUIRED", "Сначала заказ нужно оплатить")
            OrderStatus.PAID -> afterPaid(order.id)
            OrderStatus.ON_REVIEW -> {
                projects.approvePendingPhotos(order.projectId, "Одобрено сотрудником (демо)")
                buildPackage(order.id)
            }
            OrderStatus.PACKED -> orders.setStatus(order.id, OrderStatus.PRINTING, null, now)
            OrderStatus.PRINTING -> orders.setStatus(order.id, OrderStatus.QUALITY_CHECK, null, now)
            OrderStatus.QUALITY_CHECK -> {
                val track = "PP" + (1..9).joinToString("") { random.nextInt(10).toString() } + "BY"
                orders.setTrackNumber(order.id, track, now)
                orders.setStatus(order.id, OrderStatus.SHIPPED, "Трек-номер $track", now)
                notify(userId, "Заказ ${order.number} передан в доставку. Трек-номер: $track")
            }
            OrderStatus.SHIPPED -> {
                orders.setStatus(order.id, OrderStatus.DELIVERED, null, now)
                notify(userId, "Заказ ${order.number} доставлен. Спасибо, что выбрали PicPac!")
            }
            OrderStatus.DELIVERED -> throw conflict("ORDER_FINISHED", "Заказ уже доставлен")
        }
        audit.record(userId, "demo_advance", "order", order.id, "ok")
        return get(userId, order.id)
    }

    /** После оплаты: есть фото на проверке — ждём сотрудника, иначе сразу собираем пакет. */
    private fun afterPaid(orderId: Long): OrderStatus {
        val order = orders.find(orderId)!!
        val needsReview = projects.photos(order.projectId).any { it.verdict != "APPROVED" }
        return if (needsReview) {
            orders.setStatus(order.id, OrderStatus.ON_REVIEW, "Есть фото для ручной проверки", time.now().toString())
            OrderStatus.ON_REVIEW
        } else {
            buildPackage(order.id)
        }
    }

    private fun buildPackage(orderId: Long): OrderStatus {
        val order = orders.find(orderId)!!
        val project = projects.find(order.projectId)!!
        val path = packages.build(order.number, order.deliveryMethod, project, projects.photos(project.id))
        val now = time.now().toString()
        orders.setPackage(order.id, path.toAbsolutePath().toString(), now)
        orders.setStatus(order.id, OrderStatus.PACKED, "Пакет ${path.fileName}", now)
        audit.record(order.userId, "build_package", "order", order.id, "ok")
        return OrderStatus.PACKED
    }

    private fun owned(userId: Long, orderId: Long): OrderRecord =
        orders.find(orderId)?.takeIf { it.userId == userId } ?: throw notFound("Заказ не найден")

    private fun notify(userId: Long, text: String) {
        users.findById(userId)?.let { notifier.notify(it, text) }
    }

    private fun toDto(o: OrderRecord): OrderDto {
        val project = projects.find(o.projectId)!!
        return OrderDto(
            id = o.id,
            number = o.number,
            status = o.status.name,
            statusTitle = o.status.title,
            statusDescription = o.status.description,
            projectId = o.projectId,
            product = project.product,
            format = project.format,
            pages = project.pages,
            photoCount = projects.photos(project.id).size,
            deliveryMethod = o.deliveryMethod,
            address = o.address,
            itemsTotal = o.itemsTotal,
            deliveryCost = o.deliveryCost,
            total = o.total,
            totalDisplay = formatByn(o.total),
            paid = o.paymentId != null,
            lastPaymentError = o.lastPaymentError,
            trackNumber = o.trackNumber,
            packageReady = o.packagePath != null,
            history = orders.history(o.id).map {
                StatusStep(it.status.name, it.status.title, it.status.description, it.note, it.createdAt)
            },
            createdAt = o.createdAt,
        )
    }
}
