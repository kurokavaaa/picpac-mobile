package by.picpac.studio.data

enum class ProductKind { PRINT, BOOK, COLLAGE, DOCS }

/**
 * Вид печати — как на странице «Печать фото» на super.picpac.by.
 * Цифровая дешевле; во сколько раз, сайт не пишет. Берём «от 0,61 за фото» с главной против 1,30 за 10×15 —
 * это ~47%. Поэтому цена цифровой печати ориентировочная.
 */
enum class PrintType(val title: String, val note: String, val percent: Int, val approximate: Boolean) {
    PHOTO("Фотохимическая", "на фотобумаге, как в фотоателье", 100, false),
    DIGITAL("Цифровая", "дешевле, цвета проще", 47, true),
}

/** Как собирают альбом: сами или из готового макета. null — ещё не выбрали. */
enum class AlbumMode { CUSTOM, PRESET }

/** Сколько фото можно разложить на листе в свободном режиме. */
const val FREE_COLLAGE_MAX = 8

/**
 * Где лежит фото на листе свободного коллажа. Всё в долях листа (0..1),
 * поэтому раскладка не зависит от размера экрана и формата печати.
 * x, y — центр фото; w, h — ширина и высота.
 */
data class Placement(val x: Float, val y: Float, val w: Float, val h: Float) {
    companion object {
        /** Новые фото раскладываем «лесенкой», чтобы они не легли ровно друг на друга. */
        fun default(index: Int) = Placement(
            x = 0.32f + 0.12f * (index % 4),
            y = 0.28f + 0.14f * (index % 4) + 0.04f * (index / 4),
            w = 0.5f,
            h = 0.36f,
        )
    }
}

/** Шрифт оформления: переводится в настоящий шрифт уже в интерфейсе. */
enum class TemplateFont { SANS, SERIF, CURSIVE }

/**
 * Шрифты для надписей на страницах (файлы в res/font, все с кириллицей и открытой лицензией OFL).
 * Из брендбука PicPac в открытом доступе только Comfortaa. Для Wes, Mak, Marianna и Kosko
 * файлов и лицензии у нас нет — вместо них похожие открытые шрифты; note подсказывает, на что похоже.
 */
enum class TextFont(val title: String, val note: String) {
    COMFORTAA("Comfortaa", "брендбук"),
    CORMORANT("Cormorant", "как Wes"),
    FORUM("Forum", "как Mak"),
    MARCK_SCRIPT("Marck Script", "как Marianna"),
    AMATIC("Amatic SC", "как Kosko"),
    MONTSERRAT("Montserrat", "строгий"),
    CAVEAT("Caveat", "от руки"),
    LOBSTER("Lobster", "праздник"),
}

/**
 * Шаблон оформления фотокниги или коллажа: фон страниц, рамки у фото, акцентный цвет и шрифт подписей.
 * Цвета — числа ARGB, чтобы слой данных не зависел от Compose.
 */
enum class Template(
    val title: String,
    val background: Long,
    val frame: Long,
    val accent: Long,
    /** Толщина рамки вокруг фото в долях ширины страницы. */
    val frameWidth: Float,
    val rounded: Boolean,
    val font: TemplateFont,
) {
    // Темы дизайнов — как в разделе «Выбрать дизайн» на super.picpac.by, в том же порядке.
    // Имена констант старых пяти не меняем, чтобы сохранённые проекты открывались.
    FAMILY("Семья", 0xFFF7EFE6, 0xFFFFFFFF, 0xFF8C5A3C, 0.025f, false, TemplateFont.SERIF),
    BIRTHDAY("День рождения", 0xFFFFE9EC, 0xFFF6B8C4, 0xFFDD5757, 0.022f, true, TemplateFont.CURSIVE),
    KIDS("Дети", 0xFFFFF4C9, 0xFF9DD3F0, 0xFFE2774E, 0.028f, true, TemplateFont.CURSIVE),
    GRADUATION("Выпускной", 0xFFEEF1F6, 0xFF2F3E5C, 0xFF2F3E5C, 0.012f, false, TemplateFont.SERIF),
    WEDDING("Свадьба", 0xFFFBF7F0, 0xFFD8B26E, 0xFF8A6A3B, 0.01f, false, TemplateFont.SERIF),
    NEW_YEAR("Новый год", 0xFF1F3B32, 0xFFD8B26E, 0xFFD8B26E, 0.014f, false, TemplateFont.SERIF),
    LOVE_STORY("Love story", 0xFFFBEFF1, 0xFFFFFFFF, 0xFFC2546B, 0.03f, false, TemplateFont.CURSIVE),
    HOLIDAY("Праздники", 0xFFFBE3D6, 0xFFC25E39, 0xFF983F22, 0.018f, true, TemplateFont.CURSIVE),
    TRAVEL("Путешествия", 0xFFF3E9D8, 0xFFFFFFFF, 0xFF52645D, 0.03f, false, TemplateFont.SANS),
    BAPTISM("Крещение", 0xFFF4F7FB, 0xFFDCE6F2, 0xFF7A93B8, 0.014f, true, TemplateFont.SERIF),
    CREATIVE("Для творчества", 0xFFFFF8E8, 0xFFFFD166, 0xFF568CDE, 0.026f, true, TemplateFont.CURSIVE),
    INSTABOOK("Инстабук", 0xFFFFFFFF, 0xFFFFFFFF, 0xFF292316, 0.008f, false, TemplateFont.SANS),
    PORTFOLIO("Портфолио", 0xFF1E1E1E, 0xFF1E1E1E, 0xFFFFFFFF, 0.004f, false, TemplateFont.SANS),
    MINIMAL("Универсальные", 0xFFFFFFFF, 0xFFE6E1D9, 0xFF292316, 0.006f, false, TemplateFont.SANS),
    PETS("Домашние любимцы", 0xFFEFF6EC, 0xFFFFFFFF, 0xFF6B8F4E, 0.028f, true, TemplateFont.CURSIVE),
}

enum class DecorType { TEXT, STICKER }

/** Стикеры рисуются кодом (без картинок), поэтому их легко масштабировать и перекрашивать. */
enum class StickerKind(val title: String) {
    HEART("Сердце"), STAR("Звезда"), SPARKLE("Блеск"), SUN("Солнце"), FLOWER("Цветок"),
    CLOUD("Облако"), CAMERA("Камера"), CAT("Котик"), LOVE("Love"),
}

/**
 * Текст или стикер поверх страницы. Координаты и размер — в долях листа, как у фото в свободном коллаже.
 * size: для стикера — его ширина, для текста — высота букв (обе в долях ширины листа).
 */
data class Decoration(
    val id: String,
    val type: DecorType,
    val text: String = "",
    val sticker: StickerKind = StickerKind.HEART,
    val color: Long = 0xFF292316,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = 0.2f,
    /** Шрифт надписи. null — шрифт шаблона оформления. */
    val font: TextFont? = null,
)

/** Сколько фото можно положить на свободную страницу книги. */
const val FREE_PAGE_MAX = 6

/**
 * Страница фотокниги. Два режима:
 * - сетка (free = false): макет из библиотеки [Layouts], фото по ячейкам ("" — пустая ячейка);
 * - свободно (free = true): фото лежат где угодно, их позиции — в [placements] по порядку фото.
 * Поверх — украшения (тексты и стикеры).
 */
data class BookPage(
    val layoutId: String = Layouts.default(1).id,
    val photos: List<String> = emptyList(),
    val decorations: List<Decoration> = emptyList(),
    val free: Boolean = false,
    val placements: List<Placement> = emptyList(),
) {
    /** Сколько ячеек в макете страницы. */
    val layout: Int get() = Layouts.byId(layoutId).count

    /** Ячейки ровно по раскладке: лишние фото отрезаются, недостающие — пустые. */
    val slots: List<String> get() = List(layout) { photos.getOrNull(it) ?: "" }

    /** Фото, которые реально стоят на странице (без пустых ячеек). */
    val placed: List<String> get() = if (free) photos else slots.filter { it.isNotEmpty() }

    /** Другой макет. Из свободного режима фото переезжают в ячейки по порядку. */
    fun withLayout(id: String): BookPage {
        val n = Layouts.byId(id).count
        val current = if (free) photos else slots.filter { it.isNotEmpty() } + slots.filter { it.isEmpty() }
        return copy(layoutId = id, free = false, placements = emptyList(), photos = List(n) { current.getOrNull(it) ?: "" })
    }

    fun withSlot(index: Int, uri: String) = copy(photos = slots.toMutableList().also { it[index] = uri })

    /** Перетаскивание в сетке: два фото меняются ячейками. */
    fun swapSlots(a: Int, b: Int): BookPage {
        val s = slots.toMutableList()
        if (a !in s.indices || b !in s.indices || a == b) return this
        s[a] = s[b].also { s[b] = s[a] }
        return copy(photos = s)
    }

    /** Перейти в свободный режим так, чтобы фото остались ровно там, где стояли в сетке. */
    fun toFree(): BookPage {
        if (free) return this
        val filled = slots.withIndex().filter { it.value.isNotEmpty() }
        return copy(free = true, photos = filled.map { it.value }, placements = filled.map { Books.slotRect(layoutId, it.index) })
    }

    fun addFree(uri: String): BookPage =
        if (uri in photos || photos.size >= FREE_PAGE_MAX) this
        else copy(photos = photos + uri, placements = fixedPlacements() + Placement.default(photos.size))

    fun withPlacement(uri: String, p: Placement): BookPage {
        val i = photos.indexOf(uri)
        return if (i < 0) this else copy(placements = fixedPlacements().toMutableList().also { it[i] = p })
    }

    /** Последнее фото рисуется поверх остальных — выбранное переносим в конец вместе с его позицией. */
    fun bringToFront(uri: String): BookPage {
        val i = photos.indexOf(uri)
        if (i < 0 || i == photos.lastIndex) return this
        val pl = fixedPlacements()
        return copy(photos = photos - uri + uri, placements = pl.filterIndexed { j, _ -> j != i } + pl[i])
    }

    fun withoutPhoto(uri: String): BookPage {
        val i = photos.indexOf(uri)
        if (i < 0) return this
        return copy(photos = photos.filterIndexed { j, _ -> j != i }, placements = fixedPlacements().filterIndexed { j, _ -> j != i })
    }

    /** Позиции ровно по числу фото: если какой-то не хватает, ставим «лесенкой». */
    private fun fixedPlacements() = photos.indices.map { placements.getOrNull(it) ?: Placement.default(it) }
}

enum class ThemeMode(val title: String) {
    SYSTEM("Как в системе"),
    LIGHT("Светлая"),
    DARK("Тёмная"),
}

enum class Category(val title: String) { PHOTO("Фото"), ALBUMS("Альбомы"), BOOKS("Фотокниги") }

/**
 * Формат товара. Для книг цена формата = базовая × percent/100.
 * approximate = true: на сайте цены этого формата нет, процент условный (помечаем в интерфейсе).
 */
data class FormatOption(
    val id: String,
    val title: String,
    val percent: Int = 100,
    val approximate: Boolean = false,
    val printPrice: Long = 0,
    /** Для фото на документы: подпись под названием (размер в мм, требования). */
    val note: String = "",
    /** Для фото на документы: размер одного фото в мм. */
    val widthMm: Int = 0,
    val heightMm: Int = 0,
)

data class Product(
    val id: String,
    val kind: ProductKind,
    val category: Category,
    val title: String,
    val short: String,
    val description: String,
    val art: String,
    val formats: List<FormatOption>,
    val basePrice: Long = 0,
    val basePages: Int = 0,
    val pageStepPrice: Long = 0,
    val maxPages: Int = 0,
    /** Подпись выбора «глянец/мат»: «Бумага», «Обложка» или null, если выбора нет. */
    val finishTitle: String? = null,
    val photosHint: String,
    /** Варианты покрытия (обложки/бумаги), как на сайте. */
    val finishOptions: List<String> = listOf("Глянцевая", "Матовая"),
    /** Характеристики из таблицы сравнения фотокниг на super.picpac.by. */
    val specs: List<Pair<String, String>> = emptyList(),
    /** Сколько фото можно положить в книгу — как написано на super.picpac.by. 0 — без ограничений. */
    val minPhotos: Int = 0,
    val maxPhotos: Int = 0,
    /** Товара нет на сайте — вся цена ориентировочная, priceNote объясняет, откуда она. */
    val priceNote: String? = null,
)

data class CartItem(
    val id: String,
    val productId: String,
    val formatId: String,
    val pages: Int,
    val finish: String?,
    val copies: Int,
    val photos: List<String>,
    /** Для коллажа: id макета из [Layouts]. Для остальных товаров пусто. */
    val layoutId: String = "",
    /** Коллаж в свободном режиме: фото разложены вручную, позиции — в [placements] (по порядку фото). */
    val freeLayout: Boolean = false,
    val placements: List<Placement> = emptyList(),
    /** Оформление (для фотокниг и коллажей). */
    val template: Template = Template.MINIMAL,
    /** «Без рамки»: фото без рамки шаблона, встык к своей ячейке. */
    val noFrame: Boolean = false,
    /** Страницы фотокниги. Пока их не правили руками, раскладываются автоматически. */
    val bookPages: List<BookPage> = emptyList(),
    val layoutEdited: Boolean = false,
    val coverTitle: String = "",
    /** Тексты и стикеры на листе коллажа. */
    val decorations: List<Decoration> = emptyList(),
    /** Вид печати для фотопечати и коллажа. */
    val printType: PrintType = PrintType.PHOTO,
    /** Только для альбома: способ сборки и выбранный готовый макет. */
    val albumMode: AlbumMode? = null,
    val preset: String = "",
) {
    /** Для коллажа: сколько ячеек в макете. */
    val layout: Int get() = if (layoutId.isEmpty()) 0 else Layouts.byId(layoutId).count
}

data class User(
    val id: Long,
    val email: String?,
    val phone: String?,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long,
) {
    /** Уведомления — на email, если он есть, иначе SMS на телефон. */
    val notifyChannel: String
        get() = if (email != null) "email $email" else "SMS на $phone"
}

enum class Delivery(val title: String, val price: Long, val freeFrom: Long?, val eta: String) {
    BELPOST("Белпочта", 590, 15000, "в отделение почты"),
    EUROPOST("Европочта", 590, null, "до 3 рабочих дней"),
    COURIER("Курьер EMS", 590, null, "1–2 рабочих дня, до двери"),
}

/** Этапы заказа как у маркетплейсов. Производство начинается только с PAID. */
enum class OrderStatus(val title: String, val description: String) {
    AWAITING_PAYMENT("Ожидает оплаты", "Заказ создан, после оплаты он уйдёт в производство"),
    PAID("Оплачен", "Оплата прошла, заказ принят"),
    PACKED("Файлы упакованы", "Фото собраны в пакет для печати, на каждом листе свой штрихкод"),
    PRINTING("Печатается", "Печатаем и ламинируем страницы"),
    QUALITY("Контроль качества", "Каждый лист проверяет человек и печатная машина"),
    SHIPPED("Передан в доставку", "Посылку забрала служба доставки"),
    IN_TRANSIT("В пути", "Посылка едет к вам"),
    PICKUP("Ожидает получения", "Посылка в пункте выдачи или у курьера"),
    DELIVERED("Получен", "Спасибо, что выбрали PicPac!"),
}

data class OrderLine(
    val title: String,
    val details: String,
    val photos: Int,
    val preview: String?,
    val amount: Long,
)

data class Order(
    val id: Long,
    val number: String,
    val userId: Long,
    val lines: List<OrderLine>,
    val delivery: Delivery,
    val address: String,
    val recipient: String,
    val itemsTotal: Long,
    val discount: Long,
    val deliveryCost: Long,
    val total: Long,
    val status: OrderStatus,
    /** Когда заказ дошёл до каждого этапа (мс). */
    val history: Map<OrderStatus, Long>,
    val trackNumber: String?,
    val paymentError: String?,
    /** Ключ оформления: повторное нажатие «оформить» вернёт этот же заказ, а не создаст второй. */
    val checkoutToken: String,
    val createdAt: Long,
    /** Бонусы, списанные на этот заказ, и начисленные после его оплаты (в копейках). */
    val bonusSpent: Long = 0,
    val bonusEarned: Long = 0,
)

enum class ChatAuthor { ME, ASSISTANT, MANAGER, SYSTEM }

data class ChatMessage(val id: Long, val author: ChatAuthor, val text: String, val time: Long)

data class AppState(
    val onboardingDone: Boolean = false,
    /** Обучение с подсветкой кнопок пройдено или пропущено. */
    val tourDone: Boolean = false,
    /** Показ жеста перетаскивания фото в книге уже был. */
    val dragHintDone: Boolean = false,
    val users: List<User> = emptyList(),
    val currentUserId: Long? = null,
    val cart: List<CartItem> = emptyList(),
    val cartToken: String = newToken(),
    val promo: String? = null,
    val orders: List<Order> = emptyList(),
    val chat: List<ChatMessage> = emptyList(),
    val managerJoined: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Черновик проекта, который сейчас собирают в конструкторе (сохраняется между запусками). */
    val draft: CartItem? = null,
) {
    val currentUser: User? get() = users.firstOrNull { it.id == currentUserId }
}

fun newToken(): String = java.util.UUID.randomUUID().toString()
