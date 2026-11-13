package by.picpac.studio

import by.picpac.studio.account.AccountService
import by.picpac.studio.account.LinkEmailRequest
import by.picpac.studio.audit.AuditLog
import by.picpac.studio.auth.AuthService
import by.picpac.studio.auth.CaptchaService
import by.picpac.studio.auth.JwtService
import by.picpac.studio.auth.LoginRequest
import by.picpac.studio.auth.PasswordHasher
import by.picpac.studio.auth.RegisterRequest
import by.picpac.studio.auth.UserRepository
import by.picpac.studio.chat.ChatService
import by.picpac.studio.chat.StubChatAssistant
import by.picpac.studio.common.ApiException
import by.picpac.studio.common.ErrorBody
import by.picpac.studio.common.ErrorInfo
import by.picpac.studio.common.TimeSource
import by.picpac.studio.common.badRequest
import by.picpac.studio.common.unauthorized
import by.picpac.studio.config.AppConfig
import by.picpac.studio.db.Database
import by.picpac.studio.moderation.ImageModerator
import by.picpac.studio.moderation.ManualReviewModerator
import by.picpac.studio.moderation.OnnxNsfwModerator
import by.picpac.studio.notifications.Notifier
import by.picpac.studio.orders.CreateOrderRequest
import by.picpac.studio.orders.OrderRepository
import by.picpac.studio.orders.OrderService
import by.picpac.studio.orders.PayRequest
import by.picpac.studio.packaging.PackageBuilder
import by.picpac.studio.photos.CreateProjectRequest
import by.picpac.studio.photos.PhotoDto
import by.picpac.studio.photos.PhotoStorage
import by.picpac.studio.photos.ProjectRepository
import by.picpac.studio.photos.ProjectService
import by.picpac.studio.pricing.PriceCalculator
import by.picpac.studio.pricing.PriceCatalog
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.Logger

fun main() {
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }.start(wait = true)
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Сборка приложения: создаём сервисы, подключаем плагины Ktor и описываем маршруты.
 * moderatorOverride нужен тестам — туда подставляется фальшивый модератор вместо ONNX-модели.
 */
fun Application.module(
    config: AppConfig,
    moderatorOverride: ImageModerator? = null,
    time: TimeSource = TimeSource.SYSTEM,
) {
    val db = Database(config.dataDir.resolve("picpac.db"))
    val audit = AuditLog(db, time)
    val users = UserRepository(db)
    val notifier = Notifier(db, time)
    val jwt = JwtService(config, time)
    // Один экземпляр капчи на всё приложение: тот же, что выдал вопрос, проверяет ответ.
    val captcha = CaptchaService(time)
    val auth = AuthService(users, PasswordHasher(), captcha, jwt, notifier, audit, time)
    val account = AccountService(users, auth, notifier, audit)
    val calculator = PriceCalculator()
    val storage = PhotoStorage(config.dataDir)
    val projectRepo = ProjectRepository(db)
    val orderRepo = OrderRepository(db)
    val moderator = moderatorOverride ?: loadModerator(config, log)
    val projects = ProjectService(projectRepo, storage, moderator, calculator, audit, time) { orderRepo.findByProject(it) != null }
    val orders = OrderService(
        orderRepo, projectRepo, projects, users, calculator,
        PackageBuilder(config.dataDir, storage, time), notifier, audit, time,
    )
    val chat = ChatService(StubChatAssistant(), audit)

    install(CallLogging)
    install(ContentNegotiation) { json(json) }
    install(StatusPages) {
        exception<ApiException> { call, e ->
            call.respond(e.status, ErrorBody(ErrorInfo(e.code, e.message)))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorBody(ErrorInfo("BAD_REQUEST", "Некорректный запрос: проверьте поля")))
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Необработанная ошибка", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorBody(ErrorInfo("INTERNAL", "Что-то пошло не так. Прогресс сохранён — попробуйте ещё раз")),
            )
        }
    }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "picpac"
            verifier(jwt.verifier)
            validate { cred ->
                if (cred.payload.getClaim(JwtService.USER_ID_CLAIM).asLong() != null) JWTPrincipal(cred.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorBody(ErrorInfo("UNAUTHORIZED", "Нужно войти в аккаунт")))
            }
        }
    }
    monitor.subscribe(ApplicationStopped) { (moderator as? AutoCloseable)?.close() }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "moderation" to moderator::class.simpleName.orEmpty()))
        }

        route("/auth") {
            get("/captcha") { call.respond(captcha.issue()) }
            post("/register") {
                val req = call.receive<RegisterRequest>()
                call.respond(HttpStatusCode.Created, io { auth.register(req) })
            }
            post("/login") {
                val req = call.receive<LoginRequest>()
                call.respond(io { auth.login(req) })
            }
        }

        route("/pricing") {
            get("/catalog") { call.respond(PriceCatalog.catalog) }
            post("/quote") { call.respond(calculator.quote(call.receive())) }
        }

        // ИИ-помощник доступен и без входа: вопросы о цене и доставке задают до регистрации.
        post("/chat") { call.respond(chat.reply(call.receive())) }

        authenticate("auth-jwt") {
            post("/chat/handoff") { call.respond(io { chat.handoff(call.userId()) }) }

            route("/me") {
                get { call.respond(io { account.profile(call.userId()) }) }
                put("/email") {
                    val req = call.receive<LinkEmailRequest>()
                    call.respond(io { account.linkEmail(call.userId(), req) })
                }
                get("/notifications") { call.respond(io { notifier.list(call.userId()) }) }
                get("/orders") { call.respond(io { orders.list(call.userId()) }) }
            }

            route("/projects") {
                post {
                    val req = call.receive<CreateProjectRequest>()
                    call.respond(HttpStatusCode.Created, io { projects.create(call.userId(), req) })
                }
                get { call.respond(io { projects.list(call.userId()) }) }
                get("/{id}") { call.respond(io { projects.get(call.userId(), call.idParam()) }) }
                post("/{id}/photos") {
                    val userId = call.userId()
                    val projectId = call.idParam()
                    // По умолчанию Ktor принимает части до 64 КБ — для фото поднимаем лимит.
                    val multipart = call.receiveMultipart(formFieldLimit = ProjectService.MAX_PHOTO_BYTES + 1024L)
                    var uploaded: PhotoDto? = null
                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem && uploaded == null) {
                            // Читаем не больше лимита + 1 байт: если файл больше, ProjectService отклонит его, не забивая память.
                            val bytes = part.provider().readRemaining(ProjectService.MAX_PHOTO_BYTES + 1L).readByteArray()
                            val name = part.originalFileName ?: "photo"
                            val type = part.contentType?.toString() ?: ""
                            uploaded = io { projects.addPhoto(userId, projectId, name, type, bytes) }
                        }
                        part.dispose()
                    }
                    val result = uploaded ?: throw badRequest("PHOTO_MISSING", "Прикрепите фото в поле file")
                    call.respond(HttpStatusCode.Created, result)
                }
            }

            route("/orders") {
                post {
                    // Ключ повтора: приложение генерирует его один раз на нажатие «Оформить»
                    // и шлёт тот же при повторе — тогда второй заказ не создаётся.
                    val key = call.request.headers["Idempotency-Key"]?.trim()?.takeIf { it.isNotEmpty() && it.length <= 100 }
                        ?: throw badRequest("IDEMPOTENCY_KEY_REQUIRED", "Нужен заголовок Idempotency-Key")
                    val req = call.receive<CreateOrderRequest>()
                    val (order, created) = io { orders.create(call.userId(), key, req) }
                    call.respond(if (created) HttpStatusCode.Created else HttpStatusCode.OK, order)
                }
                get { call.respond(io { orders.list(call.userId()) }) }
                get("/{id}") { call.respond(io { orders.get(call.userId(), call.idParam()) }) }
                post("/{id}/pay") {
                    val text = call.receiveText()
                    val req = if (text.isBlank()) {
                        PayRequest()
                    } else {
                        try {
                            json.decodeFromString(PayRequest.serializer(), text)
                        } catch (e: SerializationException) {
                            throw badRequest("BAD_REQUEST", "Некорректный запрос оплаты")
                        }
                    }
                    call.respond(io { orders.pay(call.userId(), call.idParam(), req) })
                }
                get("/{id}/package") {
                    val file = io { orders.packageFile(call.userId(), call.idParam()) }
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment
                            .withParameter(ContentDisposition.Parameters.FileName, file.fileName.toString())
                            .toString(),
                    )
                    call.respondFile(file.toFile())
                }
            }

            if (config.demoMode) {
                post("/demo/orders/{id}/advance") { call.respond(io { orders.demoAdvance(call.userId(), call.idParam()) }) }
            }
        }
    }
}

private fun loadModerator(config: AppConfig, log: Logger): ImageModerator {
    val dir = config.nsfwModelDir
    if (dir == null) {
        log.warn("NSFW_MODEL_DIR не задан: все фото пойдут на ручную проверку")
        return ManualReviewModerator("модель не подключена")
    }
    return try {
        OnnxNsfwModerator.load(dir, config.nsfwReviewThreshold).also { log.info("NSFW-модель загружена из {}", dir) }
    } catch (e: Exception) {
        log.error("Не удалось загрузить NSFW-модель из $dir — все фото пойдут на ручную проверку", e)
        ManualReviewModerator("модель не загрузилась")
    }
}

/** Тяжёлую работу (база, диск, модель) уводим из потока, который обслуживает запросы. */
private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

private fun ApplicationCall.userId(): Long =
    principal<JWTPrincipal>()?.payload?.getClaim(JwtService.USER_ID_CLAIM)?.asLong() ?: throw unauthorized()

private fun ApplicationCall.idParam(): Long =
    parameters["id"]?.toLongOrNull() ?: throw badRequest("ID_INVALID", "Неверный номер")
