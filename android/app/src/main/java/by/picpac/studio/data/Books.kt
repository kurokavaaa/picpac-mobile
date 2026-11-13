package by.picpac.studio.data

import kotlin.math.ceil
import kotlin.random.Random

/** Раскладка фото по страницам фотокниги. */
object Books {
    /** Автомакет: фото раскладываются по страницам поровну, не больше 4 на страницу. */
    fun autoPages(photos: List<String>, pageCount: Int): List<BookPage> =
        distribute(photos, pageCount) { remaining, pagesLeft -> ceil(remaining / pagesLeft.toDouble()).toInt() }

    /**
     * Общая раскладка: [howMany] решает, сколько фото положить на очередную страницу.
     * Пока фото хватает, каждая страница получает хотя бы одно.
     */
    fun distribute(photos: List<String>, pageCount: Int, howMany: (remaining: Int, pagesLeft: Int) -> Int): List<BookPage> {
        val pages = MutableList(pageCount) { BookPage() }
        var i = 0
        for (p in 0 until pageCount) {
            val remaining = photos.size - i
            if (remaining <= 0) break
            val pagesLeft = pageCount - p
            val take = if (remaining <= pagesLeft) 1 else howMany(remaining, pagesLeft).coerceIn(1, minOf(4, remaining))
            pages[p] = BookPage(layoutId = Layouts.default(take).id, photos = photos.subList(i, i + take))
            i += take
        }
        return pages
    }

    /** Подогнать число страниц, сохранив ручную раскладку. */
    fun resize(pages: List<BookPage>, count: Int): List<BookPage> =
        if (pages.size >= count) pages.take(count) else pages + List(count - pages.size) { BookPage() }

    fun emptyPages(pages: List<BookPage>) = pages.count { page -> page.placed.isEmpty() }

    /** Где ячейка макета лежит на странице, в долях листа. */
    fun slotRect(layoutId: String, index: Int): Placement =
        Layouts.byId(layoutId).cells.getOrElse(index) { Placement.default(index) }

    /**
     * Готовый макет: раскладки страниц уже заданы. Фото, которых больше нет в проекте, убираем,
     * а пустые ячейки по порядку страниц заполняем фото, которых ещё нет на страницах.
     */
    fun fillSlots(pages: List<BookPage>, photos: List<String>): List<BookPage> {
        val alive = photos.toSet()
        val cleaned = pages.map { p -> if (p.free) p else p.copy(photos = p.slots.map { if (it in alive) it else "" }) }
        val queue = ArrayDeque(unplaced(photos, cleaned))
        return cleaned.map { p ->
            if (p.free || queue.isEmpty()) p
            else p.copy(photos = p.slots.map { it.ifEmpty { queue.removeFirstOrNull() ?: "" } })
        }
    }

    /** Фото проекта, которые ещё не попали ни на одну страницу. */
    fun unplaced(photos: List<String>, pages: List<BookPage>): List<String> {
        val used = pages.flatMap { it.photos }.toSet()
        return photos.filter { it !in used }
    }
}

/**
 * Имитация ИИ-дизайнера для демо: подбирает шаблон, «дизайнерскую» раскладку, подписи и стикеры.
 * Настоящий ИИ потребует сервера с ключом API (ключ нельзя хранить в приложении).
 * Сюда потом подставится ответ бэкенда, а экраны останутся прежними.
 */
object AiDesigner {
    private val captions = listOf(
        "Лучшие моменты", "Вместе", "Наши истории", "Тот самый день", "Счастье в деталях",
        "Лето, которое запомним", "Тёплые дни", "Просто мы", "Улыбайся!", "Навсегда",
    )
    private val covers = listOf("Наши истории · 2026", "Лучший год", "Моменты, которые хочется сохранить", "Семейный альбом")
    private val stickers = listOf(StickerKind.HEART, StickerKind.SPARKLE, StickerKind.STAR, StickerKind.FLOWER, StickerKind.SUN)

    fun compose(item: CartItem): CartItem {
        // Один и тот же набор фото даёт один и тот же результат — повтор не «прыгает».
        val random = Random(item.photos.hashCode())
        val template = Template.entries[random.nextInt(Template.entries.size)]
        val accent = template.accent
        val product = Catalog.byId(item.productId)

        return when (product.kind) {
            ProductKind.BOOK -> {
                // Ритм страниц: крупное фото, затем страницы с 2–4 снимками.
                val rhythm = listOf(1, 3, 2, 4, 2, 1, 3, 4)
                var page = 0
                val pages = Books.distribute(item.photos, item.pages) { remaining, pagesLeft ->
                    val wanted = rhythm[page++ % rhythm.size]
                    // Не даём кончиться фото раньше страниц.
                    minOf(wanted, remaining - (pagesLeft - 1))
                }.map { p ->
                    // Для разнообразия — случайный макет из библиотеки с тем же числом фото.
                    val options = Layouts.byCount(p.layout)
                    p.copy(layoutId = options[random.nextInt(options.size)].id)
                }.mapIndexed { index, p ->
                    val decor = buildList {
                        if (index % 3 == 0 && p.slots.any { it.isNotEmpty() }) {
                            add(text(captions[random.nextInt(captions.size)], accent, 0.5f, 0.94f, 0.06f))
                        }
                        if (index % 4 == 1) add(sticker(stickers[random.nextInt(stickers.size)], 0.9f, 0.08f, 0.12f))
                    }
                    p.copy(decorations = decor)
                }
                item.copy(
                    template = template,
                    bookPages = pages,
                    layoutEdited = true,
                    coverTitle = covers[random.nextInt(covers.size)],
                )
            }
            ProductKind.COLLAGE -> {
                val n = item.photos.size.coerceIn(1, Layouts.MAX_PHOTOS)
                val options = Layouts.byCount(n)
                val grid = options[random.nextInt(options.size)]
                item.copy(
                    template = template,
                    layoutId = if (item.freeLayout) item.layoutId else grid.id,
                    photos = if (item.freeLayout) item.photos else item.photos.take(n),
                    placements = if (item.freeLayout) neatPlacements(item.photos.size) else item.placements,
                    decorations = listOf(
                        text(captions[random.nextInt(captions.size)], accent, 0.5f, 0.95f, 0.055f),
                        sticker(stickers[random.nextInt(stickers.size)], 0.92f, 0.06f, 0.12f),
                    ),
                )
            }
            ProductKind.PRINT, ProductKind.DOCS -> item
        }
    }

    /** Аккуратная сетка с лёгким сдвигом для свободного коллажа. */
    private fun neatPlacements(count: Int): List<Placement> {
        val cols = if (count <= 2) 1 else 2
        val rows = ceil(count / cols.toDouble()).toInt().coerceAtLeast(1)
        return List(count) { i ->
            val c = i % cols
            val r = i / cols
            Placement(
                x = (c + 0.5f) / cols + if (r % 2 == 0) -0.02f else 0.02f,
                y = (r + 0.5f) / rows * 0.9f,
                w = 0.9f / cols,
                h = 0.82f / rows,
            )
        }
    }

    private fun text(value: String, color: Long, x: Float, y: Float, size: Float) =
        Decoration(newToken(), DecorType.TEXT, text = value, color = color, x = x, y = y, size = size)

    private fun sticker(kind: StickerKind, x: Float, y: Float, size: Float) =
        Decoration(newToken(), DecorType.STICKER, sticker = kind, x = x, y = y, size = size)
}

/**
 * Готовый макет альбома: число страниц фиксировано, раскладка каждой страницы задана заранее.
 * Таких альбомов на super.picpac.by нет — темы и раскладки придуманы для прототипа.
 */
data class AlbumPreset(
    val id: String,
    val title: String,
    val description: String,
    val template: Template,
    val pages: Int,
    /** Раскладки страниц по кругу: страница i получает pattern[i % size]. */
    val pattern: List<String>,
    val coverTitle: String,
) {
    val photoSlots: Int get() = List(pages) { Layouts.byId(pattern[it % pattern.size]).count }.sum()

    fun buildPages(): List<BookPage> = List(pages) { i -> BookPage(layoutId = pattern[i % pattern.size]) }
}

object Albums {
    val presets = listOf(
        AlbumPreset(
            "travel", "Путешествие", "Крупные пейзажи и ленты из 3–4 кадров",
            Template.TRAVEL, 24, listOf("1-full", "3-cols", "4-big-top", "2-cols", "5-two-three", "1-landscape", "4-mix-a", "3-big-left"),
            "Наше путешествие",
        ),
        AlbumPreset(
            "family", "Семейный альбом", "Спокойные сетки с белыми полями",
            Template.FAMILY, 24, listOf("1-margin", "4-grid-margin", "2-rows", "6-margin-3x2", "3-one-two", "4-landscape"),
            "Семейный альбом",
        ),
        AlbumPreset(
            "wedding", "Свадьба", "Много воздуха, одно-два фото на странице",
            Template.WEDDING, 30, listOf("1-square-big", "2-squares", "1-portrait", "3-big-right", "1-landscape-small", "2-steps-down"),
            "Наша свадьба",
        ),
        AlbumPreset(
            "baby", "Первый год малыша", "Мозаики из множества маленьких кадров",
            Template.KIDS, 24, listOf("1-margin", "7-mosaic", "4-grid", "6-big-left", "5-scatter", "3-stairs-down"),
            "Мой первый год",
        ),
    )

    fun byId(id: String): AlbumPreset? = presets.firstOrNull { it.id == id }
}
