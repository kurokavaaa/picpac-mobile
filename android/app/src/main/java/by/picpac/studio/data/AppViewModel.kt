package by.picpac.studio.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.picpac.studio.ui.Navigator
import by.picpac.studio.ui.Screen
import by.picpac.studio.ui.components.LoaderState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import kotlin.random.Random

sealed interface AuthResult {
    data class Ok(val askEmail: Boolean) : AuthResult

    /** notRegistered = такого аккаунта нет — экран входа предложит зарегистрироваться. */
    data class Error(val message: String, val notRegistered: Boolean = false) : AuthResult
}

/**
 * Вся логика прототипа без сервера: аккаунты, корзина, заказы, оплата, чат.
 * Состояние живёт здесь и сохраняется в файл на телефоне после каждого изменения.
 * Потом эти методы заменятся вызовами бэкенда, а экраны останутся прежними.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(File(app.filesDir, "picpac_state.json"))
    private val saveLock = Mutex()

    var state by mutableStateOf(storage.load())
        private set

    val nav = Navigator(if (state.onboardingDone) Screen.Home else Screen.Onboarding)

    var loader by mutableStateOf<LoaderState?>(null)
        private set

    var typing by mutableStateOf(false)
        private set

    init {
        showLoader("Готовим ваши воспоминания", null, 1600) {}
    }

    private var saveJob: Job? = null

    private fun update(change: (AppState) -> AppState) {
        state = change(state)
        // При перетаскивании стикера состояние меняется 60 раз в секунду — пишем файл не каждый раз,
        // а через 300 мс после последнего изменения.
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            saveLock.withLock { storage.save(state) }
        }
    }

    /** Сохранить немедленно — вызывается, когда приложение уходит в фон. */
    fun flush() {
        saveJob?.cancel()
        runCatching { storage.save(state) }
    }

    private fun now() = System.currentTimeMillis()

    // ---------- Загрузка ----------

    fun showLoader(title: String, subtitle: String?, durationMs: Long, then: () -> Unit) {
        val id = System.nanoTime()
        loader = LoaderState(id, title, subtitle, durationMs)
        viewModelScope.launch {
            delay(durationMs + 200)
            if (loader?.id == id) loader = null
            then()
        }
    }

    fun finishOnboarding() = update { it.copy(onboardingDone = true) }

    // ---------- Обучение ----------

    /** Текущий шаг обучения с подсветкой (по списку TourSteps). Не сохраняется: после перезапуска — с начала. */
    var tourStep by mutableStateOf(0)
        private set

    /** Пользователь нажал на подсвеченный элемент — переходим к следующему шагу. */
    fun tourTapped(key: String, stepKeys: List<String>) {
        if (state.tourDone || stepKeys.getOrNull(tourStep) != key) return
        nextTourStep(stepKeys.size)
    }

    fun nextTourStep(total: Int) {
        if (tourStep + 1 >= total) finishTour() else tourStep++
    }

    fun finishTour() = update { it.copy(tourDone = true) }

    fun finishDragHint() = update { it.copy(dragHintDone = true) }

    /** Из настроек: показать обучение ещё раз (например, комиссии). */
    fun restartTour() {
        tourStep = 0
        update { it.copy(tourDone = false, dragHintDone = false) }
        nav.tab(Screen.Home)
    }

    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    // ---------- Аккаунт ----------

    fun register(byPhone: Boolean, login: String, password: String): AuthResult {
        val email = if (!byPhone) {
            when (val check = checkEmail(login)) {
                is EmailCheck.Ok -> check.email
                is EmailCheck.Error -> return AuthResult.Error(check.message)
            }
        } else {
            null
        }
        val phone = if (byPhone) normalizePhone(login) ?: return AuthResult.Error("Телефон в формате +375 29 123-45-67") else null
        if (password.length < 6) return AuthResult.Error("Пароль должен быть не короче 6 символов")
        if (email != null && state.users.any { it.email == email }) return AuthResult.Error("Этот email уже зарегистрирован — войдите")
        if (phone != null && state.users.any { it.phone == phone }) return AuthResult.Error("Этот телефон уже зарегистрирован — войдите")

        val salt = newToken()
        val user = User(
            id = (state.users.maxOfOrNull { it.id } ?: 0) + 1,
            email = email, phone = phone,
            passwordHash = hash(password, salt), salt = salt, createdAt = now(),
        )
        update { it.copy(users = it.users + user, currentUserId = user.id) }
        return AuthResult.Ok(askEmail = email == null)
    }

    fun login(login: String, password: String): AuthResult {
        val email = normalizeEmail(login)
        val phone = normalizePhone(login)
        if (email == null && phone == null) {
            return AuthResult.Error("Введите email (name@gmail.com) или телефон (+375 29 123-45-67)")
        }
        val user = state.users.firstOrNull { (email != null && it.email == email) || (phone != null && it.phone == phone) }
        // По просьбе команды прямо говорим, что аккаунта нет. В продакшене так подсказывают,
        // какие адреса зарегистрированы, — там лучше вернуть общий ответ.
        if (user == null) {
            val what = if (phone != null) "номером" else "email"
            return AuthResult.Error("Пользователь с таким $what не зарегистрирован", notRegistered = true)
        }
        if (user.passwordHash != hash(password, user.salt)) return AuthResult.Error("Неверный пароль")
        update { it.copy(currentUserId = user.id) }
        return AuthResult.Ok(askEmail = false)
    }

    /** Привязать email к аккаунту по телефону. Возвращает текст ошибки или null. */
    fun linkEmail(raw: String): String? {
        val user = state.currentUser ?: return "Сначала войдите в аккаунт"
        val email = when (val check = checkEmail(raw)) {
            is EmailCheck.Ok -> check.email
            is EmailCheck.Error -> return check.message
        }
        if (state.users.any { it.email == email && it.id != user.id }) return "Этот email уже занят"
        update { s -> s.copy(users = s.users.map { if (it.id == user.id) it.copy(email = email) else it }) }
        return null
    }

    fun logout() = update { it.copy(currentUserId = null, managerJoined = false) }

    // ---------- Корзина ----------

    fun addToCart(item: CartItem) = update { it.copy(cart = it.cart + item, cartToken = newToken()) }

    fun changeCopies(id: String, delta: Int) = update { s ->
        s.copy(
            cart = s.cart.map { if (it.id == id) it.copy(copies = (it.copies + delta).coerceIn(1, 99)) else it },
            cartToken = newToken(),
        )
    }

    fun removeFromCart(id: String) = update { s -> s.copy(cart = s.cart.filterNot { it.id == id }, cartToken = newToken()) }

    fun clearCart() = update { it.copy(cart = emptyList(), promo = null, cartToken = newToken()) }

    fun applyPromo(code: String): Boolean {
        if (code.trim().uppercase() != PROMO_CODE) return false
        update { it.copy(promo = PROMO_CODE) }
        return true
    }

    fun removePromo() = update { it.copy(promo = null) }

    // ---------- Черновик проекта (конструктор) ----------

    /** Открыть товар: если черновик для него уже есть — продолжаем, иначе создаём новый. */
    fun openProduct(productId: String) {
        if (state.draft?.productId == productId) return
        val product = Catalog.byId(productId)
        update {
            it.copy(
                draft = CartItem(
                    id = newToken(),
                    productId = productId,
                    // По умолчанию — первый формат с ценой с сайта, а не ориентировочный.
                    formatId = (product.formats.firstOrNull { !it.approximate } ?: product.formats.first()).id,
                    pages = product.basePages,
                    finish = if (product.finishTitle != null) product.finishOptions.first() else null,
                    copies = 1,
                    photos = emptyList(),
                    layoutId = if (product.kind == ProductKind.COLLAGE) Layouts.default(4).id else "",
                    bookPages = if (product.kind == ProductKind.BOOK) Books.autoPages(emptyList(), product.basePages) else emptyList(),
                ),
            )
        }
    }

    /** Альбом: «Собрать самому». */
    fun chooseCustomAlbum() = updateDraft { it.copy(albumMode = AlbumMode.CUSTOM, preset = "", layoutEdited = false) }

    /** Альбом: готовый макет — фиксированное число страниц с заранее заданными раскладками. */
    fun applyPreset(id: String) {
        val preset = Albums.byId(id) ?: return
        updateDraft {
            it.copy(
                albumMode = AlbumMode.PRESET, preset = id, pages = preset.pages, template = preset.template,
                coverTitle = it.coverTitle.ifBlank { preset.coverTitle }, bookPages = preset.buildPages(), layoutEdited = true,
            )
        }
    }

    /** Альбом: вернуться к выбору способа сборки. Фото остаются. */
    fun resetAlbumMode() = updateDraft { it.copy(albumMode = null, preset = "", layoutEdited = false) }

    fun updateDraft(change: (CartItem) -> CartItem) = update { s -> s.copy(draft = s.draft?.let { syncPages(change(it)) }) }

    /** Новый список фото: позиции в свободном коллаже переезжают вместе с фото. */
    fun setDraftPhotos(photos: List<String>) = updateDraft { d ->
        val byUri = d.photos.zip(d.placements).toMap()
        d.copy(
            photos = photos,
            placements = if (d.freeLayout) photos.mapIndexed { i, u -> byUri[u] ?: Placement.default(i) } else d.placements,
        )
    }

    /** Пока страницы не правили руками — раскладываем заново; после правок только подгоняем число страниц. */
    private fun syncPages(item: CartItem): CartItem {
        if (Catalog.byId(item.productId).kind != ProductKind.BOOK) return item
        // Готовый макет: раскладки страниц не трогаем, только заполняем пустые ячейки новыми фото.
        if (item.albumMode == AlbumMode.PRESET) return item.copy(bookPages = Books.fillSlots(Books.resize(item.bookPages, item.pages), item.photos))
        if (!item.layoutEdited) return item.copy(bookPages = Books.autoPages(item.photos, item.pages))
        val photos = item.photos.toSet()
        return item.copy(
            bookPages = Books.resize(item.bookPages, item.pages).map { page ->
                if (page.free) {
                    // В свободном режиме убираем фото вместе с его позицией.
                    page.photos.filterNot { it in photos }.fold(page) { p, uri -> p.withoutPhoto(uri) }
                } else {
                    page.copy(photos = page.slots.map { if (it in photos) it else "" })
                }
            },
        )
    }

    /** Проверить черновик и положить в корзину. Возвращает текст ошибки или null. */
    fun addDraftToCart(): String? {
        val d = state.draft ?: return "Проект не найден"
        val product = Catalog.byId(d.productId)
        if (product.id == "album" && d.albumMode == null) return "Выберите: собрать самому или готовый макет"
        if (d.photos.isEmpty()) return "Добавьте хотя бы одно фото"
        if (product.kind == ProductKind.COLLAGE && !d.freeLayout && d.photos.size < d.layout) {
            return "Заполните все ячейки коллажа: нужно ${d.layout} фото, добавлено ${d.photos.size}"
        }
        update { it.copy(cart = it.cart + d.copy(id = newToken()), draft = null, cartToken = newToken()) }
        return null
    }

    /** «ИИ» собирает макет. Сейчас это имитация с задержкой, см. AiDesigner. */
    fun aiCompose(onDone: () -> Unit = {}) {
        if (state.draft?.photos.isNullOrEmpty()) return
        showLoader("ИИ подбирает шаблон и раскладку", "демо-режим", 2600) {
            updateDraft { AiDesigner.compose(it) }
            onDone()
        }
    }

    // ---------- Заказы ----------

    fun orderById(id: Long): Order? = state.orders.firstOrNull { it.id == id }

    /**
     * Создаёт заказ из корзины. Повторный вызов с той же корзиной вернёт тот же заказ —
     * двойное нажатие «Оформить» не создаст дубликат.
     */
    /** Бонусный баланс текущего пользователя (0, если не вошёл). */
    fun bonusBalance(): Long = state.currentUser?.let { Loyalty.balance(state, it.id) } ?: 0

    fun loyaltyLevel(): LoyaltyLevel = state.currentUser?.let { Loyalty.level(Loyalty.paidSum(state, it.id)) } ?: LoyaltyLevel.START

    fun createOrder(recipient: String, delivery: Delivery, address: String, useBonus: Boolean = false): Order? {
        val user = state.currentUser ?: return null
        state.orders.firstOrNull { it.checkoutToken == state.cartToken }?.let { return it }
        if (state.cart.isEmpty()) return null

        val itemsTotal = Pricing.itemsTotal(state.cart)
        val discount = Pricing.discount(itemsTotal, state.promo)
        val deliveryCost = Pricing.deliveryCost(delivery, itemsTotal - discount)
        val bonusSpent = if (useBonus) Loyalty.maxSpend(Loyalty.balance(state, user.id), itemsTotal - discount) else 0
        val id = (state.orders.maxOfOrNull { it.id } ?: 0) + 1
        val createdAt = now()
        val order = Order(
            id = id,
            number = "PP-%06d".format(104200 + id),
            userId = user.id,
            lines = state.cart.map { item ->
                OrderLine(
                    title = Catalog.byId(item.productId).title,
                    details = Pricing.details(item) + if (item.copies > 1) " × ${item.copies}" else "",
                    photos = item.photos.size,
                    preview = item.photos.firstOrNull(),
                    amount = Pricing.lineTotal(item),
                )
            },
            delivery = delivery,
            address = address.trim(),
            recipient = recipient.trim(),
            itemsTotal = itemsTotal,
            discount = discount,
            deliveryCost = deliveryCost,
            total = itemsTotal - discount - bonusSpent + deliveryCost,
            bonusSpent = bonusSpent,
            status = OrderStatus.AWAITING_PAYMENT,
            history = mapOf(OrderStatus.AWAITING_PAYMENT to createdAt),
            trackNumber = null,
            paymentError = null,
            checkoutToken = state.cartToken,
            createdAt = createdAt,
        )
        update { it.copy(orders = listOf(order) + it.orders, cart = emptyList(), promo = null) }
        return order
    }

    /**
     * Тестовая оплата. Уже оплаченный заказ повторно не «списывается».
     * После успеха заказ сразу упаковывается для производства — раньше оплаты он туда не попадает.
     */
    fun pay(orderId: Long, approve: Boolean, onResult: (Boolean) -> Unit) {
        val order = orderById(orderId) ?: return
        if (order.status != OrderStatus.AWAITING_PAYMENT) {
            onResult(true)
            return
        }
        showLoader("Проводим оплату", "тестовый режим", 1800) {
            if (approve) {
                val t = now()
                // Процент — по уровню, который был до этой оплаты; бонусы начисляем с денег за товары, без доставки.
                val level = Loyalty.level(Loyalty.paidSum(state, order.userId))
                val earned = Loyalty.earned(level, order.total - order.deliveryCost)
                updateOrder(orderId) {
                    it.copy(
                        bonusEarned = earned,
                        status = OrderStatus.PACKED,
                        history = it.history + mapOf(OrderStatus.PAID to t, OrderStatus.PACKED to t + 1000),
                        paymentError = null,
                    )
                }
            } else {
                updateOrder(orderId) {
                    it.copy(paymentError = "Банк отклонил оплату (демо). Деньги не списаны, заказ сохранён — попробуйте ещё раз.")
                }
            }
            onResult(approve)
        }
    }

    /** Демо: продвинуть заказ на следующий этап (в жизни это делает сканер штрихкода на производстве). */
    fun advanceOrder(orderId: Long) {
        val order = orderById(orderId) ?: return
        if (order.status == OrderStatus.AWAITING_PAYMENT || order.status == OrderStatus.DELIVERED) return
        val next = OrderStatus.entries[order.status.ordinal + 1]
        updateOrder(orderId) {
            it.copy(
                status = next,
                history = it.history + (next to now()),
                trackNumber = it.trackNumber ?: if (next == OrderStatus.SHIPPED) trackNumber() else null,
            )
        }
    }

    private fun updateOrder(id: Long, change: (Order) -> Order) = update { s ->
        s.copy(orders = s.orders.map { if (it.id == id) change(it) else it })
    }

    private fun trackNumber() = "PP" + (1..9).joinToString("") { Random.nextInt(10).toString() } + "BY"

    // ---------- Чат ----------

    fun ensureGreeting() {
        if (state.chat.isNotEmpty()) return
        addMessage(
            ChatAuthor.ASSISTANT,
            "Привет! Я ИИ-помощник PicPac. Подскажу про сроки, доставку, оплату и статус заказа. " +
                "Если не справлюсь — позову менеджера maryrose.",
        )
    }

    fun sendChat(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        addMessage(ChatAuthor.ME, clean)
        typing = true
        viewModelScope.launch {
            delay(900)
            typing = false
            if (state.managerJoined) {
                addMessage(ChatAuthor.MANAGER, managerReplies[Random.nextInt(managerReplies.size)])
            } else {
                addMessage(ChatAuthor.ASSISTANT, assistantReply(clean))
            }
        }
    }

    /** Заглушка: позже здесь будет реальная передача диалога живому менеджеру. */
    fun callManager() {
        if (state.managerJoined) return
        addMessage(ChatAuthor.SYSTEM, "Подключаем менеджера…")
        typing = true
        viewModelScope.launch {
            delay(1500)
            typing = false
            update { it.copy(managerJoined = true) }
            addMessage(ChatAuthor.SYSTEM, "maryrose подключилась к чату")
            addMessage(ChatAuthor.MANAGER, "Здравствуйте! Я maryrose, менеджер PicPac. Опишите, пожалуйста, вопрос — помогу.")
        }
    }

    private fun addMessage(author: ChatAuthor, text: String) = update { s ->
        s.copy(chat = s.chat + ChatMessage((s.chat.maxOfOrNull { it.id } ?: 0) + 1, author, text, now()))
    }

    private val managerReplies = listOf(
        "Спасибо, вижу ваш вопрос. Уточню у производства и вернусь с ответом в течение 15 минут.",
        "Поняла вас! Сейчас проверю заказ и напишу.",
        "Хороший вопрос 🙂 Передам коллегам и отвечу здесь же.",
    )

    private fun assistantReply(question: String): String {
        val q = question.lowercase()
        val lastOrder = state.currentUser?.let { u -> state.orders.firstOrNull { it.userId == u.id } }
        return when {
            listOf("где", "статус", "заказ").any { it in q } && lastOrder != null ->
                "Ваш заказ ${lastOrder.number}: «${lastOrder.status.title}». ${lastOrder.status.description}." +
                    (lastOrder.trackNumber?.let { " Трек-номер: $it." } ?: "")
            listOf("срок", "когда", "дней", "быстро").any { it in q } ->
                "Печать занимает 2–3 дня, затем доставка: курьер EMS — 1–2 рабочих дня, Европочта — до 3 рабочих дней."
            "достав" in q ->
                "Доставляем Белпочтой, Европочтой и курьером EMS. Стоимость от 5,90 руб., Белпочтой бесплатно от 150 руб."
            "оплат" in q || "карт" in q ->
                "Оплата картой онлайн: VISA, Mastercard, БЕЛКАРТ, ЕРИП. Заказ уходит в печать только после оплаты."
            listOf("брак", "качеств", "испорт").any { it in q } ->
                "Если найдёте брак печати — перепечатаем бесплатно. Пришлите фото дефекта, и я передам менеджеру."
            listOf("цен", "стоим", "сколько стоит").any { it in q } ->
                "Фотопечать от 1,30 руб. за снимок 10×15, фотокниги — от 70 руб. Точную цену покажет конструктор."
            listOf("привет", "здравств", "добрый").any { it in q } ->
                "Здравствуйте! Чем помочь: сроки, доставка, оплата или статус заказа?"
            else ->
                "Пока не знаю точного ответа. Нажмите «Позвать менеджера» — maryrose подключится к чату."
        }
    }

    // ---------- Демо ----------

    fun resetDemo() {
        storage.clear()
        state = AppState(onboardingDone = true)
        nav.tab(Screen.Home)
    }

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
            .joinToString("") { "%02x".format(it) }
}
