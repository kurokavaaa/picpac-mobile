package by.picpac.studio

import by.picpac.studio.auth.AuthResponse
import by.picpac.studio.auth.CaptchaChallenge
import by.picpac.studio.auth.RegisterRequest
import by.picpac.studio.auth.UserDto
import by.picpac.studio.chat.ChatReply
import by.picpac.studio.common.ErrorBody
import by.picpac.studio.config.AppConfig
import by.picpac.studio.moderation.ImageModerator
import by.picpac.studio.moderation.ModerationResult
import by.picpac.studio.moderation.Verdict
import by.picpac.studio.notifications.NotificationDto
import by.picpac.studio.orders.OrderDto
import by.picpac.studio.orders.PaymentResult
import by.picpac.studio.packaging.PackageManifest
import by.picpac.studio.photos.PhotoDto
import by.picpac.studio.photos.ProjectDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Проверяет сценарий целиком через HTTP, как это делало бы приложение.
 * Вместо ONNX-модели — фальшивый модератор: красный пиксель в углу = «подозрительное фото».
 */
class ApiTest {

    private class FakeModerator : ImageModerator {
        override fun check(image: BufferedImage): ModerationResult =
            if (image.getRGB(0, 0) and 0xFFFFFF == 0xFF0000) {
                ModerationResult(Verdict.NEEDS_REVIEW, 0.97, "Фото отправлено на проверку сотруднику")
            } else {
                ModerationResult(Verdict.APPROVED, 0.01, "Автопроверка пройдена")
            }
    }

    private fun app(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        val dir = Files.createTempDirectory("picpac-test")
        // Демо-эндпоинты по умолчанию выключены; тесты проходят путь заказа целиком, поэтому включаем их явно.
        val config = AppConfig(port = 0, dataDir = dir, jwtSecret = "test-secret-not-for-prod", nsfwModelDir = null, demoMode = true)
        application { module(config, moderatorOverride = FakeModerator()) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ---------- Регистрация и вход ----------

    @Test
    fun `регистрация по телефону предлагает привязать email, после привязки уведомления идут на почту`() {
        app { client ->
            val auth = client.register(phone = "+375 (29) 111-22-33")
            assertEquals("+375291112233", auth.user.phone)
            assertNull(auth.user.email)
            assertTrue(auth.user.suggestLinkEmail)
            assertEquals("SMS", auth.user.notificationChannel)

            val linked = client.put("/me/email") {
                bearerAuth(auth.token)
                contentType(ContentType.Application.Json)
                setBody("""{"email":"Anna@Example.com"}""")
            }
            assertEquals(HttpStatusCode.OK, linked.status, linked.bodyAsText())
            val user = linked.body<UserDto>()
            assertEquals("anna@example.com", user.email)
            assertEquals("EMAIL", user.notificationChannel)
            assertFalse(user.suggestLinkEmail)

            val notifications = client.get("/me/notifications") { bearerAuth(auth.token) }.body<List<NotificationDto>>()
            assertEquals("EMAIL", notifications.first().channel)
            assertEquals("SMS", notifications.last().channel) // приветствие ушло SMS, пока почты не было
        }
    }

    @Test
    fun `неверная капча отклоняется, и ту же капчу нельзя использовать повторно`() {
        app { client ->
            val captcha = client.get("/auth/captcha").body<CaptchaChallenge>()
            val wrong = client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email = "a@b.by", password = PASSWORD, captchaId = captcha.captchaId, captchaAnswer = "-1"))
            }
            assertEquals(HttpStatusCode.BadRequest, wrong.status)
            assertEquals("CAPTCHA_INVALID", wrong.body<ErrorBody>().error.code)

            val reuse = client.post("/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email = "a@b.by", password = PASSWORD, captchaId = captcha.captchaId, captchaAnswer = solve(captcha)))
            }
            assertEquals("CAPTCHA_INVALID", reuse.body<ErrorBody>().error.code)
        }
    }

    @Test
    fun `повторная регистрация и неверный пароль дают понятные ошибки, вход работает по почте и телефону`() {
        app { client ->
            client.register(email = "ivan@picpac.by", phone = "80295556677")
            val again = client.registerRaw(email = "IVAN@picpac.by")
            assertEquals(HttpStatusCode.Conflict, again.status)
            assertEquals("USER_EXISTS", again.body<ErrorBody>().error.code)

            val bad = client.login("ivan@picpac.by", "wrong-pass")
            assertEquals(HttpStatusCode.Unauthorized, bad.status)
            assertEquals("INVALID_CREDENTIALS", bad.body<ErrorBody>().error.code)

            assertEquals(HttpStatusCode.OK, client.login("ivan@picpac.by", PASSWORD).status)
            assertEquals(HttpStatusCode.OK, client.login("+375 29 555 66 77", PASSWORD).status)
        }
    }

    @Test
    fun `без входа заказ и личный кабинет недоступны`() {
        app { client ->
            val order = client.post("/orders") {
                header("Idempotency-Key", "k")
                contentType(ContentType.Application.Json)
                setBody("""{"projectId":1,"deliveryMethod":"BELPOST","address":"Минск"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, order.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get("/me").status)
            assertEquals(HttpStatusCode.Unauthorized, client.post("/chat/handoff").status)
        }
    }

    // ---------- Обязательный путь: фото → заказ → оплата → пакет → статус ----------

    @Test
    fun `полный путь заказа без дублей, с пакетом для печати и трекингом`() {
        app { client ->
            val token = client.register(email = "flow@picpac.by").token
            val project = client.createProject(token, """{"product":"CLASSIC","format":"20x20","pages":24}""")
            val photo = client.upload(token, project.id, png(Color.WHITE)).body<PhotoDto>()
            assertEquals("APPROVED", photo.verdict)

            val first = client.createOrder(token, project.id, key = "tap-1")
            assertEquals(HttpStatusCode.Created, first.status, first.bodyAsText())
            val order = first.body<OrderDto>()
            assertEquals("AWAITING_PAYMENT", order.status)
            assertEquals(8700L + 590L, order.total) // Classic 20 стр. 81,00 + 4 стр. 6,00 + Белпочта 5,90

            // Повтор с тем же ключом (например, пропал интернет) — тот же заказ, не новый.
            val retry = client.createOrder(token, project.id, key = "tap-1")
            assertEquals(HttpStatusCode.OK, retry.status)
            assertEquals(order.id, retry.body<OrderDto>().id)
            assertEquals(1, client.get("/orders") { bearerAuth(token) }.body<List<OrderDto>>().size)

            // До оплаты: ни пакета, ни производства.
            assertEquals(HttpStatusCode.Conflict, client.get("/orders/${order.id}/package") { bearerAuth(token) }.status)
            val skip = client.post("/demo/orders/${order.id}/advance") { bearerAuth(token) }
            assertEquals("PAYMENT_REQUIRED", skip.body<ErrorBody>().error.code)

            val paid = client.post("/orders/${order.id}/pay") { bearerAuth(token) }.body<PaymentResult>()
            assertTrue(paid.paid)
            assertEquals("PACKED", paid.status)
            val payAgain = client.post("/orders/${order.id}/pay") { bearerAuth(token) }.body<PaymentResult>()
            assertEquals(paid.paymentId, payAgain.paymentId) // второй раз не списываем

            val zip = client.get("/orders/${order.id}/package") { bearerAuth(token) }
            assertEquals(HttpStatusCode.OK, zip.status)
            val entries = unzip(zip.bodyAsBytes())
            val manifest = Json.decodeFromString(PackageManifest.serializer(), entries.getValue("manifest.json").decodeToString())
            assertEquals(order.number, manifest.orderNumber)
            assertEquals(1, manifest.sheets.size)
            assertEquals("PP000001-001", manifest.sheets.single().barcode)
            assertTrue(manifest.sheets.single().file in entries.keys)

            val statuses = (1..4).map {
                client.post("/demo/orders/${order.id}/advance") { bearerAuth(token) }.body<OrderDto>()
            }
            assertEquals(listOf("PRINTING", "QUALITY_CHECK", "SHIPPED", "DELIVERED"), statuses.map { it.status })
            val delivered = statuses.last()
            assertNotNull(delivered.trackNumber)
            assertEquals(
                listOf("AWAITING_PAYMENT", "PAID", "PACKED", "PRINTING", "QUALITY_CHECK", "SHIPPED", "DELIVERED"),
                delivered.history.map { it.status },
            )

            // После заказа фото менять нельзя.
            val late = client.upload(token, project.id, png(Color.WHITE))
            assertEquals("PROJECT_LOCKED", late.body<ErrorBody>().error.code)
        }
    }

    @Test
    fun `отказ банка сохраняет заказ, повторная оплата проходит`() {
        app { client ->
            val token = client.register(email = "decline@picpac.by").token
            val project = client.createProject(token, """{"product":"PRINT","format":"10x15"}""")
            repeat(3) { client.upload(token, project.id, png(Color.BLUE)) }
            val order = client.createOrder(token, project.id, key = "k-decline").body<OrderDto>()
            assertEquals(3L * 130 + 590, order.total)

            val declined = client.post("/orders/${order.id}/pay") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("""{"outcome":"decline"}""")
            }
            assertEquals(HttpStatusCode.PaymentRequired, declined.status)
            assertEquals("PAYMENT_DECLINED", declined.body<ErrorBody>().error.code)

            val afterDecline = client.get("/orders/${order.id}") { bearerAuth(token) }.body<OrderDto>()
            assertEquals("AWAITING_PAYMENT", afterDecline.status)
            assertFalse(afterDecline.paid)
            assertNotNull(afterDecline.lastPaymentError)

            val ok = client.post("/orders/${order.id}/pay") { bearerAuth(token) }.body<PaymentResult>()
            assertTrue(ok.paid)
        }
    }

    @Test
    fun `подозрительное фото отправляет оплаченный заказ на ручную проверку`() {
        app { client ->
            val token = client.register(email = "review@picpac.by").token
            val project = client.createProject(token, """{"product":"LITE","format":"20x20"}""")
            client.upload(token, project.id, png(Color.WHITE))
            val flagged = client.upload(token, project.id, png(Color.RED)).body<PhotoDto>()
            assertEquals("NEEDS_REVIEW", flagged.verdict)

            val order = client.createOrder(token, project.id, key = "k-review").body<OrderDto>()
            val paid = client.post("/orders/${order.id}/pay") { bearerAuth(token) }.body<PaymentResult>()
            assertEquals("ON_REVIEW", paid.status)
            assertEquals(HttpStatusCode.Conflict, client.get("/orders/${order.id}/package") { bearerAuth(token) }.status)

            val approved = client.post("/demo/orders/${order.id}/advance") { bearerAuth(token) }.body<OrderDto>()
            assertEquals("PACKED", approved.status)
            assertTrue(approved.packageReady)
        }
    }

    @Test
    fun `заказ требует фото, ключ повтора и не виден чужому пользователю`() {
        app { client ->
            val owner = client.register(email = "owner@picpac.by").token
            val project = client.createProject(owner, """{"product":"PREMIUM","format":"20x20"}""")
            assertEquals("PROJECT_EMPTY", client.createOrder(owner, project.id, key = "k1").body<ErrorBody>().error.code)

            client.upload(owner, project.id, png(Color.WHITE))
            val noKey = client.post("/orders") {
                bearerAuth(owner)
                contentType(ContentType.Application.Json)
                setBody("""{"projectId":${project.id},"deliveryMethod":"BELPOST","address":"Минск, ул. Тестовая, 1"}""")
            }
            assertEquals("IDEMPOTENCY_KEY_REQUIRED", noKey.body<ErrorBody>().error.code)

            val order = client.createOrder(owner, project.id, key = "k2").body<OrderDto>()
            val stranger = client.register(email = "stranger@picpac.by").token
            assertEquals(HttpStatusCode.NotFound, client.get("/orders/${order.id}") { bearerAuth(stranger) }.status)
            assertEquals(HttpStatusCode.NotFound, client.get("/projects/${project.id}") { bearerAuth(stranger) }.status)
        }
    }

    @Test
    fun `файл, который не является картинкой, отклоняется понятной ошибкой`() {
        app { client ->
            val token = client.register(email = "files@picpac.by").token
            val project = client.createProject(token, """{"product":"LITE","format":"20x20"}""")
            val resp = client.upload(token, project.id, "not an image".toByteArray())
            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertEquals("PHOTO_UNREADABLE", resp.body<ErrorBody>().error.code)
        }
    }

    @Test
    fun `заглушка ИИ-чата отвечает по ключевым словам и предлагает оператора`() {
        app { client ->
            val known = client.post("/chat") {
                contentType(ContentType.Application.Json)
                setBody("""{"message":"Сколько стоит доставка?"}""")
            }.body<ChatReply>()
            assertFalse(known.suggestHandoff)
            assertEquals("stub", known.source)

            val unknown = client.post("/chat") {
                contentType(ContentType.Application.Json)
                setBody("""{"message":"Можно напечатать на футболке?"}""")
            }.body<ChatReply>()
            assertTrue(unknown.suggestHandoff)
        }
    }

    // ---------- Помощники ----------

    private suspend fun HttpClient.registerRaw(email: String? = null, phone: String? = null): HttpResponse {
        val captcha = get("/auth/captcha").body<CaptchaChallenge>()
        return post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, phone, PASSWORD, captcha.captchaId, solve(captcha)))
        }
    }

    private suspend fun HttpClient.register(email: String? = null, phone: String? = null): AuthResponse {
        val resp = registerRaw(email, phone)
        assertEquals(HttpStatusCode.Created, resp.status, resp.bodyAsText())
        return resp.body()
    }

    private suspend fun HttpClient.login(login: String, password: String) = post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody("""{"login":"$login","password":"$password"}""")
    }

    private suspend fun HttpClient.createProject(token: String, body: String): ProjectDto {
        val resp = post("/projects") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, resp.status, resp.bodyAsText())
        return resp.body()
    }

    private suspend fun HttpClient.upload(token: String, projectId: Long, bytes: ByteArray) =
        submitFormWithBinaryData(
            "/projects/$projectId/photos",
            formData {
                append(
                    "file", bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"photo.png\"")
                    },
                )
            },
        ) { bearerAuth(token) }

    private suspend fun HttpClient.createOrder(token: String, projectId: Long, key: String) = post("/orders") {
        bearerAuth(token)
        header("Idempotency-Key", key)
        contentType(ContentType.Application.Json)
        setBody("""{"projectId":$projectId,"deliveryMethod":"BELPOST","address":"Минск, ул. Тестовая, 1"}""")
    }

    private fun solve(captcha: CaptchaChallenge): String {
        val (a, b) = Regex("(\\d+) \\+ (\\d+)").find(captcha.question)!!.destructured
        return (a.toInt() + b.toInt()).toString()
    }

    private fun png(color: Color): ByteArray {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply { this.color = color; fillRect(0, 0, 32, 32); dispose() }
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val out = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                out[entry.name] = zip.readBytes()
            }
        }
        return out
    }

    private companion object {
        const val PASSWORD = "secret123"
    }
}
